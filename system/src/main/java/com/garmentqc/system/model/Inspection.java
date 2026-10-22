package com.garmentqc.system.model;

import java.util.ArrayList;
import java.util.List;

import com.garmentqc.system.enums.WashingType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inspections")
@Getter
@Setter
@NoArgsConstructor
public class Inspection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private Line line;

    @Min(1)
    @Column(name = "inspect_quantity", nullable = false)
    private Integer inspectQuantity;

    @Min(0)
    @Column(name = "transfer_quantity", nullable = false)
    private Integer transferQuantity = 0;

    @Column(name = "comment", length = 500)
    private String comment;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InspectionDefect> inspectionDefects = new ArrayList<>();

    @Column(name = "buyer_name", nullable = false, length = 100)
    private String buyerName;

    @Column(name = "style_code", nullable = false, length = 100)
    private String styleCode;

    @Column(name = "color_name", nullable = false, length = 100)
    private String colorName;

    @Column(name = "inspection_time_slot", length = 20)
    private String inspectionTimeSlot;

    @Column(name = "handfeel", length = 100)
    private String handfeel;

    @Column(name = "grade", length = 50)
    private String grade;

    @Column(name = "process", length = 20)
    private String process;

    @Enumerated(EnumType.STRING)
    @Column(name = "washing_type", length = 10)
    private WashingType washingType;

    @Min(0)
    @Column(name = "qty_check_carton_today")
    private Integer qtyCheckCartonToday;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;
}
