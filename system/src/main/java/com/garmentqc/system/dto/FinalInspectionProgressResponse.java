package com.garmentqc.system.dto;

public record FinalInspectionProgressResponse(

        String poNumber,

        boolean preFinal20,

        boolean reFinal20,

        boolean preFinal60,

        boolean reFinal60,

        boolean preFinal80,

        boolean reFinal80) {
}