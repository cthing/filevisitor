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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;


/**
 * Convenience wrapper around {@link java.nio.file.Files#walkFileTree}.
 */
@SuppressWarnings("ParameterHidesMemberVariable")
public class MatchingTreeWalker {

    private final Path start;
    private final MatchingFileVisitor visitor;
    private int maxDepth;
    private final EnumSet<FileVisitOption> fileVisitOptions;

    public MatchingTreeWalker(final Path start, final MatchHandler matchHandler, final String... matchPatterns) {
        this(start, matchHandler, List.of(matchPatterns));
    }

    public MatchingTreeWalker(final Path start, final MatchHandler matchHandler, final List<String> matchPatterns) {
        this.start = start;
        this.visitor = new MatchingFileVisitor(matchHandler, matchPatterns);
        this.maxDepth = Integer.MAX_VALUE;
        this.fileVisitOptions = EnumSet.allOf(FileVisitOption.class);
    }

    public MatchingTreeWalker excludeHidden(final boolean excludeHidden) {
        this.visitor.excludeHidden(excludeHidden);
        return this;
    }

    public MatchingTreeWalker respectGitignore(final boolean respectGitignore) {
        this.visitor.respectGitignore(respectGitignore);
        return this;
    }

    public MatchingTreeWalker maxDepth(final int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public MatchingTreeWalker followLinks(final boolean followLinks) {
        if (followLinks) {
            this.fileVisitOptions.add(FileVisitOption.FOLLOW_LINKS);
        } else {
            this.fileVisitOptions.remove(FileVisitOption.FOLLOW_LINKS);
        }
        return this;
    }

    public Path walk() throws IOException {
        return Files.walkFileTree(this.start, this.fileVisitOptions, this.maxDepth, this.visitor);
    }
}
