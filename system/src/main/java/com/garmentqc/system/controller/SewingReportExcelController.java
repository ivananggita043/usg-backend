package com.garmentqc.system.controller;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.model.User;
import com.garmentqc.system.service.SewingReportExcelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports/excel")
@RequiredArgsConstructor
public class SewingReportExcelController {

    private final SewingReportExcelService excelService;

    @GetMapping("/sewing/daily")
    public ResponseEntity<byte[]> exportDailySewingExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User currentUser)
            throws IOException {

        byte[] excel = excelService.exportDailySewingExcel(
                date, currentUser);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Report_Sewing_Daily_"
                                + date + ".xlsx\"")
                .body(excel);
    }
}
