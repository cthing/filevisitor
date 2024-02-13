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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Matching file visitor handler that builds a list of the file and directory paths encountered during a file
 * tree walk.
 */
public class CollectingMatchHandler implements MatchHandler {

    final List<Path> paths;
    final boolean includeDirectories;

    /**
     * Constructs a collecting matcher. By default, directories as well as files are collected.
     */
    public CollectingMatchHandler() {
        this(true);
    }

    /**
     * Constructs a collecting matcher.
     *
     * @param includeDirectories {@code true} to include directories in the collection.
     */
    public CollectingMatchHandler(final boolean includeDirectories) {
        this.paths = new ArrayList<>();
        this.includeDirectories = includeDirectories;
    }

    /**
     * Obtains the files and directories encountered during a file tree walk. Note that the order of files
     * and directories in the list reflects the order in which they were reported by the file system. Therefore,
     * the order may change on subsequent invocations of the walk or when invoked on different machines.
     *
     * @return Files and directories encountered during the file tree walk.
     */
    public List<Path> getPaths() {
        return Collections.unmodifiableList(this.paths);
    }

    @Override
    public boolean file(final Path file, final BasicFileAttributes attrs) throws IOException {
        this.paths.add(file);
        return true;
    }

    @Override
    public boolean directory(final Path dir, final BasicFileAttributes attrs) {
        if (this.includeDirectories) {
            this.paths.add(dir);
        }
        return true;
    }
}
