package com.healthtracker.mood.dto;

import com.healthtracker.mood.MoodEntry;
import java.time.Instant;

public record MoodResponse(Long id, int mood, Integer energy, Instant recordedAt, String note) {

    public static MoodResponse from(MoodEntry e) {
        return new MoodResponse(e.getId(), e.getMood(), e.getEnergy(), e.getRecordedAt(), e.getNote());
    }
}
