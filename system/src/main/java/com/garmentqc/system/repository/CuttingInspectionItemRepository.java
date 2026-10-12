package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.CuttingInspection;
import com.garmentqc.system.model.CuttingInspectionItem;

public interface CuttingInspectionItemRepository extends JpaRepository<CuttingInspectionItem, String> {

    List<CuttingInspectionItem> findByCuttingInspection(CuttingInspection cuttingInspection);

}
