
package com.garmentqc.system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.CuttingGroupedReportResponse;
import com.garmentqc.system.dto.CuttingReportResponse;
import com.garmentqc.system.enums.CuttingProcessCheck;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.CuttingInspection;
import com.garmentqc.system.model.CuttingInspectionItem;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.CuttingInspectionItemRepository;
import com.garmentqc.system.repository.CuttingInspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuttingReportService {

        private static final ZoneId ZONE_ID = ZoneId.of("Asia/Jakarta");

        private final CuttingInspectionRepository inspectionRepository;
        private final CuttingInspectionItemRepository itemRepository;

        @Transactional(readOnly = true)
        public CuttingReportResponse getDailyReport(LocalDate date) {
                validateReportAccess();
                return generateReport("DAILY", date, date);
        }

        @Transactional(readOnly = true)
        public CuttingReportResponse getWeeklyReport(LocalDate date) {
                validateReportAccess();

                LocalDate startDate = getWeeklyStartDate(date);
                LocalDate endDate = startDate.plusDays(6);

                return generateReport("WEEKLY", startDate, endDate);
        }

        @Transactional(readOnly = true)
        public CuttingReportResponse getMonthlyReport(int year, int month) {
                validateReportAccess();

                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                return generateReport("MONTHLY", startDate, endDate);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getDailyBuyerReport(LocalDate date) {
                validateReportAccess();
                return generateGroupedReport("DAILY", date, date, false);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getDailyStyleReport(LocalDate date) {
                validateReportAccess();
                return generateGroupedReport("DAILY", date, date, true);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getWeeklyBuyerReport(LocalDate date) {
                validateReportAccess();

                LocalDate start = getWeeklyStartDate(date);

                return generateGroupedReport(
                                "WEEKLY",
                                start,
                                start.plusDays(6),
                                false);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getWeeklyStyleReport(LocalDate date) {
                validateReportAccess();

                LocalDate start = getWeeklyStartDate(date);

                return generateGroupedReport(
                                "WEEKLY",
                                start,
                                start.plusDays(6),
                                true);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getMonthlyBuyerReport(
                        int year,
                        int month) {
                validateReportAccess();

                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

                return generateGroupedReport(
                                "MONTHLY",
                                start,
                                end,
                                false);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getMonthlyStyleReport(
                        int year,
                        int month) {
                validateReportAccess();

                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

                return generateGroupedReport(
                                "MONTHLY",
                                start,
                                end,
                                true);
        }

        private CuttingReportResponse generateReport(
                        String reportType,
                        LocalDate startDate,
                        LocalDate endDate) {
                Instant start = startDate.atStartOfDay(ZONE_ID).toInstant();

                Instant endExclusive = endDate.plusDays(1)
                                .atStartOfDay(ZONE_ID)
                                .toInstant();

                List<CuttingInspection> inspections = inspectionRepository
                                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                                start,
                                                endExclusive);

                Map<CuttingProcessCheck, ProcessAccumulator> processMap = new EnumMap<>(CuttingProcessCheck.class);

                long totalItems = 0;
                long totalQuantityInspect = 0;
                long totalDefects = 0;

                for (CuttingInspection inspection : inspections) {
                        CuttingProcessCheck processCheck = inspection.getProcessCheck();

                        ProcessAccumulator summary = processMap.computeIfAbsent(
                                        processCheck,
                                        key -> new ProcessAccumulator());

                        summary.totalInspections++;

                        List<CuttingInspectionItem> items = itemRepository.findByCuttingInspection(inspection);

                        for (CuttingInspectionItem item : items) {
                                long quantityInspect = item.getQuantityInspect() == null
                                                ? 0L
                                                : item.getQuantityInspect();

                                long quantityDefect = item.getQuantityDefect() == null
                                                ? 0L
                                                : item.getQuantityDefect();

                                String defectTypeName = item.getDefectType() == null
                                                ? ""
                                                : item.getDefectType().getName();

                                double itemPercentage = calculatePercentage(
                                                quantityDefect,
                                                quantityInspect);

                                LocalDate inspectionDate = inspection.getCreatedAt()
                                                .atZone(ZONE_ID)
                                                .toLocalDate();

                                CuttingReportResponse.DetailRow detailRow = new CuttingReportResponse.DetailRow(
                                                item.getBuyerName(),
                                                item.getStyleCode(),
                                                item.getColorName(),
                                                item.getLot(),
                                                defectTypeName,
                                                quantityInspect,
                                                quantityDefect,
                                                itemPercentage,
                                                item.getOperatorName(),
                                                inspectionDate);

                                summary.detailRows.add(detailRow);

                                totalItems++;
                                totalQuantityInspect += quantityInspect;
                                totalDefects += quantityDefect;

                                summary.totalItems++;
                                summary.totalQuantityInspect += quantityInspect;
                                summary.totalDefects += quantityDefect;
                        }
                }

                List<CuttingReportResponse.ProcessSummary> processSummaries = new ArrayList<>();

                for (Map.Entry<CuttingProcessCheck, ProcessAccumulator> entry : processMap.entrySet()) {

                        CuttingProcessCheck processCheck = entry.getKey();
                        ProcessAccumulator summary = entry.getValue();

                        processSummaries.add(
                                        new CuttingReportResponse.ProcessSummary(
                                                        processCheck,
                                                        summary.totalInspections,
                                                        summary.totalItems,
                                                        summary.totalQuantityInspect,
                                                        summary.totalDefects,
                                                        calculatePercentage(
                                                                        summary.totalDefects,
                                                                        summary.totalQuantityInspect),
                                                        summary.detailRows));
                }

                return new CuttingReportResponse(
                                reportType,
                                startDate,
                                endDate,
                                inspections.size(),
                                totalItems,
                                totalQuantityInspect,
                                totalDefects,
                                calculatePercentage(
                                                totalDefects,
                                                totalQuantityInspect),
                                processSummaries);
        }

        private CuttingGroupedReportResponse generateGroupedReport(
                        String reportType,
                        LocalDate startDate,
                        LocalDate endDate,
                        boolean groupByStyle) {
                Instant start = startDate.atStartOfDay(ZONE_ID).toInstant();

                Instant endExclusive = endDate.plusDays(1)
                                .atStartOfDay(ZONE_ID)
                                .toInstant();

                List<CuttingInspection> inspections = inspectionRepository
                                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                                start,
                                                endExclusive);

                Map<GroupKey, GroupAccumulator> groups = new LinkedHashMap<>();

                for (CuttingInspection inspection : inspections) {
                        List<CuttingInspectionItem> items = itemRepository.findByCuttingInspection(inspection);

                        for (CuttingInspectionItem item : items) {
                                String buyer = item.getBuyerName();
                                String style = groupByStyle
                                                ? item.getStyleCode()
                                                : null;

                                GroupKey key = new GroupKey(buyer, style);

                                GroupAccumulator summary = groups.computeIfAbsent(
                                                key,
                                                ignored -> new GroupAccumulator());

                                summary.inspectionIds.add(inspection.getId());
                                summary.totalItems++;

                                long quantityInspect = item.getQuantityInspect() == null
                                                ? 0L
                                                : item.getQuantityInspect();

                                long quantityDefect = item.getQuantityDefect() == null
                                                ? 0L
                                                : item.getQuantityDefect();

                                summary.totalQuantityInspect += quantityInspect;
                                summary.totalDefects += quantityDefect;
                        }
                }

                List<CuttingGroupedReportResponse.GroupSummary> summaries = new ArrayList<>();

                for (Map.Entry<GroupKey, GroupAccumulator> entry : groups.entrySet()) {

                        GroupKey key = entry.getKey();
                        GroupAccumulator value = entry.getValue();

                        summaries.add(
                                        new CuttingGroupedReportResponse.GroupSummary(
                                                        key.buyerName(),
                                                        key.styleCode(),
                                                        value.inspectionIds.size(),
                                                        value.totalItems,
                                                        value.totalQuantityInspect,
                                                        value.totalDefects,
                                                        calculatePercentage(
                                                                        value.totalDefects,
                                                                        value.totalQuantityInspect)));
                }

                return new CuttingGroupedReportResponse(
                                reportType,
                                groupByStyle ? "STYLE" : "BUYER",
                                startDate,
                                endDate,
                                summaries);
        }

        private record GroupKey(
                        String buyerName,
                        String styleCode) {
        }

        private static class GroupAccumulator {
                private final Set<String> inspectionIds = new HashSet<>();

                private long totalItems;
                private long totalQuantityInspect;
                private long totalDefects;
        }

        private static class ProcessAccumulator {
                private long totalInspections;
                private long totalItems;
                private long totalQuantityInspect;
                private long totalDefects;

                private final List<CuttingReportResponse.DetailRow> detailRows = new ArrayList<>();
        }

        private double calculatePercentage(
                        long totalDefects,
                        long totalQuantityInspect) {
                if (totalQuantityInspect <= 0) {
                        return 0.0;
                }

                double percentage = (double) totalDefects / totalQuantityInspect * 100;

                return Math.round(percentage * 100.0) / 100.0;
        }

        private LocalDate getWeeklyStartDate(LocalDate date) {
                int daysSinceFriday = (date.getDayOfWeek().getValue() - 5 + 7) % 7;

                return date.minusDays(daysSinceFriday);
        }

        private void validateReportAccess() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication instanceof AnonymousAuthenticationToken
                                || !(authentication.getPrincipal() instanceof User currentUser)) {

                        throw new UnauthorizedException(
                                        "Autentikasi diperlukan");
                }

                if (currentUser.getRole() == null) {
                        throw new UnauthorizedException(
                                        "Role pengguna tidak valid");
                }

                String role = currentUser.getRole().name();

                if ("SUPER_ADMIN".equals(role)) {
                        return;
                }

                if ("ADMIN".equals(role)
                                && currentUser.getSection() != null
                                && "CUTTING".equals(currentUser.getSection().name())) {
                        return;
                }

                throw new UnauthorizedException(
                                "Hanya Admin CUTTING dan Super Admin yang dapat mengakses laporan cutting");
        }
}
