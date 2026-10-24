
package com.garmentqc.system.dto;

import java.time.Instant;
import java.util.List;

import com.garmentqc.system.enums.CuttingProcessCheck;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CuttingInspectionResponse {

    private String id;

    private CuttingProcessCheck processCheck;

    private String lineId;

    private String lineName;

    private String submittedById;

    private String submittedByName;

    private String comment;

    private List<StyleItemResponse> items;

    private Instant createdAt;

    private Instant updatedAt;

    @Getter
    @AllArgsConstructor
    public static class StyleItemResponse {

        private String id;

        private String buyerName;

        private String styleCode;

        private String colorName;

        private String lot;

        private String operatorName;

        private Integer quantityInspect;

        private Integer quantityDefect;

        private String defectTypeId;

        private String defectTypeName;

        private Double defectPercentage;
    }
}
