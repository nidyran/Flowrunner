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
package dev.flowrunner.sample;

import static org.assertj.core.api.Assertions.assertThat;

import dev.flowrunner.handlers.DimensionPattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

import java.util.Map;

/**
 * Covers {@link AuthenticationHandler} — a sample flow handler demonstrating capabilities.
 */
@SpringBootTest(properties = "spring.config.location=classpath:/flow-properties-test.yaml")
@TestConstructor(autowireMode = AutowireMode.ALL)
class AuthenticationHandlerTests {

    private final AuthenticationHandler authenticationHandler;

    AuthenticationHandlerTests(AuthenticationHandler authenticationHandler) {
        this.authenticationHandler = authenticationHandler;
    }

    @Test
    void runAcceptsAnyParameters() {
        authenticationHandler.run(Map.of("username", "user", "password", "pass"));
    }

    @Test
    void supportedDimensionsPatternMatchesAnyPath() {
        String pattern = authenticationHandler.supportedDimensionsPattern();

        assertThat(pattern).isEqualTo(DimensionPattern.any().build());
    }

    @Test
    void moduleReturnsAuthentication() {
        assertThat(authenticationHandler.module()).isEqualTo("Authentication");
    }
}
