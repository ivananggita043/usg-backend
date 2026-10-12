package com.garmentqc.system.model;

import com.garmentqc.system.enums.DefectCategory;
import com.garmentqc.system.enums.Section;

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
@Table(name = "defect_types")
@Getter
@Setter
@NoArgsConstructor
public class DefectType extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Section section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DefectCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defect_area_id")

    private DefectArea defectArea;

    @Column(nullable = false, length = 200)

    private String name;

    @Column(nullable = false, name = "is_active")

    private boolean active = true;
}
