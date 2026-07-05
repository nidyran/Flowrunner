package dev.flowrunner.handlers;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface FlowRunnerHandler {
    Pattern WORD_PATTERN = Pattern.compile("([A-Z]+(?=[A-Z][a-z]|$)|[A-Z][a-z]+|[a-z]+|\\d+)");

    void run(Map<String, String> parameters);

    String supportedDimensionsPattern();

    /**
     * Returns a user-friendly name for this flow.
     * The default implementation converts the class name to the title case (e.g., "LimitOTPResendsHandler" -> "Limit OTP Resends").
     * Preserves acronyms in uppercase (e.g., "OTP", "HTTP") since they are extracted as-is from the original class name.
     * Implementations can override for custom naming.
     */
    default String friendlyName() {
        return String.join(" ", classNameWords());
    }

    /**
     * Splits this handler's simple class name (with the trailing "Handler" removed)
     * into its constituent words, preserving the original casing of each word so
     * acronyms remain distinguishable from regular words.
     */
    private List<String> classNameWords() {
        String className = this.getClass().getSimpleName();
        if (className.endsWith("Handler")) {
            className = className.substring(0, className.length() - 7);
        }
        if (className.isEmpty()) {
            return Collections.emptyList();
        }
        Matcher matcher = WORD_PATTERN.matcher(className);
        List<String> words = new ArrayList<>();
        while (matcher.find()) {
            if (!matcher.group().isEmpty()) {
                words.add(matcher.group());
            }
        }
        return words;
    }

    String module();

    default String description() {
        return "";
    }

    /**
     * Returns a map of supported parameters for this flow.
     * Key: parameter name
     * value: parameter description
     * Default implementation returns an empty map.
     */
    default Map<String, String> getSupportedParameters() {
        return Collections.emptyMap();
    }
}
