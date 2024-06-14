/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cthing.filevisitor.GitIgnore.MatchResult.ALLOW;
import static org.cthing.filevisitor.GitIgnore.MatchResult.IGNORE;
import static org.cthing.filevisitor.GitIgnore.MatchResult.NONE;
import static org.cthing.filevisitor.TestHomeExtension.TEST_HOME;
import static org.junit.jupiter.params.provider.Arguments.arguments;


@ExtendWith(TestHomeExtension.class)
public class GitIgnoreTest {

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
    class LocalIgnoreTest {

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
                arguments(root, false, "months", IGNORE, "months"),
                arguments(root, false, "Cargo.lock", IGNORE, "*.lock"),
                arguments(root, false, "src/main.rs", IGNORE, "*.rs"),
                arguments(root, false, "src/main.rs", IGNORE, "src/*.rs"),
                arguments(root, false, "cat-file.c", IGNORE, "/*.c"),
                arguments(root, false, "src/main.rs", IGNORE, "/src/*.rs"),
                arguments(root, false, "src/main.rs", IGNORE, "!src/main.rs", "*.rs"),
                arguments(root, false, "src/main.rs", ALLOW, "*.rs", "!src/main.rs"),
                arguments(root, true, "foo", IGNORE, "foo/"),
                arguments(root, true, "foo", IGNORE, "foo\\/"),
                arguments(root, false, "foo", IGNORE, "**/foo"),
                arguments(root, false, "src/foo", IGNORE, "**/foo"),
                arguments(root, false, "src/foo/bar", IGNORE, "**/foo/**"),
                arguments(root, false, "wat/src/foo/bar/baz", IGNORE, "**/foo/**"),
                arguments(root, false, "foo/bar", IGNORE, "**/foo/bar"),
                arguments(root, false, "src/foo/bar", IGNORE, "**/foo/bar"),
                arguments(root, false, "abc/x", IGNORE, "abc/**"),
                arguments(root, false, "abc/x/y", IGNORE, "abc/**"),
                arguments(root, false, "abc/x/y/z", IGNORE, "abc/**"),
                arguments(root, false, "a/b", IGNORE, "a/**/b"),
                arguments(root, false, "a/x/b", IGNORE, "a/**/b"),
                arguments(root, false, "a/x/y/b", IGNORE, "a/**/b"),
                arguments(root, false, "!xy", IGNORE, "\\!xy"),
                arguments(root, false, "#foo", IGNORE, "\\#foo"),
                arguments(root, false, "./foo", IGNORE, "foo"),
                arguments(root, false, "grep/target", IGNORE, "target"),
                arguments(root, false, "./tabwriter-bin/Cargo.lock", IGNORE, "Cargo.lock"),
                arguments(root, false, "./foo/bar/baz", IGNORE, "/foo/bar/baz"),
                arguments(root, true, "xyz/foo", IGNORE, "foo/"),
                arguments("./src", true, "./src/llvm", IGNORE, "/llvm/"),
                arguments(root, true, "node_modules", IGNORE, "node_modules/ "),
                arguments(root, true, "foo/bar", IGNORE, "**/"),
                arguments(root, false, "path1/foo", IGNORE, "path1/*"),
                arguments(root, false, ".a/b", IGNORE, ".a/b"),
                arguments("./", false, ".a/b", IGNORE, ".a/b"),
                arguments(".", false, ".a/b", IGNORE, ".a/b"),
                arguments("./.", false, ".a/b", IGNORE, ".a/b"),
                arguments("././", false, ".a/b", IGNORE, ".a/b"),
                arguments("././.", false, ".a/b", IGNORE, ".a/b"),
                arguments(root, false, "[", IGNORE, "\\["),
                arguments(root, false, "?", IGNORE, "\\?"),
                arguments(root, false, "*", IGNORE, "\\*"),
                arguments(root, false, "a", IGNORE, "\\a"),
                arguments(root, false, "sfoo.rs", IGNORE, "s*.rs"),
                arguments(root, false, "foo.rs", IGNORE, "**"),
                arguments(root, false, "a/foo.rs", IGNORE, "**/**/*"),
                arguments(root, false, "foo.html", IGNORE, "*.html"),

                arguments(root, false, "months", NONE, "amonths"),
                arguments(root, false, "months", NONE, "monthsa"),
                arguments(root, false, "src/grep/src/main.rs", NONE, "/src/*.rs"),
                arguments(root, false, "mozilla-sha1/sha1.c", NONE, "/*.c"),
                arguments(root, false, "src/grep/src/main.rs", NONE, "/src/*.rs"),
                arguments(root, false, "foo", NONE, "foo/"),
                arguments(root, false, "wat/src/afoo/bar/baz", NONE, "**/foo/**"),
                arguments(root, false, "wat/src/fooa/bar/baz", NONE, "**/foo/**"),
                arguments(root, false, "foo/src/bar", NONE, "**/foo/bar"),
                arguments(root, false, "#foo", NONE, "#foo"),
                arguments(root, false, "foo", NONE, "", "", ""),
                arguments(root, true, "foo", NONE, "foo/**"),
                arguments("./third_party/protobuf", false,
                          "./third_party/protobuf/csharp/src/packages/repositories.config",
                          NONE, "m4/ltoptions.m4"),
                arguments(root, false, "foo/bar", NONE, "!/bar"),
                arguments(root, false, "src/grep/src/main.rs", NONE, "src/*.rs"),
                arguments(root, false, "path2/path1/foo", NONE, "path1/*"),
                arguments(root, false, "src/foo.rs", NONE, "s*.rs"),
                arguments(root, false, "foo.HTML", NONE, "*.html"),
                arguments(root, false, "foo.htm", NONE, "*.html"),
                arguments(root, false, "foo.HTM", NONE, "*.html"),

                arguments(root, false, "src/main.rs", ALLOW, "*.rs", "!src/main.rs"),
                arguments(root, true, "foo", ALLOW, "*", "!**/")
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
