package com.healthtracker.user;

import com.healthtracker.common.ApiException;
import java.time.DateTimeException;
import java.time.ZoneId;

final class Timezones {

    private Timezones() {}

    static String orDefault(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return "UTC";
        }
        return validate(timezone);
    }

    static String validate(String timezone) {
        try {
            return ZoneId.of(timezone.trim()).getId();
        } catch (DateTimeException e) {
            throw ApiException.badRequest("Unknown time zone '" + timezone + "'");
        }
    }
}
