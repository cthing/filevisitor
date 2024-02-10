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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nullable;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;


/**
 * Performs pattern matching on the files in a file tree.
 */
@SuppressWarnings("ParameterHidesMemberVariable")
public final class MatchingFileVisitor implements FileVisitor<Path> {

    private static final class Context {
        boolean workTree;
        final List<GitIgnore> ignores;

        Context() {
            this.ignores = new ArrayList<>();
        }
    }

    private final MatchHandler handler;
    private final List<String> matchPatterns;
    private final Deque<Context> contextStack;
    @Nullable
    private GitIgnore matcher;
    private final List<GitIgnore> baseIgnores;
    private boolean excludeHidden;
    private boolean respectGitignore;

    /**
     * Constructs a file system tree visitor.
     *
     * @param matchHandler Handler whose methods will be called when a file or directory is matched
     * @param matchPatterns Glob patterns to match files and directories.
     *      See <a href="https://git-scm.com/docs/gitignore">git-ignore</a> for the format of these patterns.
     *      Note that these patterns include files and directories rather than excluding them. As with Git
     *      ignore files, patterns later in the list are matched first.
     */
    public MatchingFileVisitor(final MatchHandler matchHandler, final String... matchPatterns) {
        this(matchHandler, List.of(matchPatterns));
    }

    /**
     * Constructs a file system tree visitor.
     *
     * @param matchHandler Handler whose methods will be called when a file or directory is matched
     * @param matchPatterns Glob patterns to match files and directories.
     *      See <a href="https://git-scm.com/docs/gitignore">git-ignore</a> for the format of these patterns.
     *      Note that these patterns include files and directories rather than excluding them. As with Git
     *      ignore files, patterns later in the list are matched first.
     */
    public MatchingFileVisitor(final MatchHandler matchHandler, final List<String> matchPatterns) {
        this.handler = matchHandler;
        this.matchPatterns = matchPatterns;
        this.contextStack = new ArrayDeque<>();
        this.baseIgnores = new ArrayList<>();
        this.excludeHidden = true;
        this.respectGitignore = true;
    }

    /**
     * Specifies whether to exclude hidden files during the visit.
     *
     * @param excludeHidden {@code true} to exclude hidden files
     * @return This visitor
     */
    public MatchingFileVisitor excludeHidden(final boolean excludeHidden) {
        this.excludeHidden = excludeHidden;
        return this;
    }

    /**
     * Specifies whether to honor Git ignore files to exclude files and directories from the visit. The default is
     * {@code false}, which means to not honor Git ignore files.
     *
     * @param respectGitignore {@code true} to honor git ignore files during the walk
     * @return This walker
     */
    public MatchingFileVisitor respectGitignore(final boolean respectGitignore) {
        this.respectGitignore = respectGitignore;
        return this;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        boolean workTree = false;

        if (this.contextStack.isEmpty()) {
            this.matcher = this.matchPatterns.isEmpty() ? null : new GitIgnore(dir, this.matchPatterns);

            if (this.respectGitignore) {
                final List<GitIgnore> ancesterIgnores = new ArrayList<>();
                Path ancesterDir = dir.getParent();
                while (ancesterDir != null) {
                    final Path ignoreFile = GitUtils.getGitignoreFile(ancesterDir);
                    if (ignoreFile != null) {
                        ancesterIgnores.add(new GitIgnore(ancesterDir, ignoreFile));
                    }

                    if (GitUtils.containsGitDir(ancesterDir)) {
                        workTree = true;

                        final Path excludeFile = GitUtils.getExcludeFile(ancesterDir);
                        if (excludeFile != null) {
                            ancesterIgnores.add(new GitIgnore(ancesterDir, excludeFile));
                        }

                        break;
                    }

                    ancesterDir = ancesterDir.getParent();
                }

                this.baseIgnores.addAll(ancesterIgnores);

                final GitIgnore globalIgnore = GitIgnore.findGlobalIgnore();
                if (globalIgnore != null) {
                    this.baseIgnores.add(globalIgnore);
                }
            }
        }

        final Context context = new Context();

        if (this.respectGitignore) {
            final Path ignoreFile = GitUtils.getGitignoreFile(dir);
            if (ignoreFile != null) {
                context.ignores.add(new GitIgnore(dir, ignoreFile));
            }

            if (GitUtils.containsGitDir(dir)) {
                workTree = true;

                final Path excludeFile = GitUtils.getExcludeFile(dir);
                if (excludeFile != null) {
                    context.ignores.add(new GitIgnore(dir, excludeFile));
                }
            }

            final Context currentContext = this.contextStack.peekFirst();
            if (currentContext != null) {
                context.ignores.addAll(currentContext.ignores);

                if (currentContext.workTree) {
                    workTree = true;
                }
            }

            context.workTree = workTree;
        }

        boolean allowed = false;

        if (this.respectGitignore) {
            if (this.matcher != null && this.matcher.matches(dir, true) != GitIgnore.MatchResult.IGNORE) {
                return SKIP_SUBTREE;
            }
            if (context.workTree) {
                for (final GitIgnore ignore : context.ignores) {
                    final GitIgnore.MatchResult result = ignore.matches(dir, true);
                    if (result == GitIgnore.MatchResult.IGNORE) {
                        return SKIP_SUBTREE;
                    }
                    if (result == GitIgnore.MatchResult.ALLOW) {
                        allowed = true;
                    }
                }
                for (final GitIgnore ignore : this.baseIgnores) {
                    final GitIgnore.MatchResult result = ignore.matches(dir, true);
                    if (result == GitIgnore.MatchResult.IGNORE) {
                        return SKIP_SUBTREE;
                    }
                    if (result == GitIgnore.MatchResult.ALLOW) {
                        allowed = true;
                    }
                }
            }
        }

        if (Files.isHidden(dir) && this.excludeHidden && !allowed) {
            return SKIP_SUBTREE;
        }

        if (!this.handler.directory(dir, attrs)) {
            this.contextStack.clear();
            return TERMINATE;
        }

        this.contextStack.push(context);

        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        final Context currentContext = this.contextStack.peekFirst();
        assert currentContext != null;

        if (this.matcher != null && this.matcher.matches(file, false) != GitIgnore.MatchResult.IGNORE) {
            return CONTINUE;
        }

        boolean allowed = false;

        if (this.respectGitignore) {
            if (currentContext.workTree) {
                for (final GitIgnore ignore : currentContext.ignores) {
                    final GitIgnore.MatchResult result = ignore.matches(file, false);
                    if (result == GitIgnore.MatchResult.IGNORE) {
                        return CONTINUE;
                    }
                    if (result == GitIgnore.MatchResult.ALLOW) {
                        allowed = true;
                    }
                }
                for (final GitIgnore ignore : this.baseIgnores) {
                    final GitIgnore.MatchResult result = ignore.matches(file, false);
                    if (result == GitIgnore.MatchResult.IGNORE) {
                        return CONTINUE;
                    }
                    if (result == GitIgnore.MatchResult.ALLOW) {
                        allowed = true;
                    }
                }
            }
        }

        if (Files.isHidden(file) && this.excludeHidden && !allowed) {
            return CONTINUE;
        }

        if (!this.handler.file(file, attrs)) {
            this.contextStack.clear();
            return TERMINATE;
        }

        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, @Nullable final IOException exc) throws IOException {
        this.contextStack.pop();

        if (exc != null) {
            throw exc;
        }

        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        throw exc;
    }
}
