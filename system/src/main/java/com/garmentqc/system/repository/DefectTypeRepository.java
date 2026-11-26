package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

        List<DefectType> findByDefectArea_IdAndNameContainingIgnoreCaseAndActiveTrue(String defectAreaId, String name);

        List<DefectType> findByDefectAreaAndActiveTrueOrderByNameAsc(DefectArea defectArea);

        @EntityGraph(attributePaths = "defectArea")
        List<DefectType> findByActiveTrueAndDefectArea_Id(String areaId);

        @EntityGraph(attributePaths = "defectArea")
        List<DefectType> findByActiveTrueAndNameContainingIgnoreCase(String name);

        @EntityGraph(attributePaths = "defectArea")
        List<DefectType> findByActiveTrue();

        List<DefectType> findBySectionAndCategoryAndActiveTrueOrderByNameAsc(Section section, DefectCategory category);

        List<DefectType> findBySectionAndActiveTrueOrderByNameAsc(Section section);

        List<DefectType> findBySectionAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(Section section,
                        String name);

        @EntityGraph(attributePaths = "defectArea")
        List<DefectType> findBySectionAndDefectArea_IdAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
                        Section section, String defectAreaId, String name);

        @EntityGraph(attributePaths = "defectArea")
        List<DefectType> findBySectionAndDefectAreaAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
                        Section section, DefectArea defectArea, String name);

        @Query("""
                        SELECT DISTINCT da
                        FROM DefectType dt
                        JOIN dt.defectArea da
                        WHERE dt.active = true
                          AND da.active = true
                          AND (:section IS NULL OR dt.section = :section)
                        ORDER BY da.name ASC
                        """)
        List<DefectArea> findDistinctActiveDefectAreas(
                        @Param("section") Section section);

        @Query("""
                        SELECT DISTINCT da
                        FROM DefectType dt
                        JOIN dt.defectArea da
                        WHERE dt.active = true
                          AND da.active = true
                          AND (:section IS NULL OR dt.section = :section)
                          AND UPPER(da.name) LIKE CONCAT('%', :keyword, '%')
                        ORDER BY da.name ASC
                        """)
        List<DefectArea> findDistinctActiveDefectAreasByKeyword(
                        @Param("section") Section section,
                        @Param("keyword") String keyword);
}
