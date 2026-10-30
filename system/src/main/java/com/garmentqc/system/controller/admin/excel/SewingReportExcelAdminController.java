package com.garmentqc.system.controller.admin.excel;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.enums.ReportGroupingType;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.UserRepository;
import com.garmentqc.system.service.SewingPeriodReportExcelService;
import com.garmentqc.system.service.SewingReportExcelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/excel/sewing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SewingReportExcelAdminController {

        private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        private final SewingReportExcelService excelService;
        private final SewingPeriodReportExcelService periodExcelService;
        private final UserRepository userRepository;

        @GetMapping("/daily/export")
        public ResponseEntity<byte[]> exportDailySewingExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
                        throws IOException {

                User currentUser = validateSewingExportAccess();

                byte[] excel = excelService.exportDailySewingExcel(
                                date,
                                currentUser);

                return buildExcelResponse(
                                excel,
                                "Report-Sewing-Daily-" + date + ".xlsx");
        }

        @GetMapping("/weekly/export")
        public ResponseEntity<byte[]> exportWeeklySewingExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

                        @RequestParam(defaultValue = "ALL") String buyer,
                        @RequestParam(defaultValue = "ALL") String style,
                        @RequestParam(defaultValue = "ALL") String line,
                        @RequestParam(defaultValue = "LINEWISE") ReportGroupingType groupingType)
                        throws IOException {

                User currentUser = validateSewingExportAccess();

                validateReportFilters(groupingType, buyer, style);

                LocalDate startDate = date.with(
                                TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));

                LocalDate endDate = startDate.plusDays(6);

                byte[] excel = periodExcelService.exportWeeklySewingExcel(
                                date,
                                buyer,
                                style,
                                line,
                                groupingType,
                                currentUser);

                return buildExcelResponse(
                                excel,
                                "Report-Sewing-" + groupingType
                                                + "-Weekly-" + startDate + "-to-" + endDate + ".xlsx");
        }

        @GetMapping("/monthly/export")
        public ResponseEntity<byte[]> exportMonthlySewingExcel(
                        @RequestParam int month,
                        @RequestParam int year,

                        @RequestParam(defaultValue = "ALL") String buyer,
                        @RequestParam(defaultValue = "ALL") String style,
                        @RequestParam(defaultValue = "ALL") String line,
                        @RequestParam(defaultValue = "DATEWISE") ReportGroupingType groupingType)
                        throws IOException {

                User currentUser = validateSewingExportAccess();

                validateReportFilters(groupingType, buyer, style);

                YearMonth yearMonth;

                try {
                        yearMonth = YearMonth.of(year, month);
                } catch (java.time.DateTimeException ex) {
                        throw new BadRequestException(
                                        "Bulan atau tahun laporan tidak valid.");
                }

                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth();

                byte[] excel = periodExcelService.exportMonthlySewingExcel(
                                month,
                                year,
                                buyer,
                                style,
                                line,
                                groupingType,
                                currentUser);

                return buildExcelResponse(
                                excel,
                                "Report-Sewing-" + groupingType
                                                + "-Monthly-" + startDate + "-to-" + endDate + ".xlsx");
        }

        private User validateSewingExportAccess() {

                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null
                                || !authentication.isAuthenticated()
                                || "anonymousUser".equals(authentication.getPrincipal())) {

                        throw new UnauthorizedException(
                                        "Anda harus login untuk mengakses laporan ini.");
                }

                User user = userRepository.findByUsername(authentication.getName())
                                .orElseThrow(() -> new UnauthorizedException(
                                                "Data user tidak ditemukan."));

                if (user.getRole() == null
                                || !"ADMIN".equals(user.getRole().name())) {

                        throw new UnauthorizedException(
                                        "Hanya Admin yang dapat mengakses laporan ini.");
                }

                if (user.getSection() == null
                                || !"SEWING".equals(user.getSection().name())) {

                        throw new UnauthorizedException(
                                        "Anda hanya dapat mengakses laporan sesuai section Anda.");
                }

                return user;
        }

        private void validateReportFilters(
                        ReportGroupingType groupingType,
                        String buyer,
                        String style) {

                if (groupingType == null) {
                        throw new BadRequestException(
                                        "Grouping type wajib dipilih.");
                }

                if (groupingType != ReportGroupingType.LINEWISE
                                && groupingType != ReportGroupingType.DATEWISE
                                && groupingType != ReportGroupingType.BUYERWISE
                                && groupingType != ReportGroupingType.STYLEWISE) {

                        throw new BadRequestException(
                                        "Grouping type tidak valid untuk laporan Sewing.");
                }

                boolean buyerSelected = isSpecificFilter(buyer);
                boolean styleSelected = isSpecificFilter(style);

                if (groupingType == ReportGroupingType.BUYERWISE && !buyerSelected) {
                        throw new BadRequestException(
                                        "Buyer wajib dipilih untuk grouping BUYERWISE.");
                }

                if (groupingType == ReportGroupingType.STYLEWISE) {

                        if (!buyerSelected) {
                                throw new BadRequestException(
                                                "Buyer wajib dipilih untuk grouping STYLEWISE.");
                        }

                        if (!styleSelected) {
                                throw new BadRequestException(
                                                "Style wajib dipilih untuk grouping STYLEWISE.");
                        }
                }
        }

        private boolean isSpecificFilter(String value) {
                return value != null
                                && !value.isBlank()
                                && !"ALL".equalsIgnoreCase(value.trim());
        }

        private ResponseEntity<byte[]> buildExcelResponse(
                        byte[] excel,
                        String filename) {

                return ResponseEntity.ok()
                                .contentType(EXCEL_MEDIA_TYPE)
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + filename + "\"")
                                .body(excel);
        }
}