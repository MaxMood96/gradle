/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.api.publish.maven.internal.publication;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.publish.maven.MavenPomRelocation;
import org.gradle.api.publish.maven.MavenPomDeploymentRepository;

import javax.inject.Inject;

public abstract class DefaultMavenPomDistributionManagement implements MavenPomDistributionManagementInternal {

    private final ObjectFactory objectFactory;
    private MavenPomRelocation relocation;
    private MavenPomDeploymentRepository repository;

    @Inject
    public DefaultMavenPomDistributionManagement(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public void relocation(Action<? super MavenPomRelocation> action) {
        if (relocation == null) {
            relocation = objectFactory.newInstance(MavenPomRelocation.class);
        }
        action.execute(relocation);
    }

    @Override
    public MavenPomRelocation getRelocation() {
        return relocation;
    }

    @Override
    public void repository(Action<? super MavenPomDeploymentRepository> action) {
        if (repository == null) {
            repository = objectFactory.newInstance(MavenPomDeploymentRepository.class);
        }
        action.execute(repository);
    }

    @Override
    public MavenPomDeploymentRepository getRepository() {
        return repository;
    }

}
