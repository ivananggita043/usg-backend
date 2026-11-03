package com.garmentqc.system.dto;

public record ProductivitySummaryResponse(

        String username,

        String nama,

        String section,

        String date,

        Integer inspectedQuantity,

        Integer effectiveMinutes,

        Double targetQuantity,

        Double productivityRate,

        Integer remainingQuantity)  {}