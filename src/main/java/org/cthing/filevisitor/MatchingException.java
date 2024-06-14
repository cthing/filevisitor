/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import java.io.IOException;
import java.io.Serial;

/**
 * Represents an error encountered while performing file pattern matching.
 */
public class MatchingException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    MatchingException(final String message) {
        super(message);
    }

    MatchingException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
