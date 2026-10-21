package com.garmentqc.system.controller.superadmin.excel;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.CuttingReportResponse;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.UserRepository;
import com.garmentqc.system.service.CuttingReportExcelService;
import com.garmentqc.system.service.CuttingReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("super-admin/excel/cutting")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CuttingReportExcelSuperAdminController {

    private final CuttingReportService reportService;
    private final CuttingReportExcelService excelService;
    private final UserRepository userRepository;

    @GetMapping("/daily/export")
    public ResponseEntity<byte[]> exportDaily(@RequestParam LocalDate date) throws IOException {
        validateCuttingExportAccess();
        CuttingReportResponse report = reportService.getDailyReport(date);
        return createExcelResponse(report, "Report-Cutting-Daily" + date + ".xlsx");
    }

    @GetMapping("/weekly/export")
    public ResponseEntity<byte[]> exportWeekly(@RequestParam LocalDate date) throws IOException {
        validateCuttingExportAccess();
        CuttingReportResponse report = reportService.getWeeklyReport(date);
        return createExcelResponse(report, "Report-Cutting-Weekly" + date + ".xlsx");
    }

    @GetMapping("/monthly/export")
    public ResponseEntity<byte[]> exportMonthly(@RequestParam int year, @RequestParam int month)
            throws IOException {
        validateCuttingExportAccess();
        CuttingReportResponse report = reportService.getMonthlyReport(year, month);
        return createExcelResponse(report,
                "Report-Cutting-" + year + "-" + String.format("%02d", month) + ".xlsx");
    }

    private ResponseEntity<byte[]> createExcelResponse(CuttingReportResponse report, String filename)
            throws IOException {
        validateCuttingExportAccess();
        byte[] content = excelService.export(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument"
                        + ".spreadsheetml.sheet"))
                .contentLength(content.length)
                .body(content);
    }

    private void validateCuttingExportAccess() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Anda harus login untuk mengakses export Cutting");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("User yang login tidak ditemukan"));

        String role = user.getRole().name();
        if ("SUPER_ADMIN".equals(role)) {
            return;
        }

        // if ("ADMIN".equals(role) && user.getSection() != null &&
        // "CUTTING".equals(user.getSection().name())) {
        // return;
        // }

        throw new UnauthorizedException(
                "Hanya Super Admin yang mempunyai akses untuk export laporan Cutting");
    }

}
