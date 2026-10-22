package com.garmentqc.system.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InspectionReportResponse {

    private String reportType;

    private LocalDate startDate;

    private LocalDate endDate;

    private long totalInspection;

    private long totalQuantityInspect;

    private long totalTransferQuantity;

    private long totalDefects;

    private long goodQc;

    private double defectPercentage;

    private List<SectionSummary> sectionSummaries;

    private List<LineSummary> lineSummaries;

    private List<DefectSummary> defectSummaries;

    private List<TimeSlotSummary> timeSlotSummaries;

    private List<WashingTypeSummary> washingTypeSummaries;

    @Getter
    @AllArgsConstructor
    public static class SectionSummary {
        private String section;

        private long totalInspect;

        private long totalQuantityInspect;

        private long totalTransferQuantity;

        private long totalDefects;

        private long goodQc;

        private double defectPercentage;
    }

    @Getter
    @AllArgsConstructor
    public static class LineSummary {

        private String lineId;

        private String lineName;

        private String section;

        private long totalInspections;

        private long totalQuantityInspect;

        private long totalTransferQuantity;

        private long totalDefects;

        private long goodQc;

        private double defectPercentage;
    }

    @Getter
    @AllArgsConstructor
    public static class DefectSummary {

        private String defectTypeId;

        private String defectTypeName;

        private long totalDefects;
    }

    @Getter
    @AllArgsConstructor
    public static class TimeSlotSummary {

        private String inspectionTimeSlot;

        private long totalInspections;

        private long totalQuantityInspect;

        private long totalTransferQuantity;

        private long totalDefects;

        private long goodQc;

        private double defectPercentage;
    }

    @Getter
    @AllArgsConstructor
    public static class WashingTypeSummary {

        private String washingType;

        private long totalInspections;

        private long totalQuantityInspect;

        private long totalTransferQuantity;

        private long totalDefects;

        private long goodQc;

        private double defectPercentage;
    }

}
