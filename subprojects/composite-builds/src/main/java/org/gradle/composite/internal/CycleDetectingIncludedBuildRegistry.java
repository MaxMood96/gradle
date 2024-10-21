/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.composite.internal;

import org.gradle.api.GradleException;
import org.gradle.api.internal.BuildDefinition;
import org.gradle.internal.build.BuildState;
import org.gradle.internal.build.IncludedBuildFactory;
import org.gradle.internal.build.IncludedBuildState;
import org.gradle.internal.event.ListenerManager;

import java.util.Optional;

public class CycleDetectingIncludedBuildRegistry extends DefaultIncludedBuildRegistry {

    private final DynamicGraphCycleDetector<BuildState> cycleDetector = new DynamicGraphCycleDetector<>();

    public CycleDetectingIncludedBuildRegistry(
        IncludedBuildFactory includedBuildFactory,
        ListenerManager listenerManager,
        BuildStateFactory buildStateFactory
    ) {
        super(includedBuildFactory, listenerManager, buildStateFactory);
    }

    @Override
    public IncludedBuildState addIncludedBuild(BuildDefinition buildDefinition, BuildState referrer) {
        IncludedBuildState includedBuild = super.addIncludedBuild(buildDefinition, referrer);
        Optional<DynamicGraphCycleDetector.Cycle<BuildState>> cycle = cycleDetector.addEdge(referrer, includedBuild);
        // If the included build was initially registered as a plugin build, any subsequent library registration
        // resulting of that build will still be considered a plugin build, and vice versa.
        // This is why we rely on the upcoming build definition, which reflects the actual user intention.
        if (buildDefinition.isPluginBuild()) {
            cycle.ifPresent(CycleDetectingIncludedBuildRegistry::reportCycle);
        }
        return includedBuild;
    }

    private static void reportCycle(DynamicGraphCycleDetector.Cycle<BuildState> cycle) {
        String path = cycle.format(buildState -> buildState.getIdentityPath().getPath());
        throw new GradleException(String.format("A cycle has been detected in the definition of plugin builds: %s. This is not supported with Isolated Projects. Please update your build definition to remove one of the edges.", path));
    }
}
