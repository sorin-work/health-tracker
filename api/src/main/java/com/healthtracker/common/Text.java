package com.healthtracker.common;

/** Small helpers for the free-text fields every feature has (notes, titles, dosages). */
public final class Text {

    private Text() {}

    /** Trims the value; blank or missing becomes null so empty notes are not stored as "". */
    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
