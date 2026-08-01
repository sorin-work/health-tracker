package com.healthtracker.medication;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "medications")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 60)
    private String dosage;

    @Column(length = 500)
    private String instructions;

    /** Taken only when needed (e.g. a painkiller). Has no schedule and no adherence. */
    @Column(name = "as_needed", nullable = false)
    private boolean asNeeded;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Last day of the course, inclusive. Null means ongoing. */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 16)
    private ScheduleType scheduleType = ScheduleType.DAILY;

    /** For {@link ScheduleType#EVERY_N_DAYS}: 2 means every other day. */
    @Column(name = "interval_days")
    private Integer intervalDays;

    /** For {@link ScheduleType#WEEKLY}. */
    @Convert(converter = DaysOfWeekConverter.class)
    @Column(name = "days_of_week", length = 80)
    private Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "medication_times", joinColumns = @JoinColumn(name = "medication_id"))
    @Column(name = "time_of_day", nullable = false)
    @OrderBy
    private List<LocalTime> times = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Medication() {}

    public Medication(Long userId, String name, Instant createdAt) {
        this.userId = userId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public boolean isScheduledOn(LocalDate day) {
        if (asNeeded || day.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && day.isAfter(endDate)) {
            return false;
        }
        return switch (scheduleType) {
            case DAILY -> true;
            case EVERY_N_DAYS -> intervalDays == null
                    || ChronoUnit.DAYS.between(startDate, day) % intervalDays == 0;
            case WEEKLY -> daysOfWeek.contains(day.getDayOfWeek());
        };
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public boolean isAsNeeded() {
        return asNeeded;
    }

    public void setAsNeeded(boolean asNeeded) {
        this.asNeeded = asNeeded;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<LocalTime> getTimes() {
        return times;
    }

    public void setTimes(List<LocalTime> times) {
        this.times.clear();
        this.times.addAll(times);
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek == null || daysOfWeek.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(daysOfWeek);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
