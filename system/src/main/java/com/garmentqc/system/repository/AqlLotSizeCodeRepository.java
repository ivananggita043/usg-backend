package com.garmentqc.system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.garmentqc.system.model.AqlLotSizeCode;

public interface AqlLotSizeCodeRepository extends JpaRepository<AqlLotSizeCode, Long> {

    @Query("""
            SELECT l
            FROM AqlLotSizeCode l
            WHERE l.inspectionLevel = :inspectionLevel
              AND l.minLotSize <= :lotSize
              AND (l.maxLotSize IS NULL OR l.maxLotSize >= :lotSize)
            """)
    Optional<AqlLotSizeCode> findByLotSizeAndInspectionLevel(
            @Param("lotSize") Integer lotSize,
            @Param("inspectionLevel") String inspectionLevel);

}