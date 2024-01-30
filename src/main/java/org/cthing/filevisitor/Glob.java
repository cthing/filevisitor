/*
 * Copyright 2024 C Thing Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cthing.filevisitor;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

/**
 * Represents a glob matching pattern. Though they may look similar, a glob pattern differs from an ignore pattern.
 * An ignore pattern has additional syntax for directory matching and negation.
 */
final class Glob {

    private static final String GLOB_SYNTAX = "glob:";

    private final String pattern;
    private final PathMatcher matcher;

    /**
     * Constructs a glob matcher based on the specified pattern.
     *
     * @param pattern Glob pattern for matching
     * @throws MatchingException if there was a problem parsing the pattern
     */
    Glob(final String pattern) throws MatchingException {
        this.pattern = pattern;
        try {
            this.matcher = FileSystems.getDefault().getPathMatcher(GLOB_SYNTAX + pattern);
        } catch (final PatternSyntaxException ex) {
            throw new MatchingException("Could not parse glob pattern: \"" + pattern + "\"", ex);
        }
    }

    /**
     * Obtains the original glob pattern.
     *
     * @return Original glob pattern.
     */
    String getPattern() {
        return this.pattern;
    }

    /**
     * Indicates whether the specified path matches this glob pattern.
     *
     * @param path Path to be matched
     * @return {@code true} if the specified path matches this glob pattern.
     */
    boolean matches(final Path path) {
        return this.matcher.matches(path);
    }

    @Override
    public String toString() {
        return this.pattern;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.pattern, ((Glob)obj).pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pattern);
    }
}
