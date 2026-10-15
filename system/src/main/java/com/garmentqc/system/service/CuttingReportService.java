
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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.CuttingGroupedReportResponse;
import com.garmentqc.system.dto.CuttingReportResponse;
import com.garmentqc.system.enums.CuttingPanel;
import com.garmentqc.system.enums.CuttingProcessCheck;
import com.garmentqc.system.model.CuttingInspection;
import com.garmentqc.system.model.CuttingInspectionDefect;
import com.garmentqc.system.model.CuttingInspectionItem;
import com.garmentqc.system.repository.CuttingInspectionDefectRepository;
import com.garmentqc.system.repository.CuttingInspectionItemRepository;
import com.garmentqc.system.repository.CuttingInspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuttingReportService {

        private static final ZoneId ZONE_ID = ZoneId.of("Asia/Jakarta");

        private final CuttingInspectionRepository inspectionRepository;
        private final CuttingInspectionItemRepository itemRepository;
        private final CuttingInspectionDefectRepository defectRepository;

        @Transactional(readOnly = true)
        public CuttingReportResponse getDailyReport(LocalDate date) {
                return generateReport("DAILY", date, date);
        }

        @Transactional(readOnly = true)
        public CuttingReportResponse getWeeklyReport(LocalDate date) {
                LocalDate startDate = date.minusDays(date.getDayOfWeek().getValue() - 1);
                LocalDate endDate = startDate.plusDays(6);

                return generateReport("WEEKLY", startDate, endDate);
        }

        @Transactional(readOnly = true)
        public CuttingReportResponse getMonthlyReport(int year, int month) {
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                return generateReport("MONTHLY", startDate, endDate);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getDailyBuyerReport(LocalDate date) {
                return generateGroupedReport("DAILY", date, date, false);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getDailyStyleReport(LocalDate date) {
                return generateGroupedReport("DAILY", date, date, true);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getWeeklyBuyerReport(LocalDate date) {
                LocalDate start = date.minusDays(date.getDayOfWeek().getValue() - 1);

                return generateGroupedReport("WEEKLY", start, start.plusDays(6), false);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getWeeklyStyleReport(LocalDate date) {
                LocalDate start = date.minusDays(date.getDayOfWeek().getValue() - 1);

                return generateGroupedReport("WEEKLY", start, start.plusDays(6), true);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getMonthlyBuyerReport(int year, int month) {
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

                return generateGroupedReport("MONTHLY", start, end, false);
        }

        @Transactional(readOnly = true)
        public CuttingGroupedReportResponse getMonthlyStyleReport(int year, int month) {
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

                return generateGroupedReport("MONTHLY", start, end, true);
        }

        private CuttingReportResponse generateReport(String reportType, LocalDate startDate, LocalDate endDate) {
                Instant start = startDate.atStartOfDay(ZONE_ID).toInstant();
                Instant endExclusive = endDate.plusDays(1)
                                .atStartOfDay(ZONE_ID)
                                .toInstant();

                List<CuttingInspection> inspections = inspectionRepository
                                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                                start, endExclusive);

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

                                List<CuttingInspectionDefect> defects = defectRepository.findByInspectionItem(item);
                                Map<CuttingPanel, Long> panelQuantities = createEmptyPanelMap();
                                long itemDefects = 0;

                                for (CuttingInspectionDefect defect : defects) {

                                        long quantity = defect.getQuantity() == null
                                                        ? 0L
                                                        : defect.getQuantity();

                                        if (defect.getPanel() != null) {
                                                panelQuantities.merge(
                                                                defect.getPanel(),
                                                                quantity,
                                                                Long::sum);
                                        }

                                        itemDefects += quantity;
                                }

                                double itemPercentage = calculatePercentage(
                                                itemDefects,
                                                quantityInspect);

                                String defectTypeName = item.getDefectType() == null
                                                ? ""
                                                : item.getDefectType().getName();

                                CuttingReportResponse.DetailRow detailRow = new CuttingReportResponse.DetailRow(
                                                item.getBuyerName(),
                                                item.getStyleCode(),
                                                item.getColorName(),
                                                item.getLot(),
                                                defectTypeName,
                                                panelQuantities,
                                                quantityInspect,
                                                itemDefects,
                                                itemPercentage,
                                                item.getOperatorName());

                                summary.detailRows.add(detailRow);

                                for (CuttingPanel panel : CuttingPanel.values()) {
                                        summary.panelSummaries.merge(
                                                        panel,
                                                        panelQuantities.getOrDefault(panel, 0L),
                                                        Long::sum);
                                }

                                totalItems++;
                                totalQuantityInspect += quantityInspect;
                                totalDefects += itemDefects;

                                summary.totalItems++;
                                summary.totalQuantityInspect += quantityInspect;
                                summary.totalDefects += itemDefects;
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
                                                        summary.panelSummaries,
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

        private CuttingGroupedReportResponse generateGroupedReport(String reportType, LocalDate startDate,
                        LocalDate endDate, boolean groupByStyle) {
                Instant start = startDate.atStartOfDay(ZONE_ID).toInstant();
                Instant endExclusive = endDate.plusDays(1)
                                .atStartOfDay(ZONE_ID)
                                .toInstant();

                List<CuttingInspection> inspections = inspectionRepository
                                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                                start, endExclusive);

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

                                long inspected = item.getQuantityInspect() == null
                                                ? 0L
                                                : item.getQuantityInspect();

                                summary.totalQuantityInspect += inspected;

                                List<CuttingInspectionDefect> defects = defectRepository.findByInspectionItem(item);
                                long defectCount = defects.stream()
                                                .mapToLong(defect -> defect.getQuantity() == null
                                                                ? 0L
                                                                : defect.getQuantity())
                                                .sum();

                                summary.totalDefects += defectCount;
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

        private Map<CuttingPanel, Long> createEmptyPanelMap() {
                Map<CuttingPanel, Long> panelQuantities = new EnumMap<>(CuttingPanel.class);

                for (CuttingPanel panel : CuttingPanel.values()) {
                        panelQuantities.put(panel, 0L);
                }

                return panelQuantities;
        }

        private record GroupKey(String buyerName, String styleCode) {

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

                private final Map<CuttingPanel, Long> panelSummaries = createInitialPanelSummaries();
                private final List<CuttingReportResponse.DetailRow> detailRows = new ArrayList<>();

                private static Map<CuttingPanel, Long> createInitialPanelSummaries() {
                        Map<CuttingPanel, Long> result = new EnumMap<>(CuttingPanel.class);
                        for (CuttingPanel panel : CuttingPanel.values()) {
                                result.put(panel, 0L);
                        }
                        return result;
                }
        }

        private double calculatePercentage(long totalDefects, long totalQuantityInspect) {
                if (totalQuantityInspect <= 0) {
                        return 0.0;
                }
                double percentage = (double) totalDefects / totalQuantityInspect * 100;

                return Math.round(percentage * 100.0) / 100.0;
        }
}
