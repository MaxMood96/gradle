/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.internal.execution;

import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.internal.execution.fingerprint.InputFingerprinter;
import org.gradle.internal.service.scopes.EventScope;
import org.gradle.internal.service.scopes.Scope.Global;

/**
 * Registered via {@link WorkInputListeners}.
 */
@EventScope(Global.class)
public interface WorkInputListener {
    /**
     * Called when the execution of the given work item is imminent, or would have been if the given file collection was not currently empty.
     * <p>
     * The given files may not all of ${@link org.gradle.internal.execution.UnitOfWork#visitRegularInputs(InputFingerprinter.InputVisitor)},
     * as only a subset of that collection may be relevant to the task execution.
     *
     * @param work the identity of the unit of work to be executed
     * @param fileSystemInputs the file system inputs relevant to the task execution
     */
    void onExecute(UnitOfWork work, FileCollectionInternal fileSystemInputs);
}
