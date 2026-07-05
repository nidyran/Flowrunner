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
package dev.flowrunner.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

/**
 * Covers validation failure isolation across branches: with two environments
 * where only prod is missing its required application instances, validation
 * throws {@link FlowConfigurationValidationException} naming exactly
 * {@code environment[prod].application}, without flagging the healthy dev
 * branch. Startup validation is disabled so the context boots and
 * {@code validate()} is invoked directly.
 */
@SpringBootTest(
        properties = {
                "spring.config.location=classpath:/flow-test-multi-invalid.yaml",
                "flowrunner.flow.validate-on-startup=false"
        })
@RequiredArgsConstructor
@TestConstructor(autowireMode = AutowireMode.ALL)
class FlowConfigurationValidatorInvalidMultiValueTests {

    private final FlowConfigurationValidator flowConfigurationValidator;

    @Test
    void reportsMissingRequiredDimensionOnlyForTheOffendingBranch() {
        assertThatThrownBy(flowConfigurationValidator::validate)
                .isInstanceOf(FlowConfigurationValidationException.class)
                .hasMessageContaining("Missing required dimension 'environment[prod].application'")
                .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("environment[dev]"));
    }
}
