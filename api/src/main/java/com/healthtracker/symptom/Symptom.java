package com.healthtracker.symptom;

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
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "symptoms")
public class Symptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String name;

    /** 1 (barely noticeable) to 10 (worst imaginable). */
    @Column(nullable = false)
    private Integer severity;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(length = 500)
    private String notes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "symptom_tags", joinColumns = @JoinColumn(name = "symptom_id"))
    @Column(name = "tag", nullable = false, length = 30)
    @OrderBy
    private Set<String> tags = new TreeSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Symptom() {}

    public Symptom(Long userId, Instant createdAt) {
        this.userId = userId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSeverity() {
        return severity;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Collection<String> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
