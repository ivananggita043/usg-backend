
package com.garmentqc.system.model;

import com.garmentqc.system.enums.CuttingProcessCheck;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cutting_defect_types", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cutting_process_defect", columnNames = { "process_check", "name" })
})

@Getter
@Setter
@NoArgsConstructor
public class CuttingDefectType extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "process_check", length = 30)
    private CuttingProcessCheck processCheck;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, name = "is_active")
    private boolean active = true;
}