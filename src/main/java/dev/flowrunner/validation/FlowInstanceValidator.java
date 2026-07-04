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
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class FlowInstanceValidator {

    private final FlowProperties flowProperties;
    private final JsonMapper jsonMapper;

    public void validate() {
        List<String> errors = new ArrayList<>();
        JsonNode instance = jsonMapper.valueToTree(flowProperties.instance());
        validate(flowProperties.dimensions(), instance, "", errors);
        if (!errors.isEmpty()) {
            throw new FlowInstanceValidationException(errors);
        }
    }

    private void validate(List<FlowDimension> dimensions, JsonNode instance, String path, List<String> errors) {
        if (dimensions == null) {
            return;
        }

        for (FlowDimension dimension : dimensions) {
            String dimensionPath = path + dimension.key();
            JsonNode node = instance == null ? null : instance.get(dimension.key());
            JsonNode value = node == null ? null : node.get("value");
            boolean hasValue = value != null && !value.isNull();

            if (dimension.required() && !hasValue) {
                errors.add("Missing required dimension '%s'".formatted(dimensionPath));
            }

            validate(dimension.children(), node, dimensionPath + ".", errors);
        }
    }
}
