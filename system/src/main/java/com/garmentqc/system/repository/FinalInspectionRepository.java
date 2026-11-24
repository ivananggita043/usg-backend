package com.garmentqc.system.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.enums.FinalInspectionPurpose;
import com.garmentqc.system.model.FinalInspection;

public interface FinalInspectionRepository extends JpaRepository<FinalInspection, String> {

    boolean existsByPoNumberAndPurpose(String poNumber, FinalInspectionPurpose purpose);

    boolean existsByPoNumberAndPurposeAndIdNot(String poNumber,
            FinalInspectionPurpose purpose,
            String id);

    List<FinalInspection> findByInspectionDateBetween(
            LocalDate startDate,
            LocalDate endDate);
}