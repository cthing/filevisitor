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

import org.cthing.annotations.NoCoverageGenerated;


/**
 * Utility methods for working with {@link java.nio.file.Path} instances.
 */
final class PathUtils {

    static final String HOME_DIR = System.getProperty("user.home");
    static final Path HOME_PATH = Path.of(HOME_DIR);

    @NoCoverageGenerated
    private PathUtils() {
    }

    /**
     * Removes the specified prefix from the specified path.
     *
     * @param prefix Prefix to remove from the path. The prefix must represent a complete path segment. For example,
     *      if the specified path represents "foo/bar/joe.java", a valid prefix is "foo" or "foo/bar" but not "fo"
     *      or "foo/ba". Specifying an invalid prefix will result in the original path being returned unchanged.
     * @param path Path whose prefix is to be removed
     * @return Path with prefix removed or the original path if the prefix was not found or if the full path would
     *      be replaced.
     */
    static Path removePrefix(final String prefix, final Path path) {
        return removePrefix(Path.of(prefix), path);
    }

    /**
     * Removes the specified prefix from the specified path.
     *
     * @param prefix Prefix to remove from the path.
     * @param path Path whose prefix is to be removed
     * @return Path with prefix removed or the original path if the prefix was not found or if the full path would
     *      be replaced.
     */
    static Path removePrefix(final Path prefix, final Path path) {
        final int prefixCount = prefix.getNameCount();
        final int pathCount = path.getNameCount();
        return (path.startsWith(prefix) && prefixCount < pathCount) ? path.subpath(prefixCount, pathCount) : path;
    }
}
