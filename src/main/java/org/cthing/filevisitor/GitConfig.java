/*
 * Copyright (C) 2024, C Thing Software
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2009, Constantine Plotnikov <constantine.plotnikov@gmail.com>
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008-2010, Google Inc.
 * Copyright (C) 2009, Google, Inc.
 * Copyright (C) 2009, JetBrains s.r.o.
 * Copyright (C) 2007-2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006-2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Thad Hughes <thadh@thad.corp.google.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.cthing.filevisitor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a <a href="https://git-scm.com/docs/git-config#_configuration_file">Git configuration</a>. Typically,
 * the configuration is read from a file such as {@code .gitconfig}.
 */
class GitConfig {

    private static final class ConfigLine {
        @Nullable
        String section;
        @Nullable
        String subsection;
        @Nullable
        String name;
        @Nullable
        String value;

        boolean match(final String aSection, @Nullable final String aSubsection, final String aKey) {
            return StringUtils.equalsIgnoreCase(this.section, aSection)
                    && StringUtils.equals(this.subsection, aSubsection)
                    && StringUtils.equalsIgnoreCase(this.name, aKey);
        }

        @Override
        public String toString() {
            if (this.section == null) {
                return "<empty>";
            }
            final StringBuilder buffer = new StringBuilder(this.section);
            if (this.subsection != null) {
                buffer.append('.').append(this.subsection);
            }
            if (this.name != null) {
                buffer.append('.').append(this.name);
            }
            if (this.value != null) {
                buffer.append('=').append(this.value);
            }
            return buffer.toString();
        }
    }


    private static final class StringReader {

        private final char[] buf;
        private int pos;

        StringReader(final String str) {
            this.buf = str.toCharArray();
        }

        int read() {
            return (this.pos >= this.buf.length) ? -1 : this.buf[this.pos++];
        }

        void unread() {
            this.pos--;
        }
    }


    private static final int MAX_DEPTH = 10;

    /**
     * Magic value indicating a missing entry.
     * <p>
     * This value is tested for reference equality in some contexts, so we
     * must ensure it is a special copy of the empty string.  It also must
     * be treated like the empty string.
     * </p>
     */
    private static final String MISSING_ENTRY = "";

    private final Path configFile;
    private final List<ConfigLine> entries;
    @Nullable
    private volatile List<ConfigLine> sorted;

    GitConfig(final Path configFile) {
        this.configFile = configFile;
        this.entries = new ArrayList<>();
    }

    @Nullable
    String getString(final String section, final String name) {
        return getString(section, null, name);
    }

    @Nullable
    String getString(final String section, @Nullable final String subsection, final String name) {
        final String[] lst = getStringList(section, subsection, name);
        return (lst == null) ? null : lst[lst.length - 1];
    }

    @Nullable
    private String[] getStringList(final String section, @Nullable final String subsection, final String name) {
        final List<ConfigLine> s = sorted();
        int idx = find(s, section, subsection, name);
        if (idx < 0) {
            return null;
        }
        final int end = end(s, idx, section, subsection, name);
        final String[] r = new String[end - idx];
        for (int i = 0; idx < end; ) {
            r[i++] = s.get(idx++).value;
        }
        return r;
    }

    private List<ConfigLine> sorted() {
        List<ConfigLine> r = this.sorted;
        if (r == null) {
            r = sort(this.entries);
            this.sorted = r;
        }
        return r;
    }

    @SuppressWarnings("Convert2streamapi")
    private static List<ConfigLine> sort(final List<ConfigLine> configLines) {
        final List<ConfigLine> sorted = new ArrayList<>(configLines.size());
        for (final ConfigLine line : configLines) {
            if (line.section != null && line.name != null) {
                sorted.add(line);
            }
        }
        sorted.sort(new LineComparator());
        return sorted;
    }

    private static final class LineComparator implements Comparator<ConfigLine>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(final ConfigLine a, final ConfigLine b) {
            return compare2(a.section, a.subsection, a.name, b.section, b.subsection, b.name);
        }
    }

    private int find(final List<ConfigLine> s, final String s1, @Nullable final String s2, final String name) {
        int low = 0;
        int high = s.size();
        while (low < high) {
            final int mid = (low + high) >>> 1;
            final ConfigLine e = s.get(mid);
            final int cmp = compare2(s1, s2, name, e.section, e.subsection, e.name);
            if (cmp < 0) {
                high = mid;
            } else if (cmp == 0) {
                return first(s, mid, s1, s2, name);
            } else {
                low = mid + 1;
            }
        }
        return -(low + 1);
    }

    private int first(final List<ConfigLine> s, final int i, final String s1, @Nullable final String s2, final String n) {
        int pos = i;
        while (0 < pos) {
            if (s.get(pos - 1).match(s1, s2, n)) {
                pos--;
            } else {
                return pos;
            }
        }
        return pos;
    }

    private int end(final List<ConfigLine> s, final int i, final String s1, @Nullable final String s2, final String n) {
        int pos = i;
        while (pos < s.size()) {
            if (s.get(pos).match(s1, s2, n)) {
                pos++;
            } else {
                return pos;
            }
        }
        return pos;
    }

    private static int compare2(@Nullable final String aSection, @Nullable final String aSubsection, @Nullable final String aName,
                                @Nullable final String bSection, @Nullable final String bSubsection, @Nullable final String bName) {
        int c = StringUtils.compareIgnoreCase(aSection, bSection);
        if (c != 0) {
            return c;
        }

        if (aSubsection == null && bSubsection != null) {
            return -1;
        }
        if (aSubsection != null && bSubsection == null) {
            return 1;
        }
        if (aSubsection != null) {
            c = aSubsection.compareTo(bSubsection);
            if (c != 0) {
                return c;
            }
        }

        return StringUtils.compareIgnoreCase(aName, bName);
    }

    /**
     * Clear this configuration and reset to the contents of the parsed string.
     *
     * @param text Git configuration content
     * @throws MatchingException if the specified text is not formatted correctly.
     */
    void fromText(final String text) throws MatchingException {
        fromTextRecurse(text, 1);
    }

    private void fromTextRecurse(final String text, final int depth) throws MatchingException {
        if (depth > MAX_DEPTH) {
            throw new MatchingException("Too many include recursions");
        }

        final StringReader reader = new StringReader(text);
        ConfigLine last = null;
        ConfigLine config = new ConfigLine();

        while (true) {
            int input = reader.read();
            if (-1 == input) {
                if (config.section != null) {
                    this.entries.add(config);
                }
                break;
            }

            final char c = (char)input;
            if ('\n' == c) {
                // End of this entry.
                this.entries.add(config);
                if (config.section != null) {
                    last = config;
                }
                config = new ConfigLine();
            } else if ('[' == c) {
                // This is a section header.
                config.section = readSectionName(reader);
                input = reader.read();
                if ('"' == input) {
                    config.subsection = readSubsectionName(reader);
                    input = reader.read();
                }
                if (']' != input) {
                    throw new MatchingException("Bad group header");
                }
            } else if (last != null) {
                // Read a value.
                config.section = last.section;
                config.subsection = last.subsection;
                reader.unread();
                config.name = readKeyName(reader);
                if (config.name.endsWith("\n")) {
                    config.name = config.name.substring(0, config.name.length() - 1);
                    config.value = MISSING_ENTRY;
                } else {
                    config.value = readValue(reader);
                }

                if ("include".equalsIgnoreCase(config.section)) {
                    addIncludedConfig(config, depth);
                }
            } else {
                throw new MatchingException("Invalid line in config file");
            }
        }
    }

    @Nullable
    private String readIncludedConfig(final String relPath) throws MatchingException {
        final Path file;
        if (relPath.startsWith("~/")) {
            file = PathUtils.HOME_PATH.resolve(relPath.substring(2));
        } else {
            final Path parent = this.configFile.getParent();
            assert parent != null;
            file = parent.resolve(relPath);
        }

        if (Files.notExists(file)) {
            return null;
        }

        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final FileNotFoundException ex) {
            return null;
        } catch (final IOException ex) {
            throw new MatchingException("Cannot read file " + relPath, ex);
        }
    }

    private void addIncludedConfig(final ConfigLine line, final int depth) throws MatchingException {
        if (!"path".equalsIgnoreCase(line.name) || line.value == null || MISSING_ENTRY.equals(line.value)) {
            throw new MatchingException("Invalid line in config file: " + line);
        }
        final String decoded = readIncludedConfig(line.value);
        if (decoded == null) {
            return;
        }

        try {
            fromTextRecurse(decoded, depth + 1);
        } catch (final MatchingException ex) {
            throw new MatchingException("Cannot read file " + line.value, ex);
        }
    }

    private static String readSectionName(final StringReader reader) throws MatchingException {
        final StringBuilder name = new StringBuilder();

        while (true) {
            int c = reader.read();

            if (c < 0) {
                throw new MatchingException("Unexpected end of config file");
            }

            if (']' == c) {
                reader.unread();
                break;
            }

            if (' ' == c || '\t' == c) {
                while (true) {
                    c = reader.read();

                    if (c < 0) {
                        throw new MatchingException("Unexpected end of config file");
                    }

                    if ('"' == c) {
                        reader.unread();
                        break;
                    }

                    if (' ' == c || '\t' == c) {
                        continue; // Skip
                    }

                    throw new MatchingException("Bad section entry: " + name);
                }
                break;
            }

            if (Character.isLetterOrDigit((char)c) || '.' == c || '-' == c) {
                name.append((char)c);
            } else {
                throw new MatchingException("Bad section entry: " + name);
            }
        }

        return name.toString();
    }

    private static String readKeyName(final StringReader reader) throws MatchingException {
        final StringBuilder name = new StringBuilder();

        while (true) {
            int c = reader.read();

            if (c < 0) {
                throw new MatchingException("Unexpected end of config file");
            }

            if ('=' == c) {
                break;
            }

            if (' ' == c || '\t' == c) {
                while (true) {
                    c = reader.read();
                    if (c < 0) {
                        throw new MatchingException("Unexpected end of config file");
                    }
                    if ('=' == c) {
                        break;
                    }
                    if (';' == c || '#' == c || '\n' == c) {
                        reader.unread();
                        break;
                    }
                    if (' ' == c || '\t' == c) {
                        continue; // Skip
                    }
                    throw new MatchingException("Bad entry delimiter");
                }
                break;
            }

            if (Character.isLetterOrDigit((char)c) || c == '-') {
                // According to the git-config documentation, the variable names are case-insensitive
                // and only alphanumeric characters and "-" are allowed.
                name.append((char)c);
            } else if ('\n' == c) {
                reader.unread();
                name.append((char)c);
                break;
            } else {
                throw new MatchingException("Bad entry name: " + name);
            }
        }

        return name.toString();
    }

    private static String readSubsectionName(final StringReader reader) throws MatchingException {
        final StringBuilder name = new StringBuilder();

        while (true) {
            int c = reader.read();

            if (c < 0) {
                break;
            }

            if ('\n' == c) {
                throw new MatchingException("Newline in quotes not allowed");
            }

            if ('\\' == c) {
                c = reader.read();
                switch (c) {
                    case -1:
                        throw new MatchingException("End of file in escape");
                    case '\\' | '"':
                        name.append((char)c);
                        continue;
                    default:
                        // C git simply drops backslashes if the escape sequence is not recognized.
                        name.append((char)c);
                        continue;
                }
            }

            if ('"' == c) {
                break;
            }

            name.append((char)c);
        }

        return name.toString();
    }

    @Nullable
    private static String readValue(final StringReader reader) throws MatchingException {
        final StringBuilder value = new StringBuilder();
        StringBuilder trailingSpaces = null;
        boolean quote = false;
        boolean inLeadingSpace = true;

        while (true) {
            int c = reader.read();

            if (c < 0) {
                break;
            }

            if ('\n' == c) {
                if (quote) {
                    throw new MatchingException("Newline in quotes not allowed");
                }
                reader.unread();
                break;
            }

            if (!quote && (';' == c || '#' == c)) {
                if (trailingSpaces != null) {
                    trailingSpaces.setLength(0);
                }
                reader.unread();
                break;
            }

            final char cc = (char)c;
            if (Character.isWhitespace(cc)) {
                if (inLeadingSpace) {
                    continue;
                }
                if (trailingSpaces == null) {
                    trailingSpaces = new StringBuilder();
                }
                trailingSpaces.append(cc);
                continue;
            }

            inLeadingSpace = false;

            if (trailingSpaces != null) {
                value.append(trailingSpaces);
                trailingSpaces.setLength(0);
            }

            if ('\\' == c) {
                c = reader.read();
                switch (c) {
                    case -1:
                        throw new MatchingException("End of file in escape");
                    case '\n':
                        continue;
                    case 't':
                        value.append('\t');
                        continue;
                    case 'b':
                        value.append('\b');
                        continue;
                    case 'n':
                        value.append('\n');
                        continue;
                    case '\\':
                        value.append('\\');
                        continue;
                    case '"':
                        value.append('"');
                        continue;
                    case '\r': {
                        final int next = reader.read();
                        if (next == '\n') {
                            continue; // CR-LF
                        } else if (next >= 0) {
                            reader.unread();
                        }
                        break;
                    }
                    default:
                        break;
                }
                throw new MatchingException("Bad escape: " + (Character.isAlphabetic(c)
                                                              ? Character.valueOf(((char)c)).toString()
                                                              : String.format("\\u%04x", c)));
            }

            if ('"' == c) {
                quote = !quote;
                continue;
            }

            value.append(cc);
        }

        return !value.isEmpty() ? value.toString() : null;
    }
}
