
package com.garmentqc.system.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "final_inspection_styles")
@Getter
@Setter
@NoArgsConstructor
public class FinalInspectionStyle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "final_inspection_id", nullable = false)
    private FinalInspection finalInspection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Column(name = "sort_order")
    private Integer sortOrder;
    @OneToMany(mappedBy = "finalInspectionStyle", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<FinalInspectionStyleColor> colors = new ArrayList<>();
}