
package com.garmentqc.system.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.CuttingGroupedReportResponse;
import com.garmentqc.system.dto.CuttingReportResponse;
import com.garmentqc.system.service.CuttingReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cutting/reports")
@RequiredArgsConstructor
public class CuttingReportController {

        private final CuttingReportService reportService;

        @GetMapping("/daily")
        public ResponseEntity<CuttingReportResponse> getDailyReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                return ResponseEntity.ok(reportService.getDailyReport(date));
        }

        @GetMapping("/weekly")
        public ResponseEntity<CuttingReportResponse> getWeeklyReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                return ResponseEntity.ok(reportService.getWeeklyReport(date));
        }

        @GetMapping("/monthly")
        public ResponseEntity<CuttingReportResponse> getMonthlyReport(@RequestParam int year, @RequestParam int month) {
                return ResponseEntity.ok(reportService.getMonthlyReport(year, month));
        }

        @GetMapping("/buyers/daily")
        public ResponseEntity<CuttingGroupedReportResponse> getDailyBuyerReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                return ResponseEntity.ok(reportService.getDailyBuyerReport(date));
        }

        @GetMapping("/buyers/weekly")
        public ResponseEntity<CuttingGroupedReportResponse> getWeeklyBuyerReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                return ResponseEntity.ok(reportService.getWeeklyBuyerReport(date));
        }

        @GetMapping("/buyers/monthly")
        public ResponseEntity<CuttingGroupedReportResponse> getMonthlyBuyerReport(@RequestParam int year,
                        @RequestParam int month) {
                return ResponseEntity.ok(reportService.getMonthlyBuyerReport(year, month));
        }

        @GetMapping("/styles/daily")
        public ResponseEntity<CuttingGroupedReportResponse> getDailyStyleReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                return ResponseEntity.ok(reportService.getDailyStyleReport(date));
        }

        @GetMapping("/styles/weekly")
        public ResponseEntity<CuttingGroupedReportResponse> getWeeklyStyleReport(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                return ResponseEntity.ok(reportService.getWeeklyStyleReport(date));
        }

        @GetMapping("/styles/monthly")
        public ResponseEntity<CuttingGroupedReportResponse> getMonthlyStyleReport(@RequestParam int year,
                        @RequestParam int month) {
                return ResponseEntity.ok(reportService.getMonthlyStyleReport(year, month));
        }
}
