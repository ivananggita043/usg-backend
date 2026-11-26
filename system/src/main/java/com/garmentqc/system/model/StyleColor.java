
package com.garmentqc.system.model;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "style_colors", uniqueConstraints = {
        @UniqueConstraint(name = "uk_style_color_name", columnNames = { "style_id", "color_name" })
})
@Getter
@Setter
@NoArgsConstructor
@BatchSize(size = 50)
public class StyleColor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Column(name = "color_name", nullable = false, length = 100)
    private String colorName;
}