package com.garmentqc.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "defect_areas", uniqueConstraints = {
                @UniqueConstraint(name = "uk_defect_area_name", columnNames = {
                                "name"
                })
})

@Getter
@Setter
@NoArgsConstructor
public class DefectArea extends BaseEntity {

        @Column(nullable = false, length = 100)
        private String name;

        @Column(nullable = false, name = "is_active")
        private boolean active = true;
}