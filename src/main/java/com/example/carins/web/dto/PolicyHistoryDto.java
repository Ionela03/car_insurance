package com.example.carins.web.dto;

import java.time.LocalDate;

public record PolicyHistoryDto(
    String type,
    LocalDate startDate,
    LocalDate endDate,
    String provider
) {}
