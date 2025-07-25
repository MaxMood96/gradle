/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.integtests.tooling.fixture

import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController

interface SomeToolingModel {
    String getMessage()
}

interface SomeToolingModelParameter {
    String getMessagePrefix()
    void setMessagePrefix(String value)
}

class SomeToolingModelBuildAction implements BuildAction<SomeToolingModel> {
    @Override
    SomeToolingModel execute(BuildController controller) {
        return controller.getModel(SomeToolingModel)
    }
}
