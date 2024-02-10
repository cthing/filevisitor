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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cthing.filevisitor.GitUtilsTest.TEST_HOME;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class GitIgnoreTest {

    @BeforeEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setup() throws IOException {
        if (Files.exists(TEST_HOME)) {
            Files.walk(TEST_HOME).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        Files.createDirectories(TEST_HOME);
    }

    @Test
    public void testTrimTrailing() {
        assertThat(GitIgnore.trimTrailing("")).isEmpty();
        assertThat(GitIgnore.trimTrailing("   ")).isEmpty();
        assertThat(GitIgnore.trimTrailing("foo/bar")).isEqualTo("foo/bar");
        assertThat(GitIgnore.trimTrailing("foo/bar    ")).isEqualTo("foo/bar");
        assertThat(GitIgnore.trimTrailing("foo/bar abc")).isEqualTo("foo/bar abc");
        assertThat(GitIgnore.trimTrailing("foo/bar\\    ")).isEqualTo("foo/bar\\ ");
    }

    @Test
    @SuppressWarnings("DuplicateExpressions")
    public void testPreparePath() {
        final Path root = Path.of("/tmp");
        assertThat(GitIgnore.preparePath(Path.of("/foo/bar"), root)).isEqualTo(Path.of("/foo/bar"));
        assertThat(GitIgnore.preparePath(Path.of("foo/bar"), root)).isEqualTo(Path.of("foo/bar"));
        assertThat(GitIgnore.preparePath(Path.of("./foo/bar"), root)).isEqualTo(Path.of("foo/bar"));
        assertThat(GitIgnore.preparePath(Path.of("/tmp/foo/bar"), root)).isEqualTo(Path.of("foo/bar"));
        assertThat(GitIgnore.preparePath(Path.of(""), root)).isEqualTo(Path.of(""));
    }

    @Nested
    class GlocalIgnoreTest {

        @Test
        public void testNoConfig() throws MatchingException {
            final GitIgnore ignore = GitIgnore.findGlobalIgnore();
            assertThat(ignore).isNull();
        }

        @Test
        public void testNoExcludesFileSetting() throws IOException {
            Files.createFile(TEST_HOME.resolve(".gitconfig"));
            final GitIgnore ignore = GitIgnore.findGlobalIgnore();
            assertThat(ignore).isNull();
        }

        @Test
        public void testExcludesFile() throws IOException {
            final Path globalDir = Files.createDirectories(TEST_HOME.resolve("global"));
            final Path ignoreFile = Files.createFile(globalDir.resolve("config"));
            Files.writeString(ignoreFile, "/foo/bar/**");

            final Path configFile = Files.createFile(TEST_HOME.resolve(".gitconfig"));
            Files.writeString(configFile, "[core]\nexcludesFile = " + ignoreFile);

            final GitIgnore ignore = GitIgnore.findGlobalIgnore();
            assertThat(ignore).isNotNull();
            assertThat(ignore.getPatterns()).hasSize(1);

            final GitIgnore.Pattern pattern = ignore.getPatterns().get(0);
            assertThat(pattern.toString()).isEqualTo("/foo/bar/**");
            assertThat(pattern.isNegated()).isFalse();
            assertThat(pattern.isDirOnly()).isFalse();
        }
    }

    public static Stream<Arguments> matchProvider() {
        final String root = "/home/foobar";

        return Stream.of(
                arguments(root, false, "months", GitIgnore.MatchResult.IGNORE, "months"),
                arguments(root, false, "Cargo.lock", GitIgnore.MatchResult.IGNORE, "*.lock"),
                arguments(root, false, "src/main.rs", GitIgnore.MatchResult.IGNORE, "*.rs"),
                arguments(root, false, "src/main.rs", GitIgnore.MatchResult.IGNORE, "src/*.rs"),
                arguments(root, false, "cat-file.c", GitIgnore.MatchResult.IGNORE, "/*.c"),
                arguments(root, false, "src/main.rs", GitIgnore.MatchResult.IGNORE, "/src/*.rs"),
                arguments(root, false, "src/main.rs", GitIgnore.MatchResult.IGNORE, "!src/main.rs", "*.rs"),
                arguments(root, true, "foo", GitIgnore.MatchResult.IGNORE, "foo/"),
                arguments(root, true, "foo", GitIgnore.MatchResult.IGNORE, "foo\\/"),
                arguments(root, false, "foo", GitIgnore.MatchResult.IGNORE, "**/foo"),
                arguments(root, false, "src/foo", GitIgnore.MatchResult.IGNORE, "**/foo"),
                arguments(root, false, "src/foo/bar", GitIgnore.MatchResult.IGNORE, "**/foo/**"),
                arguments(root, false, "wat/src/foo/bar/baz", GitIgnore.MatchResult.IGNORE, "**/foo/**"),
                arguments(root, false, "foo/bar", GitIgnore.MatchResult.IGNORE, "**/foo/bar"),
                arguments(root, false, "src/foo/bar", GitIgnore.MatchResult.IGNORE, "**/foo/bar"),
                arguments(root, false, "abc/x", GitIgnore.MatchResult.IGNORE, "abc/**"),
                arguments(root, false, "abc/x/y", GitIgnore.MatchResult.IGNORE, "abc/**"),
                arguments(root, false, "abc/x/y/z", GitIgnore.MatchResult.IGNORE, "abc/**"),
                arguments(root, false, "a/b", GitIgnore.MatchResult.IGNORE, "a/**/b"),
                arguments(root, false, "a/x/b", GitIgnore.MatchResult.IGNORE, "a/**/b"),
                arguments(root, false, "a/x/y/b", GitIgnore.MatchResult.IGNORE, "a/**/b"),
                arguments(root, false, "!xy", GitIgnore.MatchResult.IGNORE, "\\!xy"),
                arguments(root, false, "#foo", GitIgnore.MatchResult.IGNORE, "\\#foo"),
                arguments(root, false, "./foo", GitIgnore.MatchResult.IGNORE, "foo"),
                arguments(root, false, "grep/target", GitIgnore.MatchResult.IGNORE, "target"),
                arguments(root, false, "./tabwriter-bin/Cargo.lock", GitIgnore.MatchResult.IGNORE, "Cargo.lock"),
                arguments(root, false, "./foo/bar/baz", GitIgnore.MatchResult.IGNORE, "/foo/bar/baz"),
                arguments(root, true, "xyz/foo", GitIgnore.MatchResult.IGNORE, "foo/"),
                arguments("./src", true, "./src/llvm", GitIgnore.MatchResult.IGNORE, "/llvm/"),
                arguments(root, true, "node_modules", GitIgnore.MatchResult.IGNORE, "node_modules/ "),
                arguments(root, true, "foo/bar", GitIgnore.MatchResult.IGNORE, "**/"),
                arguments(root, false, "path1/foo", GitIgnore.MatchResult.IGNORE, "path1/*"),
                arguments(root, false, ".a/b", GitIgnore.MatchResult.IGNORE, ".a/b"),
                arguments("./", false, ".a/b", GitIgnore.MatchResult.IGNORE, ".a/b"),
                arguments(".", false, ".a/b", GitIgnore.MatchResult.IGNORE, ".a/b"),
                arguments("./.", false, ".a/b", GitIgnore.MatchResult.IGNORE, ".a/b"),
                arguments("././", false, ".a/b", GitIgnore.MatchResult.IGNORE, ".a/b"),
                arguments("././.", false, ".a/b", GitIgnore.MatchResult.IGNORE, ".a/b"),
                arguments(root, false, "[", GitIgnore.MatchResult.IGNORE, "\\["),
                arguments(root, false, "?", GitIgnore.MatchResult.IGNORE, "\\?"),
                arguments(root, false, "*", GitIgnore.MatchResult.IGNORE, "\\*"),
                arguments(root, false, "a", GitIgnore.MatchResult.IGNORE, "\\a"),
                arguments(root, false, "sfoo.rs", GitIgnore.MatchResult.IGNORE, "s*.rs"),
                arguments(root, false, "foo.rs", GitIgnore.MatchResult.IGNORE, "**"),
                arguments(root, false, "a/foo.rs", GitIgnore.MatchResult.IGNORE, "**/**/*"),
                arguments(root, false, "foo.html", GitIgnore.MatchResult.IGNORE, "*.html"),

                arguments(root, false, "months", GitIgnore.MatchResult.NONE, "amonths"),
                arguments(root, false, "months", GitIgnore.MatchResult.NONE, "monthsa"),
                arguments(root, false, "src/grep/src/main.rs", GitIgnore.MatchResult.NONE, "/src/*.rs"),
                arguments(root, false, "mozilla-sha1/sha1.c", GitIgnore.MatchResult.NONE, "/*.c"),
                arguments(root, false, "src/grep/src/main.rs", GitIgnore.MatchResult.NONE, "/src/*.rs"),
                arguments(root, false, "foo", GitIgnore.MatchResult.NONE, "foo/"),
                arguments(root, false, "wat/src/afoo/bar/baz", GitIgnore.MatchResult.NONE, "**/foo/**"),
                arguments(root, false, "wat/src/fooa/bar/baz", GitIgnore.MatchResult.NONE, "**/foo/**"),
                arguments(root, false, "foo/src/bar", GitIgnore.MatchResult.NONE, "**/foo/bar"),
                arguments(root, false, "#foo", GitIgnore.MatchResult.NONE, "#foo"),
                arguments(root, false, "foo", GitIgnore.MatchResult.NONE, "", "", ""),
                arguments(root, true, "foo", GitIgnore.MatchResult.NONE, "foo/**"),
                arguments("./third_party/protobuf", false,
                          "./third_party/protobuf/csharp/src/packages/repositories.config",
                          GitIgnore.MatchResult.NONE, "m4/ltoptions.m4"),
                arguments(root, false, "foo/bar", GitIgnore.MatchResult.NONE, "!/bar"),
                arguments(root, false, "src/grep/src/main.rs", GitIgnore.MatchResult.NONE, "src/*.rs"),
                arguments(root, false, "path2/path1/foo", GitIgnore.MatchResult.NONE, "path1/*"),
                arguments(root, false, "src/foo.rs", GitIgnore.MatchResult.NONE, "s*.rs"),
                arguments(root, false, "foo.HTML", GitIgnore.MatchResult.NONE, "*.html"),
                arguments(root, false, "foo.htm", GitIgnore.MatchResult.NONE, "*.html"),
                arguments(root, false, "foo.HTM", GitIgnore.MatchResult.NONE, "*.html"),

                arguments(root, false, "src/main.rs", GitIgnore.MatchResult.ALLOW, "*.rs", "!src/main.rs"),
                arguments(root, true, "foo", GitIgnore.MatchResult.ALLOW, "*", "!**/")
        );
    }

    @ParameterizedTest
    @MethodSource("matchProvider")
    public void testMatches(final ArgumentsAccessor accessor) throws MatchingException {
        final Path root = Path.of(accessor.getString(0));
        final boolean isDir = accessor.getBoolean(1);
        final Path path = Path.of(accessor.getString(2));
        final GitIgnore.MatchResult expectedResult = accessor.get(3, GitIgnore.MatchResult.class);
        final List<String> patterns = IntStream.range(4, accessor.size())
                                               .mapToObj(accessor::getString)
                                               .collect(Collectors.toList());

        final GitIgnore ignore = new GitIgnore(root, patterns);
        assertThat(ignore.matches(path, isDir)).isEqualTo(expectedResult);
    }

    @Test
    public void testPatternEquality() {
        EqualsVerifier.forClass(GitIgnore.Pattern.class)
                      .usingGetClass()
                      .suppress(Warning.ALL_FIELDS_SHOULD_BE_USED)
                      .verify();
    }
}
