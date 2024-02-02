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
