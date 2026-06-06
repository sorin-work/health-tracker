package com.healthtracker.water.dto;

import java.time.LocalDate;

public record WaterTotal(LocalDate date, int totalMl) {}
