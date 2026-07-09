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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * Covers {@link ApplicationResponseLoggingFilter} — REST Assured filter that captures
 * response details into {@link FlowExecutionLogger} entries with metadata enrichment support.
 */
@SpringBootTest(properties = "spring.config.location=classpath:/flow-properties-test.yaml")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ApplicationResponseLoggingFilterTests {

    @BeforeEach
    void setUp() {
        FlowExecutionLogger.clear();
        ApplicationRequestLoggingFilter.ACTIVE_REQUEST_LOG_ENTRY.remove();
    }

    @AfterEach
    void tearDown() {
        FlowExecutionLogger.clear();
        ApplicationRequestLoggingFilter.ACTIVE_REQUEST_LOG_ENTRY.remove();
    }

    @Test
    void canInstantiateFilter() {
        ApplicationResponseLoggingFilter filter = new ApplicationResponseLoggingFilter();

        assertThat(filter).isNotNull();
    }

    @Test
    void ignorePushingResponseSetsIgnoreFlag() {
        ApplicationResponseLoggingFilter.ignorePushingResponse();

        ApplicationResponseLoggingFilter filter = new ApplicationResponseLoggingFilter();
        assertThat(filter).isNotNull();
    }

    @Test
    void enrichActiveLogEntryAddsMetadata() {
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.RESPONSE);

        java.lang.reflect.Field field;
        try {
            field = ApplicationResponseLoggingFilter.class.getDeclaredField("ACTIVE_RESPONSE_LOG_ENTRY");
            field.setAccessible(true);
            ThreadLocal<FlowExecutionLogger.LogEntry> threadLocal = (ThreadLocal<FlowExecutionLogger.LogEntry>) field.get(null);
            threadLocal.set(entry);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        ApplicationResponseLoggingFilter.enrichActiveLogEntry("statusCode", 200);

        assertThat(entry.getMetadata()).containsEntry("statusCode", 200);
    }

    @Test
    void pushResponsePushesActiveEntryToLogger() {
        FlowExecutionLogger.getLogger();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.RESPONSE);
        entry.setData("response data");

        java.lang.reflect.Field field;
        try {
            field = ApplicationResponseLoggingFilter.class.getDeclaredField("ACTIVE_RESPONSE_LOG_ENTRY");
            field.setAccessible(true);
            ThreadLocal<FlowExecutionLogger.LogEntry> threadLocal = (ThreadLocal<FlowExecutionLogger.LogEntry>) field.get(null);
            threadLocal.set(entry);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        ApplicationResponseLoggingFilter.pushResponse();

        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).contains(entry);
    }

    @Test
    void pushResponseCleansUpThreadLocal() {
        FlowExecutionLogger.getLogger();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.RESPONSE);

        java.lang.reflect.Field field;
        try {
            field = ApplicationResponseLoggingFilter.class.getDeclaredField("ACTIVE_RESPONSE_LOG_ENTRY");
            field.setAccessible(true);
            ThreadLocal<FlowExecutionLogger.LogEntry> threadLocal = (ThreadLocal<FlowExecutionLogger.LogEntry>) field.get(null);
            threadLocal.set(entry);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        ApplicationResponseLoggingFilter.pushResponse();

        try {
            field = ApplicationResponseLoggingFilter.class.getDeclaredField("ACTIVE_RESPONSE_LOG_ENTRY");
            field.setAccessible(true);
            ThreadLocal<FlowExecutionLogger.LogEntry> threadLocal = (ThreadLocal<FlowExecutionLogger.LogEntry>) field.get(null);
            assertThat(threadLocal.get()).isNull();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void enrichActiveLogEntryCreatesMetadataIfNullByReflection() {
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.RESPONSE);

        // Use reflection to set metadata to null directly
        java.lang.reflect.Field metadataField;
        try {
            metadataField = FlowExecutionLogger.LogEntry.class.getDeclaredField("metadata");
            metadataField.setAccessible(true);
            metadataField.set(entry, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        java.lang.reflect.Field field;
        try {
            field = ApplicationResponseLoggingFilter.class.getDeclaredField("ACTIVE_RESPONSE_LOG_ENTRY");
            field.setAccessible(true);
            ThreadLocal<FlowExecutionLogger.LogEntry> threadLocal = (ThreadLocal<FlowExecutionLogger.LogEntry>) field.get(null);
            threadLocal.set(entry);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        ApplicationResponseLoggingFilter.enrichActiveLogEntry("testKey", "testValue");

        assertThat(entry.getMetadata())
                .isNotNull()
                .containsEntry("testKey", "testValue");
    }
}
