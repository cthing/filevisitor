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
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.cthing.filevisitor.Glob.CharRange;
import static org.cthing.filevisitor.Glob.Parser;
import static org.cthing.filevisitor.Glob.Token;
import static org.cthing.filevisitor.Glob.TokenType;
import static org.cthing.filevisitor.Glob.TokenType.Any;
import static org.cthing.filevisitor.Glob.TokenType.RecursivePrefix;
import static org.cthing.filevisitor.Glob.TokenType.RecursiveSuffix;
import static org.cthing.filevisitor.Glob.TokenType.RecursiveZeroOrMore;
import static org.cthing.filevisitor.Glob.TokenType.ZeroOrMore;
import static org.junit.jupiter.params.provider.Arguments.arguments;


@SuppressWarnings("UnnecessaryUnicodeEscape")
public class GlobTest {

    @Test
    public void testCharRangeEquality() {
        EqualsVerifier.forClass(CharRange.class)
                      .usingGetClass()
                      .suppress(Warning.NONFINAL_FIELDS)
                      .verify();
    }

    @Test
    public void testTokenEquality() {
        EqualsVerifier.forClass(Token.class)
                      .usingGetClass()
                      .verify();
    }

    public static Stream<Arguments> parseProvider() {
        return Stream.of(
                arguments("a", token('a')),
                arguments("ab", token('a'), token('b')),
                arguments("?", token(Any)),
                arguments("a?b", token('a'), token(Any), token('b')),
                arguments("*", token(ZeroOrMore)),
                arguments("a*b", token('a'), token(ZeroOrMore), token('b')),
                arguments("a\\*b", token('a'), token('*'), token('b')),
                arguments("*a*b*", token(ZeroOrMore), token('a'), token(ZeroOrMore), token('b'), token(ZeroOrMore)),
                arguments("**", token(RecursivePrefix)),
                arguments("a**", token('a'), token(ZeroOrMore), token(ZeroOrMore)),
                arguments("**a", token(ZeroOrMore), token(ZeroOrMore), token('a')),
                arguments("**/", token(RecursivePrefix)),
                arguments("**/a", token(RecursivePrefix), token('a')),
                arguments("**/**/a", token(RecursivePrefix), token('a')),
                arguments("**/**/", token(RecursivePrefix)),
                arguments("**/**/**/", token(RecursivePrefix)),
                arguments("/**", token(RecursiveSuffix)),
                arguments("a/**", token('a'), token(RecursiveSuffix)),
                arguments("a/**/**", token('a'), token(RecursiveSuffix)),
                arguments("/**/**", token(RecursiveSuffix)),
                arguments("/**/**/**", token(RecursiveSuffix)),
                arguments("/**a", token('/'), token(ZeroOrMore), token(ZeroOrMore), token('a')),
                arguments("/**/", token(RecursiveZeroOrMore)),
                arguments("/**/**/", token(RecursiveZeroOrMore)),
                arguments("a/**/b", token('a'), token(RecursiveZeroOrMore), token('b')),
                arguments("[a]", token(false, range('a', 'a'))),
                arguments("[!a]", token(true, range('a', 'a'))),
                arguments("[a-z]", token(false, range('a', 'z'))),
                arguments("[!a-z]", token(true, range('a', 'z'))),
                arguments("[\\!a-z]", token(false, range('\\', '\\'), range('!', '!'), range('a', 'z'))),
                arguments("[-]", token(false, range('-', '-'))),
                arguments("[]]", token(false, range(']', ']'))),
                arguments("[*]", token(false, range('*', '*'))),
                arguments("[!!]", token(true, range('!', '!'))),
                arguments("[a-]", token(false, range('a', 'a'), range('-', '-'))),
                arguments("[-a-z]", token(false, range('-', '-'), range('a', 'z'))),
                arguments("[a-z-]", token(false, range('a', 'z'), range('-', '-'))),
                arguments("[-a-z-]", token(false, range('-', '-'), range('a', 'z'), range('-', '-'))),
                arguments("[]-z]", token(false, range(']', 'z'))),
                arguments("[--z]", token(false, range('-', 'z'))),
                arguments("[ --]", token(false, range(' ', '-'))),
                arguments("[0-9a-z]", token(false, range('0', '9'), range('a', 'z'))),
                arguments("[a-z0-9]", token(false, range('a', 'z'), range('0', '9'))),
                arguments("[!0-9a-z]", token(true, range('0', '9'), range('a', 'z'))),
                arguments("[!a-z0-9]", token(true, range('a', 'z'), range('0', '9'))),
                arguments("[^a]", token(true, range('a', 'a'))),
                arguments("[^a-z]", token(true, range('a', 'z')))
        );
    }

    @ParameterizedTest
    @MethodSource("parseProvider")
    public void testParse(final ArgumentsAccessor accessor) throws MatchingException {
        final String pattern = accessor.getString(0);
        final Token[] tokens = IntStream.range(0, accessor.size() - 1)
                                        .mapToObj(i -> accessor.get(i + 1, Token.class))
                                        .toArray(Token[]::new);
        final Parser parser = new Parser(pattern);
        assertThat(parser.parse()).containsExactly(tokens);
    }

    @ParameterizedTest
    @ValueSource(strings = { "[", "[]", "[!", "[!]", "[z-a]", "[z--]" })
    public void testParseBad(final String pattern) {
        final Parser parser = new Parser(pattern);
        assertThatExceptionOfType(MatchingException.class).isThrownBy(parser::parse);
    }

    @ParameterizedTest
    @ValueSource(strings = { "a", "/a", "/a/", "/a/b", "a/b" })
    public void testLiteralMatcher(final String literal) throws MatchingException {
        final Glob glob = new Glob(literal);
        final Glob.Matcher matcher = glob.getMatcher();
        assertThat(matcher).isInstanceOfSatisfying(Glob.LiteralMatcher.class,
                                                   m ->  assertThat(m.getLiteral()).isEqualTo(literal));
    }

    @ParameterizedTest
    @ValueSource(strings = { "*.a", "**/a/b" })
    public void testNotLiteralMatcher(final String pattern) throws MatchingException {
        final Glob glob = new Glob(pattern);
        final Glob.Matcher matcher = glob.getMatcher();
        assertThat(matcher).isNotInstanceOf(Glob.LiteralMatcher.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "a", "/a/b", "a/b" })
    public void testNotLiteralMatcherCase(final String pattern) throws MatchingException {
        final Glob glob = new Glob(pattern, true);
        final Glob.Matcher matcher = glob.getMatcher();
        assertThat(matcher).isNotInstanceOf(Glob.LiteralMatcher.class);
    }

    public static Stream<Arguments> regexMatcherProvider() {
        return Stream.of(
                arguments("a", true, "(?-u)(?i)^a$"),
                arguments("?", false, "(?-u)^[^/]$"),
                arguments("*", false, "(?-u)^[^/]*$"),
                arguments("a?", false, "(?-u)^a[^/]$"),
                arguments("?a", false, "(?-u)^[^/]a$"),
                arguments("a*", false, "(?-u)^a[^/]*$"),
                arguments("*a", false, "(?-u)^[^/]*a$"),
                arguments("[*]", false, "(?-u)^[*]$"),
                arguments("[+]", false, "(?-u)^[+]$"),
                arguments("+", true, "(?-u)(?i)^\\+$"),
                arguments("\u1234", true, "(?-u)(?i)^\\u1234$"),
                arguments("**", false, "(?-u)^.*$"),
                arguments("**/", false, "(?-u)^.*$"),
                arguments("**/*", false, "(?-u)^(?:/?|.*/)[^/]*$"),
                arguments("**/**", false, "(?-u)^.*$"),
                arguments("**/**/*", false, "(?-u)^(?:/?|.*/)[^/]*$"),
                arguments("**/**/**", false, "(?-u)^.*$"),
                arguments("**/**/**/*", false, "(?-u)^(?:/?|.*/)[^/]*$"),
                arguments("a/**", false, "(?-u)^a/.*$"),
                arguments("a/**/**", false, "(?-u)^a/.*$"),
                arguments("a/**/**/**", false, "(?-u)^a/.*$"),
                arguments("a/**/b", false, "(?-u)^a(?:/|/.*/)b$"),
                arguments("a/**/**/b", false, "(?-u)^a(?:/|/.*/)b$"),
                arguments("a/**/**/**/b", false, "(?-u)^a(?:/|/.*/)b$"),
                arguments("**/b", false, "(?-u)^(?:/?|.*/)b$"),
                arguments("**/**/b", false, "(?-u)^(?:/?|.*/)b$"),
                arguments("**/**/**/b", false, "(?-u)^(?:/?|.*/)b$"),
                arguments("a**", false, "(?-u)^a[^/]*[^/]*$"),
                arguments("**a", false, "(?-u)^[^/]*[^/]*a$"),
                arguments("a**b", false, "(?-u)^a[^/]*[^/]*b$"),
                arguments("***", false, "(?-u)^[^/]*[^/]*[^/]*$"),
                arguments("/a**", false, "(?-u)^/a[^/]*[^/]*$"),
                arguments("/**a", false, "(?-u)^/[^/]*[^/]*a$"),
                arguments("/a**b", false, "(?-u)^/a[^/]*[^/]*b$"),
                arguments("[a-z]", false, "(?-u)^[a-z]$"),
                arguments("[!a-z]", false, "(?-u)^[^a-z]$")
        );
    }

    @ParameterizedTest
    @MethodSource("regexMatcherProvider")
    public void testRegexMatcher(final String pattern, final boolean caseInsensitive, final String regex)
            throws MatchingException {
        final Glob glob = new Glob(pattern, caseInsensitive);
        final Glob.Matcher matcher = glob.getMatcher();
        assertThat(matcher).isInstanceOfSatisfying(Glob.RegexMatcher.class,
                                                   m ->  assertThat(m.getPattern().pattern()).isEqualTo(regex));
    }

    public static Stream<Arguments> matchProvider() {
        return Stream.of(
                arguments("a", "a", false, true),
                arguments("a", "A", true, true),
                arguments("a*b", "a_b", false, true),
                arguments("a*b*c", "abc", false, true),
                arguments("a*b*c", "a_b_c", false, true),
                arguments("a*b*c", "a___b___c", false, true),
                arguments("abc*abc*abc", "abcabcabcabcabcabcabc", false, true),
                arguments("a*a*a*a*a*a*a*a*a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", false, true),
                arguments("a*b[xyz]c*d", "abxcdbxcddd", false, true),
                arguments("*.rs", ".rs", false, true),
                arguments("*.rs", "foo.rs", false, true),
                arguments("\u2603", "\u2603", false, true),
                arguments("some/**/needle.txt", "some/needle.txt", false, true),
                arguments("some/**/needle.txt", "some/one/needle.txt", false, true),
                arguments("some/**/needle.txt", "some/one/two/needle.txt", false, true),
                arguments("some/**/needle.txt", "some/other/needle.txt", false, true),
                arguments("**", "abcde", false, true),
                arguments("**", "", false, true),
                arguments("**", ".asdf", false, true),
                arguments("some/**/**/needle.txt", "some/needle.txt", false, true),
                arguments("some/**/**/needle.txt", "some/one/needle.txt", false, true),
                arguments("some/**/**/needle.txt", "some/one/two/needle.txt", false, true),
                arguments("some/**/**/needle.txt", "some/other/needle.txt", false, true),
                arguments("**/test", "one/two/test", false, true),
                arguments("**/test", "one/test", false, true),
                arguments("**/test", "test", false, true),
                arguments("/**/test", "/one/two/test", false, true),
                arguments("/**/test", "/one/test", false, true),
                arguments("/**/test", "/test", false, true),
                arguments("**/.*", ".abc", false, true),
                arguments("**/.*", "abc/.abc", false, true),
                arguments("**/foo/bar", "foo/bar", false, true),
                arguments(".*/**", ".abc/abc", false, true),
                arguments("test/**", "test/one", false, true),
                arguments("test/**", "test/one/two", false, true),
                arguments("some/*/needle.txt", "some/one/needle.txt", false, true),
                arguments("a[0-9]b", "a0b", false, true),
                arguments("a[0-9]b", "a9b", false, true),
                arguments("a[!0-9]b", "a_b", false, true),
                arguments("[a-z123]", "1", false, true),
                arguments("[1a-z23]", "1", false, true),
                arguments("[123a-z]", "1", false, true),
                arguments("[abc-]", "-", false, true),
                arguments("[-abc]", "-", false, true),
                arguments("[-a-c]", "b", false, true),
                arguments("[a-c-]", "b", false, true),
                arguments("[-]", "-", false, true),
                arguments("a[^0-9]b", "a_b", false, true),
                arguments("*hello.txt", "hello.txt", false, true),
                arguments("*hello.txt", "gareth_says_hello.txt", false, true),
                arguments("*hello.txt", "some\\path\\to\\hello.txt", false, true),
                arguments("*some/path/to/hello.txt", "some/path/to/hello.txt", false, true),
                arguments("_[[]_[]]_[?]_[*]_!_", "_[_]_?_*_!_", false, true),
                arguments("aBcDeFg", "aBcDeFg", true, true),
                arguments("aBcDeFg", "abcdefg", true, true),
                arguments("aBcDeFg", "ABCDEFG", true, true),
                arguments("aBcDeFg", "AbCdEfG", true, true),
                arguments("a,b", "a,b", false, true),
                arguments(",", ",", false, true),
                arguments("\\a", "a", false, true),
                arguments("**", "/x/.asdf", false, true),

                arguments("test/**", "test/", false, false),
                arguments("*hello.txt", "some/path/to/hello.txt", false, false),
                arguments("*hello.txt", "/an/absolute/path/to/hello.txt", false, false),
                arguments("*some/path/to/hello.txt", "a/bigger/some/path/to/hello.txt", false, false),
                arguments("a*b*c", "abcd", false, false),
                arguments("abc*abc*abc", "abcabcabcabcabcabcabca", false, false),
                arguments("some/**/needle.txt", "some/other/notthis.txt", false, false),
                arguments("some/**/**/needle.txt", "some/other/notthis.txt", false, false),
                arguments("/**/test", "test", false, false),
                arguments("/**/test", "/one/notthis", false, false),
                arguments("/**/test", "/notthis", false, false),
                arguments("**/.*", "ab.c", false, false),
                arguments("**/.*", "abc/ab.c", false, false),
                arguments(".*/**", "a.bc", false, false),
                arguments(".*/**", "abc/a.bc", false, false),
                arguments("a[0-9]b", "a_b", false, false),
                arguments("a[!0-9]b", "a0b", false, false),
                arguments("a[!0-9]b", "a9b", false, false),
                arguments("[!-]", "-", false, false),
                arguments("*hello.txt", "hello.txt-and-then-some", false, false),
                arguments("*hello.txt", "goodbye.txt", false, false),
                arguments("*some/path/to/hello.txt", "some/path/to/hello.txt-and-then-some", false, false),
                arguments("*some/path/to/hello.txt", "some/other/path/to/hello.txt", false, false),
                arguments("a", "foo/a", false, false),
                arguments("./foo", "foo", false, false),
                arguments("**/foo", "foofoo", false, false),
                arguments("**/foo/bar", "foofoo/bar", false, false),
                arguments("/*.c", "mozilla-sha1/sha1.c", false, false),
                arguments("*.c", "mozilla-sha1/sha1.c", false, false),
                arguments("**/m4/ltoptions.m4", "csharp/src/packages/repositories.config", false, false),
                arguments("a[^0-9]b", "a0b", false, false),
                arguments("a[^0-9]b", "a9b", false, false),
                arguments("[^-]", "-", false, false),
                arguments("some/*/needle.txt", "some/needle.txt", false, false),
                arguments("some/*/needle.txt", "some/one/two/needle.txt", false, false),
                arguments("some/*/needle.txt", "some/one/two/three/needle.txt", false, false),
                arguments(".*/**", ".abc", false, false),
                arguments("foo/**", "foo", false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("matchProvider")
    public void testMatching(final String pattern, final String path, final boolean caseInsensitive,
                             final boolean matches) throws MatchingException {
        final Glob glob = new Glob(pattern, caseInsensitive);
        assertThat(glob.getPattern()).isEqualTo(pattern);
        assertThat(glob).hasToString(pattern);
        assertThat(glob.matches(Path.of(path))).isEqualTo(matches);
    }

    @Test
    public void testBadEscape() {
        assertThatExceptionOfType(MatchingException.class).isThrownBy(() -> new Glob("abc\\"));
    }

    private static Token token(final TokenType type) {
        return new Token(type);
    }

    private static Token token(final char ch) {
        return new Token(ch);
    }

    private static Token token(final boolean negated, final CharRange... ranges) {
        return new Token(negated, Arrays.asList(ranges));
    }

    private static CharRange range(final char start, final char end) {
        return new CharRange(start, end);
    }
}
