package com.garmentqc.system.controller.visitor;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.InspectionDashboardResponse;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.InspectionDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/visitor/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VISITOR')")
public class InspectionDashboardVisitorController {

        private final InspectionDashboardService dashboardService;

        @GetMapping("/inspection-summary")
        public ResponseEntity<InspectionDashboardResponse> getInspectionSummary(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(required = false) String section,
                        @AuthenticationPrincipal User currentUser) {

                return ResponseEntity.ok(
                                dashboardService.getDashboardSummary(
                                                startDate,
                                                endDate,
                                                section,
                                                currentUser));
        }
}