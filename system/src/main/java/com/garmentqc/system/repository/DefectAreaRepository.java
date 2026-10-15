package com.garmentqc.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.DefectArea;

import java.util.List;

public interface DefectAreaRepository extends JpaRepository<DefectArea, String> {

    List<DefectArea> findAllByOrderByNameAsc();

    List<DefectArea> findByActiveTrueOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);

    List<DefectArea> findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(String name);
}