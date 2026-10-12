package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.InspectionDefect;

public interface InspectionDefectRepository extends JpaRepository<InspectionDefect, String> {

    List<InspectionDefect> findByInspection_Id(String inspectionId);

}
