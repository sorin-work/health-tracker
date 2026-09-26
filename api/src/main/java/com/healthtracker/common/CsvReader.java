package com.healthtracker.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 reader, the counterpart of {@link CsvWriter}: quoted fields may contain commas,
 * quotes ({@code ""}) and line breaks. Accepts CRLF or LF line endings and a leading byte order mark.
 */
public final class CsvReader {

    private CsvReader() {}

    /** A parsed row with the 1-based line number it started on, for error messages. */
    public record Row(int line, List<String> cells) {

        public String get(int index) {
            return index >= 0 && index < cells.size() ? cells.get(index) : "";
        }

        public boolean isBlank() {
            return cells.stream().allMatch(String::isBlank);
        }
    }

    public static List<Row> parse(String text) {
        String s = text.startsWith("﻿") ? text.substring(1) : text;
        List<Row> rows = new ArrayList<>();
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int rowStart = 1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    cell.append(c);
                }
            } else if (c == '"' && cell.isEmpty()) {
                quoted = true;
            } else if (c == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                cells.add(cell.toString());
                cell.setLength(0);
                rows.add(new Row(rowStart, List.copyOf(cells)));
                cells.clear();
                line++;
                rowStart = line;
            } else {
                cell.append(c);
            }
        }
        if (quoted) {
            throw ApiException.badRequest("Line " + rowStart + ": a quoted value is never closed");
        }
        if (!cell.isEmpty() || !cells.isEmpty()) {
            cells.add(cell.toString());
            rows.add(new Row(rowStart, List.copyOf(cells)));
        }
        return rows;
    }
}
