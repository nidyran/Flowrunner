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
package dev.flowrunner.properties;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.RequiredArgsConstructor;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

/**
 * Covers binding of {@code flowrunner.flow} properties from YAML:
 * <ul>
 *   <li>root dimensions bind with key, name, default value and required flag</li>
 *   <li>nested child dimensions bind recursively (environment → application → channel)</li>
 *   <li>configuration binds directly to the {@link FlowDimensionInstance} tree,
 *       including metadata entries and nested children</li>
 * </ul>
 */
@SpringBootTest(properties = "spring.config.location=classpath:/flow-properties-test.yaml")
@RequiredArgsConstructor
@TestConstructor(autowireMode = AutowireMode.ALL)
class FlowPropertiesTests {

    private final FlowProperties flowProperties;

    @Test
    void injectsDimensionsConfiguredAtStartup() {
        assertThat(flowProperties.dimensions())
                .extracting(FlowDimension::key, FlowDimension::name, FlowDimension::defaultValue, FlowDimension::required)
                .containsExactly(Tuple.tuple("environment", "Environment", "local", true));
    }

    @Test
    void injectsNestedChildDimensions() {
        FlowDimension environment = flowProperties.dimensions().getFirst();

        assertThat(environment.children())
                .extracting(FlowDimension::key, FlowDimension::name, FlowDimension::required)
                .containsExactly(Tuple.tuple("application", "Application", true));

        FlowDimension application = environment.children().getFirst();

        assertThat(application.children())
                .extracting(
                        FlowDimension::key,
                        FlowDimension::name,
                        FlowDimension::defaultValue,
                        FlowDimension::required)
                .containsExactly(Tuple.tuple("channel", "Channel", "Web", false));
    }

    @Test
    void bindsConfigurationToDimensionInstances() {
        assertThat(flowProperties.configuration())
                .extracting(FlowDimensionInstance::getDimension, FlowDimensionInstance::getKey)
                .containsExactly(Tuple.tuple("environment", "dev"));

        FlowDimensionInstance dev = flowProperties.configuration().getFirst();
        assertThat(dev.getMetadata()).containsEntry("host", "localhost").containsEntry("port", 8080);

        assertThat(dev.getChildren())
                .extracting(
                        FlowDimensionInstance::getDimension,
                        FlowDimensionInstance::getKey,
                        FlowDimensionInstance::getName)
                .containsExactly(Tuple.tuple("application", "customer", "Customer"));

        FlowDimensionInstance customer = dev.getChildren().get(0);
        assertThat(customer.getChildren())
                .extracting(
                        FlowDimensionInstance::getDimension,
                        FlowDimensionInstance::getKey,
                        FlowDimensionInstance::getName)
                .containsExactly(Tuple.tuple("channel", "WEB", "Web"));
    }
}
