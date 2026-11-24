
package com.garmentqc.system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.garmentqc.system.enums.AccessoryStatus;
import com.garmentqc.system.enums.DefectSeverity;
import com.garmentqc.system.enums.FinalInspectionPurpose;
import com.garmentqc.system.enums.FinalInspectionResult;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinalInspectionResponse {

    private String id;

    private LocalDate inspectionDate;

    private String buyerId;

    private String buyerName;

    private String styleId;

    private String styleCode;

    private Integer sortOrder;

    private List<ColorItem> colors;

    private String poNumber;

    private LocalDate deliveryDate;

    private String contentFabric;

    private FinalInspectionPurpose purpose;

    private String purposeLabel;

    private Integer lotBatchSize;

    private BigDecimal aqlLevel;

    private String samplingCodeLetter;

    private Integer samplingSampleSize;

    private Integer acceptanceNumber;

    private Integer rejectionNumber;

    private Integer cartonSampling;

    private Integer totalInspected;

    private Integer totalMajor;

    private Integer totalMinor;

    private Integer totalDefect;

    private List<StyleItem> styles;

    @Getter
    @Setter
    public static class StyleItem {

        private String id;

        private String styleId;

        private String styleCode;

        private Integer sortOrder;

        private List<ColorItem> colors;
    }

    @Getter
    @Setter
    public static class ColorItem {

        private String id;

        private String styleColorId;

        private String colorName;

        private Integer qtyOrder;

        private Integer qtyReady;
    }

    private List<AccessoryItem> accessories;

    @Getter
    @Setter
    public static class AccessoryItem {

        private String id;

        private String accessoryId;

        private String accessoryName;

        private String category;

        private AccessoryStatus status;
    }

    private List<DefectItem> defects;

    @Getter
    @Setter
    public static class DefectItem {

        private String id;

        private String defectTypeId;

        private String defectAreaId;

        private String defectAreaName;

        private String defectName;

        private DefectSeverity severity;

        private Integer quantity;
    }

    private String additionalComment;

    private FinalInspectionResult inspectionResult;

    // private String shipmentStatus;

    private String inspectedBy;

    private String finishingIncharge;
}
