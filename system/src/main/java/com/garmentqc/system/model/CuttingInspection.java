package com.garmentqc.system.model;

import com.garmentqc.system.enums.CuttingProcessCheck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cutting_inspections")
@Getter
@Setter
@NoArgsConstructor
public class CuttingInspection extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "proses_check", nullable = false, length = 30)
    private CuttingProcessCheck processCheck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private Line line;

    @Column(length = 500)
    private String comment;
}
