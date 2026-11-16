package com.garmentqc.system.controller.visitor;

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
@RequestMapping("/visitor/inspection/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VISITOR')")
public class InspectionReportVisitorController {

        private final InspectionReportService reportService;

        @GetMapping("/daily")
        public ResponseEntity<InspectionReportResponse> getDailyReportAllLine(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(
                                reportService.getDailyReport(date, null, currentUser));
        }

        @GetMapping("/daily/line")
        public ResponseEntity<InspectionReportResponse> getDailyReportByLine(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam String lineId,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(
                                reportService.getDailyReportByLine(date, lineId, currentUser));
        }

        @GetMapping("/weekly")
        public ResponseEntity<InspectionReportResponse> getWeeklyReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getWeeklyReport(date, section, currentUser));
        }

        @GetMapping("/monthly")
        public ResponseEntity<InspectionReportResponse> getMonthlyReport(@RequestParam int year,
                        @RequestParam int month, @RequestParam(required = false) String section,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getMonthlyReport(year, month, section, currentUser));
        }

        @GetMapping("/buyers/daily")
        public ResponseEntity<InspectionGroupedReportResponse> getDailyBuyerReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getDailyBuyerReport(date, section, currentUser));
        }

        @GetMapping("/buyers/weekly")
        public ResponseEntity<InspectionGroupedReportResponse> getWeeklyBuyerReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getWeeklyBuyerReport(date, section, currentUser));
        }

        @GetMapping("/buyers/monthly")
        public ResponseEntity<InspectionGroupedReportResponse> getMonthlyBuyerReport(@RequestParam int year,
                        @RequestParam int month, @RequestParam(required = false) String section,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getMonthlyBuyerReport(year, month, section, currentUser));
        }

        @GetMapping("/styles/daily")
        public ResponseEntity<InspectionGroupedReportResponse> getDailyStyleReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getDailyStyleReport(date, section, currentUser));
        }

        @GetMapping("/styles/weekly")
        public ResponseEntity<InspectionGroupedReportResponse> getWeeklyStyleReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(
                                reportService.getWeeklyStyleReport(date, section, currentUser));
        }

        @GetMapping("/styles/monthly")
        public ResponseEntity<InspectionGroupedReportResponse> getMonthlyStyleReport(@RequestParam int year,
                        @RequestParam int month, @RequestParam(required = false) String section,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(
                                reportService.getMonthlyStyleReport(year, month, section, currentUser));
        }

        @GetMapping("/buyers/date")
        public ResponseEntity<InspectionGroupedReportResponse> getBuyerReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(
                                reportService.getBuyerReport(startDate, endDate, null, currentUser));
        }

        @GetMapping("/styles/date")
        public ResponseEntity<InspectionGroupedReportResponse> getBuyerStyleReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(
                                reportService.getBuyerStyleReport(startDate, endDate, null, currentUser));
        }
}