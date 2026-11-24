
package com.garmentqc.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.FinalInspectionStyle;

public interface FinalInspectionStyleRepository extends JpaRepository<FinalInspectionStyle, String> {

    boolean existsByFinalInspection_IdAndStyle_StyleCodeIgnoreCase(String finalInspectionId, String styleCode);
}
