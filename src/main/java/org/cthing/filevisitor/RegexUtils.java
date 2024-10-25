/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
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
     *      character is not printable, it is Unicode escaped.
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
     *      If the character is not printable, it is Unicode escaped.
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
     * @return If the specified character is not printable, it is Unicode escaped.
     */
    private static String escapeNonPrintable(final char ch) {
        return (ch < 32 || ch > 126) ? String.format("\\u%04x", (int)ch) : String.valueOf(ch);
    }
}
