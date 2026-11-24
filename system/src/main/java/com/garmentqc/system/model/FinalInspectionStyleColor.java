
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
@Table(name = "final_inspection_style_colors")
@Getter
@Setter
@NoArgsConstructor
public class FinalInspectionStyleColor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "final_inspection_style_id", nullable = false)
    private FinalInspectionStyle finalInspectionStyle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_color_id", nullable = false)
    private StyleColor styleColor;

    @Column(name = "qty_order")
    private Integer qtyOrder;

    @Column(name = "qty_ready")
    private Integer qtyReady;
}