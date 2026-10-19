
package com.garmentqc.system.controller;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.CuttingReportResponse;
import com.garmentqc.system.service.CuttingReportExcelService;
import com.garmentqc.system.service.CuttingReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/excel/cutting")
@RequiredArgsConstructor
public class CuttingReportExcelController {

        private final CuttingReportService reportService;
        private final CuttingReportExcelService excelService;

        @GetMapping("/daily/export")
        public ResponseEntity<byte[]> exportDaily(@RequestParam LocalDate date) throws IOException {
                CuttingReportResponse report = reportService.getDailyReport(date);
                return createExcelResponse(report, "Report-Cutting-Daily" + date + ".xlsx");
        }

        @GetMapping("/weekly/export")
        public ResponseEntity<byte[]> exportWeekly(@RequestParam LocalDate date) throws IOException {
                CuttingReportResponse report = reportService.getWeeklyReport(date);
                return createExcelResponse(report, "Report-Cutting-Weekly" + date + ".xlsx");
        }

        @GetMapping("/monthly/export")
        public ResponseEntity<byte[]> exportMonthly(@RequestParam int year, @RequestParam int month)
                        throws IOException {
                CuttingReportResponse report = reportService.getMonthlyReport(year, month);
                return createExcelResponse(report,
                                "Report-Cutting-" + year + "-" + String.format("%02d", month) + ".xlsx");
        }

        private ResponseEntity<byte[]> createExcelResponse(CuttingReportResponse report, String filename)
                        throws IOException {
                byte[] content = excelService.export(report);
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument"
                                                + ".spreadsheetml.sheet"))
                                .contentLength(content.length)
                                .body(content);
        }
}
