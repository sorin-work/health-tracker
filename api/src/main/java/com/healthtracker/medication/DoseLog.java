package com.healthtracker.medication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "dose_logs")
public class DoseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medication_id", nullable = false)
    private Long medicationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The slot this log answers. For as-needed medications it is simply when it was taken. */
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DoseStatus status;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(length = 500)
    private String note;

    protected DoseLog() {}

    public DoseLog(Long medicationId, Long userId, Instant scheduledAt) {
        this.medicationId = medicationId;
        this.userId = userId;
        this.scheduledAt = scheduledAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getNote() {
        return note;
    }

    public void record(DoseStatus status, Instant recordedAt, String note) {
        this.status = status;
        this.recordedAt = recordedAt;
        this.note = note;
    }
}
