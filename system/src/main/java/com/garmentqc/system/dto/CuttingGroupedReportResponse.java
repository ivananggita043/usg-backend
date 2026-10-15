
package com.garmentqc.system.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CuttingGroupedReportResponse {

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

        private long totalInspections;

        private long totalItems;

        private long totalQuantityInspect; 

        private long totalDefects;

        private double defectPercentage;
    }
}
