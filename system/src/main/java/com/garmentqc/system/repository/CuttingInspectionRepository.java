
package com.garmentqc.system.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.CuttingInspection;

public interface CuttingInspectionRepository extends JpaRepository<CuttingInspection, String> {

    List<CuttingInspection> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant start, Instant end);

    List<CuttingInspection> findBySubmittedById(String submittedById);

    Page<CuttingInspection> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<CuttingInspection> findBySubmittedByIdOrderByCreatedAtDescIdDesc(String submittedById, Pageable pageable);
}
