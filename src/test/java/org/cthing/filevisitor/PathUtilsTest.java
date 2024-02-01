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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class PathUtilsTest {

    @Test
    public void testRemovePrfixString() {
        final Path path = Path.of("foo", "bar", "joe.java");
        assertThat(PathUtils.removePrefix("foo", path)).isEqualTo(Path.of("bar", "joe.java"));
        assertThat(PathUtils.removePrefix("foo/bar", path)).isEqualTo(Path.of("joe.java"));
        assertThat(PathUtils.removePrefix("foo/bar/joe.java", path)).isEqualTo(path);
        assertThat(PathUtils.removePrefix("foo/bar/zoo/joe.java", path)).isEqualTo(path);
        assertThat(PathUtils.removePrefix("fo", path)).isEqualTo(path);
        assertThat(PathUtils.removePrefix("foo/ba", path)).isEqualTo(path);
    }

    @Test
    public void testRemovePrfixPath() {
        final Path path = Path.of("foo", "bar", "joe.java");
        assertThat(PathUtils.removePrefix(Path.of("foo"), path)).isEqualTo(Path.of("bar", "joe.java"));
        assertThat(PathUtils.removePrefix(Path.of("foo", "bar"), path)).isEqualTo(Path.of("joe.java"));
        assertThat(PathUtils.removePrefix(path, path)).isEqualTo(path);
        assertThat(PathUtils.removePrefix(Path.of("foo", "bar", "zoo"), path)).isEqualTo(path);
        assertThat(PathUtils.removePrefix(Path.of("fo"), path)).isEqualTo(path);
        assertThat(PathUtils.removePrefix(Path.of("foo/ba"), path)).isEqualTo(path);
    }
}
