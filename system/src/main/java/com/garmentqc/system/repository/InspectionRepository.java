
package com.garmentqc.system.repository;

import com.garmentqc.system.model.Inspection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionRepository extends JpaRepository<Inspection, String> {
    
}