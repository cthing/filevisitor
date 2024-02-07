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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


public class StringUtilsTest {

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
