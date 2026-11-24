package com.garmentqc.system.dto;

import java.time.LocalDate;
import java.util.List;

import com.garmentqc.system.enums.FinalInspectionPurpose;

public record FinalInspectionReportResponse(

        String reportType,

        LocalDate startDate,

        LocalDate endDate,

        long totalInspections,

        long totalInspected,

        long totalDefects,

        long totalMajor,

        long totalMinor,

        long totalRelease,

        long totalReject,

        long totalPendingResult,

        List<PurposeSummary> purposeSummaries,

        List<BuyerSummary> buyerSummaries,

        List<ResultSummary> resultSummaries) {

    public record PurposeSummary(

            FinalInspectionPurpose purpose,

            String purposeLabel,

            long totalInspections,

            long totalInspected,

            long totalDefects,

            long totalMajor,

            long totalMinor,

            long totalRelease,

            long totalReject,

            long totalPendingResult) {
    }

    public record BuyerSummary(

            String buyerName,

            long totalInspections,

            long totalInspected,

            long totalDefects,

            long totalMajor,

            long totalMinor,

            long totalRelease,

            long totalReject,

            long totalPendingResult) {
    }

    public record ResultSummary(

            String inspectionResult,

            String resultLabel,

            long totalInspections,

            long totalInspected,

            long totalDefects,

            long totalMajor,

            long totalMinor) {
    }
}