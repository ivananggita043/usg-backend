
package com.garmentqc.system.controller.visitor;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.CuttingAnalyticsResponse;
import com.garmentqc.system.dto.CuttingGroupedReportResponse;
import com.garmentqc.system.dto.CuttingReportResponse;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.CuttingAnalyticsService;
import com.garmentqc.system.service.CuttingReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("visitor/cutting-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VISITOR')")
public class CuttingReportVisitorController {

    private final CuttingReportService cuttingReportService;
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

    @GetMapping("/daily")
    public CuttingReportResponse getDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cuttingReportService.getDailyReport(date);
    }

    @GetMapping("/weekly")
    public CuttingReportResponse getWeeklyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cuttingReportService.getWeeklyReport(date);
    }

    @GetMapping("/monthly")
    public CuttingReportResponse getMonthlyReport(@RequestParam int year, @RequestParam int month) {
        return cuttingReportService.getMonthlyReport(year, month);
    }

    @GetMapping("/daily/buyer")
    public CuttingGroupedReportResponse getDailyBuyerReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cuttingReportService.getDailyBuyerReport(date);
    }

    @GetMapping("/daily/style")
    public CuttingGroupedReportResponse getDailyStyleReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cuttingReportService.getDailyStyleReport(date);
    }

    @GetMapping("/weekly/buyer")
    public CuttingGroupedReportResponse getWeeklyBuyerReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cuttingReportService.getWeeklyBuyerReport(date);
    }

    @GetMapping("/weekly/style")
    public CuttingGroupedReportResponse getWeeklyStyleReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cuttingReportService.getWeeklyStyleReport(date);
    }

    @GetMapping("/monthly/buyer")
    public CuttingGroupedReportResponse getMonthlyBuyerReport(@RequestParam int year, @RequestParam int month) {
        return cuttingReportService.getMonthlyBuyerReport(year, month);
    }

    @GetMapping("/monthly/style")
    public CuttingGroupedReportResponse getMonthlyStyleReport(@RequestParam int year, @RequestParam int month) {
        return cuttingReportService.getMonthlyStyleReport(year, month);
    }

}
