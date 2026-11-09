package com.healthtracker.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void trimsAndTurnsBlankIntoNull() {
        assertThat(Text.trimToNull("  with food ")).isEqualTo("with food");
        assertThat(Text.trimToNull(" \t\n")).isNull();
        assertThat(Text.trimToNull("")).isNull();
        assertThat(Text.trimToNull(null)).isNull();
    }
}
