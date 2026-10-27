
package com.garmentqc.system.controller.superadmin.excel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.FinishingPeriodReportExcelService;
import com.garmentqc.system.service.FinishingReportExcelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/super-admin/excel/finishing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class FinishingReportExcelSuperAdminController {

        private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

        private static final DateTimeFormatter FILE_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

        private final FinishingReportExcelService dailyReportService;
        private final FinishingPeriodReportExcelService periodReportService;

        @GetMapping("/daily/export")
        public ResponseEntity<byte[]> exportDaily(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Authentication authentication) throws Exception {

                User currentUser = requireSuperAdmin(authentication);

                byte[] file = dailyReportService.exportDailyFinishingExcel(
                                date, currentUser);

                return excelResponse(
                                file,
                                "finishing-daily-" + date.format(FILE_DATE) + ".xlsx");
        }

        @GetMapping("/weekly/export")
        public ResponseEntity<byte[]> exportWeekly(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

                        @RequestParam(defaultValue = "ALL") String reportType,

                        @RequestParam(defaultValue = "ALL") String buyer,

                        @RequestParam(defaultValue = "ALL") String style,

                        Authentication authentication) throws Exception {

                User currentUser = requireSuperAdmin(authentication);

                String normalizedType = validateReportType(reportType);
                validateReportFilters(normalizedType, buyer, style);

                byte[] file = periodReportService.exportWeeklyFinishingExcel(
                                date,
                                normalizedType,
                                buyer,
                                style,
                                currentUser);

                LocalDate weekStart = date.with(
                                java.time.temporal.TemporalAdjusters.previousOrSame(
                                                java.time.DayOfWeek.FRIDAY));

                String filename = "finishing-weekly-"
                                + normalizedType.toLowerCase(Locale.ROOT)
                                + "-"
                                + weekStart.format(FILE_DATE)
                                + ".xlsx";

                return excelResponse(file, filename);
        }

        @GetMapping("/monthly/export")
        public ResponseEntity<byte[]> exportMonthly(
                        @RequestParam int month,
                        @RequestParam int year,

                        @RequestParam(defaultValue = "ALL") String reportType,

                        @RequestParam(defaultValue = "ALL") String buyer,

                        @RequestParam(defaultValue = "ALL") String style,

                        Authentication authentication) throws Exception {

                User currentUser = requireSuperAdmin(authentication);

                String normalizedType = validateReportType(reportType);
                validateReportFilters(normalizedType, buyer, style);

                YearMonth period;

                try {
                        period = YearMonth.of(year, month);
                } catch (java.time.DateTimeException ex) {
                        throw new BadRequestException(
                                        "Bulan atau tahun laporan tidak valid.");
                }

                byte[] file = periodReportService.exportMonthlyFinishingExcel(
                                month,
                                year,
                                normalizedType,
                                buyer,
                                style,
                                currentUser);

                String filename = "finishing-monthly-"
                                + normalizedType.toLowerCase(Locale.ROOT)
                                + "-"
                                + period.format(FILE_MONTH)
                                + ".xlsx";

                return excelResponse(file, filename);
        }

        private String validateReportType(String reportType) {
                String normalized = reportType == null || reportType.isBlank()
                                ? "ALL"
                                : reportType.trim().toUpperCase(Locale.ROOT);

                if (!normalized.equals("ALL")
                                && !normalized.equals("BUYERWISE")
                                && !normalized.equals("STYLEWISE")) {

                        throw new BadRequestException(
                                        "reportType harus ALL, BUYERWISE, atau STYLEWISE.");
                }

                return normalized;
        }

        private void validateReportFilters(
                        String reportType,
                        String buyer,
                        String style) {

                boolean buyerIsAll = buyer == null
                                || buyer.isBlank()
                                || buyer.trim().equalsIgnoreCase("ALL");

                boolean styleIsAll = style == null
                                || style.isBlank()
                                || style.trim().equalsIgnoreCase("ALL");

                if ("BUYERWISE".equals(reportType) && buyerIsAll) {
                        throw new BadRequestException(
                                        "Untuk laporan BUYERWISE, pilih satu buyer terlebih dahulu.");
                }

                if ("STYLEWISE".equals(reportType) && styleIsAll) {
                        throw new BadRequestException(
                                        "Untuk laporan STYLEWISE, pilih satu style terlebih dahulu.");
                }
        }

        private User requireSuperAdmin(Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new UnauthorizedException(
                                        "Autentikasi diperlukan.");
                }

                Object principal = authentication.getPrincipal();

                if (!(principal instanceof User user)) {
                        throw new UnauthorizedException(
                                        "Data user yang login tidak valid.");
                }

                if (user.getRole() == null
                                || !"SUPER_ADMIN".equals(user.getRole().name())) {

                        throw new UnauthorizedException(
                                        "Hanya Super Admin yang dapat export laporan Finishing.");
                }

                return user;
        }

        private ResponseEntity<byte[]> excelResponse(
                        byte[] file,
                        String filename) {

                HttpHeaders headers = new HttpHeaders();

                headers.setContentType(MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

                headers.setContentDisposition(
                                ContentDisposition.attachment()
                                                .filename(filename)
                                                .build());

                headers.setContentLength(file.length);

                return ResponseEntity.ok()
                                .headers(headers)
                                .body(file);
        }
}
