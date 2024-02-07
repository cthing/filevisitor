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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;


/**
 * Represents a collection of ignore patterns in the
 * <a href="https://git-scm.com/docs/gitignore">gitignore format</a>. Typically, the patterns are read from a file
 * called {@code .gitignore}.
 */
final class GitIgnore {

    /**
     * Represents a match pattern from an ignore file. An ignore pattern consists of a glob matching pattern, the
     * location where the pattern was found, and additional information parsed from the ignore pattern.
     */
    private static class Pattern {

        private final String pattern;
        private final Glob glob;
        private final boolean negated;
        private final boolean dirOnly;

        Pattern(final String pattern, final Glob glob, final boolean negated, final boolean dirOnly) {
            this.pattern = pattern;
            this.glob = glob;
            this.negated = negated;
            this.dirOnly = dirOnly;
        }

        public boolean isNegated() {
            return this.negated;
        }

        public boolean isDirOnly() {
            return this.dirOnly;
        }

        /**
         * Indicates whether the specified path matches this pattern.
         *
         * @param relPath Path relative to the location of the ignore file containing this pattern
         * @return {@code true} if the specified path matches this pattern.
         */
        public boolean matches(final Path relPath) {
            return this.glob.matches(relPath);
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
            return Objects.equals(this.pattern, ((Pattern)obj).pattern);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.pattern);
        }
    }

    enum MatchResult {
        IGNORE,
        ALLOW,
        NONE
    }

    private static final boolean CASE_INSENSITIVE = GitConfig.CONFIG.getBoolean("core", "ignoreCase", false);

    private final Path root;
    private final List<Pattern> patterns;

    GitIgnore(final Path root, final Path ignoreFile) throws MatchingException {
        this(root);

        parse(ignoreFile);
    }

    GitIgnore(final Path root, final List<String> ignoreLines) throws MatchingException {
        this(root);

        for (final String ignoreLine : ignoreLines) {
            parse(ignoreLine);
        }
    }

    private GitIgnore(final Path root) {
        this.root = PathUtils.removePrefix("./", root);
        this.patterns = new ArrayList<>();
    }

    @SuppressWarnings("Convert2streamapi")
    MatchResult matches(final Path path) {
        if (this.patterns.isEmpty()) {
            return MatchResult.NONE;
        }

        final boolean isDir = Files.isDirectory(path);
        final Path preparedPath = preparePath(path);

        for (final Pattern pattern : this.patterns) {
            if (pattern.matches(preparedPath) && (!pattern.isDirOnly() || isDir)) {
                return pattern.isNegated() ? MatchResult.ALLOW : MatchResult.IGNORE;
            }
        }

        return MatchResult.NONE;
    }

    @Nullable
    static GitIgnore findGlobalIgnore() throws MatchingException {
        final String excludesFile = GitConfig.CONFIG.getString("core", "excludesFile");
        if (excludesFile == null) {
            return null;
        }

        final Path excludePath = Path.of(GitUtils.expandTilde(excludesFile));
        final Path parent = excludePath.getParent();
        assert parent != null;
        return new GitIgnore(parent, excludePath);
    }

    private void parse(final Path ignoreFile) throws MatchingException {
        try (BufferedReader reader = Files.newBufferedReader(ignoreFile, StandardCharsets.UTF_8)) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                parse(line);
            }

            // According to the gitignore documentation: "within one level of precedence,
            // the last matching pattern decides the outcome".
            Collections.reverse(this.patterns);
        } catch (final IOException ex) {
            throw new MatchingException("Could not open ignore file: " + ignoreFile, ex);
        }
    }

    private void parse(final String line) throws MatchingException {
        // Comment line
        if (line.startsWith("#")) {
            return;
        }

        // Trim trailing whitespace, if it is not escaped
        String trimmedLine = trimTrailing(line);
        if (trimmedLine.isEmpty()) {
            return;
        }

        boolean negated = false;
        boolean dirOnly = false;
        boolean absolute = false;

        if (line.startsWith("\\!") || line.startsWith("\\#")) {
            trimmedLine = trimmedLine.substring(1);
            absolute = trimmedLine.charAt(0) == '/';
        } else {
            if (trimmedLine.startsWith("!")) {
                negated = true;
                trimmedLine = trimmedLine.substring(1);
            }
            if (line.startsWith("/")) {
                // According to the gitignore documentation, if a pattern starts with a slash, it can only
                // match the beginning of a path (relative to the location of gitignore). This is accomplished
                // by disallowing wildcards from matching a slash.
                trimmedLine = trimmedLine.substring(1);
                absolute = true;
            }
        }

        // If the pattern ends with a slash, it should only match directories. Otherwise, the slash should
        // not be used while globbing.
        if (trimmedLine.endsWith("/")) {
            dirOnly = true;
            trimmedLine = trimmedLine.substring(0, trimmedLine.length() - 1);
            // If the slash is escaped, remove the escape to avoid a dangling escape.
            if (trimmedLine.endsWith("\\")) {
                trimmedLine = trimmedLine.substring(0, trimmedLine.length() - 1);
            }
        }

        String globStr = trimmedLine;

        // If there is a literal slash, it is a glob that must match the entire path name.
        // Otherwise, let it match anywhere by using a "**/" prefix.
        if (!absolute && !trimmedLine.contains("/")) {
            // Prepend only if the pattern does not already contain a **/ prefix.
            if (!globStr.startsWith("**/") && !"**".equals(globStr)) {
                globStr = "**/" + globStr;
            }
        }

        // If the pattern ends with "/**", it should only match everything inside a directory, but not the
        // directory itself. Standard globs will match the directory, so add "/*" to force the match.
        if (globStr.endsWith("/**")) {
            globStr += "/*";
        }

        final Glob glob = new Glob(globStr, CASE_INSENSITIVE);
        final Pattern pattern = new Pattern(line, glob, negated, dirOnly);
        this.patterns.add(pattern);
    }

    private String trimTrailing(final String str) {
        if (str.isEmpty()) {
            return "";
        }

        for (int i = str.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(str.charAt(i)) || (i > 0 && str.charAt(i - 1) == '\\')) {
                return (i == str.length() - 1) ? str : str.substring(0, i + 1);
            }
        }

        return "";
    }

    /**
     * Prepares the specified path for relative pattern matching.
     *
     * @param path Path to be prepared
     * @return Path ready for relative pattern matching.
     */
    private Path preparePath(final Path path) {
        // A leading "./" is unnecessary. It has already been removed from the gitignore root path,
        // and must be removed from the specified path as well. In addition, remove the root path
        // from the specified path so that relative matching works.
        final Path preparedPath = PathUtils.removePrefix("./", path);

        // Remove the root path from the specified path so that relative matching works.
        return PathUtils.removePrefix(this.root, preparedPath);
    }
}
