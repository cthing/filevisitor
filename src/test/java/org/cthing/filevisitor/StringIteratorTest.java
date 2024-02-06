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
