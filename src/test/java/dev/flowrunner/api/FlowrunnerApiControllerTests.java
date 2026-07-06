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
 *   <li>{@code /config} returns the dimensions schema, configuration instances and
 *       registered handlers together</li>
 *   <li>{@code /dimensions} returns the schema tree with nested children</li>
 *   <li>{@code /configuration} returns the instance tree with metadata and nested children</li>
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
    void configReturnsDimensionsConfigurationAndHandlersTogether() {
        Map<String, Object> body = controller.getConfiguration().getBody();

        assertThat(body).containsKeys("dimensions", "configuration", "handlers");
        assertThat((List<?>) body.get("dimensions")).isNotEmpty();
        assertThat((List<?>) body.get("configuration")).isNotEmpty();
        assertThat((List<?>) body.get("handlers")).isNotEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void dimensionsReturnsSchemaTreeWithNestedChildren() {
        List<Map<String, Object>> dimensions = controller.getDimensions().getBody();

        assertThat(dimensions).hasSize(1);
        Map<String, Object> environment = dimensions.getFirst();
        assertThat(environment.get("key")).isEqualTo("environment");
        assertThat(environment.get("defaultValue")).isEqualTo("local");
        assertThat(environment.get("required")).isEqualTo(true);

        List<Map<String, Object>> applicationChildren = (List<Map<String, Object>>) environment.get("children");
        assertThat(applicationChildren).hasSize(1);
        assertThat(applicationChildren.getFirst().get("key")).isEqualTo("application");
    }

    @Test
    @SuppressWarnings("unchecked")
    void configurationReturnsInstanceTreeWithMetadataAndNestedChildren() {
        List<Map<String, Object>> configuration = controller.getConfigurationInstances().getBody();

        assertThat(configuration).hasSize(1);
        Map<String, Object> dev = configuration.getFirst();
        assertThat(dev.get("dimension")).isEqualTo("environment");
        assertThat(dev.get("key")).isEqualTo("dev");
        assertThat((Map<String, Object>) dev.get("metadata")).containsEntry("host", "localhost");

        List<Map<String, Object>> applications = (List<Map<String, Object>>) dev.get("children");
        assertThat(applications).hasSize(1);
        Map<String, Object> customer = applications.getFirst();
        assertThat(customer.get("key")).isEqualTo("customer");

        List<Map<String, Object>> channels = (List<Map<String, Object>>) customer.get("children");
        assertThat(channels).extracting(c -> c.get("key")).containsExactly("WEB");
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
