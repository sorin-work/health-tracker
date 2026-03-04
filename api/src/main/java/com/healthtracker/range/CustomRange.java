package com.healthtracker.range;

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

/** A user's own normal bounds for a metric, e.g. targets agreed with their doctor. */
@Entity
@Table(name = "custom_ranges")
public class CustomRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RangeMetric metric;

    @Column(name = "low_value", nullable = false, precision = 7, scale = 2)
    private BigDecimal low;

    @Column(name = "high_value", nullable = false, precision = 7, scale = 2)
    private BigDecimal high;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomRange() {}

    public CustomRange(Long userId, RangeMetric metric) {
        this.userId = userId;
        this.metric = metric;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public RangeMetric getMetric() {
        return metric;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(BigDecimal low, BigDecimal high, Instant now) {
        this.low = low;
        this.high = high;
        this.updatedAt = now;
    }
}
