package com.garmentqc.system.controller.superadmin.excel;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.enums.ReportGroupingType;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.SewingPeriodReportExcelService;
import com.garmentqc.system.service.SewingReportExcelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/super-admin/excel/sewing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SewingReportExcelSuperAdminController {

        private final SewingReportExcelService excelService;
        private final SewingPeriodReportExcelService periodExcelService;

        @GetMapping("/daily/export")
        public ResponseEntity<byte[]> exportDailySewingExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @AuthenticationPrincipal User currentUser) throws IOException {

                byte[] excel = excelService.exportDailySewingExcel(
                                date,
                                currentUser);

                return buildExcelResponse(
                                excel,
                                "Report-Sewing-Daily-" + date + ".xlsx");
        }

        @GetMapping("/weekly/export")
        public ResponseEntity<byte[]> exportWeeklySewingExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

                        @RequestParam(defaultValue = "ALL") String buyer,
                        @RequestParam(defaultValue = "ALL") String style,
                        @RequestParam(defaultValue = "ALL") String line,
                        @RequestParam(defaultValue = "LINEWISE") ReportGroupingType groupingType,

                        @AuthenticationPrincipal User currentUser) throws IOException {

                LocalDate startDate = date.with(
                                TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
                LocalDate endDate = startDate.plusDays(6);

                byte[] excel = periodExcelService.exportWeeklySewingExcel(
                                date,
                                buyer,
                                style,
                                line,
                                groupingType,
                                currentUser);

                return buildExcelResponse(
                                excel,
                                "Report-Sewing-" + groupingType
                                                + "-Weekly-" + startDate + "-to-" + endDate + ".xlsx");
        }

        @GetMapping("/monthly/export")
        public ResponseEntity<byte[]> exportMonthlySewingExcel(
                        @RequestParam int month,
                        @RequestParam int year,

                        @RequestParam(defaultValue = "ALL") String buyer,
                        @RequestParam(defaultValue = "ALL") String style,
                        @RequestParam(defaultValue = "ALL") String line,
                        @RequestParam(defaultValue = "DATEWISE") ReportGroupingType groupingType,

                        @AuthenticationPrincipal User currentUser) throws IOException {

                YearMonth yearMonth;

                try {
                        yearMonth = YearMonth.of(year, month);
                } catch (java.time.DateTimeException ex) {
                        throw new BadRequestException(
                                        "Bulan atau tahun laporan tidak valid.");
                }

                LocalDate startDate = yearMonth.atDay(1);

                LocalDate endDate = yearMonth.atEndOfMonth();

                byte[] excel = periodExcelService.exportMonthlySewingExcel(
                                month,
                                year,
                                buyer,
                                style,
                                line,
                                groupingType,
                                currentUser);

                return buildExcelResponse(
                                excel,
                                "Report-Sewing-" + groupingType + "-Monthly-" + startDate + "-to-" + endDate + ".xlsx");

        }

        private ResponseEntity<byte[]> buildExcelResponse(
                        byte[] excel,
                        String filename) {

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + filename + "\"")
                                .body(excel);
        }
}