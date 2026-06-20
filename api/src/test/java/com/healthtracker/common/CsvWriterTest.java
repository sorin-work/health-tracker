package com.healthtracker.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

    @Test
    void quotesLineBreaks() {
        assertThat(CsvWriter.escape("line one\nline two")).isEqualTo("\"line one\nline two\"");
        assertThat(CsvWriter.escape("windows\r\nbreak")).isEqualTo("\"windows\r\nbreak\"");
    }

    @Test
    void neutralisesSpreadsheetFormulas() {
        assertThat(CsvWriter.escape("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(CsvWriter.escape("+1 dose")).isEqualTo("'+1 dose");
        assertThat(CsvWriter.escape("@home")).isEqualTo("'@home");
    }

    @Test
    void leavesNegativeNumbersAlone() {
        assertThat(CsvWriter.escape(new BigDecimal("-1.5"))).isEqualTo("-1.5");
        assertThat(CsvWriter.escape(-3)).isEqualTo("-3");
    }
}
