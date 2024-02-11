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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import org.cthing.filevisitor.FileTreeExtension.FileTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;


@ExtendWith({FileTreeExtension.class, TestHomeExtension.class})
@SuppressWarnings("FieldMayBeFinal")
public class MatchingFileVisitorTest {

    private CollectingMatchHandler handler = new CollectingMatchHandler();

    @Nested
    @DisplayName("Plane traversal")
    class PlaneTravesalTest {

        @Test
        @DisplayName("Without Git, with hidden, no follow, without patterns")
        public void testPlane1(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            visitor.excludeHidden(false);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir3d,
                    fileTree.file3c,
                    fileTree.file3f,
                    fileTree.file3e,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2b,
                    fileTree.gitIgnore2,
                    fileTree.file2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.file3a,
                    fileTree.link2a,
                    fileTree.gitIgnore1,
                    fileTree.gitDir
            );
        }

        @Test
        @DisplayName("Without Git, without hidden, no follow, without patterns")
        public void testPlane2(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            visitor.excludeHidden(true);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2b,
                    fileTree.file2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.file3a,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Without Git, with hidden, follow links, without patterns")
        public void testPlane3(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            visitor.excludeHidden(false);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, Set.of(FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir3d,
                    fileTree.file3c,
                    fileTree.file3f,
                    fileTree.file3e,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2b,
                    fileTree.gitIgnore2,
                    fileTree.file2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.file3a,
                    fileTree.link2a,
                    fileTree.linkDir2,
                    fileTree.filel1lc,
                    fileTree.linkDir1,
                    fileTree.filel1la,
                    fileTree.filel1lb,
                    fileTree.gitIgnore1,
                    fileTree.gitDir
            );
        }

        @Test
        @DisplayName("Limit depth")
        public void testPlane4(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            visitor.excludeHidden(false);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, Set.of(), 1, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.gitIgnore1
            );
        }
    }

    @Nested
    @DisplayName("Pattern tests")
    class PatternTest {

        @Test
        @DisplayName("Literal pattern")
        public void testPattern1(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler,
                                                                        "file2d.cpp");
            visitor.excludeHidden(true);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.file2d
            );
        }

        @Test
        @DisplayName("Literal patterns")
        public void testPattern2(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler,
                                                                        "file2d.cpp", "dir2b");
            visitor.excludeHidden(true);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.file2d,
                    fileTree.dir2b
            );
        }

        @Test
        @DisplayName("Extension patterns")
        public void testPattern3(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler,
                                                                        "*.java", "*.cpp");
            visitor.excludeHidden(true);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.file2a,
                    fileTree.file3a
            );
        }

        @Test
        @DisplayName("Recursive patterns")
        public void testPattern4(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler,
                                                                        "**/dir2d/**");
            visitor.excludeHidden(true);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.file2d,
                    fileTree.file2c
            );
        }

        @Test
        @DisplayName("Negated patterns")
        public void testPattern5(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler,
                                                                        "**", "!**/dir2d");
            visitor.excludeHidden(true);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2b,
                    fileTree.file2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.file3a,
                    fileTree.link2a
            );
        }
    }

    @Nested
    @DisplayName("Git ignore tests")
    class GitTest {

        @Test
        @DisplayName("No ignores")
        public void testGit1(final FileTree fileTree) throws IOException {
            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2b,
                    fileTree.file2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.file3a,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Root ignore")
        public void testGit2(final FileTree fileTree) throws IOException {
            Files.writeString(fileTree.gitIgnore1, "**/dir2a/**");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.file3a,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Subdirectory ignore")
        public void testGit3(final FileTree fileTree) throws IOException {
            Files.writeString(fileTree.gitIgnore2, "*.txt\n!file2b.txt");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2b,
                    fileTree.file2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.file3a,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Root and subdirectory ignore")
        public void testGit4(final FileTree fileTree) throws IOException {
            Files.writeString(fileTree.gitIgnore1, "**/dir3b/**");
            Files.writeString(fileTree.gitIgnore2, "*.txt");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Root ignore with allow")
        public void testGit5(final FileTree fileTree) throws IOException {
            Files.writeString(fileTree.gitIgnore1, "*.txt\n!**/file2b.txt");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2b,
                    fileTree.file2a,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.file3a,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Parent ignore")
        public void testGit6(final FileTree fileTree) throws IOException {
            Files.writeString(fileTree.gitIgnore1, "*.java\n!file3a.java");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.dir2b, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.file3a,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Root exclude file")
        public void testGit7(final FileTree fileTree) throws IOException {
            final Path infoDir = Files.createDirectory(fileTree.gitDir.resolve("info"));
            final Path excludeFile = Files.createFile(infoDir.resolve("exclude"));
            Files.writeString(excludeFile, "**/dir2a/**\n**/dir3b\n!**/file2a.java");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Ancestor root exclude file")
        public void testGit8(final FileTree fileTree) throws IOException {
            final Path infoDir = Files.createDirectory(fileTree.gitDir.resolve("info"));
            final Path excludeFile = Files.createFile(infoDir.resolve("exclude"));
            Files.writeString(excludeFile, "**/dir2a/**\n**/dir3b");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.dir2b, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Global exclude file")
        public void testGit9(final FileTree fileTree) throws IOException {
            final Path globalDir = Files.createDirectories(TestHomeExtension.TEST_HOME.resolve("global"));
            final Path ignoreFile = Files.createFile(globalDir.resolve("config"));
            Files.writeString(ignoreFile, "**/dir2a/**\n*.c\n!file3b.c\nfile3a.java");

            final Path configFile = Files.createFile(TestHomeExtension.TEST_HOME.resolve(".gitconfig"));
            Files.writeString(configFile, "[core]\nexcludesFile = " + ignoreFile);

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.dir2b, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Root ignore directory with allow")
        public void testGit10(final FileTree fileTree) throws IOException {
            Files.writeString(fileTree.gitIgnore1, "**/dir2b\n*.txt\n!file2e.txt");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file2e,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2a,
                    fileTree.file1a
            );
        }

        @Test
        @DisplayName("Root ignore")
        public void testGit11(final FileTree fileTree) throws IOException {
            Files.writeString(fileTree.gitIgnore1, "*.java\n!file2a.java");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(MatchingFileVisitorTest.this.handler.getPaths()).containsExactlyInAnyOrder(
                    fileTree.root,
                    fileTree.dir1c,
                    fileTree.file1e,
                    fileTree.dir2d,
                    fileTree.file2d,
                    fileTree.file2c,
                    fileTree.dir2e,
                    fileTree.file2g,
                    fileTree.dir3c,
                    fileTree.file3d,
                    fileTree.file2e,
                    fileTree.file2f,
                    fileTree.file1f,
                    fileTree.file1d,
                    fileTree.dir1b,
                    fileTree.file1c,
                    fileTree.dir2c,
                    fileTree.dir1a,
                    fileTree.dir2a,
                    fileTree.file2b,
                    fileTree.file2a,
                    fileTree.file1b,
                    fileTree.file1a,
                    fileTree.dir2b,
                    fileTree.dir3a,
                    fileTree.dir3b,
                    fileTree.file3b,
                    fileTree.link2a
            );
        }

        @Test
        @DisplayName("Bad ignore")
        public void testGit12(final FileTree fileTree) throws IOException {
            Files.writeString(fileTree.gitIgnore1, "[a-z");

            final MatchingFileVisitor visitor = new MatchingFileVisitor(MatchingFileVisitorTest.this.handler);
            assertThatExceptionOfType(MatchingException.class)
                    .isThrownBy(() -> Files.walkFileTree(fileTree.root, visitor));
        }
    }

    @Nested
    @DisplayName("Termination tests")
    class TerminationTest {

        static class TerminatingFileMatchHandler extends CollectingMatchHandler {

            private final Path terminatingFile;

            TerminatingFileMatchHandler(final Path terminatingFile) {
                this.terminatingFile = terminatingFile;
            }

            @Override
            public boolean file(final Path file, final BasicFileAttributes attrs) throws IOException {
                super.file(file, attrs);
                return !file.equals(this.terminatingFile);
            }
        }

        static class TerminatingDirMatchHandler extends CollectingMatchHandler {

            private final Path terminatingDir;

            TerminatingDirMatchHandler(final Path terminatingDir) {
                this.terminatingDir = terminatingDir;
            }

            @Override
            public boolean directory(final Path dir, final BasicFileAttributes attrs) {
                super.directory(dir, attrs);
                return !dir.equals(this.terminatingDir);
            }
        }

        static class ExceptionFileMatchHandler implements MatchHandler {

            private final Path terminatingFile;

            ExceptionFileMatchHandler(final Path terminatingFile) {
                this.terminatingFile = terminatingFile;
            }

            @Override
            public boolean file(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (file.equals(this.terminatingFile)) {
                    throw new IOException("Expected exception");
                }
                return true;
            }
        }

        static class ExceptionDirMatchHandler implements MatchHandler {

            private final Path terminatingDir;

            ExceptionDirMatchHandler(final Path terminatingDir) {
                this.terminatingDir = terminatingDir;
            }

            @Override
            public boolean file(final Path file, final BasicFileAttributes attrs) throws IOException {
                return true;
            }

            @Override
            public boolean directory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                if (dir.equals(this.terminatingDir)) {
                    throw new IOException("Expected exception");
                }
                return true;
            }
        }

        @Test
        @DisplayName("Terminate by file")
        public void testTerminateByFile(final FileTree fileTree) throws IOException {
            final TerminatingFileMatchHandler termHandler = new TerminatingFileMatchHandler(fileTree.file2b);
            final MatchingFileVisitor visitor = new MatchingFileVisitor(termHandler);
            visitor.excludeHidden(false);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(termHandler.getPaths()).contains(fileTree.file2b).doesNotContain(fileTree.dir3a);
        }

        @Test
        @DisplayName("Terminate by file")
        public void testTerminateByDir(final FileTree fileTree) throws IOException {
            final TerminatingDirMatchHandler termHandler = new TerminatingDirMatchHandler(fileTree.dir2b);
            final MatchingFileVisitor visitor = new MatchingFileVisitor(termHandler);
            visitor.excludeHidden(false);
            visitor.respectGitignore(false);
            Files.walkFileTree(fileTree.root, visitor);
            assertThat(termHandler.getPaths()).contains(fileTree.dir2b).doesNotContain(fileTree.dir3a);
        }

        @Test
        @DisplayName("Terminate by file")
        public void testExceptionByFile(final FileTree fileTree) {
            final ExceptionFileMatchHandler termHandler = new ExceptionFileMatchHandler(fileTree.file2b);
            final MatchingFileVisitor visitor = new MatchingFileVisitor(termHandler);
            visitor.excludeHidden(false);
            visitor.respectGitignore(false);
            assertThatIOException().isThrownBy(() -> Files.walkFileTree(fileTree.root, visitor))
                                   .withMessageContaining("Expected");
        }

        @Test
        @DisplayName("Terminate by file")
        public void testExceptionByDir(final FileTree fileTree) {
            final ExceptionDirMatchHandler termHandler = new ExceptionDirMatchHandler(fileTree.dir2b);
            final MatchingFileVisitor visitor = new MatchingFileVisitor(termHandler);
            visitor.excludeHidden(false);
            visitor.respectGitignore(false);
            assertThatIOException().isThrownBy(() -> Files.walkFileTree(fileTree.root, visitor))
                                   .withMessageContaining("Expected");
        }
    }
}
