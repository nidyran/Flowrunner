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
package dev.flowrunner.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Collections;
import java.util.Map;

public interface FlowRunnerHandler {

    void run(Map<String, String> parameters);

    /**
     * Returns the pattern deciding which dimension instances this flow supports.
     * The pattern is a regular expression matched against dot-separated
     * instance-key paths through the configured dimension tree, one segment per
     * dimension level (e.g. {@code dev.customer.WEB} for environment
     * {@code dev} -> application {@code customer} -> channel {@code WEB}).
     * Use {@link DimensionPattern} to build it fluently, e.g.
     * {@code DimensionPattern.any().with("customer").build()} for any
     * environment -> customer application -> anything below.
     */
    String supportedDimensionsPattern();

    /**
     * Returns a user-friendly name for this flow.
     * The default implementation converts the class name to the title case (e.g., "LimitOTPResendsHandler" -> "Limit OTP Resends").
     * Preserves acronyms in uppercase (e.g., "OTP", "HTTP") since they are extracted as-is from the original class name.
     * Implementations can override for custom naming.
     */
    default String friendlyName() {
        String className = Strings.CS.removeEnd(this.getClass().getSimpleName(), "Handler");
        return String.join(" ", StringUtils.splitByCharacterTypeCamelCase(className));
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
