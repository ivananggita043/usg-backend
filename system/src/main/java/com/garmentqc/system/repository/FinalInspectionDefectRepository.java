package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.FinalInspectionDefect;

public interface FinalInspectionDefectRepository extends JpaRepository<FinalInspectionDefect, String> {

    List<FinalInspectionDefect> findByFinalInspection_Id(String finalInspectionId);
}