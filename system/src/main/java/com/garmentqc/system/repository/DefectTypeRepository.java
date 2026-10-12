package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.enums.DefectCategory;
import com.garmentqc.system.enums.Section;
import com.garmentqc.system.model.DefectArea;
import com.garmentqc.system.model.DefectType;

public interface DefectTypeRepository extends JpaRepository<DefectType, String> {

        List<DefectType> findBySection(Section section);

        List<DefectType> findBySectionAndActiveTrue(Section section);

        List<DefectType> findBySectionAndCategory(Section section, DefectCategory category);

        List<DefectType> findBySectionAndCategoryAndActiveTrue(Section section, DefectCategory category);

        List<DefectType> findBySectionAndDefectArea(Section section, DefectArea area);

        List<DefectType> findBySectionAndDefectAreaAndActiveTrue(Section section, DefectArea area);

        List<DefectType> findBySectionAndCategoryAndDefectArea(Section section, DefectCategory category,
                        DefectArea area);

        List<DefectType> findBySectionAndCategoryAndDefectAreaAndActiveTrue(Section section, DefectCategory category,
                        DefectArea area);

        boolean existsBySectionAndCategoryAndDefectAreaIsNullAndName(Section section, DefectCategory category,
                        String name);

        boolean existsBySectionAndCategoryAndDefectAreaIsNullAndNameAndIdNot(Section section, DefectCategory category,
                        String name, String id);

        boolean existsBySectionAndCategoryAndDefectAreaAndName(Section section, DefectCategory category,
                        DefectArea defectArea, String name);

        boolean existsBySectionAndCategoryAndDefectAreaAndNameAndIdNot(Section section, DefectCategory category,
                        DefectArea defectArea, String name, String id);
}
