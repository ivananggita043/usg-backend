
package com.garmentqc.system.model;

import com.garmentqc.system.enums.AccessoryStatus;

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
@Table(name = "final_inspection_accessories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_final_inspection_accessory", columnNames = { "final_inspection_id",
                "accessory_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class FinalInspectionAccessory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "final_inspection_id", nullable = false)
    private FinalInspection finalInspection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accessory_id", nullable = false)
    private Accessory accessory;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private AccessoryStatus status;
}
