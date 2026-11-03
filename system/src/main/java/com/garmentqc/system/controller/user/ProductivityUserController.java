package com.garmentqc.system.controller.user;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.DailyProductivityResponse;
import com.garmentqc.system.dto.HourlyProductivityResponse;
import com.garmentqc.system.dto.ProductivitySummaryResponse;
import com.garmentqc.system.service.ProductivityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/productivity")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class ProductivityUserController {

    private final ProductivityService productivityService;

    @GetMapping("/today")
    public DailyProductivityResponse getToday(Authentication authentication) {
        return productivityService.getDailyProductivity(
                authentication.getName(),
                LocalDate.now(ZoneId.of("Asia/Jakarta")));
    }

    @GetMapping("/daily")
    public DailyProductivityResponse getDaily(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return productivityService.getDailyProductivity(
                authentication.getName(), date);
    }

    @GetMapping("/live/summary")
    public ProductivitySummaryResponse getLiveSummary(Authentication authentication) {
        return productivityService.getLiveProductivitySummary(
                authentication.getName());
    }

    @GetMapping("/live/hourly")
    public List<HourlyProductivityResponse> getLiveHourly(Authentication authentication) {
        return productivityService.getLiveHourlyProductivity(
                authentication.getName());
    }
}