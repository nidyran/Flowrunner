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
package dev.flowrunner.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FlowRunnerHandlerTests {

    private static class TestHandler implements FlowRunnerHandler {
        @Override
        public void run(Map<String, String> parameters) {
        }

        @Override
        public String supportedDimensionsPattern() {
            return ".*";
        }

        @Override
        public String module() {
            return "test";
        }
    }

    private static class LimitOTPResendsHandler implements FlowRunnerHandler {
        @Override
        public void run(Map<String, String> parameters) {
        }

        @Override
        public String supportedDimensionsPattern() {
            return ".*";
        }

        @Override
        public String module() {
            return "auth";
        }
    }

    private static class HTTPConnectionHandler implements FlowRunnerHandler {
        @Override
        public void run(Map<String, String> parameters) {
        }

        @Override
        public String supportedDimensionsPattern() {
            return ".*";
        }

        @Override
        public String module() {
            return "network";
        }
    }

    private static class OTPHandler implements FlowRunnerHandler {
        @Override
        public void run(Map<String, String> parameters) {
        }

        @Override
        public String supportedDimensionsPattern() {
            return ".*";
        }

        @Override
        public String module() {
            return "auth";
        }
    }

    @Test
    void testFriendlyNameSimpleClass() {
        var handler = new TestHandler();
        assertEquals("Test", handler.friendlyName());
    }

    @Test
    void testFriendlyNameWithMultipleWords() {
        var handler = new LimitOTPResendsHandler();
        assertEquals("Limit OTP Resends", handler.friendlyName());
    }

    @Test
    void testFriendlyNamePreservesAcronyms() {
        var handler = new HTTPConnectionHandler();
        assertEquals("HTTP Connection", handler.friendlyName());
    }

    @Test
    void testFriendlyNamePreservesSingleAcronym() {
        var handler = new OTPHandler();
        assertEquals("OTP", handler.friendlyName());
    }

    @Test
    void testDefaultDescription() {
        var handler = new TestHandler();
        assertEquals("", handler.description());
    }

    @Test
    void testDefaultGetSupportedParameters() {
        var handler = new TestHandler();
        assertEquals(0, handler.getSupportedParameters().size());
    }
}
