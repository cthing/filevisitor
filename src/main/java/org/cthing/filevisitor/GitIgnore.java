/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
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

import org.cthing.annotations.AccessForTesting;
import org.jspecify.annotations.Nullable;


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
    @AccessForTesting
    static class Pattern {

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

        boolean isNegated() {
            return this.negated;
        }

        boolean isDirOnly() {
            return this.dirOnly;
        }

        /**
         * Indicates whether the specified path matches this pattern.
         *
         * @param relPath Path relative to the location of the ignore file containing this pattern
         * @return {@code true} if the specified path matches this pattern.
         */
        boolean matches(final Path relPath) {
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


    /**
     * By default, Git is case-sensitive when working with ignore patterns. If the
     * <a href="https://git-scm.com/docs/git-config#Documentation/git-config.txt-coreignoreCase">core.ignoreCase</a>
     * configuration variable is {@code true}, Git will be case-insensitive when matching file patterns. Use the same
     * variable to determine if glob matching in this library will be case-insensitive. As with Git, the default
     * is to be case-sensitive.
     */
    private static final boolean CASE_INSENSITIVE = GitConfig.findGlobalConfig().getBoolean("core", "ignoreCase", false);

    private final Path root;
    private final List<Pattern> patterns;

    /**
     * Constructs a Git ignore based on the contents of the specified ignore file.
     *
     * @param root Path prefix to form relative references
     * @param ignoreFile Git ignore file to read
     * @throws MatchingException if there was a problem reading the specified Git ignore file.
     */
    GitIgnore(final Path root, final Path ignoreFile) throws MatchingException {
        this(root);

        parse(ignoreFile);

        // According to the gitignore documentation: "within one level of precedence,
        // the last matching pattern decides the outcome".
        Collections.reverse(this.patterns);
    }

    /**
     * Constructs a Git ignore based on the specified ignore patterns.
     *
     * @param root Path prefix to form relative references
     * @param ignorePatterns Git ignore patterns to parse
     * @throws MatchingException if there was a problem parsing the specified ignore patterns.
     */
    GitIgnore(final Path root, final List<String> ignorePatterns) throws MatchingException {
        this(root);

        for (final String ignoreLine : ignorePatterns) {
            parse(ignoreLine);
        }

        // According to the gitignore documentation: "within one level of precedence,
        // the last matching pattern decides the outcome".
        Collections.reverse(this.patterns);
    }

    private GitIgnore(final Path root) {
        this.root = PathUtils.removePrefix("./", root);
        this.patterns = new ArrayList<>();
    }

    @AccessForTesting
    List<Pattern> getPatterns() {
        return Collections.unmodifiableList(this.patterns);
    }

    /**
     * Attempts to match the specified path with the patterns in this object.
     *
     * @param path Path to match against the patterns in this object
     * @param isDir {@code true} if the specified path should be treated as a directory.
     *      {@code false} if the specified path should be treated as a file.
     * @return One of the following will be returned:
     *      <ul>
     *          <li>NONE - if no pattern matches the specified path</li>
     *          <li>ALLOW - if a pattern matches the specified path and the path must be allowed</li>
     *          <li>IGNORE - if a pattern matches the specified path and the path must be ignored</li>
     *      </ul>
     */
    @SuppressWarnings("Convert2streamapi")
    MatchResult matches(final Path path, final boolean isDir) {
        if (this.patterns.isEmpty()) {
            return MatchResult.NONE;
        }

        final Path preparedPath = preparePath(path, this.root);

        for (final Pattern pattern : this.patterns) {
            if (pattern.matches(preparedPath) && (!pattern.isDirOnly() || isDir)) {
                return pattern.isNegated() ? MatchResult.ALLOW : MatchResult.IGNORE;
            }
        }

        return MatchResult.NONE;
    }

    /**
     * Provides the global Git ignore patterns.
     *
     * @return Global Git ignore patterns or {@code null} if no global ignore patterns are found. The global
     *      ignore file is search for according to the <a href="https://git-scm.com/docs/gitignore">gitignore</a>
     *      documentation.
     * @throws MatchingException if there is a problem reading the global ignore file.
     */
    @Nullable
    static GitIgnore findGlobalIgnore() throws MatchingException {
        final String excludesFile = GitConfig.findGlobalConfig().getString("core", "excludesFile");
        if (excludesFile == null) {
            return null;
        }

        final Path excludePath = Path.of(GitUtils.expandTilde(excludesFile));
        final Path parent = excludePath.getParent();
        assert parent != null;
        return new GitIgnore(parent, excludePath);
    }

    /**
     * Parses the specified ignore file into ignore patterns for use in matching.
     *
     * @param ignoreFile Git ignore file to parse
     * @throws MatchingException if there is a problem parsing the file.
     */
    private void parse(final Path ignoreFile) throws MatchingException {
        try (BufferedReader reader = Files.newBufferedReader(ignoreFile, StandardCharsets.UTF_8)) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                parse(line);
            }
        } catch (final IOException ex) {
            throw new MatchingException("Could not open ignore file: " + ignoreFile, ex);
        }
    }

    /**
     * Parses the specified ignore file line into an ignore pattern for use in matching.
     *
     * @param line Git ignore file line to parse
     * @throws MatchingException if there is a problem parsing the line.
     */
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

    /**
     * Trims trailing whitespace from the specified string if that whitespace is not escaped. In Git
     * ignore files, escaped trailing whitespace is preserved.
     *
     * @param str String whose trailing whitespace is to be removed
     * @return String with unescaped trailing whitespace removed.
     */
    @AccessForTesting
    static String trimTrailing(final String str) {
        if (str.isEmpty()) {
            return str;
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
     * @param rootPath Path prefix to remove to create relative paths
     * @return Path ready for relative pattern matching.
     */
    @AccessForTesting
    static Path preparePath(final Path path, final Path rootPath) {
        // A leading "./" is unnecessary. It has already been removed from the gitignore root path,
        // and must be removed from the specified path as well. In addition, remove the root path
        // from the specified path so that relative matching works.
        final Path preparedPath = PathUtils.removePrefix("./", path);

        // Remove the root path from the specified path so that relative matching works.
        return PathUtils.removePrefix(rootPath, preparedPath);
    }
}
