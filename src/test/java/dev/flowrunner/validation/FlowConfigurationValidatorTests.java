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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.flowrunner.properties.FlowConfiguration;
import dev.flowrunner.properties.FlowDimension;
import dev.flowrunner.properties.FlowProperties;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

class FlowConfigurationValidatorTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

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

    private static FlowConfiguration configuration(Object... keyValuePairs) {
        FlowConfiguration configuration = new FlowConfiguration();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            configuration.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return configuration;
    }

    private static <T> ObjectProvider<T> emptyProvider() {
        return new ObjectProvider<>() {
            @Override
            public T getIfAvailable() {
                return null;
            }
        };
    }

    private static <T> ObjectProvider<T> providerOf(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getIfAvailable() {
                return value;
            }
        };
    }

    private FlowConfigurationValidator validator(FlowProperties flowProperties) {
        return new FlowConfigurationValidator(flowProperties, jsonMapper, emptyProvider(), emptyProvider());
    }

    @Test
    void passesWhenAllRequiredDimensionsHaveValues() {
        FlowConfiguration configuration = configuration(
                "environment",
                configuration(
                        "value",
                        "dev",
                        "application",
                        configuration("value", "Customer", "channel", configuration("value", "Web"))));

        FlowConfigurationValidator validator = validator(new FlowProperties(dimensions(), configuration));

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void failsWhenARequiredDimensionIsMissing() {
        FlowConfiguration configuration = configuration("environment", configuration("value", "dev"));

        FlowConfigurationValidator validator = validator(new FlowProperties(dimensions(), configuration));

        assertThatThrownBy(validator::validate)
                .isInstanceOf(FlowConfigurationValidationException.class)
                .hasMessageContaining("environment.application");
    }

    @Test
    void failsWhenConfigurationIsEmpty() {
        FlowConfigurationValidator validator = validator(new FlowProperties(dimensions(), new FlowConfiguration()));

        assertThatThrownBy(validator::validate)
                .isInstanceOf(FlowConfigurationValidationException.class)
                .hasMessageContaining("environment");
    }

    @Test
    void callsPreAndPostLoadHooksAroundValidationWhenPresent() throws Exception {
        List<String> invocations = new ArrayList<>();
        PreLoadConfiguration preLoadConfiguration = dims -> invocations.add("pre");
        PostLoadConfiguration postLoadConfiguration = dims -> invocations.add("post");

        FlowConfiguration configuration = configuration(
                "environment",
                configuration(
                        "value",
                        "dev",
                        "application",
                        configuration("value", "Customer", "channel", configuration("value", "Web"))));

        FlowConfigurationValidator validator = new FlowConfigurationValidator(
                new FlowProperties(dimensions(), configuration),
                jsonMapper,
                providerOf(preLoadConfiguration),
                providerOf(postLoadConfiguration));

        validator.run(null);

        assertThat(invocations).containsExactly("pre", "post");
    }

    @Test
    void runsWithoutHooksWhenNonePresent() throws Exception {
        FlowConfiguration configuration = configuration(
                "environment",
                configuration(
                        "value",
                        "dev",
                        "application",
                        configuration("value", "Customer", "channel", configuration("value", "Web"))));

        FlowConfigurationValidator validator = validator(new FlowProperties(dimensions(), configuration));

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }
}
