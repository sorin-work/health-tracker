package com.healthtracker.importing;

import com.healthtracker.vital.dto.VitalReadingRequest;
import java.util.List;

/**
 * What an import did (or, for a dry run, would do). Rows with errors are never saved; the rest are
 * saved only when there are no errors at all, so a file is imported completely or not at all.
 */
public record ImportResult(
        boolean dryRun, int rows, int imported, int duplicates, List<RowError> errors, List<VitalReadingRequest> preview) {

    public record RowError(int line, String message) {}
}
