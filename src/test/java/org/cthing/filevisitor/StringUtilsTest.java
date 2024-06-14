/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.filevisitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


public class StringUtilsTest {

    @Test
    public void testToBoolean() {
        assertThat(StringUtils.toBoolean("true")).isTrue();
        assertThat(StringUtils.toBoolean("TRUE")).isTrue();
        assertThat(StringUtils.toBoolean("yes")).isTrue();
        assertThat(StringUtils.toBoolean("Yes")).isTrue();
        assertThat(StringUtils.toBoolean("on")).isTrue();
        assertThat(StringUtils.toBoolean("1")).isTrue();

        assertThat(StringUtils.toBoolean("false")).isFalse();
        assertThat(StringUtils.toBoolean("FALSE")).isFalse();
        assertThat(StringUtils.toBoolean("no")).isFalse();
        assertThat(StringUtils.toBoolean("No")).isFalse();
        assertThat(StringUtils.toBoolean("off")).isFalse();
        assertThat(StringUtils.toBoolean("0")).isFalse();

        assertThatIllegalArgumentException().isThrownBy(() -> StringUtils.toBoolean(""));
        assertThatIllegalArgumentException().isThrownBy(() -> StringUtils.toBoolean("foo"));
        assertThatIllegalArgumentException().isThrownBy(() -> StringUtils.toBoolean("2"));
        assertThatIllegalArgumentException().isThrownBy(() -> StringUtils.toBoolean("fals"));
    }
}
