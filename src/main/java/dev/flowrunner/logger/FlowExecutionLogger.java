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

import io.restassured.filter.Filter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;

@Slf4j
@Component
public class FlowExecutionLogger {
    private static final ThreadLocal<Logger> requestLogThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ignoreNextLogCompleteThreadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static Logger getLogger() {
        Logger logger = requestLogThreadLocal.get();
        if (logger == null) {
            logger = new Logger();
            requestLogThreadLocal.set(logger);
        }
        return Objects.requireNonNull(logger);
    }

    public static void setLogger(Logger logger) {
        requestLogThreadLocal.set(logger);
    }

    public static void clear() {
        requestLogThreadLocal.remove();
        ignoreNextLogCompleteThreadLocal.remove();
    }

    private static void push(LogEntry entry) {
        getLogger().push(entry);
    }

    public static Flux<LogEntry> getLogStream() {
        return getLogger().getSink().asFlux();
    }

    public static void complete() {
        if (Boolean.TRUE.equals(ignoreNextLogCompleteThreadLocal.get())) {
            ignoreNextLogCompleteThreadLocal.set(Boolean.FALSE);
            return;
        }
        getLogger().complete();
    }

    public static List<Filter> getLogFilters() {
        return List.of(new ApplicationRequestLoggingFilter(), new ApplicationResponseLoggingFilter());
    }

    public static void disableBatchLogging() {
        getLogger().setBatchLogging(false);
    }

    public static void enableBatchLogging() {
        getLogger().setBatchLogging(true);
    }

    public static void disableLogging() {
        getLogger().setEnabled(false);
    }

    public static void enableLogging() {
        getLogger().setEnabled(true);
    }

    public static void info(String data) {
        pushData(data, LogEntryType.INFO);
    }

    public static void sql(String statement) {
        pushData(statement, LogEntryType.SQL);
    }

    public static void json(String json) {
        pushData(json, LogEntryType.JSON);
    }

    public static void markdown(String markdown) {
        pushData(markdown, LogEntryType.MARKDOWN);
    }

    public static void insight(String message, String provider, String model) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", provider);
        metadata.put("model", model);
        pushData(message, LogEntryType.INSIGHT, metadata);
    }

    public static void success(String message) {
        pushData(message, LogEntryType.SUCCESS);
    }

    public static void warn(String data) {
        pushData(data, LogEntryType.WARNING);
    }

    public static void error(String data) {
        pushData(data, LogEntryType.ERROR);
        getLogger().setFailed(true);
    }

    public static void pushData(String data, LogEntryType type) {
        pushData(data, type, Collections.emptyMap());
    }

    public static void pushData(String data, LogEntryType type, Map<String, Object> metadata) {
        LogEntry entry = new LogEntry();
        entry.setKind(type);
        entry.setData(data);
        metadata = Objects.requireNonNullElse(metadata, Collections.emptyMap());
        metadata = new HashMap<>(metadata);
        metadata.put("caller", resolveCaller());
        entry.setMetadata(metadata);
        push(entry);
    }

    public static String resolveCaller() {
        try {
            String currentClassName = FlowExecutionLogger.class.getName();
            Class<?> callerClass = StackWalker
                    .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(frames -> frames
                            .filter(f -> !f.getDeclaringClass().getName().startsWith(currentClassName))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Unable to resolve caller class from stack trace")))
                    .getDeclaringClass();
            return callerClass.getSimpleName();
        } catch (Exception e) {
            log.warn("Failed to resolve caller class from stack trace, returning 'UnknownCaller'", e);
            return "UnknownCaller";
        }
    }

    public static boolean isFailed() {
        return getLogger().isFailed();
    }

    public static void section(String data) {
        section(data, false);
    }

    public static void section(String data, boolean skipIfOpen) {
        Logger logger = getLogger();
        if (logger.isForcedSection()) {
            return;
        }
        if (logger.isSectionOpen()) {
            if (skipIfOpen) {
                return;
            }
            endSection();
        }
        LogEntry entry = new LogEntry();
        entry.setKind(LogEntryType.SECTION);
        entry.setData(data);
        logger.setSectionOpen(true);
        push(entry);
    }

    public static void endSection() {
        Logger logger = getLogger();
        if (logger.isForcedSection()) {
            return;
        }
        if (logger.isSectionOpen()) {
            LogEntry entry = new LogEntry();
            entry.setKind(LogEntryType.END_SECTION);
            push(entry);
            logger.setSectionOpen(false);
        }
    }

    public static void forceSection(String data) {
        Logger logger = getLogger();
        forceEndSection();
        LogEntry entry = new LogEntry();
        entry.setKind(LogEntryType.SECTION);
        entry.setData(data);
        logger.setSectionOpen(true);
        logger.setForcedSection(true);
        push(entry);
    }

    public static void forceEndSection() {
        Logger logger = getLogger();
        if (logger.isSectionOpen()) {
            LogEntry entry = new LogEntry();
            entry.setKind(LogEntryType.END_SECTION);
            push(entry);
            logger.setSectionOpen(false);
            logger.setForcedSection(false);
        }
    }

    @Getter
    @Setter
    public static class Logger {
        private final Sinks.Many<LogEntry> sink = Sinks.many().multicast().onBackpressureBuffer();
        private final List<LogEntry> batchEntries = new ArrayList<>();

        private boolean batchLogging = true;

        private boolean enabled = true;

        private boolean failed;

        private boolean sectionOpen;
        private boolean forcedSection;

        private int totalRequests;

        public synchronized void flushTo(Logger logger) {
            complete();
            long requestCount = batchEntries.stream()
                    .filter(entry -> LogEntryType.REQUEST.equals(entry.getKind()))
                    .count();
            long responseCount = batchEntries.stream()
                    .filter(entry -> LogEntryType.RESPONSE.equals(entry.getKind()))
                    .count();
            if (requestCount != responseCount) {
                log.warn("Request/response count mismatch during flush: {} requests, {} responses", requestCount, responseCount);
            }
            batchEntries.forEach(logger::push);
        }

        public void push(LogEntry entry) {
            if (LogEntryType.REQUEST.equals(entry.getKind())) {
                totalRequests++;
            }
            if (enabled) {
                if (entry.getTimestamp() == 0) {
                    entry.setTimestamp(System.currentTimeMillis());
                }
                sink.tryEmitNext(entry);
                if (batchLogging) {
                    batchEntries.add(entry);
                }
            }
        }

        public void complete() {
            sink.tryEmitComplete();
        }
    }

    @Getter
    @Setter
    public static class LogEntry {
        private long timestamp;
        private String data = Strings.EMPTY;
        private LogEntryType kind;
        private Map<String, Object> metadata;

        public Map<String, Object> getMetadata() {
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            return metadata;
        }
    }

    public enum LogEntryType {
        REQUEST,
        RESPONSE,
        INFO,
        WARNING,
        ERROR,
        SUCCESS,
        INSIGHT,
        SECTION,
        SQL,
        JSON,
        MARKDOWN,
        END_SECTION;

        public boolean isRequestOrResponse() {
            return REQUEST.equals(this) || RESPONSE.equals(this);
        }
    }

    private FlowExecutionLogger() {
    }
}
