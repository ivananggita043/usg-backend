
package com.garmentqc.system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.InspectionDashboardResponse;
import com.garmentqc.system.dto.InspectionDashboardResponse.*;

import com.garmentqc.system.exception.BadRequestException;

import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.StyleColor;
import com.garmentqc.system.model.User;

import com.garmentqc.system.repository.InspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionDashboardService {

        private static final ZoneId ZONE_ID = ZoneId.of("Asia/Jakarta");

        private static final Set<String> ALLOWED_SECTIONS = Set.of(
                        "SEWING",
                        "FINISHING",
                        "WASHING",
                        "PACKING");

        private final InspectionRepository inspectionRepository;

        public InspectionDashboardResponse getDashboardSummary(LocalDate startDate, LocalDate endDate,
                        String requestedSection, User currentUser) {
                validateDates(startDate, endDate);

                String section = resolveSection(
                                currentUser,
                                requestedSection);

                long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

                LocalDate previousEndDate = startDate.minusDays(1);

                LocalDate previousStartDate = previousEndDate.minusDays(periodDays - 1);

                Instant queryStart = previousStartDate
                                .atStartOfDay(ZONE_ID)
                                .toInstant();

                Instant queryEnd = endDate
                                .plusDays(1)
                                .atStartOfDay(ZONE_ID)
                                .toInstant();

                List<Inspection> inspections = inspectionRepository.findDashboardInspections(
                                queryStart,
                                queryEnd);

                Map<LocalDate, DailyAccumulator> dailyData = new LinkedHashMap<>();
                Map<String, Long> buyerTotals = new HashMap<>();
                Map<BuyerStyleKey, Long> buyerStyleTotals = new HashMap<>();
                Map<BuyerDateKey, Long> buyerDailyTotals = new HashMap<>();
                Map<BuyerStyleDateKey, Long> buyerStyleDailyTotals = new HashMap<>();
                Map<DefectKey, Long> defectTotals = new HashMap<>();

                for (Inspection inspection : inspections) {
                        Line line = inspection.getLine();
                        if (line == null || line.getSection() == null) {
                                continue;
                        }

                        String inspectionSection = line.getSection().name();

                        if (section != null && !section.equals(inspectionSection)) {
                                continue;
                        }

                        LocalDate inspectionDate = inspection.getCreatedAt()
                                        .atZone(ZONE_ID)
                                        .toLocalDate();

                        DailyAccumulator daily = dailyData.computeIfAbsent(
                                        inspectionDate,
                                        ignored -> new DailyAccumulator());

                        daily.totalInspections++;

                        daily.totalQuantityInspect += valueOrZero(inspection.getInspectQuantity());

                        boolean isCurrentPeriod = !inspectionDate.isBefore(startDate)
                                        && !inspectionDate.isAfter(endDate);

                        StyleColor styleColor = inspection.getStyleColor();

                        String buyerName = normalizeName(
                                        styleColor != null
                                                        && styleColor.getStyle() != null
                                                        && styleColor.getStyle().getBuyer() != null
                                                                        ? styleColor.getStyle().getBuyer().getName()
                                                                        : null,
                                        "Unknown Buyer");

                        String styleCode = normalizeName(
                                        styleColor != null && styleColor.getStyle() != null
                                                        ? styleColor.getStyle().getStyleCode()
                                                        : null,
                                        "Unknown Style");

                        for (InspectionDefect defect : safeDefects(inspection)) {
                                long defectQuantity = valueOrZero(defect.getQuantity());

                                daily.totalDefects += defectQuantity;

                                if (!isCurrentPeriod || defectQuantity <= 0) {
                                        continue;
                                }

                                buyerTotals.merge(
                                                buyerName,
                                                defectQuantity,
                                                Long::sum);

                                BuyerStyleKey buyerStyleKey = new BuyerStyleKey(
                                                buyerName,
                                                styleCode);

                                buyerStyleTotals.merge(
                                                buyerStyleKey,
                                                defectQuantity,
                                                Long::sum);

                                BuyerDateKey buyerDateKey = new BuyerDateKey(
                                                inspectionDate,
                                                buyerName);

                                buyerDailyTotals.merge(
                                                buyerDateKey,
                                                defectQuantity,
                                                Long::sum);

                                BuyerStyleDateKey buyerStyleDateKey = new BuyerStyleDateKey(
                                                inspectionDate,
                                                buyerName,
                                                styleCode);

                                buyerStyleDailyTotals.merge(
                                                buyerStyleDateKey,
                                                defectQuantity,
                                                Long::sum);

                                if (defect.getDefectType() != null) {

                                        var defectType = defect.getDefectType();

                                        String defectName = normalizeName(
                                                        defectType.getName(),
                                                        "Unknown Defect");

                                        String category = defectType.getCategory() == null
                                                        ? "UNKNOWN"
                                                        : defectType.getCategory().name();

                                        String area = defectType.getDefectArea() == null
                                                        ? "Tanpa Area"
                                                        : normalizeName(
                                                                        defectType.getDefectArea().getName(),
                                                                        "Tanpa Area");

                                        DefectKey defectKey = new DefectKey(
                                                        defectName,
                                                        category,
                                                        area);

                                        defectTotals.merge(
                                                        defectKey,
                                                        defectQuantity,
                                                        Long::sum);
                                }
                        }
                }

                List<InspectionTrendPoint> inspectionTrend = buildInspectionTrend(
                                startDate,
                                endDate,
                                dailyData);

                List<DefectTrendPoint> defectTrend = buildDefectTrend(
                                startDate,
                                endDate,
                                dailyData);

                PeriodSummary currentPeriod = summarizePeriod(
                                startDate,
                                endDate,
                                dailyData);

                PeriodSummary previousPeriod = summarizePeriod(
                                previousStartDate,
                                previousEndDate,
                                dailyData);

                List<BuyerDefectPoint> buyerDefects = buildBuyerDefects(
                                buyerTotals,
                                currentPeriod.totalDefects());

                List<BuyerDefectTrendPoint> buyerDefectTrend = buildBuyerDefectTrend(buyerDailyTotals);

                List<BuyerStyleDefectPoint> buyerStyleDefects = buildBuyerStyleDefects(
                                buyerStyleTotals,
                                currentPeriod.totalDefects());

                List<BuyerStyleDefectTrendPoint> buyerStyleDefectTrend = buildBuyerStyleDefectTrend(
                                buyerStyleDailyTotals);

                List<TopDefectPoint> topDefects = buildTopDefects(
                                defectTotals,
                                currentPeriod.totalDefects());

                PeriodComparison comparison = new PeriodComparison(
                                currentPeriod,
                                previousPeriod);

                Growth growth = calculateGrowth(
                                currentPeriod,
                                previousPeriod);

                return new InspectionDashboardResponse(
                                startDate,
                                endDate,
                                previousStartDate,
                                previousEndDate,
                                inspectionTrend,
                                defectTrend,
                                buyerDefects,
                                buyerDefectTrend,
                                buyerStyleDefects,
                                buyerStyleDefectTrend,
                                topDefects,
                                comparison,
                                growth);
        }

        private List<InspectionTrendPoint> buildInspectionTrend(
                        LocalDate startDate,
                        LocalDate endDate,
                        Map<LocalDate, DailyAccumulator> dailyData) {

                return startDate.datesUntil(endDate.plusDays(1))
                                .map(date -> {
                                        DailyAccumulator daily = dailyData.getOrDefault(
                                                        date,
                                                        new DailyAccumulator());

                                        return new InspectionTrendPoint(
                                                        date,
                                                        daily.totalInspections,
                                                        daily.totalQuantityInspect);
                                })
                                .toList();
        }

        private List<DefectTrendPoint> buildDefectTrend(LocalDate startDate, LocalDate endDate,
                        Map<LocalDate, DailyAccumulator> dailyData) {
                return startDate.datesUntil(endDate.plusDays(1))
                                .map(date -> {
                                        DailyAccumulator daily = dailyData.getOrDefault(
                                                        date,
                                                        new DailyAccumulator());

                                        return new DefectTrendPoint(
                                                        date,
                                                        daily.totalDefects,
                                                        calculatePercentage(
                                                                        daily.totalDefects,
                                                                        daily.totalQuantityInspect));
                                })
                                .toList();
        }

        private List<BuyerDefectPoint> buildBuyerDefects(Map<String, Long> buyerTotals, long totalDefects) {
                return buyerTotals.entrySet()
                                .stream()
                                .sorted(Map.Entry.<String, Long>comparingByValue()
                                                .reversed()
                                                .thenComparing(Map.Entry.comparingByKey()))
                                .map(entry -> new BuyerDefectPoint(
                                                entry.getKey(),
                                                entry.getValue(),
                                                calculatePercentage(
                                                                entry.getValue(),
                                                                totalDefects)))
                                .toList();
        }

        private List<BuyerDefectTrendPoint> buildBuyerDefectTrend(
                        Map<BuyerDateKey, Long> buyerDailyTotals) {

                return buyerDailyTotals.entrySet()
                                .stream()
                                .sorted(Comparator
                                                .comparing(
                                                                (Map.Entry<BuyerDateKey, Long> e) -> e
                                                                                .getKey().date())
                                                .thenComparing(
                                                                e -> e.getKey().buyerName()))
                                .map(entry -> new BuyerDefectTrendPoint(
                                                entry.getKey().date(),
                                                entry.getKey().buyerName(),
                                                entry.getValue()))
                                .toList();
        }

        private List<BuyerStyleDefectPoint> buildBuyerStyleDefects(
                        Map<BuyerStyleKey, Long> buyerStyleTotals,
                        long totalDefects) {

                return buyerStyleTotals.entrySet()
                                .stream()
                                .sorted(
                                                Comparator.<Map.Entry<BuyerStyleKey, Long>>comparingLong(
                                                                Map.Entry::getValue)
                                                                .reversed()
                                                                .thenComparing(
                                                                                e -> e.getKey().buyerName())
                                                                .thenComparing(
                                                                                e -> e.getKey().styleCode()))
                                .map(entry -> new BuyerStyleDefectPoint(
                                                entry.getKey().buyerName(),
                                                entry.getKey().styleCode(),
                                                entry.getValue(),
                                                calculatePercentage(
                                                                entry.getValue(),
                                                                totalDefects)))
                                .toList();
        }

        private List<BuyerStyleDefectTrendPoint> buildBuyerStyleDefectTrend(
                        Map<BuyerStyleDateKey, Long> totals) {

                return totals.entrySet()
                                .stream()
                                .sorted(
                                                Comparator
                                                                .comparing(
                                                                                (Map.Entry<BuyerStyleDateKey, Long> e) -> e
                                                                                                .getKey().date())
                                                                .thenComparing(
                                                                                e -> e.getKey().buyerName())
                                                                .thenComparing(
                                                                                e -> e.getKey().styleCode()))
                                .map(entry -> new BuyerStyleDefectTrendPoint(
                                                entry.getKey().date(),
                                                entry.getKey().buyerName(),
                                                entry.getKey().styleCode(),
                                                entry.getValue()))
                                .toList();
        }

        private List<TopDefectPoint> buildTopDefects(Map<DefectKey, Long> defectTotals, long totalDefects) {
                return defectTotals.entrySet()
                                .stream()
                                .sorted(
                                                Comparator.<Map.Entry<DefectKey, Long>>comparingLong(
                                                                Map.Entry::getValue)
                                                                .reversed()
                                                                .thenComparing(
                                                                                e -> e.getKey().defectName()))
                                .limit(3)
                                .map(entry -> new TopDefectPoint(
                                                entry.getKey().defectName(),
                                                entry.getKey().category(),
                                                entry.getKey().area(),
                                                entry.getValue(),
                                                calculatePercentage(
                                                                entry.getValue(),
                                                                totalDefects)))
                                .toList();
        }

        private PeriodSummary summarizePeriod(LocalDate startDate, LocalDate endDate,
                        Map<LocalDate, DailyAccumulator> dailyData) {
                long totalInspections = 0;
                long totalQuantityInspect = 0;
                long totalDefects = 0;

                for (LocalDate date : startDate.datesUntil(endDate.plusDays(1)).toList()) {
                        DailyAccumulator daily = dailyData.get(date);

                        if (daily == null) {
                                continue;
                        }

                        totalInspections += daily.totalInspections;
                        totalQuantityInspect += daily.totalQuantityInspect;
                        totalDefects += daily.totalDefects;
                }

                return new PeriodSummary(
                                totalInspections,
                                totalQuantityInspect,
                                totalDefects,
                                calculatePercentage(
                                                totalDefects,
                                                totalQuantityInspect));
        }

        private Growth calculateGrowth(PeriodSummary current, PeriodSummary previous) {
                return new Growth(
                                calculateGrowthPercentage(
                                                current.totalInspections(),
                                                previous.totalInspections()),
                                calculateGrowthPercentage(
                                                current.totalQuantityInspect(),
                                                previous.totalQuantityInspect()),
                                calculateGrowthPercentage(
                                                current.totalDefects(),
                                                previous.totalDefects()),
                                roundTwoDecimals(
                                                current.defectPercentage()
                                                                - previous.defectPercentage()));
        }

        private Double calculateGrowthPercentage(long current, long previous) {
                if (previous == 0) {
                        return null;
                }

                double growth = ((double) current - previous) / previous * 100.0;

                return roundTwoDecimals(growth);
        }

        private double calculatePercentage(long defects, long denominator) {
                if (denominator <= 0) {
                        return 0.0;
                }

                return roundTwoDecimals(
                                (double) defects / denominator * 100.0);
        }

        private double roundTwoDecimals(double value) {
                return Math.round(value * 100.0) / 100.0;
        }

        private String normalizeName(String value, String fallback) {
                return value == null || value.isBlank()
                                ? fallback
                                : value.trim();
        }

        private String resolveSection(User currentUser, String requestedSection) {
                if (currentUser == null || currentUser.getRole() == null) {
                        throw new AccessDeniedException("User tidak memiliki akses");
                }

                String role = currentUser.getRole().name();

                if ("SUPER_ADMIN".equals(role) || "VISITOR".equals(role)) {
                        return normalizeSection(requestedSection);
                }

                if ("ADMIN".equals(role)) {

                        if (currentUser.getSection() == null) {
                                throw new AccessDeniedException("Admin belum memiliki section");
                        }

                        return currentUser.getSection().name();
                }

                throw new AccessDeniedException("Anda tidak memiliki akses ke dashboard");
        }

        private String normalizeSection(String section) {
                if (section == null || section.isBlank()) {
                        return null;
                }

                String normalized = section.trim().toUpperCase(Locale.ROOT);

                if ("ALL".equals(normalized)) {
                        return null;
                }

                if (!ALLOWED_SECTIONS.contains(normalized)) {
                        throw new BadRequestException("Section harus SEWING, FINISHING, WASHING, PACKING, atau ALL");
                }

                return normalized;
        }

        private void validateDates(LocalDate startDate, LocalDate endDate) {
                if (startDate == null || endDate == null) {
                        throw new BadRequestException("Start date dan end date wajib diisi");
                }

                if (startDate.isAfter(endDate)) {
                        throw new BadRequestException("Start date tidak boleh setelah end date");
                }
        }

        private long valueOrZero(Integer value) {
                return value == null ? 0L : value.longValue();
        }

        private List<InspectionDefect> safeDefects(
                        Inspection inspection) {

                if (inspection.getInspectionDefects() == null) {
                        return List.of();
                }

                return inspection.getInspectionDefects();
        }

        private record BuyerStyleKey(
                        String buyerName,
                        String styleCode) {
        }

        private record BuyerDateKey(
                        LocalDate date,
                        String buyerName) {
        }

        private record BuyerStyleDateKey(
                        LocalDate date,
                        String buyerName,
                        String styleCode) {
        }

        private record DefectKey(
                        String defectName,
                        String category,
                        String area) {
        }

        private static class DailyAccumulator {

                private long totalInspections;
                private long totalQuantityInspect;
                private long totalDefects;
        }
}
