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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.flowrunner.properties.FlowDimension;
import dev.flowrunner.properties.FlowProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class FlowInstanceValidatorTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static List<FlowDimension> dimensions() {
        return List.of(new FlowDimension(
                "environment",
                "Environment",
                "local",
                true,
                List.of(new FlowDimension(
                        "application",
                        "Application",
                        null,
                        true,
                        List.of(new FlowDimension("channel", "Channel", "Web", false, List.of()))))));
    }

    @Test
    void passesWhenAllRequiredDimensionsHaveValues() {
        Map<String, Object> instance = Map.of(
                "environment",
                Map.of(
                        "value",
                        "dev",
                        "application",
                        Map.of("value", "Customer", "channel", Map.of("value", "Web"))));

        FlowInstanceValidator validator =
                new FlowInstanceValidator(new FlowProperties(dimensions(), instance), objectMapper);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void failsWhenARequiredDimensionIsMissing() {
        Map<String, Object> instance = Map.of("environment", Map.of("value", "dev"));

        FlowInstanceValidator validator =
                new FlowInstanceValidator(new FlowProperties(dimensions(), instance), objectMapper);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(FlowInstanceValidationException.class)
                .hasMessageContaining("environment.application");
    }

    @Test
    void failsWhenInstanceIsEmpty() {
        FlowInstanceValidator validator =
                new FlowInstanceValidator(new FlowProperties(dimensions(), Map.of()), objectMapper);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(FlowInstanceValidationException.class)
                .hasMessageContaining("environment");
    }
}
