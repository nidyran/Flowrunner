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

import dev.flowrunner.properties.FlowDimension;
import dev.flowrunner.properties.FlowProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class FlowConfigurationValidator implements ApplicationRunner {

    private final ObjectProvider<PostLoadConfiguration> postLoadConfiguration;
    private final ObjectProvider<PreLoadConfiguration> preLoadConfiguration;
    private final FlowProperties flowProperties;
    private final JsonMapper jsonMapper;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        preLoadConfiguration.orderedStream().forEach(hook -> hook.preLoadConfiguration(flowProperties.dimensions()));

        validate();

        postLoadConfiguration
                .orderedStream()
                .forEach(hook -> hook.postLoadConfiguration(flowProperties.dimensions()));
    }

    public void validate() {
        List<String> errors = new ArrayList<>();
        JsonNode configuration = jsonMapper.valueToTree(flowProperties.configuration());
        validate(flowProperties.dimensions(), configuration, Strings.EMPTY, errors);
        if (!errors.isEmpty()) {
            throw new FlowConfigurationValidationException(errors);
        }
    }

    private void validate(List<FlowDimension> dimensions, JsonNode configuration, String path, List<String> errors) {
        if (dimensions == null) {
            return;
        }

        for (FlowDimension dimension : dimensions) {
            String dimensionPath = path + dimension.key();
            JsonNode node = configuration.path(dimension.key());
            boolean hasValue = !node.path("value").isMissingNode() && !node.path("value").isNull();

            if (dimension.required() && !hasValue) {
                errors.add("Missing required dimension '%s'".formatted(dimensionPath));
            }

            validate(dimension.children(), node, dimensionPath + ".", errors);
        }
    }
}
