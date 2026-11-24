package com.garmentqc.system.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.AqlLevel;

public interface AqlLevelRepository extends JpaRepository<AqlLevel, Long> {

    Optional<AqlLevel> findByAqlValueAndActiveTrue(BigDecimal aqlValue);

}