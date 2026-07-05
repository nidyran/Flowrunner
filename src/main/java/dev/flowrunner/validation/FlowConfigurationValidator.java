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
import dev.flowrunner.properties.FlowDimensionInstance;
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

@Component
@RequiredArgsConstructor
public class FlowConfigurationValidator implements ApplicationRunner {

    private final ObjectProvider<PostLoadConfigurationVisitor> postLoadConfiguration;
    private final ObjectProvider<PreLoadConfigurationVisitor> preLoadConfiguration;
    private final FlowProperties flowProperties;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        preLoadConfiguration.orderedStream().forEach(hook -> hook.preLoadConfiguration(flowProperties.configuration()));

        validate();

        postLoadConfiguration
                .orderedStream()
                .forEach(hook -> hook.postLoadConfiguration(flowProperties.configuration()));
    }

    public void validate() {
        List<String> errors = new ArrayList<>();
        validate(flowProperties.dimensions(), flowProperties.configuration(), Strings.EMPTY, errors);
        if (!errors.isEmpty()) {
            throw new FlowConfigurationValidationException(errors);
        }
    }

    private void validate(
            List<FlowDimension> dimensions, List<FlowDimensionInstance> instances, String path, List<String> errors) {
        if (dimensions == null) {
            return;
        }

        for (FlowDimension dimension : dimensions) {
            String dimensionPath = path + dimension.key();
            List<FlowDimensionInstance> matching = instances.stream()
                    .filter(instance -> dimension.key().equals(instance.getDimension()))
                    .toList();

            if (dimension.required() && matching.isEmpty()) {
                errors.add("Missing required dimension '%s'".formatted(dimensionPath));
            }

            for (FlowDimensionInstance instance : matching) {
                validate(
                        dimension.children(),
                        instance.getChildren(),
                        "%s[%s].".formatted(dimensionPath, instance.getKey()),
                        errors);
            }
        }
    }
}
