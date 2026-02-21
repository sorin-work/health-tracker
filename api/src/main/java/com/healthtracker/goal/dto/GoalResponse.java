package com.healthtracker.goal.dto;

import com.healthtracker.goal.Goal;
import com.healthtracker.goal.GoalProgress;
import com.healthtracker.goal.GoalType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record GoalResponse(
        Long id,
        GoalType type,
        String unit,
        BigDecimal targetValue,
        BigDecimal startValue,
        LocalDate startDate,
        LocalDate targetDate,
        String notes,
        boolean archived,
        Instant achievedAt,
        BigDecimal currentValue,
        double progressPercent,
        boolean achieved,
        Boolean onTrack) {

    public static GoalResponse from(Goal g, GoalProgress p) {
        return new GoalResponse(
                g.getId(),
                g.getType(),
                g.getType().unit(),
                g.getTargetValue(),
                g.getStartValue(),
                g.getStartDate(),
                g.getTargetDate(),
                g.getNotes(),
                g.isArchived(),
                g.getAchievedAt(),
                p.currentValue(),
                p.percent(),
                p.achieved(),
                p.onTrack());
    }
}
