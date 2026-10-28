package com.garmentqc.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.Buyer;

public interface BuyerRepository extends JpaRepository<Buyer, String> {

    boolean existsByNameIgnoreCase(String name);
    
}
