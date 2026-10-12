package com.garmentqc.system.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table (name = "buyer")
@Getter 
@Setter 
@NoArgsConstructor 
public class Buyer extends BaseEntity {

    @Column (nullable = false, length = 100)
    private String name;

    @OneToMany (mappedBy = "buyer", cascade = CascadeType.ALL)
    private List<Style> styles = new ArrayList<>();    
}
