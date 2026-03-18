package com.healthtracker.common;

import java.util.List;

/** Minimal RFC 4180 writer. Rows end with CRLF so Excel is happy. */
public final class CsvWriter {

    private final StringBuilder out = new StringBuilder();

    public CsvWriter row(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(escape(values[i]));
        }
        out.append("\r\n");
        return this;
    }

    public CsvWriter row(List<?> values) {
        return row(values.toArray());
    }

    @Override
    public String toString() {
        return out.toString();
    }

    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
