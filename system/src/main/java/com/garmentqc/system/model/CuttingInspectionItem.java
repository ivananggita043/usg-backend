
package com.garmentqc.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cutting_inspection_item")
@Getter
@Setter
@NoArgsConstructor
public class CuttingInspectionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cutting_inspection_id", nullable = false)
    private CuttingInspection cuttingInspection;

    @Column(name = "buyer_name", nullable = false, length = 100)
    private String buyerName;

    @Column(name = "style_code", nullable = false, length = 100)
    private String styleCode;

    @Column(name = "color_name", nullable = false, length = 100)
    private String colorName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cutting_defect_type_id", nullable = false)
    private CuttingDefectType defectType;

    @Column(name = "lot", nullable = false, length = 100)
    private String lot;

    @Column(name = "operator_name", nullable = false, length = 100)
    private String operatorName;

    @Column(name = "quantity_inspect", nullable = false)
    private Integer quantityInspect;
}