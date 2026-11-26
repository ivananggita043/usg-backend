
package com.garmentqc.system.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.garmentqc.system.enums.FinalInspectionPurpose;
import com.garmentqc.system.enums.FinalInspectionResult;

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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "final_inspections")
@Getter
@Setter
@NoArgsConstructor
public class FinalInspection extends BaseEntity {

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @Column(name = "po_number", length = 100)
    private String poNumber;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "content_fabric", length = 255)
    private String contentFabric;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private FinalInspectionPurpose purpose;

    @Column(name = "aql_level", precision = 5, scale = 3)
    private BigDecimal aqlLevel;

    @Column(name = "carton_sampling")
    private Integer cartonSampling;

    @Column(name = "total_inspected")
    private Integer totalInspected;

    @Column(name = "total_defect")
    private Integer totalDefect;
    @OneToMany(mappedBy = "finalInspection", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<FinalInspectionStyle> styles = new ArrayList<>();

    @OneToMany(mappedBy = "finalInspection", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<FinalInspectionAccessory> accessories = new ArrayList<>();

    @OneToMany(mappedBy = "finalInspection", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<FinalInspectionDefect> defects = new ArrayList<>();

    @Column(name = "additional_comment", columnDefinition = "TEXT")
    private String additionalComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_result", length = 30)
    private FinalInspectionResult inspectionResult;

    @Column(name = "shipment_status", length = 20)
    private String shipmentStatus;

    @Column(name = "lot_batch_size", nullable = false)
    private Integer lotBatchSize;

    @Column(name = "sampling_code_letter", length = 2)
    private String samplingCodeLetter;

    @Column(name = "sampling_sample_size")
    private Integer samplingSampleSize;

    @Column(name = "acceptance_number")
    private Integer acceptanceNumber;

    @Column(name = "rejection_number")
    private Integer rejectionNumber;

    @Column(name = "inspected_by", length = 150)
    private String inspectedBy;

    @Column(name = "finishing_incharge", length = 150)
    private String finishingIncharge;

    @Column(name = "total_major")
    private Integer totalMajor;

    @Column(name = "total_minor")
    private Integer totalMinor;

}
