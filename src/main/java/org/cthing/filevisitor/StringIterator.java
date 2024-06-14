/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

/**
 * Provides forward and reverse iteration over a string. In addition, it is possible to peek at the next and
 * previous characters.
 */
class StringIterator {

    private final String str;
    private int pos;

    /**
     * Constructs an iterator over the specified string.
     *
     * @param str String on which to iterate
     */
    StringIterator(final String str) {
        this.str = str;
        this.pos = 0;
    }

    /**
     * Indicates whether the iterator has reached the last character.
     *
     * @return {@code true} if the iterator has not reached the last character.
     */
    boolean hasNext() {
        return this.pos < this.str.length();
    }

    /**
     * Indicates whether the iterator has reached the first character.
     *
     * @return {@code true} if the iterator has not reached the first character.
     */
    boolean hasPrev() {
        return this.pos > 0;
    }

    /**
     * Obtains the current character and advances the iterator.
     *
     * @return Current character or -1 if the iterator is already at the last character.
     */
    int next() {
        return hasNext() ? this.str.charAt(this.pos++) : -1;
    }

    /**
     * Moves the iterator back one character and returns it.
     *
     * @return Previous character or -1 if the iterator is already at the first character.
     */
    int prev() {
        if (hasPrev()) {
            this.pos--;
            return hasPrev() ? this.str.charAt(this.pos - 1) : -1;
        }
        return -1;
    }

    /**
     * Obtains the current character without advancing the iterator.
     *
     * @return Current character or -1 if the iterator is already at the last character.
     */
    int peekNext() {
        return hasNext() ? this.str.charAt(this.pos) : -1;
    }

    /**
     * Obtains the previous character without moving the iterator back.
     *
     * @return Previous character or -1 if the iterator is already at the first character.
     */
    int peekPrev() {
        return this.pos > 1 ? this.str.charAt(this.pos - 2) : -1;
    }

    /**
     * Resets the iterator to the start of the string.
     */
    void reset() {
        this.pos = 0;
    }
}
