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
package dev.flowrunner.config;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.RequiredArgsConstructor;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@SpringBootTest
@RequiredArgsConstructor
@TestConstructor(autowireMode = AutowireMode.ALL)
class FlowPropertiesTests {

    private final FlowProperties flowProperties;

    @DynamicPropertySource
    static void dimensions(DynamicPropertyRegistry registry) {
        registry.add("flowrunner.flow.dimensions[0].key", () -> "application");
        registry.add("flowrunner.flow.dimensions[0].name", () -> "Application");
        registry.add("flowrunner.flow.dimensions[0].required", () -> true);

        registry.add("flowrunner.flow.dimensions[1].key", () -> "environment");
        registry.add("flowrunner.flow.dimensions[1].name", () -> "Environment");
        registry.add("flowrunner.flow.dimensions[1].defaultValue", () -> "Dev");
        registry.add("flowrunner.flow.dimensions[1].required", () -> true);

        registry.add("flowrunner.flow.dimensions[2].key", () -> "channel");
        registry.add("flowrunner.flow.dimensions[2].name", () -> "Channel");
        registry.add("flowrunner.flow.dimensions[2].defaultValue", () -> "Web");
        registry.add("flowrunner.flow.dimensions[2].required", () -> false);
    }

    @Test
    void injectsDimensionsConfiguredAtStartup() {
        assertThat(flowProperties.dimensions())
                .extracting(
                        FlowDimension::key,
                        FlowDimension::name,
                        FlowDimension::defaultValue,
                        FlowDimension::required)
                .containsExactly(
                        Tuple.tuple("application", "Application", null, true),
                        Tuple.tuple("environment", "Environment", "Dev", true),
                        Tuple.tuple("channel", "Channel", "Web", false));
    }
}
