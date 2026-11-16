package com.garmentqc.system.controller.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.DailyProductivityResponse;
import com.garmentqc.system.dto.HourlyProductivityResponse;
import com.garmentqc.system.dto.ProductivitySummaryResponse;
import com.garmentqc.system.service.ProductivityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/management/productivity/{userId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductivityManagementAdminController {

        private final ProductivityService productivityService;

        @GetMapping("/daily")
        public DailyProductivityResponse getDaily(Authentication authentication, @PathVariable String userId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                return productivityService.getDailyProductivityForAdmin(
                                authentication.getName(),
                                userId,
                                date);
        }

        @GetMapping("/live/summary")
        public ProductivitySummaryResponse getLiveSummary(Authentication authentication, @PathVariable String userId) {
                return productivityService.getLiveProductivitySummaryForAdmin(
                                authentication.getName(),
                                userId);
        }

        @GetMapping("/live/hourly")
        public List<HourlyProductivityResponse> getLiveHourly(Authentication authentication,
                        @PathVariable String userId) {
                return productivityService.getLiveHourlyProductivityForAdmin(
                                authentication.getName(),
                                userId);
        }
}