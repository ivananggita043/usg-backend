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
import com.garmentqc.system.dto.SectionProductivitySummaryResponse;
import com.garmentqc.system.service.ProductivityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/management/productivity")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductivityManagementAdminController {

        private final ProductivityService productivityService;

        @GetMapping("/section/{section}/daily")
        public List<ProductivitySummaryResponse> getDailyBySection(Authentication authentication,
                        @PathVariable String section,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                return productivityService.getSectionDailyProductivity(authentication.getName(), section, date);
        }

        @GetMapping("/section/{section}/daily/summary")
        public SectionProductivitySummaryResponse getDailySectionSummary(
                        Authentication authentication,
                        @PathVariable String section,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                return productivityService.getSectionDailySummary(
                                authentication.getName(),
                                section,
                                date);
        }

        @GetMapping("/section/{section}/live")
        public List<ProductivitySummaryResponse> getLiveBySection(Authentication authentication,
                        @PathVariable String section) {
                return productivityService.getSectionLiveProductivity(authentication.getName(), section);
        }

        @GetMapping("/section/{section}/live/summary")
        public SectionProductivitySummaryResponse getLiveSectionSummary(
                        Authentication authentication,
                        @PathVariable String section) {

                return productivityService.getSectionLiveSummary(
                                authentication.getName(),
                                section);
        }

        @GetMapping("/{userId}/daily")
        public DailyProductivityResponse getDaily(Authentication authentication, @PathVariable String userId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                return productivityService.getDailyProductivityForAdmin(authentication.getName(), userId, date);
        }

        @GetMapping("/{userId}/live/summary")
        public ProductivitySummaryResponse getLiveSummary(Authentication authentication, @PathVariable String userId) {
                return productivityService.getLiveProductivitySummaryForAdmin(authentication.getName(), userId);
        }

        @GetMapping("/{userId}/live/hourly")
        public List<HourlyProductivityResponse> getLiveHourly(Authentication authentication,
                        @PathVariable String userId) {
                return productivityService.getLiveHourlyProductivityForAdmin(authentication.getName(), userId);
        }
}