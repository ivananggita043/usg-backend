
package com.garmentqc.system.model;

import com.garmentqc.system.enums.CuttingPanel;

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
@Table(name = "cutting_inspection_defects", uniqueConstraints = {
                @UniqueConstraint(name = "uk_cutting_item_panel", columnNames = {
                                "cutting_inspection_item_id",
                                "panel"
                })
})

@Getter
@Setter
@NoArgsConstructor
public class CuttingInspectionDefect extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "cutting_inspection_item_id", nullable = false)
        private CuttingInspectionItem inspectionItem;

        @Enumerated(EnumType.STRING)
        @Column(name = "panel", nullable = false, length = 30)
        private CuttingPanel panel;

        @Column(name = "quantity", nullable = false)
        private Integer quantity;
}  