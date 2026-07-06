/*
 * MIT License
 *
 * Copyright (c) 2026 Flowrunner Contributors
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
 */
package dev.flowrunner.api;

import dev.flowrunner.handlers.DimensionPattern;
import dev.flowrunner.handlers.FlowRunnerHandler;
import dev.flowrunner.properties.FlowDimension;
import dev.flowrunner.properties.FlowDimensionInstance;
import dev.flowrunner.properties.FlowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@ConditionalOnProperty(name = "flowrunner.ui.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping
public class FlowrunnerController {
    private final FlowProperties flowProperties;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String dashboard(Model model) {
        return "flowrunner";
    }

    @RestController
    @RequiredArgsConstructor
    @RequestMapping("/api/flowrunner")
    public static class FlowrunnerApiController {
        private final FlowProperties flowProperties;
        private final ApplicationContext applicationContext;
        private final ObjectMapper objectMapper;

        @GetMapping("/config")
        public ResponseEntity<Map<String, Object>> getConfiguration() {
            Map<String, Object> config = new LinkedHashMap<>();

            // Dimensions schema
            config.put("dimensions", flowProperties.dimensions().stream()
                    .map(this::dimensionToMap)
                    .collect(Collectors.toList()));

            // Configuration instances
            config.put("configuration", flowProperties.configuration().stream()
                    .map(this::instanceToMap)
                    .collect(Collectors.toList()));

            // Available handlers
            config.put("handlers", getAvailableHandlers());

            return ResponseEntity.ok(config);
        }

        @GetMapping("/dimensions")
        public ResponseEntity<List<Map<String, Object>>> getDimensions() {
            return ResponseEntity.ok(flowProperties.dimensions().stream()
                    .map(this::dimensionToMap)
                    .collect(Collectors.toList()));
        }

        @GetMapping("/configuration")
        public ResponseEntity<List<Map<String, Object>>> getConfigurationInstances() {
            return ResponseEntity.ok(flowProperties.configuration().stream()
                    .map(this::instanceToMap)
                    .collect(Collectors.toList()));
        }

        @GetMapping("/handlers")
        public ResponseEntity<List<Map<String, Object>>> getHandlers() {
            return ResponseEntity.ok(getAvailableHandlers());
        }

        private List<Map<String, Object>> getAvailableHandlers() {
            return applicationContext.getBeansOfType(FlowRunnerHandler.class).values()
                    .stream()
                    .map(handler -> {
                        Map<String, Object> handlerInfo = new LinkedHashMap<>();
                        handlerInfo.put("type", handler.getClass().getSimpleName());
                        handlerInfo.put("name", handler.getClass().getName());
                        String pattern = extractDimensionPattern(handler);
                        handlerInfo.put("supportedDimensionsPattern", pattern);
                        return handlerInfo;
                    })
                    .collect(Collectors.toList());
        }

        private String extractDimensionPattern(FlowRunnerHandler handler) {
            try {
                java.lang.reflect.Method method = handler.getClass()
                        .getMethod("supportedDimensionsPattern");
                Object result = method.invoke(handler);
                return result != null ? result.toString() : ".*";
            } catch (Exception e) {
                return ".*";
            }
        }

        private Map<String, Object> dimensionToMap(FlowDimension dimension) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key", dimension.key());
            map.put("name", dimension.name());
            map.put("defaultValue", dimension.defaultValue());
            map.put("required", dimension.required());
            if (dimension.children() != null && !dimension.children().isEmpty()) {
                map.put("children", dimension.children().stream()
                        .map(this::dimensionToMap)
                        .collect(Collectors.toList()));
            }
            return map;
        }

        private Map<String, Object> instanceToMap(FlowDimensionInstance instance) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("dimension", instance.getDimension());
            map.put("key", instance.getKey());
            map.put("name", instance.getName());
            if (instance.getMetadata() != null && !instance.getMetadata().isEmpty()) {
                map.put("metadata", instance.getMetadata());
            }
            if (instance.getChildren() != null && !instance.getChildren().isEmpty()) {
                map.put("children", instance.getChildren().stream()
                        .map(this::instanceToMap)
                        .collect(Collectors.toList()));
            }
            return map;
        }
    }
}
