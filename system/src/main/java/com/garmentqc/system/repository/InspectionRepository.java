
package com.garmentqc.system.repository;

import com.garmentqc.system.enums.Section;
import com.garmentqc.system.model.Inspection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InspectionRepository extends JpaRepository<Inspection, String> {

        List<Inspection> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant start, Instant end);

        List<Inspection> findBySubmittedBy_IdAndCreatedAtBetween(String userId, Instant start, Instant end);

        boolean existsByLine_IdAndInspectionTimeSlotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(String lineId,
                        String inspectionTimeSlot, Instant start, Instant end);

        @Query(value = """
                        SELECT i.id
                        FROM Inspection i
                        WHERE i.line.section = :section
                        ORDER BY i.createdAt DESC, i.id DESC
                        """, countQuery = """
                        SELECT COUNT(i.id)
                        FROM Inspection i
                        WHERE i.line.section = :section
                        """)
        Page<String> findIdsByLine_Section(@Param("section") Section section, Pageable pageable);

        @Query(value = """
                        SELECT i.id
                        FROM Inspection i
                        WHERE i.submittedBy.id = :userId
                        ORDER BY i.createdAt DESC, i.id DESC
                        """, countQuery = """
                        SELECT COUNT(i.id)
                        FROM Inspection i
                        WHERE i.submittedBy.id = :userId
                        """)
        Page<String> findIdsBySubmittedBy_Id(@Param("userId") String userId, Pageable pageable);

        @Query(value = """
                        SELECT i.id
                        FROM Inspection i
                        WHERE i.submittedBy.id = :userId
                          AND i.line.section = :section
                        ORDER BY i.createdAt DESC, i.id DESC
                        """, countQuery = """
                        SELECT COUNT(i.id)
                        FROM Inspection i
                        WHERE i.submittedBy.id = :userId
                          AND i.line.section = :section
                        """)
        Page<String> findIdsBySubmittedBy_IdAndLine_Section(@Param("userId") String userId,
                        @Param("section") Section section,
                        Pageable pageable);

        @Query("""
                        SELECT i.line.id AS lineId,
                               i.line.name AS lineName,
                               COUNT(i.id) AS totalInspections,
                               COALESCE(SUM(i.inspectQuantity), 0) AS actualQuantity
                        FROM Inspection i
                        WHERE i.createdAt >= :start
                          AND i.createdAt < :end
                          AND i.line.section = :section
                        GROUP BY i.line.id, i.line.name
                        """)
        List<LineInspectionTotalProjection> findLineInspectionTotals(@Param("start") Instant start,
                        @Param("end") Instant end,
                        @Param("section") Section section);

        @Query("""
                        SELECT i
                        FROM Inspection i
                        JOIN FETCH i.line l
                        WHERE i.createdAt >= :start
                          AND i.createdAt < :end
                        """)
        List<Inspection> findDashboardTrendInspections(@Param("start") Instant start, @Param("end") Instant end);

        @Query("""
                        SELECT DISTINCT i
                        FROM Inspection i
                        JOIN FETCH i.line l
                        LEFT JOIN FETCH i.inspectionDefects d
                        LEFT JOIN FETCH d.defectType dt
                        LEFT JOIN FETCH dt.defectArea da
                        WHERE i.createdAt >= :start
                          AND i.createdAt < :end
                        """)
        List<Inspection> findDashboardInspections(@Param("start") Instant start, @Param("end") Instant end);

        @Query("""
                        SELECT DISTINCT b.name
                        FROM Buyer b
                        WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        ORDER BY b.name ASC
                        """)
        List<String> searchBuyers(@Param("keyword") String keyword);

        @Query("""
                        SELECT DISTINCT s.styleCode
                        FROM Style s
                        WHERE LOWER(s.buyer.name) = LOWER(:buyerName)
                          AND LOWER(s.styleCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        ORDER BY s.styleCode ASC
                        """)
        List<String> searchStyles(
                        @Param("buyerName") String buyerName,
                        @Param("keyword") String keyword);

        @Query("""
                        SELECT DISTINCT sc.colorName
                        FROM StyleColor sc
                        WHERE LOWER(sc.style.buyer.name) = LOWER(:buyerName)
                          AND LOWER(sc.style.styleCode) = LOWER(:styleCode)
                          AND LOWER(sc.colorName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        ORDER BY sc.colorName ASC
                        """)
        List<String> searchColors(
                        @Param("buyerName") String buyerName,
                        @Param("styleCode") String styleCode,
                        @Param("keyword") String keyword);

        @Query("""
                        SELECT DISTINCT i
                        FROM Inspection i
                        LEFT JOIN FETCH i.line
                        LEFT JOIN FETCH i.styleColor sc
                        LEFT JOIN FETCH sc.style s
                        LEFT JOIN FETCH s.buyer
                        LEFT JOIN FETCH i.inspectionDefects d
                        LEFT JOIN FETCH d.defectType dt
                        LEFT JOIN FETCH dt.defectArea
                        WHERE i.createdAt >= :start
                          AND i.createdAt < :end
                        """)
        List<Inspection> findReportInspectionsWithDetails(
                        @Param("start") Instant start,
                        @Param("end") Instant end);

        @Query("""
                        SELECT DISTINCT i
                        FROM Inspection i
                        JOIN FETCH i.line
                        JOIN FETCH i.styleColor sc
                        JOIN FETCH sc.style s
                        JOIN FETCH s.buyer
                        LEFT JOIN FETCH i.inspectionDefects d
                        LEFT JOIN FETCH d.defectType
                        WHERE i.id IN :ids
                        """)
        List<Inspection> findAllWithDetailsByIdIn(
                        @Param("ids") List<String> ids);

        @Query("""
                        SELECT DISTINCT i
                        FROM Inspection i
                        JOIN FETCH i.line
                        JOIN FETCH i.styleColor sc
                        JOIN FETCH sc.style s
                        JOIN FETCH s.buyer
                        LEFT JOIN FETCH i.inspectionDefects d
                        LEFT JOIN FETCH d.defectType dt
                        LEFT JOIN FETCH dt.defectArea
                        LEFT JOIN FETCH i.submittedBy
                        WHERE i.id = :id
                        """)
        Optional<Inspection> findByIdWithDetails(@Param("id") String id);
}
