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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nullable;

import org.cthing.annotations.AccessForTesting;


/**
 * Represents a glob matching pattern. Though they may look similar, a glob pattern differs from an ignore pattern.
 * An ignore pattern has additional syntax for directory matching and negation.
 */
final class Glob {

    /**
     * Lexical type for parsed glob patterns.
     */
    private enum TokenType {
        /** A character literal (e.g. "a"). */
        Literal,
        /** Any single character (i.e. "?"). */
        Any,
        /** Zero or more characters (i.e. "*"). */
        ZeroOrMore,
        /** Any directory path prefix (i.e. "{@literal **}/"). */
        RecursivePrefix,
        /** Any directory path suffix (i.e. "/{@literal **}"). */
        RecursiveSuffix,
        /** Any directory path within a pattern (e.g. "a/{@literal **}/b"). */
        RecursiveZeroOrMore,
        /** A character class (e.g. [f-h]). */
        CharClass
    }

    @FunctionalInterface
    private interface Matcher {
        boolean matches(Path path);
    }

    private static class LiteralMatcher implements Matcher {

        private final String literal;

        LiteralMatcher(final String literal) {
            this.literal = literal;
        }

        @Override
        public boolean matches(final Path path) {
            return this.literal.equals(path.toString());
        }
    }

    private static class RegexMatcher implements Matcher {

        private final Pattern pattern;

        RegexMatcher(final Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(final Path path) {
            return this.pattern.matcher(path.toString()).matches();
        }
    }

    private static class CharRange {

        char start;
        char end;

        CharRange(final char start, final char end) {
            this.start = start;
            this.end = end;


        }

        @Override
        public String toString() {
            return "[" + this.start + '-' + this.end + ']';
        }
    }

    @AccessForTesting
    static class Token {

        final TokenType type;
        final char ch;
        final boolean negated;
        final List<CharRange> ranges;

        Token(final TokenType type) {
            this.type = type;
            this.ch = 0;
            this.negated = false;
            this.ranges = List.of();
        }

        Token(final char ch) {
            this.type = TokenType.Literal;
            this.ch = ch;
            this.negated = false;
            this.ranges = List.of();
        }

        Token(final boolean negated, final List<CharRange> ranges) {
            this.type = TokenType.CharClass;
            this.ch = 0;
            this.negated = negated;
            this.ranges = ranges;
        }
    }

    @AccessForTesting
    static class Parser {

        private final StringIterator iterator;
        private final LinkedList<Token> tokens;

        Parser(final String pattern) {
            this.iterator = new StringIterator(pattern);
            this.tokens = new LinkedList<>();
        }

        List<Token> parse() throws MatchingException {
            for (int ch = this.iterator.next(); ch != -1; ch = this.iterator.next()) {
                switch (ch) {
                    case '?':
                        this.tokens.addLast(new Token(TokenType.Any));
                        break;
                    case '*':
                        parseStar();
                        break;
                    case '[':
                        parseCharClass();
                        break;
                    case '\\':
                        parseBackslash();
                        break;
                    default:
                        this.tokens.addLast(new Token((char)ch));
                        break;
                }
            }

            return Collections.unmodifiableList(this.tokens);
        }

        private void parseStar() {
            final int prev = this.iterator.peekPrev();
            if (this.iterator.peekNext() == '*') {
                this.tokens.addLast(new Token(TokenType.ZeroOrMore));
                return;
            }

            final int star = this.iterator.next();
            assert star == '*';

            if (this.tokens.isEmpty()) {
                if (this.iterator.peekNext() == '/') {
                    this.tokens.addLast(new Token(TokenType.RecursivePrefix));
                    final int sep = this.iterator.next();
                    assert sep == '/';
                } else {
                    this.tokens.addLast(new Token(TokenType.ZeroOrMore));
                    this.tokens.addLast(new Token(TokenType.ZeroOrMore));
                }
                return;
            }

            if (prev != '/') {
                this.tokens.addLast(new Token(TokenType.ZeroOrMore));
                this.tokens.addLast(new Token(TokenType.ZeroOrMore));
                return;
            }

            final boolean suffix;
            final int nextCh = this.iterator.peekNext();
            if (nextCh < 0) {
                this.iterator.next();
                suffix = true;
            } else if (nextCh == '/') {
                this.iterator.next();
                suffix = false;
            } else {
                this.tokens.addLast(new Token(TokenType.ZeroOrMore));
                this.tokens.addLast(new Token(TokenType.ZeroOrMore));
                return;
            }

            final Token removedToken = this.tokens.removeLast();
            if (removedToken.type == TokenType.RecursivePrefix) {
                this.tokens.addLast(removedToken);
            } else if (removedToken.type == TokenType.RecursiveSuffix) {
                this.tokens.addLast(removedToken);
            } else {
                if (suffix) {
                    this.tokens.addLast(new Token(TokenType.RecursiveSuffix));
                } else {
                    this.tokens.addLast(new Token(TokenType.RecursiveZeroOrMore));
                }
            }
        }

        private void parseCharClass() throws MatchingException {
            final List<CharRange> ranges = new ArrayList<>();

            final int nextCh = this.iterator.peekNext();
            final boolean negated;
            if (nextCh == '!' || nextCh == '^') {
                final int ch = this.iterator.next();
                assert ch == '!' || ch == '^';
                negated = true;
            } else {
                negated = false;
            }

            boolean first = true;
            boolean inRange = false;
            loop: while (true) {
                final int ch = this.iterator.next();
                if (ch < 0) {
                    throw new MatchingException("Character class not closed");
                }

                switch (ch) {
                    case ']':
                        if (first) {
                            ranges.add(new CharRange(']', ']'));
                        } else {
                            break loop;
                        }
                        break;
                    case '-':
                        if (first) {
                            ranges.add(new CharRange('-', '-'));
                        } else if (inRange) {
                            final CharRange r = ranges.get(ranges.size() - 1);
                            r.end = '-';
                            if (r.end < r.start) {
                                throw new MatchingException("Invalid character range: " + r);
                            }
                            inRange = false;
                        } else {
                            assert !ranges.isEmpty();
                            inRange = true;
                        }
                        break;
                    default:
                        if (inRange) {
                            final CharRange r = ranges.get(ranges.size() - 1);
                            r.end = (char)ch;
                            if (r.end < r.start) {
                                throw new MatchingException("Invalid character range: " + r);
                            }
                        } else {
                            ranges.add(new CharRange((char)ch, (char)ch));
                        }
                        inRange = false;
                        break;
                }

                first = false;
            }

            if (inRange) {
                ranges.add(new CharRange('-', '-'));
            }

            this.tokens.addLast(new Token(negated, ranges));
        }

        private void parseBackslash() throws MatchingException {
            final int ch = this.iterator.next();
            if (ch < 0) {
                throw new MatchingException("Incomplete escape");
            }
            this.tokens.addLast(new Token((char)ch));
        }
    }

    private final String pattern;
    private final Matcher matcher;
    private final boolean caseInsensitive;

    /**
     * Constructs a glob matcher based on the specified pattern.
     *
     * @param pattern Glob pattern for matching
     * @throws MatchingException if there was a problem parsing the pattern
     */
    Glob(final String pattern) throws MatchingException {
        this(pattern, false);
    }

    /**
     * Constructs a glob matcher based on the specified pattern.
     *
     * @param pattern Glob pattern for matching
     * @param caseInsensitive {@code true} if matches should be case-insensitive
     * @throws MatchingException if there was a problem parsing the pattern
     */
    Glob(final String pattern, final boolean caseInsensitive) throws MatchingException {
        this.pattern = pattern;
        this.caseInsensitive = caseInsensitive;

        final Parser parser = new Parser(this.pattern);
        final List<Token> tokens = parser.parse();

        Matcher m = literalMatcher(tokens);
        if (m == null) {
            m = regexMatcher(tokens);
        }
        this.matcher = m;
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

    @Nullable
    private Matcher literalMatcher(final List<Token> tokens) {
        if (this.caseInsensitive) {
            return null;
        }

        final StringBuilder buffer = new StringBuilder();
        for (final Token token : tokens) {
            if (token.type != TokenType.Literal) {
                return null;
            }
            buffer.append(token.ch);
        }
        return buffer.isEmpty() ? null : new LiteralMatcher(buffer.toString());
    }

    private Matcher regexMatcher(final List<Token> tokens) throws MatchingException {
        final Pattern regex = tokensToRegex(tokens);
        return new RegexMatcher(regex);
    }

    private Pattern tokensToRegex(final List<Token> tokens) throws MatchingException {
        final StringBuilder buffer = new StringBuilder();

        // Disable Unicode-aware case folding
        buffer.append("(?-u)");

        if (this.caseInsensitive) {
            buffer.append("(?i)");
        }

        buffer.append('^');

        // If the entire glob is "**", match everything.
        if (tokens.size() == 1 && tokens.get(0).type == TokenType.RecursivePrefix) {
            buffer.append(".*$");
            return Pattern.compile(buffer.toString());
        }

        for (final Token token : tokens) {
            switch (token.type) {
                case Literal -> buffer.append(RegexUtils.escape(token.ch));
                case Any -> buffer.append("[^/]");
                case ZeroOrMore -> buffer.append("[^/]*");
                case RecursivePrefix -> buffer.append("(?:/?|.*/)");
                case RecursiveSuffix -> buffer.append("/.*");
                case RecursiveZeroOrMore -> buffer.append("(?:/|/.*/)");
                case CharClass -> {
                    buffer.append('[');
                    if (token.negated) {
                        buffer.append('^');
                    }
                    for (final CharRange range : token.ranges) {
                        if (range.start == range.end) {
                            buffer.append(RegexUtils.escapeCharClass(range.start));
                        } else {
                            buffer.append(RegexUtils.escapeCharClass(range.start))
                                  .append('-')
                                  .append(RegexUtils.escapeCharClass(range.end));
                        }
                    }
                    buffer.append(']');
                }
                default -> { }
            }
        }

        buffer.append('$');

        try {
            return Pattern.compile(buffer.toString());
        } catch (final PatternSyntaxException ex) {
            throw new MatchingException("Could not create regular expression for pattern: " + this.pattern, ex);
        }
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
