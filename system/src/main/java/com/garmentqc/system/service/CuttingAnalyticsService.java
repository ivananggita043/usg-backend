
package com.garmentqc.system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.CuttingAnalyticsResponse;
import com.garmentqc.system.dto.CuttingAnalyticsResponse.DailyData;
import com.garmentqc.system.dto.CuttingAnalyticsResponse.MetricSummary;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.CuttingInspectionItem;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.CuttingInspectionItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuttingAnalyticsService {

        private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");
        private final CuttingInspectionItemRepository itemRepository;

        @Transactional(readOnly = true)
        public CuttingAnalyticsResponse getAnalytics(LocalDate startDate, LocalDate endDate, User currentUser) {
                validateAccess(currentUser);
                validateDateRange(startDate, endDate);

                long rangeDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

                LocalDate previousEndDate = startDate.minusDays(1);
                LocalDate previousStartDate = previousEndDate.minusDays(rangeDays - 1);

                List<CuttingInspectionItem> currentItems = getItems(startDate, endDate, currentUser);
                List<CuttingInspectionItem> previousItems = getItems(previousStartDate, previousEndDate, currentUser);

                Map<LocalDate, DailyTotal> currentDailyTotals = aggregateByDate(currentItems);
                Map<LocalDate, DailyTotal> previousDailyTotals = aggregateByDate(previousItems);

                List<DailyData> defectPercentageChart = new ArrayList<>();
                List<DailyData> totalInspectChart = new ArrayList<>();
                List<DailyData> totalDefectChart = new ArrayList<>();

                DailyTotal currentTotal = new DailyTotal();
                DailyTotal previousTotal = new DailyTotal();

                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                        DailyTotal daily = currentDailyTotals.getOrDefault(
                                        date, new DailyTotal());

                        defectPercentageChart.add(new DailyData(
                                        date,
                                        calculatePercentage(daily.totalDefect, daily.totalInspect)));

                        totalInspectChart.add(new DailyData(
                                        date,
                                        daily.totalInspect));

                        totalDefectChart.add(new DailyData(
                                        date,
                                        daily.totalDefect));

                        currentTotal.add(daily);
                }

                for (DailyTotal daily : previousDailyTotals.values()) {
                        previousTotal.add(daily);
                }

                double currentDefectPercentage = calculatePercentage(
                                currentTotal.totalDefect,
                                currentTotal.totalInspect);

                double previousDefectPercentage = calculatePercentage(
                                previousTotal.totalDefect,
                                previousTotal.totalInspect);

                return new CuttingAnalyticsResponse(
                                startDate,
                                endDate,
                                previousStartDate,
                                previousEndDate,

                                defectPercentageChart,
                                totalInspectChart,
                                totalDefectChart,

                                createSummary(
                                                currentDefectPercentage,
                                                previousDefectPercentage),

                                createSummary(
                                                currentTotal.totalInspect,
                                                previousTotal.totalInspect),

                                createSummary(
                                                currentTotal.totalDefect,
                                                previousTotal.totalDefect));
        }

        private List<CuttingInspectionItem> getItems(LocalDate startDate, LocalDate endDate, User currentUser) {
                Instant start = startDate.atStartOfDay(ZONE).toInstant();

                Instant end = endDate.plusDays(1)
                                .atStartOfDay(ZONE)
                                .toInstant();

                if ("USER".equals(currentUser.getRole().name())) {
                        return itemRepository.findItemsByInspectionDateRangeAndUserId(
                                        start,
                                        end,
                                        currentUser.getId());
                }

                return itemRepository.findItemsByInspectionDateRange(start, end);
        }

        private Map<LocalDate, DailyTotal> aggregateByDate(List<CuttingInspectionItem> items) {
                Map<LocalDate, DailyTotal> result = new HashMap<>();

                for (CuttingInspectionItem item : items) {

                        LocalDate date = item.getCuttingInspection()
                                        .getCreatedAt()
                                        .atZone(ZONE)
                                        .toLocalDate();

                        int quantityInspect = item.getQuantityInspect() == null
                                        ? 0
                                        : item.getQuantityInspect();

                        int quantityDefect = item.getQuantityDefect() == null
                                        ? 0
                                        : item.getQuantityDefect();

                        DailyTotal daily = result.computeIfAbsent(
                                        date, key -> new DailyTotal());

                        daily.totalInspect += quantityInspect;
                        daily.totalDefect += quantityDefect;
                }

                return result;
        }

        private MetricSummary createSummary(double currentValue, double previousValue) {
                double difference = currentValue - previousValue;

                Double percentageChange = previousValue == 0
                                ? null
                                : (difference / previousValue) * 100.0;

                return new MetricSummary(
                                round(currentValue),
                                round(previousValue),
                                round(difference),
                                percentageChange == null
                                                ? null
                                                : round(percentageChange));
        }

        private double calculatePercentage(double totalDefect, double totalInspect) {
                if (totalInspect <= 0) {
                        return 0.0;
                }

                return round((totalDefect / totalInspect) * 100.0);
        }

        private double round(double value) {
                return Math.round(value * 100.0) / 100.0;
        }

        private void validateDateRange(LocalDate startDate, LocalDate endDate) {
                if (startDate == null || endDate == null) {
                        throw new BadRequestException("Start date dan end date wajib diisi");
                }

                if (startDate.isAfter(endDate)) {
                        throw new BadRequestException("Start date tidak boleh setelah end date");
                }
        }

        private void validateAccess(User user) {
                if (user == null || user.getRole() == null) {
                        throw new UnauthorizedException("Akses ditolak");
                }

                String role = user.getRole().name();

                if ("SUPER_ADMIN".equals(role) || "VISITOR".equals(role)) {
                        return;
                }

                if (!"ADMIN".equals(role) && !"USER".equals(role)) {
                        throw new UnauthorizedException("Akses ditolak");
                }

                if (user.getSection() == null || !"CUTTING".equals(user.getSection().name())) {
                        throw new UnauthorizedException("Akses hanya untuk pengguna section CUTTING");
                }
        }

        private static class DailyTotal {
                private double totalInspect;
                private double totalDefect;

                private void add(DailyTotal other) {
                        this.totalInspect += other.totalInspect;
                        this.totalDefect += other.totalDefect;
                }
        }
}
