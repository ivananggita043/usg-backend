package com.garmentqc.system.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InspectionResponse {

    private String id;

    private String buyerName;

    private String styleCode;

    private String colorName;

    private String lineName;

    private String section;

    private Integer inspectQuantity;

    private Integer totalDefect;

    private Double defectPercentage;

    private Integer transferQuantity;

    private String comment;

    private String inspectionTimeSlot;

    private String handfeel;

    private String grade;

    private String process;

    private List<DefectItem> defects;

    private String submittedById;

    private String submittedByName;

    private Instant createdAt;

    private Instant updatedAt;

    @Getter
    @AllArgsConstructor
    public static class DefectItem {

        private String id;

        private String defectTypeId;

        private Integer quantity;
    }
}