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
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Objects;


/**
 * @author nidhal.ben-yarou on 5/11/2026
 */
@Slf4j
public class ApplicationResponseLoggingFilter extends ResponseLoggingFilter {
    private static final ThreadLocal<FlowExecutionLogger.LogEntry> ACTIVE_RESPONSE_LOG_ENTRY = new ThreadLocal<>();

    // A flag to tell the filter not to push the response, I will enrich the metadata of the reponse
    private static final ThreadLocal<Boolean> IGNORE_PUSHING_RESPONSE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public ApplicationResponseLoggingFilter() {
        super(LogDetail.ALL, true, ApplicationLoggerUtils.createPrintStream(ACTIVE_RESPONSE_LOG_ENTRY));
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        FlowExecutionLogger.LogEntry logEntry = new FlowExecutionLogger.LogEntry();
        logEntry.setKind(FlowExecutionLogger.LogEntryType.RESPONSE);
        ACTIVE_RESPONSE_LOG_ENTRY.set(logEntry);
        Response filter;
        try {
            filter = super.filter(requestSpec, responseSpec, ctx);
        } finally {
            if (Boolean.TRUE.equals(IGNORE_PUSHING_RESPONSE.get())) {
                IGNORE_PUSHING_RESPONSE.set(Boolean.FALSE);
            } else {
                pushResponse();
            }
        }
        return filter;
    }

    public static void ignorePushingResponse() {
        IGNORE_PUSHING_RESPONSE.set(Boolean.TRUE);
    }

    public static void enrichActiveLogEntry(String key, Object value) {
        FlowExecutionLogger.LogEntry logEntry = Objects.requireNonNull(ACTIVE_RESPONSE_LOG_ENTRY.get());
        if (logEntry.getMetadata() == null) {
            logEntry.setMetadata(new HashMap<>());
        }
        logEntry.getMetadata().put(key, value);
    }

    public static void pushResponse() {
        if (ApplicationRequestLoggingFilter.ACTIVE_REQUEST_LOG_ENTRY.get() != null) {
            log.error("The active request is not pushed by the curl log handler, This happens when the requests fail before the curl log handler is executed.");
            ApplicationRequestLoggingFilter.pushRequest();
        }
        FlowExecutionLogger.getLogger().push(Objects.requireNonNull(ACTIVE_RESPONSE_LOG_ENTRY.get()));
        ACTIVE_RESPONSE_LOG_ENTRY.remove();
    }
}
