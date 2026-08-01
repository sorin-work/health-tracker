package com.healthtracker.medication;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Stores a set of weekdays as {@code MONDAY,THURSDAY}; an empty set is stored as null. */
@Converter
public class DaysOfWeekConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream().sorted().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String value) {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (value != null && !value.isBlank()) {
            Arrays.stream(value.split(",")).map(String::trim).map(DayOfWeek::valueOf).forEach(days::add);
        }
        return days;
    }
}
