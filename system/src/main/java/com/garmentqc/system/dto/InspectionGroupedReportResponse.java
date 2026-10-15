
package com.garmentqc.system.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InspectionGroupedReportResponse {

    private String reportType;

    private String groupBy;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<GroupSummary> summaries;

    @Getter
    @AllArgsConstructor
    public static class GroupSummary {

        private String buyerName;

        private String styleCode;

        private long totalStyles;

        private long totalInspections;

        private long totalQuantityInspect;

        private long totalTransferQuantity;

        private long totalDefects;

        private double defectPercentage;
    }
}
