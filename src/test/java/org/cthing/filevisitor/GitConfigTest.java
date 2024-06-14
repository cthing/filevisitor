/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class GitConfigTest {

    @Test
    public void testReadValue() throws MatchingException {
        assertThat(GitConfig.readValue(new StringIterator("abc"))).isEqualTo("abc");
        assertThat(GitConfig.readValue(new StringIterator("   abc"))).isEqualTo("abc");
        assertThat(GitConfig.readValue(new StringIterator("abc\n"))).isEqualTo("abc");
        assertThat(GitConfig.readValue(new StringIterator("\"abc\""))).isEqualTo("abc");
        assertThat(GitConfig.readValue(new StringIterator("abc  ; Comment"))).isEqualTo("abc");
        assertThat(GitConfig.readValue(new StringIterator("abc  # Comment"))).isEqualTo("abc");
        assertThat(GitConfig.readValue(new StringIterator("\"abc   \""))).isEqualTo("abc   ");
        assertThat(GitConfig.readValue(new StringIterator("\"   abc\""))).isEqualTo("   abc");
        assertThat(GitConfig.readValue(new StringIterator("\"\tabc\""))).isEqualTo("\tabc");
        assertThat(GitConfig.readValue(new StringIterator("\"\\t\\nabc\""))).isEqualTo("\t\nabc");
        assertThat(GitConfig.readValue(new StringIterator("\"\\\nabc\""))).isEqualTo("abc");
        assertThat(GitConfig.readValue(new StringIterator("\"\\\r\nabc\""))).isEqualTo("abc");
        assertThat(GitConfig.readValue(new StringIterator("\"\\nabc\""))).isEqualTo("\nabc");
        assertThat(GitConfig.readValue(new StringIterator("\"\\babc\""))).isEqualTo("\babc");
        assertThat(GitConfig.readValue(new StringIterator("\"\\\\abc\""))).isEqualTo("\\abc");
        assertThat(GitConfig.readValue(new StringIterator("\"\\\"abc\""))).isEqualTo("\"abc");

        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readValue(new StringIterator("\"\n\"")))
                .withMessageContaining("Newline");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readValue(new StringIterator("\\")))
                .withMessageContaining("End of file in escape");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readValue(new StringIterator("\\a")))
                .withMessageContaining("Bad escape: a");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readValue(new StringIterator("\\\u0001")))
                .withMessageContaining("Bad escape: \\u0001");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readValue(new StringIterator("\"\\\rabc\"")));
    }

    @Test
    public void testReadKeyName() throws MatchingException {
        assertThat(GitConfig.readKeyName(new StringIterator("abc="))).isEqualTo("abc");
        assertThat(GitConfig.readKeyName(new StringIterator("a-b-c="))).isEqualTo("a-b-c");
        assertThat(GitConfig.readKeyName(new StringIterator("abc \t=\t"))).isEqualTo("abc");
        assertThat(GitConfig.readKeyName(new StringIterator("abc\n"))).isEqualTo("abc\n");
        assertThat(GitConfig.readKeyName(new StringIterator(" #"))).isEmpty();

        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readKeyName(new StringIterator("")))
                .withMessageContaining("Unexpected end of config file");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readKeyName(new StringIterator(" ")))
                .withMessageContaining("Unexpected end of config file");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readKeyName(new StringIterator(" |")))
                .withMessageContaining("Bad entry delimiter");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readKeyName(new StringIterator("a$")))
                .withMessageContaining("Bad entry name: a$");
    }

    @Test
    public void testReadSubsectionName() throws MatchingException {
        assertThat(GitConfig.readSubsectionName(new StringIterator("abc\""))).isEqualTo("abc");
        assertThat(GitConfig.readSubsectionName(new StringIterator("abc\\\\"))).isEqualTo("abc\\");
        assertThat(GitConfig.readSubsectionName(new StringIterator("abc\\\""))).isEqualTo("abc\"");
        assertThat(GitConfig.readSubsectionName(new StringIterator("abc\\z"))).isEqualTo("abcz");

        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readSubsectionName(new StringIterator("abc\n\"")))
                .withMessageContaining("Newline");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readSubsectionName(new StringIterator("\\")))
                .withMessageContaining("End of file in escape");
    }

    @Test
    public void testReadSectionName() throws MatchingException {
        assertThat(GitConfig.readSectionName(new StringIterator("abc \""))).isEqualTo("abc");
        assertThat(GitConfig.readSectionName(new StringIterator("abc  \t\""))).isEqualTo("abc");
        assertThat(GitConfig.readSectionName(new StringIterator("abc]"))).isEqualTo("abc");
        assertThat(GitConfig.readSectionName(new StringIterator("abc-de.f]"))).isEqualTo("abc-de.f");

        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readSectionName(new StringIterator("")))
                .withMessageContaining("Unexpected end of config file");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readSectionName(new StringIterator(" ")))
                .withMessageContaining("Unexpected end of config file");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readSectionName(new StringIterator("ab $]")))
                .withMessageContaining("Bad section name: ab$");
        assertThatExceptionOfType(MatchingException.class)
                .isThrownBy(() -> GitConfig.readSectionName(new StringIterator("ab$]")))
                .withMessageContaining("Bad section name: ab$");
    }

    @Nested
    class ParsingTest {

        @Test
        public void testSimple() throws MatchingException {
            final GitConfig config = new GitConfig(configFile("simple.txt"));
            assertThat(config.getString("user", "name")).isEqualTo("C Thing Software");
            assertThat(config.getString("user", "username")).isEqualTo("cthing");
            assertThat(config.getString("USER", "username")).isEqualTo("cthing");
            assertThat(config.getString("user", "email")).isEqualTo("cthing@foobar.com");
            assertThat(config.getString("user", "EMAIL")).isEqualTo("cthing@foobar.com");
            assertThat(config.getBoolean("user", "aligned", false)).isTrue();
            assertThat(config.getString("user", "junk")).isNull();
            assertThat(config.getString("missing", "email")).isNull();
            assertThat(config.getBoolean("missing", "email", true)).isTrue();
        }

        @Test
        public void testComplex() throws MatchingException {
            final GitConfig config = new GitConfig(configFile("complex.txt"));
            assertThat(config.getString("color", "ui")).isEqualTo("auto");
            assertThat(config.getString("color", "branch", "local")).isEqualTo("green bold");
        }

        @Test
        public void testInclude() throws MatchingException {
            final GitConfig config = new GitConfig(configFile("include.txt"));
            assertThat(config.getString("core", "filemode")).isEqualTo("true");
        }

        @Test
        public void testDuplicates() throws MatchingException {
            final GitConfig config = new GitConfig(configFile("duplicates.txt"));
            assertThat(config.getString("user", "name")).isEqualTo("C Thing Software");
            assertThat(config.getString("user", "username")).isEqualTo("bill");
            assertThat(config.getString("user", "email")).isEqualTo("joe@cthing.com");
        }

        @Test
        public void testGroupOnly() throws MatchingException {
            final GitConfig config = new GitConfig(configFile("group-only.txt"));
            assertThat(config.getString("user", "foo")).isNull();
        }

        @Test
        public void testKeyOnly() throws MatchingException {
            final GitConfig config = new GitConfig(configFile("key-only.txt"));
            assertThat(config.getString("user", "foo")).isEmpty();
            assertThat(config.getBoolean("user", "foo", false)).isTrue();
        }

        @Test
        public void testEmpty() throws MatchingException {
            final GitConfig config = new GitConfig(configFile("empty.txt"));
            assertThat(config.getString("user", "name")).isNull();
        }

        @Test
        public void testIncludeTooDeep() {
            assertThatExceptionOfType(MatchingException.class)
                    .isThrownBy(() -> new GitConfig(configFile("bad-include-depth.txt")))
                    .withMessageContaining("Too many include recursions");
        }

        @Test
        public void testBadGroup() {
            assertThatExceptionOfType(MatchingException.class)
                    .isThrownBy(() -> new GitConfig(configFile("bad-group.txt")))
                    .withMessageContaining("Bad group header");
        }

        @Test
        public void testBadLine() {
            assertThatExceptionOfType(MatchingException.class)
                    .isThrownBy(() -> new GitConfig(configFile("bad-line.txt")))
                    .withMessageContaining("Invalid line in config file");
        }

        @Test
        public void testBadIncludeLine() {
            assertThatExceptionOfType(MatchingException.class)
                    .isThrownBy(() -> new GitConfig(configFile("bad-include-line.txt")))
                    .withMessageContaining("Invalid line in config file");
        }

        @Test
        public void testMissingIncludeFile() {
            assertThatExceptionOfType(MatchingException.class)
                    .isThrownBy(() -> new GitConfig(configFile("missing-include.txt")))
                    .withMessageContaining("Cannot read file");
        }

        private Path configFile(final String filename) {
            final URL url = getClass().getResource("/git-config/" + filename);
            assertThat(url).isNotNull();
            return Path.of(url.getPath());
        }
    }
}
