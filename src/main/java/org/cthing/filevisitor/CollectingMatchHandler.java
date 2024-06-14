/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
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
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public boolean directory(final Path dir, final BasicFileAttributes attrs) {
        if (this.includeDirectories) {
            this.paths.add(dir);
        }
        return true;
    }
}
