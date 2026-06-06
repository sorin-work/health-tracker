package com.healthtracker.water.dto;

import com.healthtracker.water.WaterIntake;
import java.time.Instant;

public record WaterEntry(Long id, int amountMl, Instant loggedAt) {

    public static WaterEntry from(WaterIntake w) {
        return new WaterEntry(w.getId(), w.getAmountMl(), w.getLoggedAt());
    }
}
