
package com.garmentqc.system.model;

import com.garmentqc.system.enums.AccessoryCategory;

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
@Table(name = "accessories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_accessory_category_name", columnNames = { "category", "name" })
})
@Getter
@Setter
@NoArgsConstructor
public class Accessory extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessoryCategory category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
