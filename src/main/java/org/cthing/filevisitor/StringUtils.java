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

import org.cthing.annotations.NoCoverageGenerated;


/**
 * Utility methods for working with strings.
 */
final class StringUtils {

    @NoCoverageGenerated
    private StringUtils() {
    }

    /**
     * Obtains the boolean value of the specified string. In keeping with the
     * <a href="https://git-scm.com/docs/git-config#Documentation/git-config.txt-boolean">Git config boolean</a>
     * documentation, the following strings are considered {@code true}:
     * <ul>
     *     <li>true</li>
     *     <li>yes</li>
     *     <li>on</li>
     *     <li>1</li>
     * </ul>
     * and the following string are considered {@code false}:
     * <ul>
     *     <li>false</li>
     *     <li>no</li>
     *     <li>off</li>
     *     <li>0</li>
     * </ul>
     * Any other string will throw an {@code IllegalArgumentException}.
     *
     * @param value String representation of a boolean value
     * @return Boolean value corresponding to the specified string value
     * @throws IllegalArgumentException if the string is not one of the expected boolean representations.
     */
    static boolean toBoolean(final String value) {
        if ("true".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value)
                || "1".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)
                || "no".equalsIgnoreCase(value)
                || "off".equalsIgnoreCase(value)
                || "0".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean string value");
    }
}
