package com.healthtracker.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.healthtracker.common.CsvReader.Row;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvReaderTest {

    @Test
    void readsPlainRowsWithEitherLineEnding() {
        List<Row> rows = CsvReader.parse("a,b,c\r\n1,2,3\n4,,6");

        assertThat(rows).extracting(Row::cells).containsExactly(
                List.of("a", "b", "c"), List.of("1", "2", "3"), List.of("4", "", "6"));
    }

    @Test
    void readsQuotedCommasQuotesAndLineBreaks() {
        List<Row> rows = CsvReader.parse("notes,x\r\n\"after a run, tired\",\"say \"\"hi\"\"\"\r\n\"two\nlines\",y\r\n");

        assertThat(rows.get(1).cells()).containsExactly("after a run, tired", "say \"hi\"");
        assertThat(rows.get(2).cells()).containsExactly("two\nlines", "y");
        assertThat(rows.get(2).line()).isEqualTo(3);
    }

    @Test
    void roundTripsWhatTheWriterProduces() {
        String csv = new CsvWriter().row("measured_at", "notes").row("2026-02-01 07:30", "a, \"b\"\nc").toString();

        assertThat(CsvReader.parse(csv).get(1).cells()).containsExactly("2026-02-01 07:30", "a, \"b\"\nc");
    }

    @Test
    void skipsAByteOrderMark() {
        assertThat(CsvReader.parse("﻿type,value\n").getFirst().cells()).containsExactly("type", "value");
    }

    @Test
    void keepsTrackOfLineNumbersAfterMultiLineCells() {
        List<Row> rows = CsvReader.parse("h\n\"a\nb\nc\"\nnext\n");

        assertThat(rows).extracting(Row::line).containsExactly(1, 2, 5);
    }

    @Test
    void rejectsAnUnclosedQuote() {
        assertThatThrownBy(() -> CsvReader.parse("a\n\"oops,1\n")).hasMessageContaining("Line 2");
    }

    @Test
    void missingCellsReadAsEmpty() {
        Row row = CsvReader.parse("1,2").getFirst();

        assertThat(row.get(5)).isEmpty();
        assertThat(CsvReader.parse(",,").getFirst().isBlank()).isTrue();
    }
}
