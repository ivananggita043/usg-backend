package com.garmentqc.system.controller.superadmin.excel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
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

import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.UserRepository;
import com.garmentqc.system.service.FinishingReportExcelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("super-admin/excel/finishing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class FinishingReportExcelSuperAdminController {

        private final FinishingReportExcelService reportService;
        private final UserRepository userRepository;

        @GetMapping("/daily/export")
        public ResponseEntity<byte[]> downloadExcel(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Authentication authentication) throws Exception {
                validatefinishingExportAccess();
                User currentUser = (User) authentication.getPrincipal();
                byte[] file = reportService.exportDailyFinishingExcel(
                                date, currentUser);
                String filename = "Report-Finishing-Daily-"
                                + date.format(DateTimeFormatter.ISO_DATE)
                                + ".xlsx";

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                ContentDisposition.attachment()
                                                                .filename(filename)
                                                                .build()
                                                                .toString())
                                .body(file);
        }

        private void validatefinishingExportAccess() {
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
                // ("PACKING".equals(user.getSection().name())
                // || "FINISHING".equals(user.getSection().name()))) {
                // return;
                // }

                throw new UnauthorizedException(
                                "Hanya Super Admin yang mempunyai akses untuk export laporan Finishing");
        }
}