package com.garmentqc.system.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.garmentqc.system.model.CuttingInspection;
import com.garmentqc.system.model.CuttingInspectionItem;

public interface CuttingInspectionItemRepository extends JpaRepository<CuttingInspectionItem, String> {

  List<CuttingInspectionItem> findByCuttingInspection(CuttingInspection cuttingInspection);

  @Query("""
      SELECT item
      FROM CuttingInspectionItem item
      JOIN FETCH item.cuttingInspection inspection
      WHERE inspection.createdAt >= :start
        AND inspection.createdAt < :end
      """)
  List<CuttingInspectionItem> findItemsByInspectionDateRange(
      @Param("start") Instant start,
      @Param("end") Instant end);

  @Query("""
      SELECT item
      FROM CuttingInspectionItem item
      JOIN FETCH item.cuttingInspection inspection
      WHERE inspection.createdAt >= :start
        AND inspection.createdAt < :end
        AND inspection.submittedBy.id = :userId
      """)
  List<CuttingInspectionItem> findItemsByInspectionDateRangeAndUserId(
      @Param("start") Instant start,
      @Param("end") Instant end,
      @Param("userId") String userId);

  @Query("""
      SELECT item
      FROM CuttingInspectionItem item
      JOIN FETCH item.cuttingInspection inspection
      JOIN FETCH inspection.line
      LEFT JOIN FETCH inspection.submittedBy
      JOIN FETCH item.styleColor sc
      JOIN FETCH sc.style s
      JOIN FETCH s.buyer
      LEFT JOIN FETCH item.defectType
      WHERE inspection.id IN :inspectionIds
      """)
  List<CuttingInspectionItem> findAllWithDetailsByInspectionIds(
      @Param("inspectionIds") List<String> inspectionIds);

  // @Query("""
  // SELECT item
  // FROM CuttingInspectionItem item
  // JOIN FETCH item.cuttingInspection inspection
  // LEFT JOIN FETCH item.styleColor sc
  // LEFT JOIN FETCH sc.style style
  // LEFT JOIN FETCH style.buyer
  // LEFT JOIN FETCH item.defectType
  // WHERE inspection IN :inspections
  // """)
  // List<CuttingInspectionItem> findAllWithDetailsByCuttingInspectionIn(
  // @Param("inspections") List<CuttingInspection> inspections);

  @Query("""
      SELECT item
      FROM CuttingInspectionItem item
      JOIN FETCH item.cuttingInspection inspection
      LEFT JOIN FETCH item.styleColor sc
      LEFT JOIN FETCH sc.style style
      LEFT JOIN FETCH style.buyer
      LEFT JOIN FETCH item.defectType
      WHERE inspection IN :inspections
      """)
  List<CuttingInspectionItem> findAllWithDetailsByCuttingInspectionIn(
      @Param("inspections") List<CuttingInspection> inspections);
}
