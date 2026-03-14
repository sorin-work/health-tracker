package com.healthtracker.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CsvWriterTest {

    @Test
    void writesRowsWithCrlf() {
        String csv = new CsvWriter().row("a", "b").row(1, null, true).toString();

        assertThat(csv).isEqualTo("a,b\r\n1,,true\r\n");
    }

    @Test
    void quotesCommasAndQuotes() {
        assertThat(CsvWriter.escape("Headache, mild")).isEqualTo("\"Headache, mild\"");
        assertThat(CsvWriter.escape("the \"bad\" one")).isEqualTo("\"the \"\"bad\"\" one\"");
    }
}
