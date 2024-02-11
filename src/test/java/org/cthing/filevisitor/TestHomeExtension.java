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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

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
            Files.walk(TEST_HOME).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        Files.createDirectories(TEST_HOME);
    }
}
