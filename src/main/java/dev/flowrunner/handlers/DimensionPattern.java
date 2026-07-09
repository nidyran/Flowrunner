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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fluent builder for {@link FlowRunnerHandler#supportedDimensionsPattern()} values.
 * A pattern is matched against dot-separated instance-key paths through the
 * configured dimension tree (e.g. {@code dev.customer.WEB}), one segment per
 * dimension level:
 *
 * <pre>
 * // any environment -&gt; customer application -&gt; anything below
 * DimensionPattern.any().with("customer").build();
 *
 * // dev or uat environment -&gt; any application -&gt; WEB channel, exactly that depth
 * DimensionPattern.anyOf("dev", "uat").any().with("WEB").exact().build();
 * </pre>
 *
 * By default a pattern also matches any deeper path below its last segment;
 * end the chain with {@link Builder#exact()} to match that depth only. Keys
 * are quoted, so regex metacharacters in instance keys are matched literally.
 */
public final class DimensionPattern {

    private DimensionPattern() {
    }

    public static Builder any() {
        return new Builder().any();
    }

    public static Builder with(String key) {
        return new Builder().with(key);
    }

    public static Builder anyOf(String... keys) {
        return new Builder().anyOf(keys);
    }

    public static final class Builder {

        private static final String ANY_SEGMENT = "[^.]+";
        private static final String SEGMENT_SEPARATOR = "\\.";
        private static final String ANY_DESCENDANTS = "(?:\\..*)?";

        private final List<String> segments = new ArrayList<>();
        private boolean exact;

        private Builder() {
        }

        public Builder any() {
            segments.add(ANY_SEGMENT);
            return this;
        }

        public Builder with(String key) {
            segments.add(Pattern.quote(key));
            return this;
        }

        public Builder anyOf(String... keys) {
            segments.add(Arrays.stream(keys).map(Pattern::quote).collect(Collectors.joining("|", "(?:", ")")));
            return this;
        }

        /**
         * Restricts the pattern to paths of exactly this depth instead of also
         * matching any deeper path below the last segment.
         */
        public Builder exact() {
            exact = true;
            return this;
        }

        public String build() {
            String joinedSegments = String.join(SEGMENT_SEPARATOR, segments);
            if (exact) {
                return joinedSegments;
            } else {
                return joinedSegments + ANY_DESCENDANTS;
            }
        }
    }
}
