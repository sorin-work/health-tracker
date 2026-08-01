package com.healthtracker.medication;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class DaysOfWeekConverterTest {

    private final DaysOfWeekConverter converter = new DaysOfWeekConverter();

    @Test
    void storesDaysInWeekOrder() {
        assertThat(converter.convertToDatabaseColumn(EnumSet.of(DayOfWeek.THURSDAY, DayOfWeek.MONDAY)))
                .isEqualTo("MONDAY,THURSDAY");
        assertThat(converter.convertToDatabaseColumn(EnumSet.noneOf(DayOfWeek.class))).isNull();
    }

    @Test
    void readsThemBack() {
        assertThat(converter.convertToEntityAttribute("MONDAY,THURSDAY"))
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }
}
