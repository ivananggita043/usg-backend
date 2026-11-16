package com.garmentqc.system.controller.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.InspectionDashboardResponse;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.InspectionDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InspectionDashboardAdminController {

    private final InspectionDashboardService dashboardService;

    @GetMapping("/inspection/summary")
    public ResponseEntity<InspectionDashboardResponse> getInspectionSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User currentUser) {
                
        return ResponseEntity.ok(
                dashboardService.getDashboardSummary(
                        startDate,
                        endDate,
                        null,
                        currentUser));
    }
}