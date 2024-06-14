/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;


@SuppressWarnings("ResultOfMethodCallIgnored")
class FileTreeExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    @SuppressWarnings("unused")
    static class FileTree {
        final Path root = Files.createTempDirectory("filevisitor");
        final Path linkRoot = Files.createTempDirectory("filevisitor");

        final Path gitDir = createDir(this.root, ".git");
        final Path gitIgnore1 = createFile(this.root, ".gitignore");
        final Path dir1a = createDir(this.root, "dir1a");
        final Path dir1b = createDir(this.root, "dir1b");
        final Path dir1c = createDir(this.root, "dir1c");

        final Path file1a = createFile(this.dir1a, "file1a");
        final Path file1b = createFile(this.dir1a, "file1b.txt");
        final Path file1c = createFile(this.dir1b, "file1c.txt");
        final Path file1d = createFile(this.dir1c, "file1d.foo");
        final Path file1e = createFile(this.dir1c, "file1e.bar");
        final Path file1f = createFile(this.dir1c, "file1f.bar");

        final Path dir2a = createDir(this.dir1a, "dir2a");
        final Path dir2b = createDir(this.dir1a, "dir2b");
        final Path dir2c = createDir(this.dir1b, "dir2c");
        final Path dir2d = createDir(this.dir1c, "dir2d");
        final Path dir2e = createDir(this.dir1c, "dir2e");

        final Path gitIgnore2 = createFile(this.dir2a, ".gitignore");

        final Path file2a = createFile(this.dir2a, "file2a.java");
        final Path file2b = createFile(this.dir2a, "file2b.txt");
        final Path file2c = createFile(this.dir2d, "file2c.cpp");
        final Path file2d = createFile(this.dir2d, "file2d.cpp");
        final Path file2e = createFile(this.dir2e, "file2e.txt");
        final Path file2f = createFile(this.dir2e, "file2f.txt");
        final Path file2g = createFile(this.dir2e, "file2g");

        final Path link2a = Files.createSymbolicLink(this.dir2b.resolve("link2a"), this.linkRoot);

        final Path dir3a = createDir(this.dir2b, "dir3a");
        final Path dir3b = createDir(this.dir2b, "dir3b");
        final Path dir3c = createDir(this.dir2e, "dir3c");
        final Path dir3d = createDir(this.dir2c, ".dir3d");

        final Path file3a = createFile(this.dir3b, "file3a.java");
        final Path file3b = createFile(this.dir3b, "file3b.c");
        final Path file3c = createFile(this.dir3d, "file3c.cpp");
        final Path file3d = createFile(this.dir3c, "file3d.txt");
        final Path file3e = createFile(this.dir3d, "file3e.txt");
        final Path file3f = createFile(this.dir3d, "file3f.sh");

        final Path linkDir1Actual = createDir(this.linkRoot, "linkDir1");
        final Path linkDir1 = this.dir2b.resolve("link2a/linkDir1");
        final Path linkDir2Actual = createDir(this.linkRoot, "linkDir2");
        final Path linkDir2 = this.dir2b.resolve("link2a/linkDir2");

        final Path filel1laActual = createFile(this.linkDir1Actual, "filel1la");
        final Path filel1la = this.linkDir1.resolve("filel1la");
        final Path filel1lbActual = createFile(this.linkDir1Actual, "filel1lb");
        final Path filel1lb = this.linkDir1.resolve("filel1lb");
        final Path filel1lcActual = createFile(this.linkDir2Actual, "filel1lc");
        final Path filel1lc = this.linkDir2.resolve("filel1lc");

        FileTree() throws IOException {
        }

        private static Path createDir(final Path parent, final String subdir) throws IOException {
            final Path dir = parent.resolve(subdir);
            return Files.createDirectories(dir);
        }

        private static Path createFile(final Path parent, final String filename) throws IOException {
            final Path file = parent.resolve(filename);
            return Files.createFile(file);
        }
    }

    @SuppressWarnings("NotNullFieldNotInitialized")
    private FileTree fileTreeContext;

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        this.fileTreeContext = new FileTree();
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) throws Exception {
        deleteAll(this.fileTreeContext.root);
        deleteAll(this.fileTreeContext.linkRoot);
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext,
                                     final ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(FileTree.class);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext,
                                   final ExtensionContext extensionContext) throws ParameterResolutionException {
        return this.fileTreeContext;
    }

    private static void deleteAll(final Path root) throws IOException {
        if (Files.exists(root)) {
            try (Stream<Path> fileStream = Files.walk(root)) {
                fileStream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }
}
