/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class StringIteratorTest {

    @Test
    public void testWithContent() {
        final StringIterator iter = new StringIterator("abc");

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.hasPrev()).isFalse();
        assertThat(iter.peekNext()).isEqualTo('a');
        assertThat(iter.peekPrev()).isEqualTo(-1);
        assertThat(iter.next()).isEqualTo('a');
        assertThat(iter.peekNext()).isEqualTo('b');
        assertThat(iter.peekPrev()).isEqualTo(-1);
        assertThat(iter.prev()).isEqualTo(-1);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.hasPrev()).isFalse();
        assertThat(iter.next()).isEqualTo('a');

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.hasPrev()).isTrue();
        assertThat(iter.next()).isEqualTo('b');
        assertThat(iter.peekNext()).isEqualTo('c');
        assertThat(iter.peekPrev()).isEqualTo('a');
        assertThat(iter.prev()).isEqualTo('a');

        assertThat(iter.next()).isEqualTo('b');
        assertThat(iter.next()).isEqualTo('c');
        assertThat(iter.peekNext()).isEqualTo(-1);
        assertThat(iter.peekPrev()).isEqualTo('b');
        assertThat(iter.hasNext()).isFalse();
        assertThat(iter.hasPrev()).isTrue();

        assertThat(iter.next()).isEqualTo(-1);
        assertThat(iter.hasNext()).isFalse();
        assertThat(iter.hasPrev()).isTrue();
    }

    @Test
    public void testEmpty() {
        final StringIterator iter = new StringIterator("");

        assertThat(iter.hasNext()).isFalse();
        assertThat(iter.hasPrev()).isFalse();
        assertThat(iter.peekNext()).isEqualTo(-1);
        assertThat(iter.peekPrev()).isEqualTo(-1);
        assertThat(iter.next()).isEqualTo(-1);
        assertThat(iter.peekNext()).isEqualTo(-1);
        assertThat(iter.peekPrev()).isEqualTo(-1);
        assertThat(iter.prev()).isEqualTo(-1);
    }

    @Test
    public void testReset() {
        final StringIterator iter = new StringIterator("abc");

        assertThat(iter.next()).isEqualTo('a');
        assertThat(iter.next()).isEqualTo('b');
        iter.reset();
        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.hasPrev()).isFalse();
        assertThat(iter.peekNext()).isEqualTo('a');
        assertThat(iter.peekPrev()).isEqualTo(-1);
        assertThat(iter.next()).isEqualTo('a');
    }
}
