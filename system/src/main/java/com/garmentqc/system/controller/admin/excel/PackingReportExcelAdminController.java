
package com.garmentqc.system.controller.admin.excel;

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
@RequestMapping("/admin/excel/packing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PackingReportExcelAdminController {

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

                return excelResponse(
                                excel,
                                "Report-Packing-Daily-" + date + ".xlsx");
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

                return excelResponse(
                                excel,
                                "Report-Packing-Weekly-" + date + ".xlsx");
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

                return excelResponse(
                                excel,
                                "Report-Packing-Monthly-" + year + "-" + month + ".xlsx");
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
                                || !"ADMIN".equals(user.getRole().name())) {

                        throw new UnauthorizedException(
                                        "Hanya Admin yang dapat mengakses export laporan Packing.");
                }

                if (user.getSection() == null
                                || !"PACKING".equals(user.getSection().name())) {

                        throw new UnauthorizedException(
                                        "Anda hanya dapat mengakses laporan sesuai section Packing.");
                }

                return user;
        }

        private ResponseEntity<byte[]> excelResponse(
                        byte[] excel,
                        String filename) {

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + filename + "\"")
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .contentLength(excel.length)
                                .body(excel);
        }
}
