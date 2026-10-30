
package com.garmentqc.system.controller.admin.excel;

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

import com.garmentqc.system.enums.ReportGroupingType;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.FinishingPeriodReportExcelService;
import com.garmentqc.system.service.FinishingReportExcelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/excel/finishing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FinishingReportExcelAdminController {

        private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

        private static final DateTimeFormatter FILE_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

        private final FinishingReportExcelService dailyReportService;
        private final FinishingPeriodReportExcelService periodReportService;

        @GetMapping("/daily/export")
        public ResponseEntity<byte[]> exportDaily(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Authentication authentication) throws Exception {

                User currentUser = requireAdminFinishing(authentication);

                byte[] file = dailyReportService.exportDailyFinishingExcel(
                                date,
                                currentUser);

                return excelResponse(
                                file,
                                "finishing-daily-" + date.format(FILE_DATE) + ".xlsx");
        }

        @GetMapping("/weekly/export")
        public ResponseEntity<byte[]> exportWeekly(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

                        @RequestParam(defaultValue = "DATEWISE") ReportGroupingType groupingType,

                        @RequestParam(defaultValue = "ALL") String buyer,

                        @RequestParam(defaultValue = "ALL") String style,

                        Authentication authentication) throws Exception {

                User currentUser = requireAdminFinishing(authentication);

                validateReportFilters(groupingType, buyer, style);

                byte[] file = periodReportService.exportWeeklyFinishingExcel(
                                date,
                                groupingType,
                                buyer,
                                style,
                                currentUser);

                LocalDate weekStart = date.with(
                                java.time.temporal.TemporalAdjusters.previousOrSame(
                                                java.time.DayOfWeek.FRIDAY));

                String filename = "Report-Finishing-Weekly"
                                + groupingType.name().toLowerCase(Locale.ROOT)
                                + "-"
                                + weekStart.format(FILE_DATE)
                                + ".xlsx";

                return excelResponse(file, filename);
        }

        @GetMapping("/monthly/export")
        public ResponseEntity<byte[]> exportMonthly(
                        @RequestParam int month,
                        @RequestParam int year,

                        @RequestParam(defaultValue = "DATEWISE") ReportGroupingType groupingType,

                        @RequestParam(defaultValue = "ALL") String buyer,

                        @RequestParam(defaultValue = "ALL") String style,

                        Authentication authentication) throws Exception {

                User currentUser = requireAdminFinishing(authentication);

                validateReportFilters(groupingType, buyer, style);

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
                                groupingType,
                                buyer,
                                style,
                                currentUser);

                String filename = "Report-Finishing-Monthly"
                                + groupingType.name().toLowerCase(Locale.ROOT)
                                + "-"
                                + period.format(FILE_MONTH)
                                + ".xlsx";

                return excelResponse(file, filename);
        }

        private void validateReportFilters(
                        ReportGroupingType groupingType,
                        String buyer,
                        String style) {

                if (groupingType == null) {
                        throw new BadRequestException(
                                        "Grouping laporan wajib dipilih.");
                }

                if (groupingType != ReportGroupingType.DATEWISE
                                && groupingType != ReportGroupingType.BUYERWISE
                                && groupingType != ReportGroupingType.STYLEWISE) {

                        throw new BadRequestException(
                                        "Grouping Finishing hanya mendukung DATEWISE, BUYERWISE, dan STYLEWISE.");
                }

                boolean buyerIsAll = buyer == null
                                || buyer.isBlank()
                                || buyer.trim().equalsIgnoreCase("ALL");

                boolean styleIsAll = style == null
                                || style.isBlank()
                                || style.trim().equalsIgnoreCase("ALL");

                if (groupingType == ReportGroupingType.BUYERWISE && buyerIsAll) {
                        throw new BadRequestException(
                                        "Untuk laporan BUYERWISE, pilih satu buyer terlebih dahulu.");
                }

                if (groupingType == ReportGroupingType.STYLEWISE && styleIsAll) {
                        throw new BadRequestException(
                                        "Untuk laporan STYLEWISE, pilih satu style terlebih dahulu.");
                }
        }

        private User requireAdminFinishing(Authentication authentication) {

                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new UnauthorizedException("Autentikasi diperlukan.");
                }

                Object principal = authentication.getPrincipal();

                if (!(principal instanceof User user)) {
                        throw new UnauthorizedException("Data user yang login tidak valid.");
                }

                if (user.getRole() == null
                                || !"ADMIN".equals(user.getRole().name())) {
                        throw new UnauthorizedException(
                                        "Hanya Admin yang dapat mengakses laporan Finishing.");
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