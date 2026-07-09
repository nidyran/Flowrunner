/*
 * Copyright (c) 2026 Nidhal Ben Yarou
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
 * 
 */
package dev.flowrunner.logger;

import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;

/**
 * @author nidhal.ben-yarou on 5/11/2026
 */
public class ApplicationRequestLoggingFilter extends RequestLoggingFilter implements OrderedFilter {
    public static final ThreadLocal<FlowExecutionLogger.LogEntry> ACTIVE_REQUEST_LOG_ENTRY = new ThreadLocal<>();

    public ApplicationRequestLoggingFilter() {
        super(LogDetail.ALL, true, ApplicationLoggerUtils.createPrintStream(ACTIVE_REQUEST_LOG_ENTRY));
    }

    public static void pushRequest() {
        FlowExecutionLogger.LogEntry logEntry = ACTIVE_REQUEST_LOG_ENTRY.get();
        Assertions.assertNotNull(logEntry, "No active request log entry found. Ensure that the request logging filter is properly configured.");
        FlowExecutionLogger.getLogger().push(logEntry);
        ACTIVE_REQUEST_LOG_ENTRY.remove();
    }

    @Override
    @SneakyThrows
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        FlowExecutionLogger.LogEntry logEntry = new FlowExecutionLogger.LogEntry();
        logEntry.setKind(FlowExecutionLogger.LogEntryType.REQUEST);
        ACTIVE_REQUEST_LOG_ENTRY.set(logEntry);
        return super.filter(requestSpec, responseSpec, ctx);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
