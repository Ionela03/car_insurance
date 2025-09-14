package com.example.carins.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimHistoryDto(
    String type,
    LocalDate claimDate,
    BigDecimal amount,
    String description
) {}