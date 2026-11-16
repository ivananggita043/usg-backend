
package com.garmentqc.system.controller.visitor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.CuttingAnalyticsResponse;
import com.garmentqc.system.dto.CuttingInspectionResponse;
import com.garmentqc.system.enums.CuttingProcessCheck;
import com.garmentqc.system.model.CuttingDefectType;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.CuttingAnalyticsService;
import com.garmentqc.system.service.CuttingInspectionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/visitor/cutting-inspections")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VISITOR')")
public class CuttingInspectionVisitorController {

    private final CuttingInspectionService cuttingInspectionService;
    private final CuttingAnalyticsService cuttingAnalyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<CuttingAnalyticsResponse> getAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                cuttingAnalyticsService.getAnalytics(
                        startDate,
                        endDate,
                        currentUser));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CuttingInspectionResponse>> getAll(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(cuttingInspectionService.getAll(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuttingInspectionResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(cuttingInspectionService.getById(id, currentUser));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CuttingDefectType>> searchDefectTypes(
            @RequestParam CuttingProcessCheck processCheck,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                cuttingInspectionService.searchDefectTypes(
                        processCheck, search, currentUser));
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<CuttingInspectionResponse>> getAllPaginated(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100));

        return ResponseEntity.ok(
                cuttingInspectionService.getAllPaginated(
                        currentUser, pageable));
    }
}
