/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;


/**
 * Called when a directory or file matches the patterns specified to {@link MatchingFileVisitor}.
 */
@FunctionalInterface
@SuppressWarnings("RedundantThrows")
public interface MatchHandler {

    /**
     * Called when a file is matched.
     *
     * @param file File that has been matched
     * @param attrs Attributes of the matched file
     * @return {@code true} to continue matching or {@code false} to terminate matching
     * @throws IOException if the handler method encounters a problem
     */
    boolean file(Path file, BasicFileAttributes attrs) throws IOException;

    /**
     * Called when a directory is matched.
     *
     * @param dir Directory that has been matched
     * @param attrs Attributes of the matched directory
     * @return {@code true} to continue matching or {@code false} to terminate matching
     * @throws IOException if the handler method encounters a problem
     */
    default boolean directory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        return true;
    }
}
