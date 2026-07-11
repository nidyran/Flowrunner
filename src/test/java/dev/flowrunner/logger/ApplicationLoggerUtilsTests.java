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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.PrintStream;

/**
 * Covers {@link ApplicationLoggerUtils} — utility for creating custom PrintStreams that
 * capture output to log entries.
 */
@SpringBootTest(properties = "spring.config.location=classpath:/flow-properties-test.yaml")
class ApplicationLoggerUtilsTests {

    @Test
    void createPrintStreamWritesCharacterToLogEntry() {
        ThreadLocal<FlowExecutionLogger.LogEntry> logEntry = new ThreadLocal<>();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        logEntry.set(entry);

        PrintStream printStream = ApplicationLoggerUtils.createPrintStream(logEntry);
        printStream.print("A");

        assertThat(entry.getData()).contains("A");
    }

    @Test
    void createPrintStreamWritesBytesToLogEntry() throws Exception {
        ThreadLocal<FlowExecutionLogger.LogEntry> logEntry = new ThreadLocal<>();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        logEntry.set(entry);

        PrintStream printStream = ApplicationLoggerUtils.createPrintStream(logEntry);
        printStream.write("Hello".getBytes());
        printStream.flush();

        assertThat(entry.getData()).contains("Hello");
    }

    @Test
    void createPrintStreamConcatenatesMultipleWrites() {
        ThreadLocal<FlowExecutionLogger.LogEntry> logEntry = new ThreadLocal<>();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        logEntry.set(entry);

        PrintStream printStream = ApplicationLoggerUtils.createPrintStream(logEntry);
        printStream.print("Hello");
        printStream.print(" ");
        printStream.print("World");
        printStream.flush();

        assertThat(entry.getData()).contains("Hello World");
    }

    @Test
    void createPrintStreamHandlesUTF8() {
        ThreadLocal<FlowExecutionLogger.LogEntry> logEntry = new ThreadLocal<>();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        logEntry.set(entry);

        PrintStream printStream = ApplicationLoggerUtils.createPrintStream(logEntry);
        printStream.print("Café");
        printStream.flush();

        assertThat(entry.getData()).contains("Café");
    }

    @Test
    void createPrintStreamEnablesAutoFlush() {
        ThreadLocal<FlowExecutionLogger.LogEntry> logEntry = new ThreadLocal<>();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        logEntry.set(entry);

        PrintStream printStream = ApplicationLoggerUtils.createPrintStream(logEntry);
        printStream.println("Line");

        assertThat(entry.getData()).contains("Line");
    }

    @Test
    void createPrintStreamWritesSingleByte() {
        ThreadLocal<FlowExecutionLogger.LogEntry> logEntry = new ThreadLocal<>();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        logEntry.set(entry);

        PrintStream printStream = ApplicationLoggerUtils.createPrintStream(logEntry);
        printStream.write('X');
        printStream.flush();

        assertThat(entry.getData()).contains("X");
    }

    @Test
    void createPrintStreamWritesBytesWithOffsetAndLength() {
        ThreadLocal<FlowExecutionLogger.LogEntry> logEntry = new ThreadLocal<>();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        logEntry.set(entry);

        PrintStream printStream = ApplicationLoggerUtils.createPrintStream(logEntry);
        byte[] data = "HelloWorld".getBytes();
        printStream.write(data, 5, 5);
        printStream.flush();

        assertThat(entry.getData()).contains("World");
    }

    @Test
    void createPrintStreamWritesBytesWithZeroOffset() {
        ThreadLocal<FlowExecutionLogger.LogEntry> logEntry = new ThreadLocal<>();
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        logEntry.set(entry);

        PrintStream printStream = ApplicationLoggerUtils.createPrintStream(logEntry);
        byte[] data = "Test".getBytes();
        printStream.write(data, 0, 4);
        printStream.flush();

        assertThat(entry.getData()).contains("Test");
    }
}
