/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cthing.filevisitor.TestHomeExtension.TEST_HOME;
import static org.cthing.filevisitor.TestHomeExtension.TEST_HOME_DIR;


@ExtendWith(TestHomeExtension.class)
public class GitUtilsTest {

    @Nested
    class FindGlobalConfigFileTest {
        @Test
        public void testNoConfig() {
            assertThat(GitUtils.findGlobalConfigFile()).isNull();
        }

        @Test
        public void testHomeConfig() throws IOException {
            final Path configFile = Files.createFile(TEST_HOME.resolve(".gitconfig"));
            assertThat(GitUtils.findGlobalConfigFile()).isEqualTo(configFile);
        }

        @Test
        public void testXdgConfigHome() throws IOException {
            final Path xdgDir = Files.createDirectories(TEST_HOME.resolve("xdg"));
            final Path configDir = Files.createDirectories(xdgDir.resolve("git"));
            final Path configFile = Files.createFile(configDir.resolve("config"));
            assertThat(GitUtils.findGlobalConfigFile()).isEqualTo(configFile);
        }
    }

    @Test
    public void testContainsGitDir() throws IOException {
        assertThat(GitUtils.containsGitDir(TEST_HOME)).isFalse();

        Files.createDirectories(TEST_HOME.resolve(".git"));
        assertThat(GitUtils.containsGitDir(TEST_HOME)).isTrue();
    }

    @Test
    public void testGetExcludeFile() throws IOException {
        assertThat(GitUtils.getExcludeFile(TEST_HOME)).isNull();

        final Path infoDir = Files.createDirectories(TEST_HOME.resolve(".git/info"));
        final Path excludeFile = Files.createFile(infoDir.resolve("exclude"));
        assertThat(GitUtils.getExcludeFile(TEST_HOME)).isEqualTo(excludeFile);
    }

    @Test
    public void testGetGitignoreFile() throws IOException {
        assertThat(GitUtils.getGitignoreFile(TEST_HOME)).isNull();

        final Path gitignore = Files.createFile(TEST_HOME.resolve(".gitignore"));
        assertThat(GitUtils.getGitignoreFile(TEST_HOME)).isEqualTo(gitignore);
    }

    @Test
    public void testExpandTilde() {
        assertThat(GitUtils.expandTilde("/foo/bar/joe.txt")).isEqualTo("/foo/bar/joe.txt");
        assertThat(GitUtils.expandTilde("~/bar/joe.txt")).isEqualTo(TEST_HOME_DIR + "/bar/joe.txt");
        assertThat(GitUtils.expandTilde("")).isEmpty();
    }
}
