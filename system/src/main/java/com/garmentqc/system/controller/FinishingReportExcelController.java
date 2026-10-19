package com.garmentqc.system.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.model.User;
import com.garmentqc.system.service.FinishingReportExcelService;

@RestController
@RequestMapping("/excel/finishing")
public class FinishingReportExcelController {

        private final FinishingReportExcelService reportService;

        public FinishingReportExcelController(FinishingReportExcelService reportService) {
                this.reportService = reportService;
        }

        @GetMapping("/daily/export")
        public ResponseEntity<byte[]> downloadExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Authentication authentication) throws Exception {

                User currentUser = (User) authentication.getPrincipal();
                byte[] file = reportService.exportDailyFinishingExcel(
                                date, currentUser);
                String filename = "Report-Finishing-Daily-"
                                + date.format(DateTimeFormatter.ISO_DATE)
                                + ".xlsx";

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                ContentDisposition.attachment()
                                                                .filename(filename)
                                                                .build()
                                                                .toString())
                                .body(file);
        }
}