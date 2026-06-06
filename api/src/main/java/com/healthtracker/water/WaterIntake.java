package com.healthtracker.water;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "water_intake")
public class WaterIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount_ml", nullable = false)
    private Integer amountMl;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WaterIntake() {}

    public WaterIntake(Long userId, int amountMl, Instant loggedAt, Instant createdAt) {
        this.userId = userId;
        this.amountMl = amountMl;
        this.loggedAt = loggedAt;
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

    public Integer getAmountMl() {
        return amountMl;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
