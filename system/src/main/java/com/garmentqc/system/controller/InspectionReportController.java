
package com.garmentqc.system.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/inspection/reports")
@RequiredArgsConstructor
public class InspectionReportController {

        private final InspectionReportService reportService;

        @GetMapping("/daily")
        public ResponseEntity<InspectionReportResponse> getDailyReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getDailyReport(date, section, currentUser));
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

        @GetMapping("/daily/admin")
        public ResponseEntity<InspectionReportResponse> getDailyReportAdmin(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getDailyReport(date, section, currentUser));
        }

        @GetMapping("/weekly/admin")
        public ResponseEntity<InspectionReportResponse> getWeeklyReportAdmin(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getWeeklyReport(date, section, currentUser));
        }

        @GetMapping("/monthly/admin")
        public ResponseEntity<InspectionReportResponse> getMonthlyReportAdmin(@RequestParam int year,
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

        @GetMapping("/buyers/daily/admin")
        public ResponseEntity<InspectionGroupedReportResponse> getDailyBuyerReportAdmin(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getDailyBuyerReport(date, section, currentUser));
        }

        @GetMapping("/buyers/weekly/admin")
        public ResponseEntity<InspectionGroupedReportResponse> getWeeklyBuyerReportAdmin(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getWeeklyBuyerReport(date, section, currentUser));
        }

        @GetMapping("/buyers/monthly/admin")
        public ResponseEntity<InspectionGroupedReportResponse> getMonthlyBuyerReportAdmin(@RequestParam int year,
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
                return ResponseEntity.ok(reportService.getWeeklyStyleReport(date, section, currentUser));
        }

        @GetMapping("/styles/monthly")
        public ResponseEntity<InspectionGroupedReportResponse> getMonthlyStyleReport(@RequestParam int year,
                        @RequestParam int month, @RequestParam(required = false) String section,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getMonthlyStyleReport(year, month, section, currentUser));
        }

        @GetMapping("/styles/daily/admin")
        public ResponseEntity<InspectionGroupedReportResponse> getDailyStyleReportAdmin(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getDailyStyleReport(date, section, currentUser));
        }

        @GetMapping("/styles/weekly/admin")
        public ResponseEntity<InspectionGroupedReportResponse> getWeeklyStyleReportAdmin(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String section, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getWeeklyStyleReport(date, section, currentUser));
        }

        @GetMapping("/styles/monthly/admin")
        public ResponseEntity<InspectionGroupedReportResponse> getMonthlyStyleReportAdmin(@RequestParam int year,
                        @RequestParam int month, @RequestParam(required = false) String section,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(reportService.getMonthlyStyleReport(year, month, section, currentUser));
        }
}
