
package com.garmentqc.system.model;

import com.garmentqc.system.enums.DefectSeverity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "final_inspections_defects", uniqueConstraints = {
                @UniqueConstraint(name = "uk_final_inspection_defect_type_severity", columnNames = {
                                "final_inspection_id",
                                "defect_type_id",
                                "severity"
                })
})
@Getter
@Setter
@NoArgsConstructor
public class FinalInspectionDefect extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "final_inspection_id", nullable = false)
        private FinalInspection finalInspection;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "defect_type_id", nullable = false)
        private DefectType defectType;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 10)
        private DefectSeverity severity;

        @Column(nullable = false)
        private Integer quantity = 0;
}
