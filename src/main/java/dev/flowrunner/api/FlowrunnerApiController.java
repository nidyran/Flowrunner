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

import dev.flowrunner.handlers.FlowRunnerHandler;
import dev.flowrunner.properties.FlowDimension;
import dev.flowrunner.properties.FlowDimensionInstance;
import dev.flowrunner.properties.FlowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flowrunner")
public class FlowrunnerApiController {
    // TODO: claude: create a new service, and use it instead of these two
    private final FlowProperties flowProperties;
    // TODO: claude: replace application context with injection of ObjectProvider instead ( in the new service )
    private final ApplicationContext applicationContext;

    // TODO: claude both APIs should call the new service to get the response
    @GetMapping("/config")
    public ResponseEntity<FlowProperties> getConfiguration() {
        return ResponseEntity.ok(flowProperties);
    }

    @GetMapping("/handlers")
    public ResponseEntity<List<Map<String, Object>>> getHandlers() {
        return ResponseEntity.ok(getAvailableHandlers());
    }

    /**
     * TODO: claude: move this to a dedicate serivce to list and map the response in the desired format.
     */
    private List<Map<String, Object>> getAvailableHandlers() {
        return applicationContext.getBeansOfType(FlowRunnerHandler.class).values()
                .stream()
                .map(handler -> {
                    Map<String, Object> handlerInfo = new LinkedHashMap<>();
                    handlerInfo.put("type", handler.getClass().getSimpleName());
                    handlerInfo.put("name", handler.getClass().getName());
                    handlerInfo.put("friendlyName", handler.friendlyName());
                    handlerInfo.put("module", handler.module());
                    handlerInfo.put("supportedParameters", handler.getSupportedParameters());
                    handlerInfo.put("supportedDimensionsPattern", handler.supportedDimensionsPattern());
                    return handlerInfo;
                })
                .toList();
    }
}
