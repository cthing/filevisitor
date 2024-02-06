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

import javax.annotation.Nullable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class StringUtilsTest {

    public void testToBoolean() {
        assertThat(StringUtils.toBoolean("true")).isTrue();
        assertThat(StringUtils.toBoolean("TRUE")).isTrue();
        assertThat(StringUtils.toBoolean("yes")).isTrue();
        assertThat(StringUtils.toBoolean("Yes")).isTrue();
        assertThat(StringUtils.toBoolean("on")).isTrue();
        assertThat(StringUtils.toBoolean("1")).isTrue();

        assertThat(StringUtils.toBoolean("false")).isFalse();
        assertThat(StringUtils.toBoolean("FALSE")).isFalse();
        assertThat(StringUtils.toBoolean("no")).isFalse();
        assertThat(StringUtils.toBoolean("No")).isFalse();
        assertThat(StringUtils.toBoolean("off")).isFalse();
        assertThat(StringUtils.toBoolean("0")).isFalse();

        assertThatIllegalArgumentException().isThrownBy(() -> StringUtils.toBoolean(""));
        assertThatIllegalArgumentException().isThrownBy(() -> StringUtils.toBoolean("foo"));
        assertThatIllegalArgumentException().isThrownBy(() -> StringUtils.toBoolean("2"));
        assertThatIllegalArgumentException().isThrownBy(() -> StringUtils.toBoolean("fals"));
    }

    public static Stream<Arguments> equalsProvider() {
        final String s = "foo";
        return Stream.of(
                arguments(null, null, true),
                arguments("", "", true),
                arguments(s, s, true),
                arguments("abc", "abc", true),
                arguments("abc", "ABC", true),
                arguments("aBc", "ABC", true),
                arguments(null, "", false),
                arguments("", null, false),
                arguments("abc", null, false),
                arguments(null, "abc", false),
                arguments("abc", "abcd", false),
                arguments("ABC", "abcd", false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalsProvider")
    public void testEqualsIgnoreCase(@Nullable final String s1, @Nullable final String s2, final boolean equal) {
        assertThat(StringUtils.equalsIgnoreCase(s1, s2)).isEqualTo(equal);
    }

    public static Stream<Arguments> compareProvider() {
        final String s = "foo";
        return Stream.of(
                arguments(null, null, 0),
                arguments(null, "", -1),
                arguments("", null, 1),
                arguments("", "", 0),
                arguments("a", "", 1),
                arguments("", "a", -11),
                arguments(s, s, 0),
                arguments("abc", "abc", 0),
                arguments("abc", "ABC", 1),
                arguments("def", "abc", 1),
                arguments("DEF", "abc", -1),
                arguments("abc", "abcd", -1),
                arguments("abcd", "abc", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("compareProvider")
    public void testCompare(@Nullable final String s1, @Nullable final String s2, final int result) {
        if (result == 0) {
            assertThat(StringUtils.compare(s1, s2)).isZero();
        } else if (result < 0) {
            assertThat(StringUtils.compare(s1, s2)).isNegative();
        } else {
            assertThat(StringUtils.compare(s1, s2)).isPositive();
        }
    }

    public static Stream<Arguments> compareInsensitiveProvider() {
        final String s = "foo";
        return Stream.of(
                arguments(null, null, 0),
                arguments(null, "", -1),
                arguments("", null, 1),
                arguments("", "", 0),
                arguments("a", "", 1),
                arguments("", "a", -11),
                arguments(s, s, 0),
                arguments("abc", "abc", 0),
                arguments("abc", "ABC", 0),
                arguments("def", "abc", 1),
                arguments("DEF", "abc", 1),
                arguments("abc", "abcd", -1),
                arguments("abcd", "abc", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("compareInsensitiveProvider")
    public void testCompareIgnoreCase(@Nullable final String s1, @Nullable final String s2, final int result) {
        if (result == 0) {
            assertThat(StringUtils.compareIgnoreCase(s1, s2)).isZero();
        } else if (result < 0) {
            assertThat(StringUtils.compareIgnoreCase(s1, s2)).isNegative();
        } else {
            assertThat(StringUtils.compareIgnoreCase(s1, s2)).isPositive();
        }
    }
}
