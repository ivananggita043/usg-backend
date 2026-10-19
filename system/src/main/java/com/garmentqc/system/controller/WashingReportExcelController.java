
package com.garmentqc.system.controller;

import com.garmentqc.system.model.User;
import com.garmentqc.system.service.WashingReportExcelService;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/excel/washing")
@RequiredArgsConstructor
public class WashingReportExcelController {

    private final WashingReportExcelService reportService;

    @GetMapping("/daily")
    public ResponseEntity<byte[]> exportDailyWashingExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User currentUser) throws IOException {

        byte[] excel = reportService.exportDailyWashingExcel(
                date, currentUser);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Report-Washing-Daily-"
                                + date + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument"
                                + ".spreadsheetml.sheet"))
                .body(excel);
    }
}
