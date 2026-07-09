package dev.flowrunner.sample;

import dev.flowrunner.handlers.DimensionPattern;
import dev.flowrunner.handlers.FlowRunnerHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author nidhal.ben-yarou on 7/9/2026
 */
@Component
@RequiredArgsConstructor
public class AuthenticationHandler implements FlowRunnerHandler {
    @Override
    public void run(Map<String, String> parameters) {
        // No implementation for now, this flow handler is just for demonstration purposes
    }

    @Override
    public String supportedDimensionsPattern() {
        return DimensionPattern.any().build();
    }

    @Override
    public String module() {
        return "Authentication";
    }
}
