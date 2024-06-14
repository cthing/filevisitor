/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;


/**
 * Convenience wrapper around {@link java.nio.file.Files#walkFileTree} and {@link MatchingFileVisitor} to perform
 * pattern matching on the files in a file tree.
 */
@SuppressWarnings("ParameterHidesMemberVariable")
public class MatchingTreeWalker {

    private final Path start;
    private final MatchingFileVisitor visitor;
    private int maxDepth;
    private final EnumSet<FileVisitOption> fileVisitOptions;

    /**
     * Constructs a file system tree walker.
     *
     * @param start Directory in which to start the walk
     * @param matchHandler Handler whose methods will be called when a file or directory is matched
     * @param matchPatterns Glob patterns to match files and directories.
     *      See <a href="https://git-scm.com/docs/gitignore">git-ignore</a> for the format of these patterns.
     *      Note that these patterns include files and directories rather than excluding them. As with Git
     *      ignore files, patterns later in the list are matched first. If no patterns are specified, all
     *      files and directories are considered a match.
     */
    public MatchingTreeWalker(final Path start, final MatchHandler matchHandler, final String... matchPatterns) {
        this(start, matchHandler, List.of(matchPatterns));
    }

    /**
     * Constructs a file system tree walker.
     *
     * @param start Directory in which to start the walk
     * @param matchHandler Handler whose methods will be called when a file or directory is matched
     * @param matchPatterns Glob patterns to match files and directories.
     *      See <a href="https://git-scm.com/docs/gitignore">git-ignore</a> for the format of these patterns.
     *      Note that these patterns include files and directories rather than excluding them. As with Git
     *      ignore files, patterns later in the list are matched first. If no patterns are specified, all
     *      files and directories are considered a match.
     */
    public MatchingTreeWalker(final Path start, final MatchHandler matchHandler, final List<String> matchPatterns) {
        this.start = start;
        this.visitor = new MatchingFileVisitor(matchHandler, matchPatterns);
        this.maxDepth = Integer.MAX_VALUE;
        this.fileVisitOptions = EnumSet.noneOf(FileVisitOption.class);
    }

    /**
     * Specifies whether to exclude hidden files and directories from the walk. By default, hidden files and
     * directories are excluded.
     *
     * @param excludeHidden {@code true} to exclude hidden files
     * @return This walker
     */
    public MatchingTreeWalker excludeHidden(final boolean excludeHidden) {
        this.visitor.excludeHidden(excludeHidden);
        return this;
    }

    /**
     * Specifies whether to honor Git ignore files to exclude files and directories from the walk. The default is
     * {@code false}, which means to not honor Git ignore files. If enabled, all parent ignore files, and any global
     * ignore file is honored.
     *
     * @param respectGitignore {@code true} to honor git ignore files during the walk
     * @return This walker
     */
    public MatchingTreeWalker respectGitignore(final boolean respectGitignore) {
        this.visitor.respectGitignore(respectGitignore);
        return this;
    }

    /**
     * Specifies a maximum depth for the walk. The default is {@link Integer#MAX_VALUE}, which means that there is
     * no depth limit.
     *
     * @param maxDepth Maximum tree depth for the walk or {@link Integer#MAX_VALUE} for unlimited depth
     * @return This walker
     */
    public MatchingTreeWalker maxDepth(final int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    /**
     * Specifies whether to follow symbolic links. The default is to not follow symbolic links.
     *
     * @param followLinks {@code true} to follow symbolic links
     * @return This walker
     */
    public MatchingTreeWalker followLinks(final boolean followLinks) {
        if (followLinks) {
            this.fileVisitOptions.add(FileVisitOption.FOLLOW_LINKS);
        } else {
            this.fileVisitOptions.remove(FileVisitOption.FOLLOW_LINKS);
        }
        return this;
    }

    /**
     * Performs the walk of the file system tree.
     *
     * @return The starting path of the walk.
     * @throws IOException if a problem was encountered during the walk.
     */
    public Path walk() throws IOException {
        return Files.walkFileTree(this.start, this.fileVisitOptions, this.maxDepth, this.visitor);
    }
}
