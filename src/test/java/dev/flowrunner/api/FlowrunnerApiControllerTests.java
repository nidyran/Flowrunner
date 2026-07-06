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
package dev.flowrunner.api;

import static org.assertj.core.api.Assertions.assertThat;

import dev.flowrunner.handlers.FlowRunnerHandler;
import dev.flowrunner.properties.FlowDimension;
import dev.flowrunner.properties.FlowDimensionInstance;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

import java.util.List;
import java.util.Map;

/**
 * Covers {@link FlowrunnerApiController} against the {@code flow-properties-test.yaml}
 * fixture (environment -> application -> channel dimensions):
 * <ul>
 *   <li>{@code /config} returns the bound {@link dev.flowrunner.properties.FlowProperties}
 *       record as-is, with its dimensions schema and configuration instance tree</li>
 *   <li>{@code /handlers} returns type, friendly name, module, supported parameters and
 *       the real {@code supportedDimensionsPattern()} for every registered
 *       {@link FlowRunnerHandler} bean</li>
 * </ul>
 */
@SpringBootTest(properties = "spring.config.location=classpath:/flow-properties-test.yaml")
@RequiredArgsConstructor
@TestConstructor(autowireMode = AutowireMode.ALL)
class FlowrunnerApiControllerTests {

    static class SendSmsHandler implements FlowRunnerHandler {
        @Override
        public void run(Map<String, String> parameters) {
            // no-op: exercises the controller's handler listing only
        }

        @Override
        public String supportedDimensionsPattern() {
            return "dev\\.customer\\..*";
        }

        @Override
        public String module() {
            return "notification";
        }

        @Override
        public Map<String, String> getSupportedParameters() {
            return Map.of("phoneNumber", "Recipient phone number");
        }
    }

    @TestConfiguration
    static class HandlerTestConfig {
        @Bean
        FlowRunnerHandler sendSmsHandler() {
            return new SendSmsHandler();
        }
    }

    private final FlowrunnerApiController controller;

    @Test
    void configReturnsDimensionsSchemaWithNestedChildren() {
        List<FlowDimension> dimensions = controller.getConfiguration().getBody().dimensions();

        assertThat(dimensions).hasSize(1);
        FlowDimension environment = dimensions.getFirst();
        assertThat(environment.key()).isEqualTo("environment");
        assertThat(environment.defaultValue()).isEqualTo("local");
        assertThat(environment.required()).isTrue();

        assertThat(environment.children()).hasSize(1);
        assertThat(environment.children().getFirst().key()).isEqualTo("application");
    }

    @Test
    void configReturnsConfigurationInstanceTreeWithMetadataAndNestedChildren() {
        List<FlowDimensionInstance> configuration = controller.getConfiguration().getBody().configuration();

        assertThat(configuration).hasSize(1);
        FlowDimensionInstance dev = configuration.getFirst();
        assertThat(dev.getDimension()).isEqualTo("environment");
        assertThat(dev.getKey()).isEqualTo("dev");
        assertThat(dev.getMetadata()).containsEntry("host", "localhost");

        assertThat(dev.getChildren()).hasSize(1);
        FlowDimensionInstance customer = dev.getChildren().getFirst();
        assertThat(customer.getKey()).isEqualTo("customer");

        assertThat(customer.getChildren()).extracting(FlowDimensionInstance::getKey).containsExactly("WEB");
    }

    @Test
    void handlersExposeFriendlyNameModuleParametersAndRealDimensionsPattern() {
        List<Map<String, Object>> handlers = controller.getHandlers().getBody();

        assertThat(handlers).hasSize(1);
        Map<String, Object> handler = handlers.getFirst();
        assertThat(handler.get("friendlyName")).isEqualTo("Send Sms");
        assertThat(handler.get("module")).isEqualTo("notification");
        assertThat(handler.get("supportedDimensionsPattern")).isEqualTo("dev\\.customer\\..*");
        assertThat((Map<String, String>) handler.get("supportedParameters"))
                .containsEntry("phoneNumber", "Recipient phone number");
    }
}
