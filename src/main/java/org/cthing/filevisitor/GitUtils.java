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

import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;


/**
 * Utilities for obtaining information about the Git version control system.
 */
final class GitUtils {

    private static final Path HOME_GITCONFIG = PathUtils.HOME_PATH.resolve(".gitconfig");
    private static final Path HOME_CONFIG_GITCONFIG = PathUtils.HOME_PATH.resolve(".config/git/config");
    @Nullable
    private static final String XDG_CONFIG_HOME = System.getenv("XDG_CONFIG_HOME");
    private static final Path HOME_CONFIG_CONFIG = (XDG_CONFIG_HOME == null)
                                                   ? HOME_CONFIG_GITCONFIG
                                                   : Path.of(XDG_CONFIG_HOME, "git", "config");
    private static final Path GIT_DIR = Path.of(".git");
    private static final Path GIT_EXCLUDE_FILE = GIT_DIR.resolve("info/exclude");
    private static final Path GITIGNORE = Path.of(".gitignore");


    private GitUtils() {
    }

    @Nullable
    static Path findGlobalConfigFile() {
        if (Files.isReadable(HOME_GITCONFIG)) {
            return HOME_GITCONFIG;
        }
        return Files.isReadable(HOME_CONFIG_CONFIG) ? HOME_CONFIG_CONFIG : null;
    }

    static boolean containsGitDir(final Path path) {
        Path dir = path;
        if (!Files.isDirectory(dir)) {
            dir = dir.getParent();
            if (dir == null) {
                return false;
            }
        }

        final Path gitDir = dir.resolve(GIT_DIR);
        return Files.isDirectory(gitDir);
    }

    @Nullable
    static Path getExcludeFile(final Path path) {
        Path dir = path;
        if (!Files.isDirectory(dir)) {
            dir = dir.getParent();
            if (dir == null) {
                return null;
            }
        }

        final Path excludeFile = dir.resolve(GIT_EXCLUDE_FILE);
        return Files.isReadable(excludeFile) ? excludeFile : null;
    }

    @Nullable
    static Path getGitignoreFile(final Path path) {
        Path dir = path;
        if (!Files.isDirectory(dir)) {
            dir = dir.getParent();
            if (dir == null) {
                return null;
            }
        }

        final Path ignoreFile = dir.resolve(GITIGNORE);
        return Files.isReadable(ignoreFile) ? ignoreFile : null;
    }
}
