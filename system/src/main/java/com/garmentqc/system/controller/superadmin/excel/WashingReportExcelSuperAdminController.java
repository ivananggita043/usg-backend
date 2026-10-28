
package com.garmentqc.system.controller.superadmin.excel;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.UserRepository;
import com.garmentqc.system.service.WashingPeriodReportExcelService;
import com.garmentqc.system.service.WashingReportExcelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/super-admin/excel/washing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class WashingReportExcelSuperAdminController {

        private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_DATE;

        private final WashingReportExcelService reportService;
        private final WashingPeriodReportExcelService periodReportService;
        private final UserRepository userRepository;

        @GetMapping("/daily/export")
        public ResponseEntity<byte[]> exportDailyWashingExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @AuthenticationPrincipal User currentUser) throws IOException {

                validateWashingExportAccess();

                byte[] excel = reportService.exportDailyWashingExcel(
                                date, currentUser);

                return excelResponse(
                                excel,
                                "Report-Washing-Daily-" + date.format(DATE_FORMAT) + ".xlsx");
        }

        @GetMapping("/weekly/export")
        public ResponseEntity<byte[]> exportWeeklyWashingExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

                        @RequestParam(defaultValue = "ALL") String reportType,

                        @RequestParam(defaultValue = "ALL") String buyer,

                        @RequestParam(defaultValue = "ALL") String style,

                        @AuthenticationPrincipal User currentUser) throws IOException {

                validateWashingExportAccess();

                String normalizedType = validateReportType(reportType);
                validateReportFilters(normalizedType, buyer, style);

                byte[] excel = periodReportService.exportWeeklyWashingExcel(
                                date,
                                normalizedType,
                                buyer,
                                style,
                                currentUser);

                LocalDate weekStart = date.with(
                                java.time.temporal.TemporalAdjusters.previousOrSame(
                                                java.time.DayOfWeek.FRIDAY));

                String filename = "Report-Washing-Weekly-"
                                + normalizedType
                                + "-"
                                + weekStart.format(DATE_FORMAT)
                                + ".xlsx";

                return excelResponse(excel, filename);
        }

        @GetMapping("/monthly/export")
        public ResponseEntity<byte[]> exportMonthlyWashingExcel(
                        @RequestParam int month,
                        @RequestParam int year,

                        @RequestParam(defaultValue = "ALL") String reportType,

                        @RequestParam(defaultValue = "ALL") String buyer,

                        @RequestParam(defaultValue = "ALL") String style,

                        @AuthenticationPrincipal User currentUser) throws IOException {

                validateWashingExportAccess();

                String normalizedType = validateReportType(reportType);
                validateReportFilters(normalizedType, buyer, style);

                final YearMonth period;

                try {
                        period = YearMonth.of(year, month);
                } catch (java.time.DateTimeException ex) {
                        throw new BadRequestException(
                                        "Bulan atau tahun laporan tidak valid.");
                }

                byte[] excel = periodReportService.exportMonthlyWashingExcel(
                                month,
                                year,
                                normalizedType,
                                buyer,
                                style,
                                currentUser);

                String filename = "Report-Washing-Monthly-"
                                + normalizedType
                                + "-"
                                + period.format(MONTH_FORMAT)
                                + ".xlsx";

                return excelResponse(excel, filename);
        }

        private String validateReportType(String reportType) {
                String normalized = reportType == null || reportType.isBlank()
                                ? "ALL"
                                : reportType.trim().toUpperCase(Locale.ROOT);

                if (!List.of("ALL", "BUYERWISE", "STYLEWISE")
                                .contains(normalized)) {

                        throw new BadRequestException("Report type harus ALL, BUYERWISE, atau STYLEWISE.");
                }

                return normalized;
        }

        private void validateReportFilters(
                        String reportType,
                        String buyer,
                        String style) {

                boolean buyerIsAll = buyer == null
                                || buyer.isBlank()
                                || "ALL".equalsIgnoreCase(buyer.trim());

                boolean styleIsAll = style == null
                                || style.isBlank()
                                || "ALL".equalsIgnoreCase(style.trim());

                if ("BUYERWISE".equals(reportType) && buyerIsAll) {
                        throw new BadRequestException(
                                        "Untuk laporan BUYERWISE, pilih satu buyer terlebih dahulu.");
                }

                if ("STYLEWISE".equals(reportType) && styleIsAll) {
                        throw new BadRequestException(
                                        "Untuk laporan STYLEWISE, pilih satu style terlebih dahulu.");
                }
        }

        private void validateWashingExportAccess() {
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication instanceof AnonymousAuthenticationToken
                                || authentication.getName() == null
                                || authentication.getName().isBlank()
                                || "anonymousUser".equals(authentication.getName())) {

                        throw new UnauthorizedException(
                                        "Anda harus login untuk mengakses export Washing.");
                }

                User user = userRepository
                                .findByUsername(authentication.getName())
                                .orElseThrow(() -> new UnauthorizedException(
                                                "User yang login tidak ditemukan."));

                if (user.getRole() == null
                                || !"SUPER_ADMIN".equals(user.getRole().name())) {

                        throw new UnauthorizedException(
                                        "Hanya Super Admin yang mempunyai akses untuk export laporan Washing.");
                }
        }

        private ResponseEntity<byte[]> excelResponse(
                        byte[] excel,
                        String filename) {

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + filename + "\"")
                                .contentType(EXCEL_MEDIA_TYPE)
                                .contentLength(excel.length)
                                .body(excel);
        }
}
