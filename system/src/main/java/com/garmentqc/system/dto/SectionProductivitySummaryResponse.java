package com.garmentqc.system.dto;

public record SectionProductivitySummaryResponse(
        String section,

        String date,

        int totalOperators,

        int activeOperators,

        int completedOperators,

        int totalInspectedQuantity,

        int totalTargetQuantity,

        double productivityRate,

        int remainingQuantity) {
}