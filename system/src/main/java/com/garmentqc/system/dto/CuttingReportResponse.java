
package com.garmentqc.system.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.garmentqc.system.enums.CuttingPanel;
import com.garmentqc.system.enums.CuttingProcessCheck;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CuttingReportResponse {

    private String reportType;

    private LocalDate startDate;

    private LocalDate endDate;

    private long totalInspections;

    private long totalItems;

    private long totalQuantityInspect;

    private long totalDefects;

    private double defectPercentage;

    private List<ProcessSummary> processSummaries;

    @Getter
    @AllArgsConstructor
    public static class ProcessSummary {

        private CuttingProcessCheck processCheck;

        private long totalInspections;

        private long totalItems;

        private long totalQuantityInspect;

        private long totalDefects;

        private double defectPercentage;

        private Map<CuttingPanel, Long> panelSummaries;

        private List<DetailRow> detailRows;
    }

    @Getter
    @AllArgsConstructor
    public static class DetailRow {

        private String buyerName;

        private String styleCode;

        private String colorName;

        private String lot;

        private String defectTypeName;

        private Map<CuttingPanel, Long> panelQuantities;

        private long quantityInspect;

        private long totalDefects;

        private double defectPercentage;

        private String operatorName;
    }
}
