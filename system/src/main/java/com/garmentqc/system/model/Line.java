package com.garmentqc.system.model;

import com.garmentqc.system.enums.Section;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lines")
@Getter
@Setter
@NoArgsConstructor
public class Line extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Section section;

    @Column(nullable = false)
    private boolean active = true;

}
