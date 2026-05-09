package com.healthtracker.medication;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
        return endDate == null || !day.isAfter(endDate);
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
