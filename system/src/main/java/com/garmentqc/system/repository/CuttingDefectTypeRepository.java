package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.enums.CuttingProcessCheck;
import com.garmentqc.system.model.CuttingDefectType;

public interface CuttingDefectTypeRepository extends JpaRepository<CuttingDefectType, String> {

    List<CuttingDefectType> findByActiveTrue();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);

    List<CuttingDefectType> findByProcessCheckAndActiveTrue(CuttingProcessCheck processCheck);

}
