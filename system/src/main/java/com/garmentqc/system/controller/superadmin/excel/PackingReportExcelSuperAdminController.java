
package com.garmentqc.system.controller.superadmin.excel;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.garmentqc.system.enums.ReportGroupingType;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.UserRepository;
import com.garmentqc.system.service.PackingPeriodReportExcelService;
import com.garmentqc.system.service.PackingReportExcelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("super-admin/excel/packing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PackingReportExcelSuperAdminController {

        private final PackingReportExcelService packingReportExcelService;
        private final PackingPeriodReportExcelService packingPeriodReportExcelService;
        private final UserRepository userRepository;

        @GetMapping("/daily/export")
        public ResponseEntity<byte[]> exportDailyPackingExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Authentication authentication) throws IOException {

                User currentUser = validatePackingExportAccess(authentication);

                byte[] excel = packingReportExcelService
                                .exportDailyPackingExcel(date, currentUser);

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=Report-Packing-Daily-" + date + ".xlsx")
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .body(excel);
        }

        @GetMapping("/weekly/export")
        public ResponseEntity<byte[]> exportWeeklyPackingExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

                        @RequestParam(defaultValue = "ALL") String buyer,
                        @RequestParam(defaultValue = "ALL") String style,
                        @RequestParam(defaultValue = "ALL") String line,
                        @RequestParam(defaultValue = "DATEWISE") ReportGroupingType groupingType,

                        Authentication authentication) throws IOException {

                User currentUser = validatePackingExportAccess(authentication);

                byte[] excel = packingPeriodReportExcelService
                                .exportWeeklyPackingExcel(
                                                date,
                                                buyer,
                                                style,
                                                line,
                                                groupingType,
                                                currentUser);

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=Report-Packing-Weekly-" + date + ".xlsx")
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .body(excel);
        }

        @GetMapping("/monthly/export")
        public ResponseEntity<byte[]> exportMonthlyPackingExcel(
                        @RequestParam int month,
                        @RequestParam int year,

                        @RequestParam(defaultValue = "ALL") String buyer,
                        @RequestParam(defaultValue = "ALL") String style,
                        @RequestParam(defaultValue = "ALL") String line,
                        @RequestParam(defaultValue = "DATEWISE") ReportGroupingType groupingType,

                        Authentication authentication) throws IOException {

                User currentUser = validatePackingExportAccess(authentication);

                byte[] excel = packingPeriodReportExcelService
                                .exportMonthlyPackingExcel(
                                                month,
                                                year,
                                                buyer,
                                                style,
                                                line,
                                                groupingType,
                                                currentUser);

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=Report-Packing-Monthly-"
                                                                + year + "-" + month + ".xlsx")
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .body(excel);
        }

        private User validatePackingExportAccess(Authentication authentication) {

                Authentication currentAuthentication = authentication != null
                                ? authentication
                                : SecurityContextHolder.getContext().getAuthentication();

                if (currentAuthentication == null
                                || !currentAuthentication.isAuthenticated()
                                || currentAuthentication instanceof AnonymousAuthenticationToken
                                || currentAuthentication.getName() == null
                                || currentAuthentication.getName().isBlank()
                                || "anonymousUser".equals(currentAuthentication.getName())) {

                        throw new UnauthorizedException(
                                        "Anda harus login untuk mengakses export Packing.");
                }

                User user = userRepository.findByUsername(
                                currentAuthentication.getName())
                                .orElseThrow(() -> new UnauthorizedException(
                                                "User yang login tidak ditemukan."));

                if (user.getRole() == null
                                || !"SUPER_ADMIN".equals(user.getRole().name())) {

                        throw new UnauthorizedException(
                                        "Hanya Super Admin yang mempunyai akses untuk export laporan Packing.");
                }

                return user;
        }
}
