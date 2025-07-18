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

package org.gradle.api.tasks.wrapper.internal

import org.gradle.api.GradleException
import spock.lang.Specification

class GradleVersionResolverTest extends Specification {
    def "get version from json"() {
        when:
        def version = GradleVersionResolver.getVersion("""{ "version" : "7.6" }""", "latest")

        then:
        version == "7.6"
    }

    def "get version from empty json"() {
        when:
        def version = GradleVersionResolver.getVersion("{ }", "latest")

        then:
        def e = thrown(GradleException)
        e.message == "There is currently no version information available for 'latest'."
    }

    def "right alternatives are suggested for bad version"() {
        when:
        GradleVersionResolver.parseVersionString("invalid")

        then:
        def e = thrown(GradleVersionResolver.WrapperVersionException)
        e.message == "Invalid version specified for argument '--gradle-version'"
        e.getResolutions() == [
            "Specify a valid Gradle release listed on https://gradle.org/releases/.",
            "Use one of the following dynamic version specifications: 'latest', 'release-candidate', 'release-milestone', 'release-nightly', 'nightly'."
        ]
    }

    def "get versions from json"() {
        when:
        def versions = GradleVersionResolver.getVersions("""[{ "version" : "7.6" }, { "version" : "9.0.3" }]""")

        then:
        versions == ["7.6", "9.0.3"]
    }
}
