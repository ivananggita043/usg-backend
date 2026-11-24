
package com.garmentqc.system.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "aql_levels")
public class AqlLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aql_value", nullable = false, unique = true, precision = 5, scale = 3)
    private BigDecimal aqlValue;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public BigDecimal getAqlValue() {
        return aqlValue;
    }

    public void setAqlValue(BigDecimal aqlValue) {
        this.aqlValue = aqlValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
