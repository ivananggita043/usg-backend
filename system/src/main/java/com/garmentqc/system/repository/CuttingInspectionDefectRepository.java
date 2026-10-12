package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.CuttingInspectionDefect;
import com.garmentqc.system.model.CuttingInspectionItem;

public interface CuttingInspectionDefectRepository extends JpaRepository<CuttingInspectionDefect, String> {

    List<CuttingInspectionDefect> findByInspectionItem(CuttingInspectionItem inspectionItem);

}
