package com.garmentqc.system.model;

import com.garmentqc.system.enums.Section;

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
@Table(name = "section_productivity_targets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_productivity_section", columnNames = "section")
})
@Getter
@Setter
@NoArgsConstructor
public class SectionProductivityTarget extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private Section section;

    @Column(name = "target_per_hour", nullable = false)
    private Integer targetPerHour = 40;

    @Column(nullable = false)
    private boolean active = true;

}
