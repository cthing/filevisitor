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

import javax.annotation.Nullable;

import org.cthing.annotations.NoCoverageGenerated;


/**
 * Utility methods for working with strings.
 */
final class StringUtils {

    @NoCoverageGenerated
    private StringUtils() {
    }

    /**
     * Indicates whether the two specified string are equal without regard to case. This method accepts {@code null}
     * arguments.
     *
     * @param str1 First string to test
     * @param str2 Second string to test
     * @return {@code true} if the specified string are equal without regard to case.
     */
    @SuppressWarnings("StringEquality")
    public static boolean equalsIgnoreCase(@Nullable final String str1, @Nullable final String str2) {
        if (str1 == str2) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        if (str1.length() != str2.length()) {
            return false;
        }
        return str1.equalsIgnoreCase(str2);
    }

    /**
     * Compares two strings with regard to case. This method accepts {@code null} arguments. A {@code null} value
     * is considered less that a non-{@code null} value. If both strings are {@code null}, they are considered equal.
     *
     * @param str1 First string to compare
     * @param str2 Second string to compare
     * @return Zero if both strings are equal with regard to case. A negative value if the first string is
     *      considered less than the second. A positive value if the first is greater than the second.
     */
    @SuppressWarnings("StringEquality")
    static int compare(@Nullable final String str1, @Nullable final String str2) {
        if (str1 == str2) {
            return 0;
        }
        if (str1 == null) {
            return -1;
        }
        if (str2 == null) {
            return 1;
        }
        return str1.compareTo(str2);
    }

    /**
     * Compares two strings without regard to case. This method accepts {@code null} arguments. A {@code null} value
     * is considered less that a non-{@code null} value. If both strings are {@code null}, they are considered equal.
     *
     * @param str1 First string to compare
     * @param str2 Second string to compare
     * @return Zero if both strings are equal without regard to case. A negative value if the first string is
     *      considered less than the second. A positive value if the first is greater than the second.
     */
    @SuppressWarnings("StringEquality")
    static int compareIgnoreCase(@Nullable final String str1, @Nullable final String str2) {
        if (str1 == str2) {
            return 0;
        }
        if (str1 == null) {
            return -1;
        }
        if (str2 == null) {
            return 1;
        }
        return str1.compareToIgnoreCase(str2);
    }
}
