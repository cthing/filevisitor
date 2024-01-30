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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;


/**
 * Called when a directory or file matches the patterns specified to {@link MatchingFileVisitor}.
 */
@FunctionalInterface
public interface MatchHandler {

    boolean file(Path file, BasicFileAttributes attrs) throws IOException;

    default boolean directory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        return true;
    }
}
