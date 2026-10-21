
package com.garmentqc.system.controller.admin.excel;

import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.UserRepository;
import com.garmentqc.system.service.WashingReportExcelService;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("admin/excel/washing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WashingReportExcelAdminController {

    private final WashingReportExcelService reportService;
    private final UserRepository userRepository;

    @GetMapping("/daily/export")
    public ResponseEntity<byte[]> exportDailyWashingExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User currentUser) throws IOException {

        validatewashingExportAccess();

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

    private void validatewashingExportAccess() {
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

        if ("ADMIN".equals(role) && user.getSection() != null && ("WASHING".equals(user.getSection().name()))) {
            return;
        }

        throw new UnauthorizedException(
                "Hanya Admin Washing yang mempunyai akses untuk export laporan Washing");
    }
}
