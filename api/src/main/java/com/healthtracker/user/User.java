package com.healthtracker.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneId;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_system", nullable = false, length = 16)
    private UnitSystem unitSystem = UnitSystem.METRIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "glucose_unit", nullable = false, length = 16)
    private GlucoseUnit glucoseUnit = GlucoseUnit.MG_DL;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {}

    public User(String email, String passwordHash, String displayName, String timezone, Instant createdAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.timezone = timezone;
        this.createdAt = createdAt;
    }

    public ZoneId zone() {
        return ZoneId.of(timezone);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public UnitSystem getUnitSystem() {
        return unitSystem;
    }

    public void setUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
    }

    public GlucoseUnit getGlucoseUnit() {
        return glucoseUnit;
    }

    public void setGlucoseUnit(GlucoseUnit glucoseUnit) {
        this.glucoseUnit = glucoseUnit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
