
package com.garmentqc.system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.garmentqc.system.enums.AccessoryStatus;
import com.garmentqc.system.enums.DefectSeverity;
import com.garmentqc.system.enums.FinalInspectionPurpose;
import com.garmentqc.system.enums.FinalInspectionResult;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinalInspectionRequest {

    @NotNull
    private LocalDate inspectionDate;

    private String poNumber;

    private LocalDate deliveryDate;

    @NotNull
    private String buyerId;

    private String contentFabric;

    @NotNull
    private FinalInspectionPurpose purpose;

    private Integer lotBatchSize;

    @NotNull
    @Positive
    private BigDecimal aqlLevel;

    @PositiveOrZero
    private Integer cartonSampling;

    @PositiveOrZero
    private Integer totalInspected;

    @Valid
    private List<DefectItem> defects;

    @Valid
    private List<StyleItem> styles;

    @Getter
    @Setter
    public static class StyleItem {

        @NotBlank
        private String styleId;

        @Valid
        private List<ColorItem> colors;
    }

    @Getter
    @Setter
    public static class ColorItem {

        @NotBlank
        private String styleColorId;

        @PositiveOrZero
        private Integer qtyOrder;

        @PositiveOrZero
        private Integer qtyReady;
    }

    @Getter
    @Setter
    @Schema(name = "FinalInspectionDefectItem")
    public static class DefectItem {

        @NotBlank
        private String defectTypeId;

        @NotNull
        private DefectSeverity severity;

        @NotNull
        @Positive
        private Integer quantity;
    }

    @Valid
    private List<AccessoryItem> accessories;

    @Getter
    @Setter
    public static class AccessoryItem {

        @NotBlank
        private String accessoryId;

        @NotNull
        private AccessoryStatus status;
    }

    private String additionalComment;

    private FinalInspectionResult inspectionResult;

    private String inspectedBy;

    private String finishingIncharge;
}
