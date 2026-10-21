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

import com.garmentqc.system.dto.InspectionGroupedReportResponse;
import com.garmentqc.system.dto.InspectionReportResponse;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.InspectionReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/inspection/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InspectionReportAdminController {

    private final InspectionReportService reportService;

    @GetMapping("/daily")
    public ResponseEntity<InspectionReportResponse> getDailyReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reportService.getDailyReport(date, null, currentUser));
    }

    @GetMapping("/weekly")
    public ResponseEntity<InspectionReportResponse> getWeeklyReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reportService.getWeeklyReport(date, null, currentUser));
    }

    @GetMapping("/monthly")
    public ResponseEntity<InspectionReportResponse> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reportService.getMonthlyReport(year, month, null, currentUser));
    }

    @GetMapping("/buyers/daily")
    public ResponseEntity<InspectionGroupedReportResponse> getDailyBuyerReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reportService.getDailyBuyerReport(date, null, currentUser));
    }

    @GetMapping("/buyers/weekly")
    public ResponseEntity<InspectionGroupedReportResponse> getWeeklyBuyerReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reportService.getWeeklyBuyerReport(date, null, currentUser));
    }

    @GetMapping("/buyers/monthly")
    public ResponseEntity<InspectionGroupedReportResponse> getMonthlyBuyerReport(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reportService.getMonthlyBuyerReport(year, month, null, currentUser));
    }


    @GetMapping("/styles/daily")
    public ResponseEntity<InspectionGroupedReportResponse> getDailyStyleReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reportService.getDailyStyleReport(date, null, currentUser));
    }

    @GetMapping("/styles/weekly")
    public ResponseEntity<InspectionGroupedReportResponse> getWeeklyStyleReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reportService.getWeeklyStyleReport(date, null, currentUser));
    }

    @GetMapping("/styles/monthly")
    public ResponseEntity<InspectionGroupedReportResponse> getMonthlyStyleReport(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reportService.getMonthlyStyleReport(year, month, null, currentUser));
    }
}