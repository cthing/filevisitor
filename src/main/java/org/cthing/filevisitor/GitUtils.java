/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.nio.file.Files;
import java.nio.file.Path;

import org.cthing.annotations.NoCoverageGenerated;
import org.jspecify.annotations.Nullable;


/**
 * Utilities for obtaining information about the Git version control system.
 */
final class GitUtils {

    private static final Path HOME_GITCONFIG = PathUtils.HOME_PATH.resolve(".gitconfig");
    private static final Path HOME_CONFIG_GITCONFIG = PathUtils.HOME_PATH.resolve(".config/git/config");
    @Nullable
    private static final String XDG_CONFIG_HOME = System.getenv("XDG_CONFIG_HOME");
    private static final Path HOME_CONFIG_CONFIG = (XDG_CONFIG_HOME == null || XDG_CONFIG_HOME.isEmpty())
                                                   ? HOME_CONFIG_GITCONFIG
                                                   : Path.of(XDG_CONFIG_HOME, "git", "config");
    private static final Path GIT_DIR = Path.of(".git");
    private static final Path GIT_EXCLUDE_FILE = GIT_DIR.resolve("info/exclude");
    private static final Path GITIGNORE = Path.of(".gitignore");


    @NoCoverageGenerated
    private GitUtils() {
    }

    /**
     * Searches for the current user's global Git config file, if one exists.
     *
     * @return User's global Git config file. If a {@code ~/.gitconfig} file exists, it is returned. Otherwise,
     *      if the {@code $XDG_CONFIG_HOME} environment variable is set, the file {@code $XDG_CONFIG_HOME/git/config}
     *      is returned, if it exists. If the {@code $XDG_CONFIG_HOME} environment variable is not set, the file
     *      {@code ~/.config/git/config} file is returned, if it exists. If no config file is found or is not
     *      readable, {@code null} is returned.
     */
    @Nullable
    static Path findGlobalConfigFile() {
        if (Files.isReadable(HOME_GITCONFIG)) {
            return HOME_GITCONFIG;
        }
        return Files.isReadable(HOME_CONFIG_CONFIG) ? HOME_CONFIG_CONFIG : null;
    }

    /**
     * Indicates whether the specified directory contains a {@code .git} directory.
     *
     * @param dir Directory in which to look for a {@code .git} directory
     * @return {@code true} if the specified directory contains a {@code .git} directory.
     */
    static boolean containsGitDir(final Path dir) {
        return Files.isDirectory(dir.resolve(GIT_DIR));
    }

    /**
     * Attempts to obtain the exclusion file {@code .git/info/exclude} under the specified directory.
     *
     * @param dir Directory in which to look for the {@code .git/info/exclude} file
     * @return The exclusion file {@code .git/info/exclude} under the specified directory, or {@code null} if
     *      the file does not exist or is not readable.
     */
    @Nullable
    static Path getExcludeFile(final Path dir) {
        final Path excludeFile = dir.resolve(GIT_EXCLUDE_FILE);
        return Files.isReadable(excludeFile) ? excludeFile : null;
    }

    /**
     * Attempts to obtain the {@code .gitignore} file in the specified directory.
     *
     * @param dir Directory in which to look for a {@code .gitignore} file
     * @return Path of the {@code .gitignore} file if it exists, or {@code null} if it does not or is not readable.
     */
    @Nullable
    static Path getGitignoreFile(final Path dir) {
        final Path ignoreFile = dir.resolve(GITIGNORE);
        return Files.isReadable(ignoreFile) ? ignoreFile : null;
    }

    /**
     * Replaces a tilde prefix in the specified path with the current user's home directory. If no tilde
     * is present, the original path is returned.
     *
     * @param path Path in which tilde is to be replaced with the home directory
     * @return Path with tilde replaced with the home directory.
     */
    static String expandTilde(final String path) {
        return path.startsWith("~/") ? PathUtils.HOME_DIR + path.substring(1) : path;
    }
}
