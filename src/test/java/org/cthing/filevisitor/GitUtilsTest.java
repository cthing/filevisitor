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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class GitUtilsTest {

    private static final String TEST_HOME_DIR = System.getProperty("cthing.filevisitor.home");
    private static final Path TEST_HOME = Path.of(TEST_HOME_DIR);

    @BeforeEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setup() throws IOException {
        if (Files.exists(TEST_HOME)) {
            Files.walk(TEST_HOME).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        Files.createDirectories(TEST_HOME);
    }

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
