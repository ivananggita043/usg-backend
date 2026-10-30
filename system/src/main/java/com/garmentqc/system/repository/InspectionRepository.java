
package com.garmentqc.system.repository;

import com.garmentqc.system.model.Inspection;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionRepository extends JpaRepository<Inspection, String> {

    List<Inspection> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant start, Instant end);

    boolean existsByBuyerNameIgnoreCase(String buyerName);

    boolean existsByStyleCodeIgnoreCase(String styleCode);

    boolean existsByBuyerNameIgnoreCaseAndStyleCodeIgnoreCase(String buyerName, String styleCode);

}