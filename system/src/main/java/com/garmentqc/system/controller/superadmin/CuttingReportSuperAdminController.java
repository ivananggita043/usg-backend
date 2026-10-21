
package com.garmentqc.system.controller.superadmin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.CuttingGroupedReportResponse;
import com.garmentqc.system.dto.CuttingReportResponse;
import com.garmentqc.system.service.CuttingReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("super-admin/cutting-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CuttingReportSuperAdminController {

    private final CuttingReportService cuttingReportService;
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
