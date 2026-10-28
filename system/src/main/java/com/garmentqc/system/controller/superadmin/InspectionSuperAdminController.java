
package com.garmentqc.system.controller.superadmin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.DefectSearchResponse;
import com.garmentqc.system.dto.InspectionRequest;
import com.garmentqc.system.dto.InspectionResponse;
import com.garmentqc.system.service.InspectionService;

@RestController
@RequestMapping("/super-admin/inspections")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class InspectionSuperAdminController {

    private final InspectionService inspectionService;

    public InspectionSuperAdminController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @PostMapping
    public ResponseEntity<InspectionResponse> create(@RequestBody InspectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspectionService.create(request));
    }

    @GetMapping("/all")
    public ResponseEntity<List<InspectionResponse>> getAll(@RequestParam(required = false) String section) {
        return ResponseEntity.ok(
                inspectionService.getAll(section));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(inspectionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InspectionResponse> update(@PathVariable String id, @RequestBody InspectionRequest request) {
        return ResponseEntity.ok(inspectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        inspectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/defects/search")
    public ResponseEntity<List<DefectSearchResponse>> searchDefects(
            @RequestParam(required = false) String defectAreaId,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(inspectionService.searchDefects(defectAreaId, keyword));
    }

    @GetMapping("/defect-areas/search")
    public ResponseEntity<List<DefectSearchResponse.Area>> searchDefectAreas(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(
                inspectionService.searchDefectAreas(keyword)
                        .stream()
                        .map(a -> new DefectSearchResponse.Area(a.getId(), a.getName()))
                        .toList());
    }

}
