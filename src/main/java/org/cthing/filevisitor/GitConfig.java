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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.cthing.annotations.AccessForTesting;
import org.jspecify.annotations.Nullable;


/**
 * Represents a <a href="https://git-scm.com/docs/git-config#_configuration_file">Git configuration</a>. Typically,
 * the configuration is read from a file such as {@code .gitconfig}. This code has been heavily modified and stripped
 * down from the original JGit
 * <a href="https://eclipse.googlesource.com/jgit/jgit/+/refs/heads/master/org.eclipse.jgit/src/org/eclipse/jgit/lib/Config.java">
 * Config</a> class.
 */
final class GitConfig {

    private static final class ConfigKey {
        @Nullable
        String section;
        @Nullable
        String subsection;
        @Nullable
        String name;

        ConfigKey() {
        }

        ConfigKey(@Nullable final String section, @Nullable final String subsection, @Nullable final String name) {
            this.section = section;
            this.subsection = subsection;
            this.name = name;
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
            return buffer.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            final ConfigKey that = (ConfigKey)obj;
            return Objects.equals(this.section, that.section)
                    && Objects.equals(this.subsection, that.subsection)
                    && Objects.equals(this.name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.section, this.subsection, this.name);
        }
    }

    private static final int MAX_DEPTH = 10;

    private final Map<ConfigKey, List<String>> configEntries;

    /**
     * Creates a Git configuration object based on the contents of the specified file.
     *
     * @param configFile Git configuration file to read
     * @throws MatchingException if there was a problem reading the configuration file.
     */
    GitConfig(final Path configFile) throws MatchingException {
        this();

        final Path basePath = configFile.getParent();
        assert basePath != null;

        final String config = readConfig(configFile);
        parse(config, basePath, 1);
    }

    private GitConfig() {
        this.configEntries = new HashMap<>();
    }

    /**
     * Searches for the current user's global Git config, if one exists.
     *
     * @return User's global Git config. See {@link GitUtils#findGlobalConfigFile()} for details on finding the
     *      global configuration file.
     */
    static GitConfig findGlobalConfig() {
        final Path configFile = GitUtils.findGlobalConfigFile();
        try {
            return configFile == null ? new GitConfig() : new GitConfig(configFile);
        } catch (final MatchingException ex) {
            return new GitConfig();
        }
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
        final List<String> lst = getStringList(section, subsection, name);
        return (lst == null) ? null : lst.get(lst.size() - 1);
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
    private List<String> getStringList(final String section, @Nullable final String subsection, final String name) {
        final ConfigKey key = new ConfigKey(section.toLowerCase(Locale.ROOT), subsection,
                                            name.toLowerCase(Locale.ROOT));
        return this.configEntries.get(key);
    }

    /**
     * Parses the specified Git configuration.
     *
     * @param configuration Contents of a Git configuration file
     * @param depth Current recursive inclusion depth. Prevents unnecessarily deep or infinite inclusion.
     * @throws MatchingException if there is a problem parsing the configuration.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private void parse(final String configuration, final Path basePath, final int depth)
            throws MatchingException {
        final StringIterator iterator = new StringIterator(configuration);
        ConfigKey last = null;
        ConfigKey configKey = new ConfigKey();
        String configValue = null;
        boolean inComment = false;

        while (true) {
            int input = iterator.next();
            if (-1 == input) {
                if (configKey.section != null) {
                    addValue(configKey, configValue);
                }
                break;
            }

            final char ch = (char)input;

            if ('\n' == ch) {
                // End of this entry.
                addValue(configKey, configValue);
                if (configKey.section != null) {
                    last = configKey;
                }
                configKey = new ConfigKey();
                configValue = null;
                inComment = false;
            } else if (inComment || (configKey.section == null && Character.isWhitespace(ch))) { // SUPPRESS CHECKSTYLE Skip
                // Skip
            } else if (isComment(ch)) {
                inComment = true;
            } else if ('[' == ch) {
                // Group header.
                configKey.section = readSectionName(iterator);
                input = iterator.next();
                if ('"' == input) {
                    configKey.subsection = readSubsectionName(iterator);
                    input = iterator.next();
                }
                if (']' != input) {
                    throw new MatchingException("Bad group header");
                }
            } else if (last != null) {
                // Value.
                configKey.section = last.section;
                configKey.subsection = last.subsection;
                iterator.prev();
                configKey.name = readKeyName(iterator);
                if (configKey.name.endsWith("\n")) {
                    configKey.name = configKey.name.substring(0, configKey.name.length() - 1);
                    configValue = "";
                } else {
                    configValue = readValue(iterator);
                }

                // Include another Git config file
                if ("include".equalsIgnoreCase(configKey.section)) {
                    includeConfig(configKey, configValue, basePath, depth);
                }
            } else {
                throw new MatchingException("Invalid line in config file: " + configKey);
            }
        }
    }

    /**
     * Adds the specified value to the list of values for the specified key.
     *
     * @param key Configuration key to which the specified value should be added
     * @param value Value to add to the list of values for the key
     */
    private void addValue(final ConfigKey key, @Nullable final String value) {
        if (key.section != null && key.name != null && value != null) {
            this.configEntries.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
    }

    /**
     * Handles the inclusion of a Git configuration file into the current configuration file.
     *
     * @param configKey Configuration key containing the include directive
     * @param configValue Configuration value for the key
     * @param basePath Path to resolve relative includes
     * @param depth Current recursive inclusion depth. Prevents unnecessarily deep or infinite inclusion.
     * @throws MatchingException if there was a problem including the configuration file
     */
    private void includeConfig(final ConfigKey configKey, @Nullable final String configValue,
                               final Path basePath, final int depth) throws MatchingException {
        if (depth > MAX_DEPTH) {
            throw new MatchingException("Too many include recursions");
        }

        if (!"path".equalsIgnoreCase(configKey.name) || configValue == null || configValue.isEmpty()) {
            throw new MatchingException("Invalid line in config file: " + configKey);
        }

        final String expandedPath = GitUtils.expandTilde(configValue);
        final String config = readConfig(basePath.resolve(expandedPath));
        parse(config, basePath, depth + 1);
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

        return name.toString().toLowerCase(Locale.ROOT);
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

        return name.toString().toLowerCase(Locale.ROOT);
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
