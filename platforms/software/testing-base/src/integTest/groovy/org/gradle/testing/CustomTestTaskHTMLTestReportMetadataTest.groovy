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

package org.gradle.testing

import org.gradle.api.internal.tasks.testing.report.VerifiesGenericTestReportResults
import org.gradle.integtests.fixtures.AbstractIntegrationSpec

/**
 * Tests the HTML report generated by a custom test task IRT displaying recorded metadata.
 */
final class CustomTestTaskHTMLTestReportMetadataTest extends AbstractIntegrationSpec implements VerifiesGenericTestReportResults {
    def "emits test report with metadata"() {
        given:
        buildFile << registerSimpleFailingCustomTestTaskWithMetadata()

        when:
        fails(":failing", "--S")

        then:
        failure.assertHasCause("Test(s) failed.")
        failure.assertHasErrorOutput("See the test results for more details: " + resultsUrlFor("failing"))

        and: "Verify the metadata is present in the single test report for the suite"
        def results = resultsFor("failing")
        results.testPath(":failing suite")
            .assertMetadata(["suitekey"])
        results.testPath(":failing suite:failing test")
            .assertMetadata(["testkey", "testkey2", "testkey3", "testkey4"])

        and: "Also in the aggregate report"
        def aggregateResults = aggregateResults()
        aggregateResults.testPath(":failing suite")
            .assertChildCount(1, 1, 0)
        aggregateResults.testPath(":failing suite:failing test")
            .assertMetadata(["testkey", "testkey2", "testkey3", "testkey4"])
    }

    def "emits test report with metadata with multiple values in metadata event"() {
        given:
        buildFile << registerSimpleFailingCustomTestTaskWithMultiValueMetadataEvents()

        when:
        fails(":failing")

        then:
        failure.assertHasCause("Test(s) failed.")
        failure.assertHasErrorOutput("See the test results for more details: " + resultsUrlFor("failing"))

        and:
        def results = resultsFor("failing")
        results.testPath(":failing suite:failing test")
            .assertMetadata(["group1key1", "group1key2", "group2key1", "group2key2"])
    }

    def "emits complex aggregated test report with metadata"() {
        given:
        buildFile << registerMultipleSuitesWithSuccessfulAndFailingCustomTestTasksWithMetadata()

        when:
        fails(":failing")

        then:
        failure.assertHasCause("Test(s) failed.")
        failure.assertHasErrorOutput("See the test results for more details: " + resultsUrlFor("integration-tests"))

        and: "Verify the metadata is present in the aggregate report"
        def aggregateResults = aggregateResults()

        aggregateResults.testPath("Unit Tests Suite")
            .assertChildCount(2, 0, 0)
        aggregateResults.testPath("Unit Tests Suite:test1")
            .assertMetadata(["key1"])
        aggregateResults.testPath("Unit Tests Suite:test2")
            .assertMetadata(["key2"])

        aggregateResults.testPath("Slow Integration Tests Suite")
            .assertChildCount(1, 0, 0)
            .assertMetadata(["suitekey1"])
        aggregateResults.testPath("Slow Integration Tests Suite:intTest1")
            .assertMetadata(["ikey1"])

        aggregateResults.testPath("Slower Integration Tests Suite")
            .assertChildCount(2, 1, 0)
            .assertMetadata(["suitekey2", "suitekey2-another"])
        aggregateResults.testPath("Slower Integration Tests Suite:intTest2")
            .assertMetadata(["ikey2"])
        aggregateResults.testPath("Slower Integration Tests Suite:intTest3")
            .assertMetadata(["ikey3"])
    }

    def "emits test report with metadata with rendered values"() {
        given:
        buildFile << registerSimpleFailingCustomTestTaskWithDifferentRenderableMetadataEvents()

        when:
        fails(":failing")

        then:
        failure.assertHasCause("Test(s) failed.")
        failure.assertHasErrorOutput("See the test results for more details: " + resultsUrlFor("failing"))

        and:
        def results = resultsFor("failing")
        results.testPath(":failing suite:failing test")
            .assertMetadata(["stringKey": "This is a string",
                             "stringKey2": "This is another string",
                             "booleanKey1": "true",
                             "booleanKey2": "false",
                             "fileKey": "<a href=\"file:${testDirectory.file('somefile.txt').absolutePath.replace(" ", "%20")}\">somefile.txt</a>".toString(),
                             "longStringKey": "This is a incredibly long string, and will be truncated: abcdefghijklmnopqrstuvwxyz abcdefghijklmnop...",
                             "intKey": "1",
                             "intKey2": "2",
                             "longKey": "5000000000000000",
                             "uriKey": "<a href=\"https://www.google.com\">https://www.google.com</a>",
                             "unknownKey": "<span class=\"unrenderable\">[unrenderable type]</span>"])
    }

    private registerSimpleFailingCustomTestTaskWithMetadata(String name = "failing") {
        assert !name.toCharArray().any { it.isWhitespace() }
        return """
            class TestValue implements Serializable {
                String name
                String address

                TestValue(String name, String address) {
                    this.name = name
                    this.address = address
                }
            }

            abstract class ${name}CustomTestTask extends DefaultTask {
                @Inject
                abstract TestEventReporterFactory getTestEventReporterFactory()

                @Inject
                abstract ProjectLayout getLayout()

                @TaskAction
                void runTests() {
                    try (def reporter = testEventReporterFactory.createTestEventReporter(
                        "${name}",
                        getLayout().getBuildDirectory().dir("test-results/${name}").get(),
                        getLayout().getBuildDirectory().dir("reports/tests/${name}").get()
                    )) {
                       reporter.started(java.time.Instant.now())
                       try (def mySuite = reporter.reportTestGroup("${name} suite")) {
                            mySuite.started(java.time.Instant.now())
                            mySuite.metadata(Instant.now(), 'suitekey', 'suitevalue')
                            try (def myTest = mySuite.reportTest("${name} test", "failing test")) {
                                 myTest.started(java.time.Instant.now())
                                 myTest.metadata(Instant.now(), 'testkey', 'testvalue')
                                 myTest.metadata(Instant.now(), 'testkey2', 1)
                                 myTest.metadata(Instant.now(), 'testkey3', ['a', 'b', 'c'])
                                 myTest.metadata(Instant.now(), 'testkey4', new TestValue("Bob", "123 Main St"))
                                 myTest.output(Instant.now(), TestOutputEvent.Destination.StdOut, "This is a test output on stdout")
                                 myTest.failed(java.time.Instant.now(), "failure message")
                            }
                            mySuite.failed(java.time.Instant.now())
                       }
                       reporter.failed(java.time.Instant.now())
                   }
                }
            }

            tasks.register("${name}", ${name}CustomTestTask)
        """
    }

    private registerSimpleFailingCustomTestTaskWithMultiValueMetadataEvents(String name = "failing") {
        assert !name.toCharArray().any { it.isWhitespace() }
        return """
            abstract class ${name}CustomTestTask extends DefaultTask {
                @Inject
                abstract TestEventReporterFactory getTestEventReporterFactory()

                @Inject
                abstract ProjectLayout getLayout()

                @TaskAction
                void runTests() {
                    try (def reporter = testEventReporterFactory.createTestEventReporter(
                        "${name}",
                        getLayout().getBuildDirectory().dir("test-results/${name}").get(),
                        getLayout().getBuildDirectory().dir("reports/tests/${name}").get()
                    )) {
                       reporter.started(java.time.Instant.now())
                       try (def mySuite = reporter.reportTestGroup("${name} suite")) {
                            mySuite.started(java.time.Instant.now())
                            try (def myTest = mySuite.reportTest("${name} test", "failing test")) {
                                 myTest.started(java.time.Instant.now())
                                 myTest.metadata(Instant.now(), ['group1key1': 'group1value1', 'group1key2': 'group1value2'])
                                 myTest.metadata(Instant.now(), ['group2key1': 'group2value1', 'group2key2': 'group2value2'])
                                 myTest.failed(java.time.Instant.now(), "failure message")
                            }
                            mySuite.failed(java.time.Instant.now())
                       }
                       reporter.failed(java.time.Instant.now())
                   }
                }
            }

            tasks.register("${name}", ${name}CustomTestTask)
        """
    }

    private registerSimpleFailingCustomTestTaskWithDifferentRenderableMetadataEvents(String name = "failing") {
        assert !name.toCharArray().any { it.isWhitespace() }
        return """
            abstract class ${name}CustomTestTask extends DefaultTask {
                @Inject
                abstract TestEventReporterFactory getTestEventReporterFactory()

                @Inject
                abstract ProjectLayout getLayout()

                @TaskAction
                void runTests() {
                    try (def reporter = testEventReporterFactory.createTestEventReporter(
                        "${name}",
                        getLayout().getBuildDirectory().dir("test-results/${name}").get(),
                        getLayout().getBuildDirectory().dir("reports/tests/${name}").get()
                    )) {
                       reporter.started(java.time.Instant.now())
                       try (def mySuite = reporter.reportTestGroup("${name} suite")) {
                            mySuite.started(java.time.Instant.now())
                            try (def myTest = mySuite.reportTest("${name} test", "failing test")) {
                                 myTest.started(java.time.Instant.now())
                                 myTest.metadata(Instant.now(), ['stringKey': 'This is a string', 'stringKey2': 'This is another string'])
                                 myTest.metadata(Instant.now(), ['booleanKey1': true, 'booleanKey2': false])
                                 myTest.metadata(Instant.now(), 'fileKey', layout.projectDirectory.file('somefile.txt'))
                                 myTest.metadata(Instant.now(), 'longStringKey', 'This is a incredibly long string, and will be truncated: abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz')
                                 myTest.metadata(Instant.now(), ['intKey': 1, 'intKey2': 2])
                                 myTest.metadata(Instant.now(), 'longKey', 5000000000000000L)
                                 myTest.metadata(Instant.now(), 'uriKey', new URI('https://www.google.com'))
                                 myTest.metadata(Instant.now(), 'unknownKey', Instant.now())
                                 myTest.failed(java.time.Instant.now(), "failure message")
                            }
                            mySuite.failed(java.time.Instant.now())
                       }
                       reporter.failed(java.time.Instant.now())
                   }
                }
            }

            tasks.register("${name}", ${name}CustomTestTask)
        """
    }

    // Is it even realistic that a single test task would create multiple reporters?
    // We'll test it anyway.
    private registerMultipleSuitesWithSuccessfulAndFailingCustomTestTasksWithMetadata(String name = "failing") {
        assert !name.toCharArray().any { it.isWhitespace() }
        return """
            abstract class ${name}CustomTestTask extends DefaultTask {
                @Inject
                abstract TestEventReporterFactory getTestEventReporterFactory()

                @Inject
                abstract ProjectLayout getLayout()

                @TaskAction
                void runTests() {
                   try (def reporter = testEventReporterFactory.createTestEventReporter(
                        "Unit Tests",
                        getLayout().getBuildDirectory().dir("test-results/unit-tests").get(),
                        getLayout().getBuildDirectory().dir("reports/tests/unit-tests").get()
                    )) {
                       reporter.started(java.time.Instant.now())
                       try (def mySuite = reporter.reportTestGroup("Unit Tests Suite")) {
                            mySuite.started(java.time.Instant.now())
                            mySuite.output(Instant.now(), TestOutputEvent.Destination.StdOut, "This is suite output on stdout")
                            try (def myTest = mySuite.reportTest("test1", "successful test")) {
                                 myTest.started(java.time.Instant.now())
                                 myTest.metadata(Instant.now(), 'key1', 'value1')
                                 myTest.output(Instant.now(), TestOutputEvent.Destination.StdOut, "This is a test output on stdout")
                                 myTest.succeeded(java.time.Instant.now())
                            }
                            try (def myTest = mySuite.reportTest("test2", "successful test 2")) {
                                 myTest.started(java.time.Instant.now())
                                 myTest.metadata(Instant.now(), 'key2', 'value2')
                                 myTest.output(Instant.now(), TestOutputEvent.Destination.StdOut, "This is another test output on stdout")
                                 myTest.succeeded(java.time.Instant.now())
                            }
                            mySuite.succeeded(java.time.Instant.now())
                       }
                       reporter.succeeded(java.time.Instant.now())
                    }

                    // Run failing tests last, else the task exits after the first group fails and this doesn't execute
                    try (def reporter = testEventReporterFactory.createTestEventReporter(
                        "Integration Tests",
                        getLayout().getBuildDirectory().dir("test-results/integration-tests").get(),
                        getLayout().getBuildDirectory().dir("reports/tests/integration-tests").get()
                    )) {
                       reporter.started(java.time.Instant.now())
                       try (def mySuite = reporter.reportTestGroup("Slow Integration Tests Suite")) {
                            mySuite.started(java.time.Instant.now())
                            mySuite.metadata(Instant.now(), 'suitekey1', 'suitevalue1')
                            try (def myTest = mySuite.reportTest("intTest1", "successful integration test")) {
                                 myTest.started(java.time.Instant.now())
                                 myTest.metadata(Instant.now(), 'ikey1', 'ivalue1')
                                 myTest.succeeded(java.time.Instant.now())
                            }
                            mySuite.succeeded(java.time.Instant.now())
                       }

                       try (def mySuite = reporter.reportTestGroup("Slower Integration Tests Suite")) {
                            mySuite.started(java.time.Instant.now())
                            mySuite.metadata(Instant.now(), 'suitekey2', 'suitevalue2')
                            mySuite.metadata(Instant.now(), 'suitekey2-another', 'suitevalue2-another')
                            try (def myTest = mySuite.reportTest("intTest2", "successful integration test 2")) {
                                 myTest.started(java.time.Instant.now())
                                 myTest.metadata(Instant.now(), 'ikey2', 'ivalue2')
                                 myTest.output(Instant.now(), TestOutputEvent.Destination.StdOut, "This is another test output on stdout")
                                 myTest.succeeded(java.time.Instant.now())
                            }
                            try (def myTest = mySuite.reportTest("intTest3", "failing integration test")) {
                                 myTest.started(java.time.Instant.now())
                                 myTest.metadata(Instant.now(), 'ikey3', 'ivalue3')
                                 myTest.output(Instant.now(), TestOutputEvent.Destination.StdOut, "This is a test output on stdout")
                                 myTest.failed(java.time.Instant.now(), "failure message")
                            }
                            mySuite.failed(java.time.Instant.now())
                       }
                       reporter.failed(java.time.Instant.now())
                   }
                }
            }

            tasks.register("${name}", ${name}CustomTestTask)
        """
    }
}
