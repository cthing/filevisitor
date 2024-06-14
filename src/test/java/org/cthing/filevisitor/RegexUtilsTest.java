/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class RegexUtilsTest {

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public static Stream<Arguments> escapeProvider() {
        return Stream.of(
            arguments('a', "a"),
            arguments('0', "0"),
            arguments(' ', " "),
            arguments('Z', "Z"),
            arguments('\n', "\\u000a"),
            arguments('\u00AE', "\\u00ae"),
            arguments('^', "\\^"),
            arguments('$', "\\$"),
            arguments('.', "\\."),
            arguments('|', "\\|"),
            arguments('?', "\\?"),
            arguments('*', "\\*"),
            arguments('+', "\\+"),
            arguments('(', "\\("),
            arguments(')', "\\)"),
            arguments('[', "\\["),
            arguments(']', "\\]"),
            arguments('{', "\\{"),
            arguments('}', "\\}")
        );
    }

    @ParameterizedTest
    @MethodSource("escapeProvider")
    public void testEscape(final char ch, final String escaped) {
        assertThat(RegexUtils.escape(ch)).isEqualTo(escaped);
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public static Stream<Arguments> escapeClassProvider() {
        return Stream.of(
                arguments('a', "a"),
                arguments('0', "0"),
                arguments(' ', " "),
                arguments('Z', "Z"),
                arguments('\n', "\\u000a"),
                arguments('\u00AE', "\\u00ae"),
                arguments('^', "\\^"),
                arguments('[', "\\["),
                arguments(']', "\\]")
        );
    }

    @ParameterizedTest
    @MethodSource("escapeClassProvider")
    public void testEscapeClass(final char ch, final String escaped) {
        assertThat(RegexUtils.escapeCharClass(ch)).isEqualTo(escaped);
    }
}
