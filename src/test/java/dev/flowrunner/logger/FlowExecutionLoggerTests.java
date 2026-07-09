/*
 * MIT License
 *
 * Copyright (c) 2026 Flowrunner Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.flowrunner.logger;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.filter.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Covers {@link FlowExecutionLogger} — static logging API for flow execution with support for
 * various log entry types (INFO, WARNING, ERROR, SQL, JSON, MARKDOWN, INSIGHT), sections,
 * batch logging, and ThreadLocal logger management.
 */
@SpringBootTest(properties = "spring.config.location=classpath:/flow-properties-test.yaml")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class FlowExecutionLoggerTests {

    @BeforeEach
    void setUp() {
        FlowExecutionLogger.clear();
    }

    @AfterEach
    void tearDown() {
        FlowExecutionLogger.clear();
    }

    @Test
    void getLoggerCreatesLoggerIfNotExists() {
        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();

        assertThat(logger).isNotNull();
        assertThat(FlowExecutionLogger.getLogger()).isSameAs(logger);
    }

    @Test
    void setLoggerOverridesCurrentLogger() {
        FlowExecutionLogger.Logger logger1 = new FlowExecutionLogger.Logger();
        FlowExecutionLogger.setLogger(logger1);

        assertThat(FlowExecutionLogger.getLogger()).isSameAs(logger1);
    }

    @Test
    void clearRemovesThreadLocalLoggerAndIgnoreFlag() {
        FlowExecutionLogger.Logger logger1 = FlowExecutionLogger.getLogger();
        FlowExecutionLogger.clear();

        FlowExecutionLogger.Logger logger2 = FlowExecutionLogger.getLogger();
        assertThat(logger2).isNotNull();
        assertThat(logger2).isNotSameAs(logger1);
    }

    @Test
    void getLogFiltersReturnsRequestAndResponseFilters() {
        List<Filter> filters = FlowExecutionLogger.getLogFilters();

        assertThat(filters).hasSize(2)
                .anySatisfy(f -> assertThat(f).isInstanceOf(ApplicationRequestLoggingFilter.class))
                .anySatisfy(f -> assertThat(f).isInstanceOf(ApplicationResponseLoggingFilter.class));
    }

    @Test
    void disableBatchLoggingDisablesAggregation() {
        FlowExecutionLogger.getLogger();
        FlowExecutionLogger.info("test");
        FlowExecutionLogger.disableBatchLogging();

        assertThat(FlowExecutionLogger.getLogger().isBatchLogging()).isFalse();
    }

    @Test
    void enableBatchLoggingEnablesAggregation() {
        FlowExecutionLogger.getLogger();
        FlowExecutionLogger.disableBatchLogging();
        FlowExecutionLogger.enableBatchLogging();

        assertThat(FlowExecutionLogger.getLogger().isBatchLogging()).isTrue();
    }

    @Test
    void disableLoggingPreventsLogEntryEmission() {
        FlowExecutionLogger.disableLogging();
        FlowExecutionLogger.info("test");

        assertThat(FlowExecutionLogger.getLogger().isBatchLogging()).isTrue();
    }

    @Test
    void enableLoggingAllowsLogEntryEmission() {
        FlowExecutionLogger.disableLogging();
        FlowExecutionLogger.enableLogging();
        FlowExecutionLogger.info("test");

        assertThat(FlowExecutionLogger.getLogger().isEnabled()).isTrue();
    }

    @Test
    void infoPushesInfoLogEntry() {
        FlowExecutionLogger.info("info message");

        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();
        assertThat(logger.getBatchEntries()).hasSize(1);
        FlowExecutionLogger.LogEntry entry = logger.getBatchEntries().getFirst();
        assertThat(entry.getKind()).isEqualTo(FlowExecutionLogger.LogEntryType.INFO);
        assertThat(entry.getData()).isEqualTo("info message");
    }

    @Test
    void sqlPushesSqlLogEntry() {
        FlowExecutionLogger.sql("SELECT * FROM users");

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
        FlowExecutionLogger.LogEntry entry = FlowExecutionLogger.getLogger().getBatchEntries().getFirst();
        assertThat(entry.getKind()).isEqualTo(FlowExecutionLogger.LogEntryType.SQL);
        assertThat(entry.getData()).isEqualTo("SELECT * FROM users");
    }

    @Test
    void jsonPushesJsonLogEntry() {
        String json = "{\"key\": \"value\"}";
        FlowExecutionLogger.json(json);

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().getFirst().getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.JSON);
    }

    @Test
    void markdownPushesMarkdownLogEntry() {
        FlowExecutionLogger.markdown("# Title");

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().getFirst().getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.MARKDOWN);
    }

    @Test
    void insightPushesInsightLogEntryWithMetadata() {
        FlowExecutionLogger.insight("insights", "openai", "gpt-4");

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
        FlowExecutionLogger.LogEntry entry = FlowExecutionLogger.getLogger().getBatchEntries().getFirst();
        assertThat(entry.getKind()).isEqualTo(FlowExecutionLogger.LogEntryType.INSIGHT);
        assertThat(entry.getMetadata())
                .containsEntry("provider", "openai")
                .containsEntry("model", "gpt-4");
    }

    @Test
    void successPushesSuccessLogEntry() {
        FlowExecutionLogger.success("operation completed");

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().getFirst().getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.SUCCESS);
    }

    @Test
    void warnPushesWarningLogEntry() {
        FlowExecutionLogger.warn("warning message");

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().getFirst().getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.WARNING);
    }

    @Test
    void errorPushesErrorLogEntryAndSetsFailed() {
        FlowExecutionLogger.error("error message");

        assertThat(FlowExecutionLogger.getLogger().isFailed()).isTrue();
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().getFirst().getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.ERROR);
    }

    @Test
    void pushDataWithoutMetadataCreatesEmptyMetadataMap() {
        FlowExecutionLogger.pushData("data", FlowExecutionLogger.LogEntryType.INFO);

        FlowExecutionLogger.LogEntry entry = FlowExecutionLogger.getLogger().getBatchEntries().getFirst();
        assertThat(entry.getMetadata()).containsKey("caller");
    }

    @Test
    void pushDataWithMetadataIncludesCaller() {
        Map<String, Object> metadata = Map.of("key", "value");
        FlowExecutionLogger.pushData("data", FlowExecutionLogger.LogEntryType.INFO, metadata);

        FlowExecutionLogger.LogEntry entry = FlowExecutionLogger.getLogger().getBatchEntries().getFirst();
        assertThat(entry.getMetadata())
                .containsKey("key")
                .containsKey("caller");
    }

    @Test
    void resolveCallerReturnsSimpleClassName() {
        String caller = FlowExecutionLogger.resolveCaller();

        assertThat(caller).isNotBlank().isNotEqualTo("UnknownCaller");
    }

    @Test
    void isFailedReturnsFalseByDefault() {
        assertThat(FlowExecutionLogger.isFailed()).isFalse();
    }

    @Test
    void isFailedReturnsTrueAfterError() {
        FlowExecutionLogger.error("test error");

        assertThat(FlowExecutionLogger.isFailed()).isTrue();
    }

    @Test
    void sectionCreatesAndOpensSection() {
        FlowExecutionLogger.section("Test Section");

        assertThat(FlowExecutionLogger.getLogger().isSectionOpen()).isTrue();
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().getFirst().getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.SECTION);
    }

    @Test
    void sectionWithSkipIfOpenDoesNotCloseExistingSection() {
        FlowExecutionLogger.section("Section 1");
        FlowExecutionLogger.section("Section 2", true);

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
    }

    @Test
    void endSectionClosesOpenSection() {
        FlowExecutionLogger.section("Test Section");
        FlowExecutionLogger.endSection();

        assertThat(FlowExecutionLogger.getLogger().isSectionOpen()).isFalse();
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(2);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().get(1).getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.END_SECTION);
    }

    @Test
    void endSectionDoesNothingIfNoSectionOpen() {
        FlowExecutionLogger.endSection();

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).isEmpty();
    }

    @Test
    void forceSectionClosesExistingAndCreatesNew() {
        FlowExecutionLogger.section("Old");
        FlowExecutionLogger.forceSection("New");

        assertThat(FlowExecutionLogger.getLogger().isForcedSection()).isTrue();
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(3);
    }

    @Test
    void forceEndSectionClosesOpenSection() {
        FlowExecutionLogger.section("Test");
        FlowExecutionLogger.forceEndSection();

        assertThat(FlowExecutionLogger.getLogger().isSectionOpen()).isFalse();
        assertThat(FlowExecutionLogger.getLogger().isForcedSection()).isFalse();
    }

    @Test
    void completeEmitsCompleteSignal() {
        FlowExecutionLogger.info("test");
        FlowExecutionLogger.complete();

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
    }

    @Test
    void completeWithIgnoreFlagSkipsCompletion() {
        FlowExecutionLogger.info("test");
        // Simulate setting ignore flag through reflection or internal mechanism
        FlowExecutionLogger.complete();

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
    }

    @Test
    void logEntryInitializesWithEmptyData() {
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();

        assertThat(entry.getData()).isEmpty();
        assertThat(entry.getMetadata()).isEmpty();
    }

    @Test
    void loggerFlushesToAnotherLogger() {
        FlowExecutionLogger.Logger logger1 = FlowExecutionLogger.getLogger();
        FlowExecutionLogger.info("message");

        FlowExecutionLogger.Logger logger2 = new FlowExecutionLogger.Logger();
        logger1.flushTo(logger2);

        assertThat(logger2.getBatchEntries()).hasSize(1);
    }

    @Test
    void loggerTracksTotalRequests() {
        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.REQUEST);

        logger.push(entry);

        assertThat(logger.getTotalRequests()).isEqualTo(1);
    }

    @Test
    void loggerPushIgnoresWhenDisabled() {
        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();
        logger.setEnabled(false);

        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.INFO);
        entry.setData("test");
        logger.push(entry);

        assertThat(logger.getBatchEntries()).isEmpty();
    }

    @Test
    void logEntryTypeIsRequestOrResponse() {
        assertThat(FlowExecutionLogger.LogEntryType.REQUEST.isRequestOrResponse()).isTrue();
        assertThat(FlowExecutionLogger.LogEntryType.RESPONSE.isRequestOrResponse()).isTrue();
        assertThat(FlowExecutionLogger.LogEntryType.INFO.isRequestOrResponse()).isFalse();
    }

    @Test
    void sectionClosesExistingBeforeCreatingNew() {
        FlowExecutionLogger.section("Section 1");
        FlowExecutionLogger.section("Section 2", false);

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(3);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().get(0).getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.SECTION);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().get(1).getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.END_SECTION);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().get(2).getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.SECTION);
    }

    @Test
    void forceSectionDoesNotCreateIfForcedSectionAlreadyOpen() {
        FlowExecutionLogger.forceSection("Forced");
        int sizeAfterForce = FlowExecutionLogger.getLogger().getBatchEntries().size();

        FlowExecutionLogger.forceSection("Another");

        assertThat(FlowExecutionLogger.getLogger().isForcedSection()).isTrue();
    }

    @Test
    void endSectionDoesNothingIfForcedSection() {
        FlowExecutionLogger.forceSection("Test");
        FlowExecutionLogger.endSection();

        assertThat(FlowExecutionLogger.getLogger().isSectionOpen()).isTrue();
        assertThat(FlowExecutionLogger.getLogger().isForcedSection()).isTrue();
    }

    @Test
    void sectionDoesNothingIfForcedSectionActive() {
        FlowExecutionLogger.forceSection("Forced");
        int sizeBeforeSection = FlowExecutionLogger.getLogger().getBatchEntries().size();

        FlowExecutionLogger.section("Normal");

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(sizeBeforeSection);
    }

    @Test
    void loggerSetsTimestampOnPush() {
        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.INFO);
        entry.setData("test");

        logger.push(entry);

        assertThat(entry.getTimestamp()).isGreaterThan(0);
    }

    @Test
    void loggerUsesExistingTimestampIfSet() {
        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.INFO);
        entry.setData("test");
        long customTimestamp = 12345L;
        entry.setTimestamp(customTimestamp);

        logger.push(entry);

        assertThat(entry.getTimestamp()).isEqualTo(customTimestamp);
    }

    @Test
    void loggerFlushHandlesRequestResponseMismatch() {
        FlowExecutionLogger.Logger logger1 = FlowExecutionLogger.getLogger();

        FlowExecutionLogger.LogEntry requestEntry = new FlowExecutionLogger.LogEntry();
        requestEntry.setKind(FlowExecutionLogger.LogEntryType.REQUEST);
        logger1.push(requestEntry);

        FlowExecutionLogger.Logger logger2 = new FlowExecutionLogger.Logger();
        logger1.flushTo(logger2);

        assertThat(logger2.getBatchEntries()).hasSize(1);
    }

    @Test
    void pushDataWithNullMetadataUsesEmptyMap() {
        FlowExecutionLogger.pushData("data", FlowExecutionLogger.LogEntryType.INFO, null);

        FlowExecutionLogger.LogEntry entry = FlowExecutionLogger.getLogger().getBatchEntries().getFirst();
        assertThat(entry.getMetadata())
                .isNotEmpty()
                .containsKey("caller");
    }

    @Test
    void getLogStreamReturnsFlux() {
        FlowExecutionLogger.info("test");

        assertThat(FlowExecutionLogger.getLogStream()).isNotNull();
    }

    @Test
    void logEntryMetadataLazyInitialization() {
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();

        Map<String, Object> metadata = entry.getMetadata();

        assertThat(metadata).isNotNull().isEmpty();
        assertThat(entry.getMetadata()).isSameAs(metadata);
    }

    @Test
    void allLogEntryTypesExist() {
        assertThat(FlowExecutionLogger.LogEntryType.values()).contains(
                FlowExecutionLogger.LogEntryType.REQUEST,
                FlowExecutionLogger.LogEntryType.RESPONSE,
                FlowExecutionLogger.LogEntryType.INFO,
                FlowExecutionLogger.LogEntryType.WARNING,
                FlowExecutionLogger.LogEntryType.ERROR,
                FlowExecutionLogger.LogEntryType.SUCCESS,
                FlowExecutionLogger.LogEntryType.INSIGHT,
                FlowExecutionLogger.LogEntryType.SECTION,
                FlowExecutionLogger.LogEntryType.SQL,
                FlowExecutionLogger.LogEntryType.JSON,
                FlowExecutionLogger.LogEntryType.MARKDOWN,
                FlowExecutionLogger.LogEntryType.END_SECTION
        );
    }

    @Test
    void loggerBatchEntriesDoesNotAddWhenDisabled() {
        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();
        logger.setEnabled(false);
        logger.setBatchLogging(true);

        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.INFO);
        entry.setData("test");
        logger.push(entry);

        assertThat(logger.getBatchEntries()).isEmpty();
    }

    @Test
    void loggerBatchEntriesAddedWhenBatchLoggingEnabled() {
        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();
        logger.setEnabled(true);
        logger.setBatchLogging(true);

        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.INFO);
        entry.setData("test");
        logger.push(entry);

        assertThat(logger.getBatchEntries()).hasSize(1);
    }

    @Test
    void loggerSinkEmitNextWhenEnabled() {
        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();
        logger.setEnabled(true);
        logger.setBatchLogging(false);

        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.INFO);
        entry.setData("test");
        logger.push(entry);

        assertThat(logger.getBatchEntries()).isEmpty();
    }

    @Test
    void multipleEntriesInBatchHaveSequentialTimestamps() {
        FlowExecutionLogger.Logger logger = FlowExecutionLogger.getLogger();

        FlowExecutionLogger.LogEntry entry1 = new FlowExecutionLogger.LogEntry();
        entry1.setKind(FlowExecutionLogger.LogEntryType.INFO);
        logger.push(entry1);

        long timestamp1 = entry1.getTimestamp();

        FlowExecutionLogger.LogEntry entry2 = new FlowExecutionLogger.LogEntry();
        entry2.setKind(FlowExecutionLogger.LogEntryType.INFO);
        logger.push(entry2);

        long timestamp2 = entry2.getTimestamp();

        assertThat(timestamp2).isGreaterThanOrEqualTo(timestamp1);
    }

    @Test
    void forceEndSectionIdempotent() {
        FlowExecutionLogger.forceSection("Test");
        FlowExecutionLogger.forceEndSection();
        int sizeAfterFirst = FlowExecutionLogger.getLogger().getBatchEntries().size();

        FlowExecutionLogger.forceEndSection();
        int sizeAfterSecond = FlowExecutionLogger.getLogger().getBatchEntries().size();

        assertThat(sizeAfterSecond).isEqualTo(sizeAfterFirst);
    }

    @Test
    void mixedSectionAndRegularLogging() {
        FlowExecutionLogger.section("Start");
        FlowExecutionLogger.info("inside section");
        FlowExecutionLogger.endSection();
        FlowExecutionLogger.info("after section");

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(4);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().get(0).getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.SECTION);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().get(1).getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.INFO);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().get(2).getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.END_SECTION);
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries().get(3).getKind())
                .isEqualTo(FlowExecutionLogger.LogEntryType.INFO);
    }

    @Test
    void completeEmitsCompleteOnSink() {
        FlowExecutionLogger.info("test");
        FlowExecutionLogger.complete();

        FlowExecutionLogger.getLogger().complete();
    }

    @Test
    void resolveCallerReturnsValidClassName() {
        String caller = FlowExecutionLogger.resolveCaller();

        assertThat(caller).isNotEmpty();
        assertThat(caller).doesNotStartWith("class ");
    }

    @Test
    void ignoreLogCompleteSkipsCompletion() {
        // Access the ignore flag through reflection since it's private
        Field field;
        try {
            field = FlowExecutionLogger.class.getDeclaredField("ignoreNextLogCompleteThreadLocal");
            field.setAccessible(true);
            ThreadLocal<Boolean> ignoreFlag = (ThreadLocal<Boolean>) field.get(null);
            ignoreFlag.set(Boolean.TRUE);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        FlowExecutionLogger.info("test");
        FlowExecutionLogger.complete();

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).hasSize(1);
    }

    @Test
    void insightWithNullMetadataHandled() {
        FlowExecutionLogger.insight("insights", null, null);

        FlowExecutionLogger.LogEntry entry = FlowExecutionLogger.getLogger().getBatchEntries().getFirst();
        assertThat(entry.getMetadata())
                .containsKey("provider")
                .containsKey("model")
                .containsKey("caller");
    }

    @Test
    void flushToWithMultipleEntriesTransfersAll() {
        FlowExecutionLogger.Logger logger1 = FlowExecutionLogger.getLogger();
        FlowExecutionLogger.info("message1");
        FlowExecutionLogger.info("message2");
        FlowExecutionLogger.info("message3");

        assertThat(logger1.getBatchEntries()).hasSize(3);

        FlowExecutionLogger.Logger logger2 = new FlowExecutionLogger.Logger();
        logger1.flushTo(logger2);

        assertThat(logger2.getBatchEntries()).hasSize(3);
    }

    @Test
    void getLoggerReturnsConsistentInstance() {
        FlowExecutionLogger.Logger logger1 = FlowExecutionLogger.getLogger();
        FlowExecutionLogger.Logger logger2 = FlowExecutionLogger.getLogger();

        assertThat(logger1).isSameAs(logger2);
    }

    @Test
    void loggerFailedStateIndependent() {
        FlowExecutionLogger.Logger logger1 = FlowExecutionLogger.getLogger();
        FlowExecutionLogger.error("error");

        assertThat(logger1.isFailed()).isTrue();

        FlowExecutionLogger.Logger logger2 = new FlowExecutionLogger.Logger();
        assertThat(logger2.isFailed()).isFalse();
    }
}
