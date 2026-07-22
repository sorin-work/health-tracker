package com.healthtracker.export;

import java.util.Locale;

public enum ExportDataset {
    VITALS,
    MEDICATIONS,
    DOSES,
    SYMPTOMS,
    ACTIVITIES,
    MOOD,
    WATER,
    JOURNAL;

    public String fileStem() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ExportDataset fromPath(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
