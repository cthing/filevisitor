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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.cthing.annotations.AccessForTesting;


/**
 * Represents a <a href="https://git-scm.com/docs/git-config#_configuration_file">Git configuration</a>. Typically,
 * the configuration is read from a file such as {@code .gitconfig}. This code has been heavily modified and stripped
 * down from the original JGit
 * <a href="https://eclipse.googlesource.com/jgit/jgit/+/refs/heads/master/org.eclipse.jgit/src/org/eclipse/jgit/lib/Config.java">
 * Config</a> class.
 */
final class GitConfig {

    private static final class ConfigLine {
        @Nullable
        String section;
        @Nullable
        String subsection;
        @Nullable
        String name;
        @Nullable
        String value;

        boolean match(final String otherSection, @Nullable final String otherSubsection, final String otherKey) {
            return StringUtils.equalsIgnoreCase(this.section, otherSection)
                    && Objects.equals(this.subsection, otherSubsection)
                    && StringUtils.equalsIgnoreCase(this.name, otherKey);
        }

        /**
         * Compares this with the specified section, subsection and name. According to
         * <a href="https://git-scm.com/docs/git-config#_configuration_file">git-config</a>, section names and key names
         * are compared case-insensitive and subsections are compared case-sensitive. Sections are compared, followed
         * by subsections, and then key names.
         *
         * @param other Other configuration line to compare
         * @return 0 if all items are equal. A negative value if this section, subsection, or name is less than
         *      the corresponding specified items. A positive value if this section, subsection, or name is greater
         *      than the corresponding specified items.
         */
        int compare(final ConfigLine other) {
            return compare(other.section, other.subsection, other.name);
        }

        /**
         * Compares this with the specified section, subsection and name. According to
         * <a href="https://git-scm.com/docs/git-config#_configuration_file">git-config</a>, section names and key names
         * are compared case-insensitive and subsections are compared case-sensitive. Sections are compared, followed
         * by subsections, and then key names.
         *
         * @param otherSection Other section name
         * @param otherSubsection Other subsection name
         * @param otherName Other key name
         * @return 0 if all items are equal. A negative value if this section, subsection, or name is less than
         *      the corresponding specified items. A positive value if this section, subsection, or name is greater
         *      than the corresponding specified items.
         */
        int compare(@Nullable final String otherSection, @Nullable final String otherSubsection,
                    @Nullable final String otherName) {
            int c = StringUtils.compareIgnoreCase(this.section, otherSection);
            if (c != 0) {
                return c;
            }

            c = StringUtils.compare(this.subsection, otherSubsection);
            if (c != 0) {
                return c;
            }

            return StringUtils.compareIgnoreCase(this.name, otherName);
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


    private static final int MAX_DEPTH = 10;

    private final ConfigLine[] configEntries;

    /**
     * Creates a Git configuration object based on the contents of the specified file.
     *
     * @param configFile Git configuration file to read
     * @throws MatchingException if there was a problem reading the configuration file.
     */
    GitConfig(final Path configFile) throws MatchingException {
        final Path basePath = configFile.getParent();
        assert basePath != null;

        final String config = readConfig(configFile);
        this.configEntries = parse(config, basePath, 1).stream()
                                                       .filter(line -> line.section != null && line.name != null)
                                                       .sorted(ConfigLine::compare)
                                                       .toArray(ConfigLine[]::new);
    }

    /**
     * Obtains the configuration value with the specified name in the specified section.
     *
     * @param section Section of the configuration in which to find the value
     * @param name Name of the value within the specified section
     * @return Configuration value or {@code null} if the value was not found.
     */
    @Nullable
    String getString(final String section, final String name) {
        return getString(section, null, name);
    }

    /**
     * Obtains the configuration value with the specified name in the specified section and subsection.
     *
     * @param section Section of the configuration in which to find the value
     * @param subsection Subsection of the configuration in which to find the value
     * @param name Name of the value within the specified section and subsection
     * @return Configuration value or {@code null} if the value was not found.
     */
    @Nullable
    String getString(final String section, @Nullable final String subsection, final String name) {
        final String[] lst = getStringList(section, subsection, name);
        return (lst == null) ? null : lst[lst.length - 1];
    }

    /**
     * Obtains a boolean value with the specified name in the specified section.
     *
     * @param section Section of the configuration in which to find the value
     * @param name Name of the value within the specified section
     * @param defaultValue Value to return if the item was not found
     * @return {@code true} if an entry is found with a value of "true", "on", "yes" or "1", or if an entry
     *      is not found and the default value is {@code true}. Returns {@code false} if an entry is found
     *      with a value of "false", "off", "no" or "0", or if an entry is not found and the default value
     *      is {@code false}.
     */
    boolean getBoolean(final String section, final String name, final boolean defaultValue) {
        return getBoolean(section, null, name, defaultValue);
    }

    /**
     * Obtains a boolean value with the specified name in the specified section and subsection.
     *
     * @param section Section of the configuration in which to find the value
     * @param subsection Subsection of the configuration section in which to find the value
     * @param name Name of the value within the specified section
     * @param defaultValue Value to return if the item was not found
     * @return {@code true} if an entry is found with a value of "true", "on", "yes" or "1", or if an entry
     *      is not found and the default value is {@code true}. Returns {@code false} if an entry is found
     *      with a value of "false", "off", "no" or "0", or if an entry is not found and the default value
     *      is {@code false}.
     */
    boolean getBoolean(final String section, @Nullable final String subsection, final String name,
                       final boolean defaultValue) {
        final String valueStr = getString(section, subsection, name);
        if (valueStr == null) {
            return defaultValue;
        }
        if (valueStr.isEmpty()) {
            return true;
        }
        return StringUtils.toBoolean(valueStr);
    }

    /**
     * A Git configuration can have multiple entries for the same key in the same section and subsection. This
     * method obtains all values matching the specified section, subsection and key name.
     *
     * @param section Name of the section to match
     * @param subsection Name of the subsection to match or {@code null} if there is no subsection
     * @param name Name of the key to match
     * @return Values corresponding to the specified section, subsection and key name.
     */
    @Nullable
    private String[] getStringList(final String section, @Nullable final String subsection, final String name) {
        int start = find(section, subsection, name);
        if (start < 0) {
            return null;
        }

        final int end = findEnd(start, section, subsection, name);

        final String[] r = new String[end - start];
        for (int i = 0; start < end; i++, start++) {
            r[i] = this.configEntries[start].value;
        }
        return r;
    }

    /**
     * Attempts to find the specified configuration line in the list of lines. The list is assumed to be sorted.
     *
     * @param section Name of the section to find
     * @param subsection Name of the subsection to find or {@code null} if no subsection
     * @param name Name of the key to find
     * @return Index of the last occurrence of the matching configuration line or a negative value if not found.
     */
    private int find(final String section, @Nullable final String subsection, final String name) {
        int low = 0;
        int high = this.configEntries.length;
        while (low < high) {
            final int mid = (low + high) >>> 1;
            final ConfigLine e = this.configEntries[mid];
            final int cmp = e.compare(section, subsection, name);
            if (cmp > 0) {
                high = mid;
            } else if (cmp < 0) {
                low = mid + 1;
            } else {
                return findStart(mid, section, subsection, name);
            }
        }
        return -(low + 1);
    }

    /**
     * Obtains the index of the first configuration line matching the specified section, subsection and name,
     * starting at the specified index. The list is assumed to be sorted and the configuration entry is present.
     *
     * @param idx Starting index to search
     * @param section Name of the section to match
     * @param subsection Name of the subsection to match or {@code null} if no subsection
     * @param name Name of the key to match
     * @return Index of the first configuration entry matching the specified configuration line.
     */
    private int findStart(final int idx, final String section, @Nullable final String subsection, final String name) {
        int i = idx;
        while (i > 0 && this.configEntries[i - 1].match(section, subsection, name)) {
            i--;
        }
        return i;
    }

    /**
     * Obtains the index of the last configuration line matching the specified section, subsection and name,
     * starting at the specified index. The list is assumed to be sorted and the configuration entry is present.
     *
     * @param idx Starting index to search
     * @param section Name of the section to match
     * @param subsection Name of the subsection to match or {@code null} if no subsection
     * @param name Name of the key to match
     * @return Index after the last configuration entry matching the specified configuration line.
     */
    private int findEnd(final int idx, final String section, @Nullable final String subsection, final String name) {
        int i = idx;
        while (i < this.configEntries.length && this.configEntries[i].match(section, subsection, name)) {
            i++;
        }
        return i;
    }

    /**
     * Parses the specified Git configuration.
     *
     * @param configuration Contents of a Git configuration file
     * @param depth Current recursive inclusion depth. Prevents unnecessarily deep or infinite inclusion.
     * @throws MatchingException if there is a problem parsing the configuration.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private static List<ConfigLine> parse(final String configuration, final Path basePath, final int depth)
            throws MatchingException {
        final List<ConfigLine> entries = new ArrayList<>();
        final StringIterator iterator = new StringIterator(configuration);
        ConfigLine last = null;
        ConfigLine configLine = new ConfigLine();
        boolean inComment = false;

        while (true) {
            int input = iterator.next();
            if (-1 == input) {
                if (configLine.section != null) {
                    entries.add(configLine);
                }
                break;
            }

            final char ch = (char)input;

            if ('\n' == ch) {
                // End of this entry.
                entries.add(configLine);
                if (configLine.section != null) {
                    last = configLine;
                }
                configLine = new ConfigLine();
                inComment = false;
            } else if (inComment || (configLine.section == null && Character.isWhitespace(ch))) { // SUPPRESS CHECKSTYLE Skip
                // Skip
            } else if (isComment(ch)) {
                inComment = true;
            } else if ('[' == ch) {
                // Group header.
                configLine.section = readSectionName(iterator);
                input = iterator.next();
                if ('"' == input) {
                    configLine.subsection = readSubsectionName(iterator);
                    input = iterator.next();
                }
                if (']' != input) {
                    throw new MatchingException("Bad group header");
                }
            } else if (last != null) {
                // Value.
                configLine.section = last.section;
                configLine.subsection = last.subsection;
                iterator.prev();
                configLine.name = readKeyName(iterator);
                if (configLine.name.endsWith("\n")) {
                    configLine.name = configLine.name.substring(0, configLine.name.length() - 1);
                    configLine.value = "";
                } else {
                    configLine.value = readValue(iterator);
                }

                // Include another Git config file
                if ("include".equalsIgnoreCase(configLine.section)) {
                    entries.addAll(includeConfig(configLine, basePath, depth));
                }
            } else {
                throw new MatchingException("Invalid line in config file: " + configLine);
            }
        }

        return entries;
    }

    /**
     * Handles the inclusion of a Git configuration file into the current configuration file.
     *
     * @param configLine Configuration file line containing the include directive
     * @param basePath Path to resolve relative includes
     * @param depth Current recursive inclusion depth. Prevents unnecessarily deep or infinite inclusion.
     * @return Included configuration lines
     * @throws MatchingException if there was a problem including the configuration file
     */
    private static List<ConfigLine> includeConfig(final ConfigLine configLine, final Path basePath, final int depth)
            throws MatchingException {
        if (depth > MAX_DEPTH) {
            throw new MatchingException("Too many include recursions");
        }

        if (!"path".equalsIgnoreCase(configLine.name) || configLine.value == null || configLine.value.isEmpty()) {
            throw new MatchingException("Invalid line in config file: " + configLine);
        }

        final String expandedPath = GitUtils.expandTilde(configLine.value);
        final String config = readConfig(basePath.resolve(expandedPath));
        return parse(config, basePath, depth + 1);
    }

    /**
     * Reads the specified Git configuration file.
     *
     * @param file Git configuration file to be read
     * @return Complete contents of the configuration file
     * @throws MatchingException if there was a problem reading the configuration file.
     */
    private static String readConfig(final Path file) throws MatchingException {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new MatchingException("Cannot read file " + file, ex);
        }
    }

    /**
     * Parses the name of a section from the specified string iterator.
     *
     * @param iterator Iterator over the input config file
     * @return Section name parsed from the specified iterator.
     * @throws MatchingException if there was a problem parsing a name.
     */
    @AccessForTesting
    static String readSectionName(final StringIterator iterator) throws MatchingException {
        final StringBuilder name = new StringBuilder();

        while (true) {
            int ch = iterator.next();

            if (ch < 0) {
                throw new MatchingException("Unexpected end of config file");
            }

            if (']' == ch) {
                iterator.prev();
                break;
            }

            if (' ' == ch || '\t' == ch) {
                while (true) {
                    ch = iterator.next();

                    if (ch < 0) {
                        throw new MatchingException("Unexpected end of config file");
                    }

                    if ('"' == ch) {
                        iterator.prev();
                        break;
                    }

                    if (' ' == ch || '\t' == ch) {
                        continue;
                    }

                    throw new MatchingException("Bad section name: " + name + (char)ch);
                }
                break;
            }

            if (Character.isLetterOrDigit((char)ch) || '.' == ch || '-' == ch) {
                name.append((char)ch);
            } else {
                throw new MatchingException("Bad section name: " + name + (char)ch);
            }
        }

        return name.toString();
    }

    /**
     * Parses the name of a subsection from the specified string iterator.
     *
     * @param iterator Iterator over the input config file
     * @return Subsection name parsed from the specified iterator.
     * @throws MatchingException if there was a problem parsing a name.
     */
    @AccessForTesting
    static String readSubsectionName(final StringIterator iterator) throws MatchingException {
        final StringBuilder name = new StringBuilder();

        while (true) {
            int ch = iterator.next();

            if (ch < 0) {
                break;
            }

            if ('\n' == ch) {
                throw new MatchingException("Newline in quotes not allowed");
            }

            // Escaped character
            if ('\\' == ch) {
                ch = iterator.next();
                switch (ch) {
                    case -1:
                        throw new MatchingException("End of file in escape");
                    case '\\':
                        name.append('\\');
                        continue;
                    case '"':
                        name.append('"');
                        continue;
                    default:
                        // Drop backslashes if the escape sequence is not recognized.
                        name.append((char)ch);
                        continue;
                }
            }

            if ('"' == ch) {
                break;
            }

            name.append((char)ch);
        }

        return name.toString();
    }

    /**
     * Parses the name of a configuration key from the specified string iterator.
     *
     * @param iterator Iterator over the input config file
     * @return Configuration key name parsed from the specified iterator.
     * @throws MatchingException if there was a problem parsing a key.
     */
    @AccessForTesting
    static String readKeyName(final StringIterator iterator) throws MatchingException {
        final StringBuilder name = new StringBuilder();

        while (true) {
            int ch = iterator.next();

            if (ch < 0) {
                throw new MatchingException("Unexpected end of config file");
            }

            if ('=' == ch) {
                break;
            }

            if (' ' == ch || '\t' == ch) {
                while (true) {
                    ch = iterator.next();

                    if (ch < 0) {
                        throw new MatchingException("Unexpected end of config file");
                    }

                    if ('=' == ch) {
                        break;
                    }

                    if (isComment(ch) || '\n' == ch) {
                        iterator.prev();
                        break;
                    }

                    if (' ' == ch || '\t' == ch) {
                        continue;
                    }

                    throw new MatchingException("Bad entry delimiter: " + (char)ch);
                }
                break;
            }

            // According to the git-config documentation, names are case-insensitive and
            // only alphanumeric characters and "-" are allowed.
            if (Character.isLetterOrDigit(ch) || ch == '-') {
                name.append((char)ch);
            } else if ('\n' == ch) {
                iterator.prev();
                name.append('\n');
                break;
            } else {
                throw new MatchingException("Bad entry name: " + name + (char)ch);
            }
        }

        return name.toString();
    }

    /**
     * Parses a value from the specified string iterator.
     *
     * @param iterator Iterator over the input config file
     * @return Value parsed from the specified iterator or {@code null} if no value could be parsed.
     * @throws MatchingException if there was a problem parsing a value.
     */
    @AccessForTesting
    @Nullable
    static String readValue(final StringIterator iterator) throws MatchingException {
        final StringBuilder value = new StringBuilder();
        final StringBuilder trailingSpaces = new StringBuilder();
        boolean inQuote = false;
        boolean inLeadingSpace = true;

        while (true) {
            int ch = iterator.next();

            // End of the configuration
            if (ch == -1) {
                break;
            }

            // Newline
            if ('\n' == ch) {
                if (inQuote) {
                    throw new MatchingException("Newline in quotes not allowed");
                }
                iterator.prev();
                break;
            }

            // Trailing comment
            if (!inQuote && isComment(ch)) {
                trailingSpaces.setLength(0);
                iterator.prev();
                break;
            }

            // Whitespace
            if (Character.isWhitespace(ch)) {
                if (!inLeadingSpace) {
                    trailingSpaces.append((char)ch);
                }
                continue;
            }

            inLeadingSpace = false;

            if (!trailingSpaces.isEmpty()) {
                value.append(trailingSpaces);
                trailingSpaces.setLength(0);
            }

            final char savedCh = (char)ch;

            // Escaped character
            if ('\\' == ch) {
                ch = iterator.next();
                switch (ch) {
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
                        final int next = iterator.next();
                        // CR-LF
                        if (next == '\n') {
                            continue;
                        }
                        break;
                    }
                    default:
                        break;
                }
                throw new MatchingException("Bad escape: " + (Character.isAlphabetic(ch)
                                                              ? (char)ch
                                                              : String.format("\\u%04x", ch)));
            }

            // Quote
            if ('"' == ch) {
                inQuote = !inQuote;
                continue;
            }

            // Everything else
            value.append(savedCh);
        }

        return value.isEmpty() ? null : value.toString();
    }

    /**
     * Indicates whether the specified character begins a comment.
     *
     * @param ch Character to test
     * @return {@code true} if the specified character begins a comment.
     */
    private static boolean isComment(final int ch) {
        return ';' == ch || '#' == ch;
    }
}
