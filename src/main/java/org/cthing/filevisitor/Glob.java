/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.cthing.annotations.AccessForTesting;
import org.jspecify.annotations.Nullable;


/**
 * Represents a glob matching pattern. Though they may look similar, a glob pattern differs from an ignore pattern.
 * An ignore pattern has additional syntax for directory matching and negation.
 */
final class Glob {

    /**
     * Lexical type for parsed glob patterns.
     */
    enum TokenType {
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


    /**
     * Interface implemented by glob pattern matchers.
     */
    @AccessForTesting
    @FunctionalInterface
    interface Matcher {
        /**
         * Indicates whether the specified path matches the pattern represented by this matcher.
         *
         * @param path Path to test
         * @return {@code true} if the specified path matches.
         */
        boolean matches(Path path);
    }


    /**
     * Matches literal strings (e.g. "abc").
     */
    @AccessForTesting
    static class LiteralMatcher implements Matcher {

        private final String literal;

        LiteralMatcher(final String literal) {
            this.literal = literal;
        }

        @Override
        public boolean matches(final Path path) {
            return this.literal.equals(path.toString());
        }

        String getLiteral() {
            return this.literal;
        }
    }


    /**
     * Matches regular expressions.
     */
    @AccessForTesting
    static class RegexMatcher implements Matcher {

        private final Pattern pattern;

        RegexMatcher(final Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(final Path path) {
            return this.pattern.matcher(path.toString()).matches();
        }

        Pattern getPattern() {
            return this.pattern;
        }
    }


    /**
     * Represents a range of characters with an inclusive start and end character (e.g. [a-z]).
     */
    @AccessForTesting
    static class CharRange {

        final char start;
        char end;

        CharRange(final char start, final char end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "(" + this.start + ',' + this.end + ')';
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            final CharRange charRange = (CharRange)obj;
            return this.start == charRange.start && this.end == charRange.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.start, this.end);
        }
    }


    /**
     * A token generated by parsing a glob pattern.
     */
    @AccessForTesting
    static class Token {

        final TokenType type;
        final char ch;
        final boolean negated;
        final List<CharRange> ranges;

        /**
         * Constructs a token with the specified type.
         *
         * @param type Type for the token
         */
        Token(final TokenType type) {
            this.type = type;
            this.ch = 0;
            this.negated = false;
            this.ranges = List.of();
        }

        /**
         * Constructs a literal token representing the specified character.
         *
         * @param ch Literal character
         */
        Token(final char ch) {
            this.type = TokenType.Literal;
            this.ch = ch;
            this.negated = false;
            this.ranges = List.of();
        }

        /**
         * Constructs a token representing a character class.
         *
         * @param negated {@code true} if the token excludes rather than includes the characters in the class
         * @param ranges Characters in the character class
         */
        Token(final boolean negated, final Collection<CharRange> ranges) {
            this.type = TokenType.CharClass;
            this.ch = 0;
            this.negated = negated;
            this.ranges = List.copyOf(ranges);
        }

        @Override
        public String toString() {
            if (this.type == TokenType.Literal) {
                return String.valueOf(this.ch);
            }
            if (this.type == TokenType.CharClass) {
                return "["
                        + (this.negated ? '!' : "")
                        + this.ranges.stream()
                                     .map(CharRange::toString)
                                     .collect(Collectors.joining(","))
                        + "]";
            }
            return this.type.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            final Token token = (Token)obj;
            return this.ch == token.ch
                    && this.negated == token.negated
                    && this.type == token.type
                    && Objects.equals(this.ranges, token.ranges);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.ch, this.negated, this.ranges);
        }
    }


    /**
     * Parses a glob pattern.
     */
    @AccessForTesting
    static class Parser {

        private final StringIterator iterator;
        private final LinkedList<Token> tokens;

        Parser(final String pattern) {
            this.iterator = new StringIterator(pattern);
            this.tokens = new LinkedList<>();
        }

        /**
         * Parses the glob pattern into a list of tokens.
         *
         * @return Tokens parsed from the glob pattern.
         * @throws MatchingException if there is a problem parsing the glob pattern.
         */
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
            if (this.iterator.peekNext() != '*') {
                this.tokens.addLast(new Token(TokenType.ZeroOrMore));
                return;
            }

            final int star = this.iterator.next();
            assert star == '*';

            if (this.tokens.isEmpty()) {
                if (this.iterator.peekNext() == '/' || this.iterator.peekNext() < 0) {
                    this.tokens.addLast(new Token(TokenType.RecursivePrefix));
                    this.iterator.next();
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

    @AccessForTesting
    Matcher getMatcher() {
        return this.matcher;
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

    /**
     * Creates a matcher based on the literal string generated from the specified tokens.
     *
     * @param tokens Parser tokens on which to base the matcher
     * @return Matcher based on the literal string generated from the specified tokens. If
     *      any token does not represent a literal or if case-insensitive matching is specified,
     *      this method will return {@code null}.
     */
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

    /**
     * Creates a matcher based on a regular expression generated from the specified tokens.
     *
     * @param tokens Parser tokens on which to base the matcher
     * @return Matcher based on the regular expression generated by the specified tokens
     * @throws MatchingException if there is a problem creating the matcher.
     */
    private Matcher regexMatcher(final List<Token> tokens) throws MatchingException {
        final Pattern regex = tokensToRegex(tokens);
        return new RegexMatcher(regex);
    }

    /**
     * Generates a regular expression from the specified parser tokens.
     *
     * @param tokens Parser tokens from which to generate a regular expression
     * @return Regular expression based on the specified tokens
     * @throws MatchingException if there is a problem generating the regular expression
     */
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
