package com.garmentqc.system.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "styles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_buyer_style_code", columnNames = { "buyer_id", "style_code" })
})

@Getter
@Setter
@NoArgsConstructor
public class Style extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Buyer buyer;

    @Column(name = "style_code", nullable = false, length = 100)
    private String styleCode;

    @OneToMany(mappedBy = "style", cascade = CascadeType.ALL)
    private List<StyleColor> colors = new ArrayList<>();

}
