package com.healthtracker.vital;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One measurement. {@code value} holds the main number (systolic for blood pressure, hours for
 * sleep) and {@code secondaryValue} the diastolic pressure.
 */
@Entity
@Table(name = "vital_readings")
public class VitalReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VitalType type;

    @Column(name = "value", nullable = false, precision = 7, scale = 2)
    private BigDecimal value;

    @Column(name = "secondary_value", precision = 7, scale = 2)
    private BigDecimal secondaryValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "glucose_context", length = 32)
    private GlucoseContext glucoseContext;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VitalReading() {}

    public VitalReading(Long userId, VitalType type, Instant createdAt) {
        this.userId = userId;
        this.type = type;
        this.createdAt = createdAt;
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

    public VitalType getType() {
        return type;
    }

    public void setType(VitalType type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getSecondaryValue() {
        return secondaryValue;
    }

    public void setSecondaryValue(BigDecimal secondaryValue) {
        this.secondaryValue = secondaryValue;
    }

    public GlucoseContext getGlucoseContext() {
        return glucoseContext;
    }

    public void setGlucoseContext(GlucoseContext glucoseContext) {
        this.glucoseContext = glucoseContext;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(Instant measuredAt) {
        this.measuredAt = measuredAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
