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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * Covers {@link ApplicationRequestLoggingFilter} — REST Assured filter that captures
 * request details into {@link FlowExecutionLogger} entries.
 */
@SpringBootTest(properties = "spring.config.location=classpath:/flow-properties-test.yaml")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ApplicationRequestLoggingFilterTests {

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
        ApplicationRequestLoggingFilter filter = new ApplicationRequestLoggingFilter();

        assertThat(filter).isNotNull();
    }

    @Test
    void pushRequestPushesActiveEntryToLogger() {
        FlowExecutionLogger.getLogger();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.REQUEST);
        entry.setData("request data");
        ApplicationRequestLoggingFilter.ACTIVE_REQUEST_LOG_ENTRY.set(entry);

        ApplicationRequestLoggingFilter.pushRequest();

        assertThat(ApplicationRequestLoggingFilter.ACTIVE_REQUEST_LOG_ENTRY.get()).isNull();
        assertThat(FlowExecutionLogger.getLogger().getBatchEntries()).contains(entry);
    }

    @Test
    void pushRequestThrowsWhenNoActiveEntry() {
        assertThatThrownBy(ApplicationRequestLoggingFilter::pushRequest)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("No active request log entry");
    }

    @Test
    void getOrderReturnsLowestPrecedence() {
        ApplicationRequestLoggingFilter filter = new ApplicationRequestLoggingFilter();

        assertThat(filter.getOrder()).isEqualTo(Integer.MAX_VALUE);
    }
}
