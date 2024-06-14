/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


/**
 * At the start of every test, creates a directory that acts as the user's home directory for the duration of
 * the test. The use case is to create Git user directories and files under this home directory.
 */
class TestHomeExtension implements BeforeEachCallback {

    static final String TEST_HOME_DIR = System.getProperty("cthing.filevisitor.home");
    static final Path TEST_HOME = Path.of(TEST_HOME_DIR);

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void beforeEach(final ExtensionContext context) throws Exception {
        if (Files.exists(TEST_HOME)) {
            try (Stream<Path> fileStream = Files.walk(TEST_HOME)) {
                fileStream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
        Files.createDirectories(TEST_HOME);
    }
}
