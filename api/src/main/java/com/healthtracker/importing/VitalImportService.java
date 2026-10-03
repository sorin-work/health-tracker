package com.healthtracker.importing;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.CsvReader;
import com.healthtracker.common.CsvReader.Row;
import com.healthtracker.importing.ImportResult.RowError;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.GlucoseContext;
import com.healthtracker.vital.VitalReading;
import com.healthtracker.vital.VitalReadingRepository;
import com.healthtracker.vital.VitalType;
import com.healthtracker.vital.VitalValidator;
import com.healthtracker.vital.dto.VitalReadingRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports vital readings from CSV. The columns are the ones the vitals export writes (so an export
 * can be imported into another account), in any order; only measured_at, type and value are needed.
 */
@Service
public class VitalImportService {

    static final int MAX_ROWS = 10_000;
    static final int PREVIEW_ROWS = 10;
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd[ ]['T']HH:mm[:ss]");

    private static final Logger log = LoggerFactory.getLogger(VitalImportService.class);

    private final VitalReadingRepository readings;
    private final UserService userService;
    private final Clock clock;

    public VitalImportService(VitalReadingRepository readings, UserService userService, Clock clock) {
        this.readings = readings;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public ImportResult importCsv(Long userId, String csv, boolean dryRun) {
        List<Row> rows = CsvReader.parse(csv);
        if (rows.isEmpty()) {
            throw ApiException.badRequest("The file is empty");
        }
        Map<String, Integer> columns = columns(rows.getFirst());
        List<Row> data = rows.subList(1, rows.size()).stream().filter(r -> !r.isBlank()).toList();
        if (data.size() > MAX_ROWS) {
            throw ApiException.badRequest("Import at most " + MAX_ROWS + " rows at a time");
        }

        ZoneId zone = userService.zoneOf(userId);
        Instant now = clock.instant();
        List<RowError> errors = new ArrayList<>();
        List<VitalReadingRequest> parsed = new ArrayList<>();
        for (Row row : data) {
            try {
                VitalReadingRequest request = toRequest(row, columns, zone);
                VitalValidator.validate(request, now);
                parsed.add(request);
            } catch (ApiException | IllegalArgumentException | DateTimeParseException e) {
                errors.add(new RowError(row.line(), message(e)));
            }
        }

        List<VitalReadingRequest> fresh = withoutDuplicates(userId, parsed);
        int duplicates = parsed.size() - fresh.size();
        boolean save = !dryRun && errors.isEmpty();
        if (save) {
            List<VitalReading> entities = new ArrayList<>(fresh.size());
            for (VitalReadingRequest r : fresh) {
                VitalReading reading = new VitalReading(userId, r.type(), now);
                reading.setValue(r.value());
                reading.setSecondaryValue(r.secondaryValue());
                reading.setGlucoseContext(r.glucoseContext());
                reading.setMeasuredAt(r.measuredAt());
                reading.setNotes(r.notes());
                entities.add(reading);
            }
            readings.saveAll(entities);
            log.info("Imported {} readings for user {} ({} duplicates skipped)", entities.size(), userId, duplicates);
        }
        return new ImportResult(
                !save,
                data.size(),
                save ? fresh.size() : 0,
                duplicates,
                errors,
                fresh.stream().limit(PREVIEW_ROWS).toList());
    }

    private static Map<String, Integer> columns(Row header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.cells().size(); i++) {
            columns.put(header.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        for (String required : List.of("measured_at", "type", "value")) {
            if (!columns.containsKey(required)) {
                throw ApiException.badRequest("The first row needs a '" + required + "' column");
            }
        }
        return columns;
    }

    private static VitalReadingRequest toRequest(Row row, Map<String, Integer> columns, ZoneId zone) {
        VitalType type = VitalType.valueOf(cell(row, columns, "type").toUpperCase(Locale.ROOT).replace(' ', '_'));
        String secondary = cell(row, columns, "secondary_value");
        String context = cell(row, columns, "glucose_context");
        String notes = unprotect(cell(row, columns, "notes"));
        return new VitalReadingRequest(
                type,
                number(cell(row, columns, "value")),
                secondary.isEmpty() ? null : number(secondary),
                context.isEmpty() ? null : GlucoseContext.valueOf(context.toUpperCase(Locale.ROOT)),
                instant(cell(row, columns, "measured_at"), zone),
                notes.isEmpty() ? null : notes.length() > 500 ? notes.substring(0, 500) : notes);
    }

    private static String cell(Row row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null ? "" : row.get(index).trim();
    }

    private static BigDecimal number(String value) {
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + value + "' is not a number");
        }
    }

    /** Local times like {@code 2026-02-01 07:30} are in the user's zone; ISO instants are used as they are. */
    private static Instant instant(String value, ZoneId zone) {
        if (value.endsWith("Z") || value.matches(".*[+-]\\d{2}:\\d{2}$")) {
            return OffsetDateTime.parse(value).toInstant();
        }
        return LocalDateTime.parse(value, LOCAL).atZone(zone).toInstant();
    }

    /** The export prefixes cells such as {@code =x} with a quote so spreadsheets do not run them. */
    private static String unprotect(String value) {
        return value.length() > 1 && value.charAt(0) == '\'' && "=+-@".indexOf(value.charAt(1)) >= 0
                ? value.substring(1)
                : value;
    }

    /** Drops rows that are already stored (same type, time and value), and repeats within the file. */
    private List<VitalReadingRequest> withoutDuplicates(Long userId, List<VitalReadingRequest> parsed) {
        if (parsed.isEmpty()) {
            return parsed;
        }
        Instant first = parsed.stream().map(VitalReadingRequest::measuredAt).min(Instant::compareTo).orElseThrow();
        Instant last = parsed.stream().map(VitalReadingRequest::measuredAt).max(Instant::compareTo).orElseThrow();
        Set<String> seen = new HashSet<>();
        for (VitalReading r : readings.findInRange(userId, null, first, last.plusSeconds(1))) {
            seen.add(key(r.getType(), r.getMeasuredAt(), r.getValue()));
        }
        List<VitalReadingRequest> fresh = new ArrayList<>();
        for (VitalReadingRequest r : parsed) {
            if (seen.add(key(r.type(), r.measuredAt(), r.value()))) {
                fresh.add(r);
            }
        }
        return fresh;
    }

    private static String key(VitalType type, Instant at, BigDecimal value) {
        return type + "@" + at.getEpochSecond() + "=" + value.stripTrailingZeros().toPlainString();
    }

    private static String message(Exception e) {
        if (e instanceof IllegalArgumentException && e.getMessage() != null && e.getMessage().startsWith("No enum")) {
            return "Unknown type or meal context";
        }
        if (e instanceof DateTimeParseException) {
            return "Dates look like 2026-02-01 07:30";
        }
        return e.getMessage();
    }
}
