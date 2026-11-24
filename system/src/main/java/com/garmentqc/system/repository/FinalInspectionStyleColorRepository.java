
package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.FinalInspectionStyleColor;

public interface FinalInspectionStyleColorRepository extends JpaRepository<FinalInspectionStyleColor, String> {

        List<FinalInspectionStyleColor> findByFinalInspectionStyle_Id(String finalInspectionStyleId);

        boolean existsByFinalInspectionStyle_IdAndStyleColor_ColorNameIgnoreCase(String finalInspectionStyleId,
                        String colorName);
}
