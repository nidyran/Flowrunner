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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Raw binding target for {@code flowrunner.flow.configuration}. Each dimension is a list
 * of entries keyed by the dimension value, with a body holding an optional display name,
 * arbitrary metadata and the child dimension lists:
 *
 * <pre>
 * environment:
 *   - dev:
 *       metadata:
 *         host: localhost
 *       application:
 *         - customer:
 *             name: Customer
 * </pre>
 */
public class FlowConfiguration extends LinkedHashMap<String, Object> {

    private static final String NAME = "name";
    private static final String METADATA = "metadata";

    public List<FlowDimensionInstance> instances() {
        return instancesOf(this);
    }

    @SuppressWarnings("unchecked")
    private static List<FlowDimensionInstance> instancesOf(Map<String, Object> dimensions) {
        List<FlowDimensionInstance> instances = new ArrayList<>();
        dimensions.forEach((key, entries) -> ((Map<String, Map<String, Object>>) entries)
                .values()
                .forEach(entry -> entry.forEach(
                        (value, body) -> instances.add(instanceOf(key, value, (Map<String, Object>) body)))));
        return List.copyOf(instances);
    }

    @SuppressWarnings("unchecked")
    private static FlowDimensionInstance instanceOf(String key, String value, Map<String, Object> body) {
        Map<String, Object> children = new LinkedHashMap<>(Objects.requireNonNullElse(body, Map.of()));
        String name = (String) children.remove(NAME);
        Map<String, Object> metadata = (Map<String, Object>) Objects.requireNonNullElse(children.remove(METADATA), Map.of());
        return new FlowDimensionInstance(key, value, name, Map.copyOf(metadata), instancesOf(children));
    }
}
