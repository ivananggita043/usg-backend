package com.garmentqc.system.dto;

import com.garmentqc.system.model.DefectType;

public record DefectSearchResponse(
        String id,
        
        String name,
        
        String section,
        
        String category,
        
        String areaId,
        
        String areaName) {

    public static DefectSearchResponse from(DefectType t) {
        return new DefectSearchResponse(
                t.getId(),
                t.getName(),
                t.getSection() == null ? null : t.getSection().name(),
                t.getCategory() == null ? null : t.getCategory().name(),
                t.getDefectArea() == null ? null : t.getDefectArea().getId(),
                t.getDefectArea() == null ? null : t.getDefectArea().getName());
    }

    public record Area(String id, String name) {
        
    }
}