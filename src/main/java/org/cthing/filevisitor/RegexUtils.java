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

import org.cthing.annotations.NoCoverageGenerated;


/**
 * Utility methods for working with regular expressions.
 */
final class RegexUtils {

    @NoCoverageGenerated
    private RegexUtils() {
    }

    /**
     * Escapes characters for use in regular expressions.
     *
     * @param ch Character to escape
     * @return If the specified character has special meaning in a regular expression, it is escaped. If the
     *      character is not printable, it is unicode escaped.
     */
    static String escape(final char ch) {
        return switch (ch) {
            case '^', '$', '.', '|', '?', '*', '+', '(', ')', '[', ']', '{', '}' -> "\\" + ch;
            default -> escapeNonPrintable(ch);
        };
    }

    /**
     * Escapes characters for use in regular expression character classes.
     *
     * @param ch Character to escape
     * @return If the specified character has special meaning in a regular expression character class, it is escaped.
     *      If the character is not printable, it is unicode escaped.
     */
    static String escapeCharClass(final char ch) {
        return switch (ch) {
            case '^', '[', ']' -> "\\" + ch;
            default -> escapeNonPrintable(ch);
        };
    }

    /**
     * Escapes non-printable characters.
     *
     * @param ch Character to escape
     * @return If the specified character is not printable, it is unicode escaped.
     */
    private static String escapeNonPrintable(final char ch) {
        return (ch < 32 || ch > 126) ? String.format("\\u%04x", (int)ch) : String.valueOf(ch);
    }
}
