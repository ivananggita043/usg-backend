
package com.garmentqc.system.controller.visitor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.DefectSearchResponse;
import com.garmentqc.system.dto.InspectionResponse;
import com.garmentqc.system.service.InspectionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/visitor/inspections")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VISITOR')")
public class InspectionVisitorController {

    private final InspectionService inspectionService;

    @GetMapping("/all")
    public ResponseEntity<List<InspectionResponse>> getAll(
            @RequestParam(required = false) String section) {
        return ResponseEntity.ok(inspectionService.getAll(section));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(inspectionService.getById(id));
    }

    @GetMapping("/defects/search")
    public ResponseEntity<List<DefectSearchResponse>> searchDefects(
            @RequestParam(required = false) String defectAreaId,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(
                inspectionService.searchDefects(defectAreaId, keyword));
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

    @GetMapping("/paginated")
    public ResponseEntity<Page<InspectionResponse>> getAllPaginated(
            @RequestParam(required = false) String section,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100));

        return ResponseEntity.ok(
                inspectionService.getAllPaginated(section, pageable));
    }
}
