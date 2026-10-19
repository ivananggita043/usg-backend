
package com.garmentqc.system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.InspectionGroupedReportResponse;
import com.garmentqc.system.dto.InspectionReportResponse;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.InspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionReportService {

        private static final ZoneId ZONE_ID = ZoneId.of("Asia/Jakarta");

        private static final Set<String> ALLOWED_SECTIONS = Set.of(
                        "SEWING",
                        "FINISHING",
                        "WASHING");

        private final InspectionRepository inspectionRepository;

        public InspectionReportResponse getDailyReport(LocalDate date, String section) {
                validateDate(date);
                return generateReport("DAILY", date, date, section, null);
        }

        public InspectionReportResponse getDailyReport(LocalDate date, String section, User currentUser) {
                validateDate(date);
                String authorizedSection = resolveSectionForUser(currentUser, section);
                return generateReport("DAILY", date, date, authorizedSection, currentUser);
        }

        public InspectionReportResponse getWeeklyReport(LocalDate date, String section) {
                validateDate(date);
                LocalDate startDate = date.with(java.time.DayOfWeek.FRIDAY);
                LocalDate endDate = startDate.plusDays(6);
                return generateReport("WEEKLY", startDate, endDate, section, null);
        }

        public InspectionReportResponse getWeeklyReport(LocalDate date, String section, User currentUser) {
                validateDate(date);
                LocalDate startDate = date.with(java.time.DayOfWeek.FRIDAY);
                LocalDate endDate = startDate.plusDays(6);
                String authorizedSection = resolveSectionForUser(currentUser, section);
                return generateReport("WEEKLY", startDate, endDate, authorizedSection, currentUser);
        }

        public InspectionReportResponse getMonthlyReport(int year, int month, String section) {
                LocalDate startDate = getMonthStart(year, month);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                return generateReport("MONTHLY", startDate, endDate, section, null);
        }

        public InspectionReportResponse getMonthlyReport(int year, int month, String section, User currentUser) {
                LocalDate startDate = getMonthStart(year, month);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                String authorizedSection = resolveSectionForUser(currentUser, section);
                return generateReport("MONTHLY", startDate, endDate, authorizedSection, currentUser);
        }

        public InspectionGroupedReportResponse getDailyBuyerReport(LocalDate date, String section) {
                validateDate(date);
                return generateGroupedReport("DAILY", date, date, section, true, null);
        }

        public InspectionGroupedReportResponse getDailyBuyerReport(LocalDate date, String section, User currentUser) {
                validateDate(date);
                String authorizedSection = resolveSectionForUser(currentUser, section);
                return generateGroupedReport("DAILY", date, date, authorizedSection, true, currentUser);
        }

        public InspectionGroupedReportResponse getWeeklyBuyerReport(LocalDate date, String section) {
                validateDate(date);
                LocalDate startDate = date.with(java.time.DayOfWeek.FRIDAY);
                LocalDate endDate = startDate.plusDays(6);
                return generateGroupedReport("WEEKLY", startDate, endDate, section, true, null);
        }

        public InspectionGroupedReportResponse getWeeklyBuyerReport(LocalDate date, String section, User currentUser) {
                validateDate(date);
                LocalDate startDate = date.with(java.time.DayOfWeek.FRIDAY);
                LocalDate endDate = startDate.plusDays(6);
                String authorizedSection = resolveSectionForUser(currentUser, section);
                return generateGroupedReport("WEEKLY", startDate, endDate, authorizedSection, true, currentUser);
        }

        public InspectionGroupedReportResponse getMonthlyBuyerReport(int year, int month, String section) {
                LocalDate startDate = getMonthStart(year, month);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                return generateGroupedReport("MONTHLY", startDate, endDate, section, true, null);
        }

        public InspectionGroupedReportResponse getMonthlyBuyerReport(int year, int month, String section,
                        User currentUser) {
                LocalDate startDate = getMonthStart(year, month);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                String authorizedSection = resolveSectionForUser(currentUser, section);
                return generateGroupedReport("MONTHLY", startDate, endDate, authorizedSection, true, currentUser);
        }

        public InspectionGroupedReportResponse getDailyStyleReport(LocalDate date, String section) {
                validateDate(date);
                return generateGroupedReport("DAILY", date, date, section, false, null);
        }

        public InspectionGroupedReportResponse getDailyStyleReport(LocalDate date, String section, User currentUser) {
                validateDate(date);
                String authorizedSection = resolveSectionForUser(currentUser, section);
                return generateGroupedReport("DAILY", date, date, authorizedSection, false, currentUser);
        }

        public InspectionGroupedReportResponse getWeeklyStyleReport(LocalDate date, String section) {
                validateDate(date);
                LocalDate startDate = date.with(java.time.DayOfWeek.FRIDAY);
                LocalDate endDate = startDate.plusDays(6);
                return generateGroupedReport("WEEKLY", startDate, endDate, section, false, null);
        }

        public InspectionGroupedReportResponse getWeeklyStyleReport(LocalDate date, String section, User currentUser) {
                validateDate(date);
                LocalDate startDate = date.with(java.time.DayOfWeek.FRIDAY);
                LocalDate endDate = startDate.plusDays(6);
                String authorizedSection = resolveSectionForUser(currentUser, section);
                return generateGroupedReport("WEEKLY", startDate, endDate, authorizedSection, false, currentUser);
        }

        public InspectionGroupedReportResponse getMonthlyStyleReport(int year, int month, String section) {
                LocalDate startDate = getMonthStart(year, month);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                return generateGroupedReport("MONTHLY", startDate, endDate, section, false, null);
        }

        public InspectionGroupedReportResponse getMonthlyStyleReport(int year, int month, String section,
                        User currentUser) {
                LocalDate startDate = getMonthStart(year, month);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                String authorizedSection = resolveSectionForUser(currentUser, section);
                return generateGroupedReport("MONTHLY", startDate, endDate, authorizedSection, false, currentUser);
        }

        public String resolveSectionForUser(User currentUser, String requestedSection) {
                if (currentUser == null || currentUser.getRole() == null) {
                        throw new AccessDeniedException("User tidak memiliki akses");
                }

                String role = currentUser.getRole().name();
                if ("SUPER_ADMIN".equals(role)) {
                        return normalizeSection(requestedSection);
                }

                if ("ADMIN".equals(role)) {
                        return resolveOwnSection(currentUser, requestedSection, "Admin");
                }

                if ("USER".equals(role)) {
                        return resolveOwnSection(currentUser, requestedSection, "User/QC");
                }

                throw new AccessDeniedException("Role tidak diizinkan mengakses laporan ini");
        }

        private String resolveOwnSection(User currentUser, String requestedSection, String roleLabel) {
                if (currentUser.getSection() == null) {
                        throw new AccessDeniedException(roleLabel + " belum memiliki section");
                }

                String ownSection = currentUser.getSection().name();

                if (requestedSection == null || requestedSection.isBlank()) {
                        return ownSection;
                }

                String requested = requestedSection.trim().toUpperCase(Locale.ROOT);

                if ("ALL".equals(requested) || !ownSection.equals(requested)) {
                        throw new AccessDeniedException(
                                        roleLabel + " hanya dapat mengakses section " + ownSection);
                }
                return ownSection;
        }

        private InspectionReportResponse generateReport(String reportType, LocalDate startDate, LocalDate endDate,
                        String sectionFilter, User currentUser) {
                String normalizedSection = normalizeSection(sectionFilter);
                List<Inspection> inspections = findInspections(startDate, endDate);

                long totalQuantityInspect = 0;
                long totalTransferQuantity = 0;
                long totalDefects = 0;

                Map<String, SectionAccumulator> sections = new LinkedHashMap<>();
                Map<String, LineAccumulator> lines = new LinkedHashMap<>();
                Map<String, DefectAccumulator> defectTypes = new LinkedHashMap<>();

                int totalInspections = 0;
                for (Inspection inspection : inspections) {

                        if (!canViewInspection(inspection, currentUser)) {
                                continue;
                        }

                        Line line = inspection.getLine();

                        if (line == null || line.getSection() == null) {
                                continue;
                        }

                        String sectionName = line.getSection().name();

                        if (normalizedSection != null
                                        && !normalizedSection.equals(sectionName)) {
                                continue;
                        }

                        long inspectQuantity = valueOrZero(inspection.getInspectQuantity());
                        long transferQuantity = valueOrZero(inspection.getTransferQuantity());
                        long inspectionDefects = 0;

                        SectionAccumulator section = sections.computeIfAbsent(
                                        sectionName,
                                        ignored -> new SectionAccumulator());

                        String lineId = line.getId();

                        LineAccumulator lineSummary = lines.computeIfAbsent(
                                        lineId,
                                        ignored -> new LineAccumulator(
                                                        lineId, line.getName(), sectionName));

                        totalInspections++;

                        section.totalInspections++;
                        section.totalQuantityInspect += inspectQuantity;
                        section.totalTransferQuantity += transferQuantity;

                        lineSummary.totalInspections++;
                        lineSummary.totalQuantityInspect += inspectQuantity;
                        lineSummary.totalTransferQuantity += transferQuantity;

                        totalQuantityInspect += inspectQuantity;
                        totalTransferQuantity += transferQuantity;

                        for (InspectionDefect defect : safeDefects(inspection)) {
                                long quantity = valueOrZero(defect.getQuantity());
                                inspectionDefects += quantity;

                                DefectType type = defect.getDefectType();
                                if (type == null) {
                                        continue;
                                }

                                String typeId = type.getId();
                                DefectAccumulator defectSummary = defectTypes.computeIfAbsent(
                                                typeId,
                                                ignored -> new DefectAccumulator(
                                                                typeId, type.getName()));

                                defectSummary.totalDefects += quantity;
                        }
                        section.totalDefects += inspectionDefects;
                        lineSummary.totalDefects += inspectionDefects;
                        totalDefects += inspectionDefects;
                }

                List<InspectionReportResponse.SectionSummary> sectionResults = new ArrayList<>();

                sections.forEach((name, item) -> sectionResults.add(
                                new InspectionReportResponse.SectionSummary(
                                                name,
                                                item.totalInspections,
                                                item.totalQuantityInspect,
                                                item.totalTransferQuantity,
                                                item.totalDefects,
                                                calculatePercentage(
                                                                item.totalDefects,
                                                                item.totalQuantityInspect))));

                List<InspectionReportResponse.LineSummary> lineResults = new ArrayList<>();
                lines.forEach((id, item) -> lineResults.add(
                                new InspectionReportResponse.LineSummary(
                                                item.lineId,
                                                item.lineName,
                                                item.section,
                                                item.totalInspections,
                                                item.totalQuantityInspect,
                                                item.totalTransferQuantity,
                                                item.totalDefects,
                                                calculatePercentage(
                                                                item.totalDefects,
                                                                item.totalQuantityInspect))));

                List<InspectionReportResponse.DefectSummary> defectResults = new ArrayList<>();
                defectTypes.values().forEach(item -> defectResults.add(
                                new InspectionReportResponse.DefectSummary(
                                                item.defectTypeId,
                                                item.defectTypeName,
                                                item.totalDefects)));

                defectResults.sort(
                                (first, second) -> Long.compare(
                                                second.getTotalDefects(),
                                                first.getTotalDefects()));

                List<InspectionReportResponse.TimeSlotSummary> timeSlotResults = buildTimeSlotSummaries(
                                inspections.stream()
                                                .filter(inspection -> canViewInspection(inspection, currentUser))
                                                .filter(inspection -> inspection.getLine() != null
                                                                && inspection.getLine().getSection() != null)
                                                .filter(inspection -> normalizedSection == null
                                                                || normalizedSection.equals(
                                                                                inspection.getLine().getSection()
                                                                                                .name()))
                                                .toList());

                return new InspectionReportResponse(
                                reportType,
                                startDate,
                                endDate,
                                totalInspections,
                                totalQuantityInspect,
                                totalTransferQuantity,
                                totalDefects,
                                calculatePercentage(totalDefects, totalQuantityInspect),
                                sectionResults,
                                lineResults,
                                defectResults,
                                timeSlotResults);
        }

        private InspectionGroupedReportResponse generateGroupedReport(String reportType, LocalDate startDate,
                        LocalDate endDate, String sectionFilter, boolean buyerWise, User currentUser) {

                String normalizedSection = normalizeSection(sectionFilter);
                List<Inspection> inspections = findInspections(startDate, endDate);
                Map<String, GroupAccumulator> groups = new LinkedHashMap<>();

                for (Inspection inspection : inspections) {

                        if (!canViewInspection(inspection, currentUser)) {
                                continue;
                        }

                        Line line = inspection.getLine();

                        if (line == null || line.getSection() == null) {
                                continue;
                        }

                        String sectionName = line.getSection().name();

                        if (normalizedSection != null
                                        && !normalizedSection.equals(sectionName)) {
                                continue;
                        }

                        String buyerName = normalizeText(inspection.getBuyerName());
                        String styleCode = normalizeText(inspection.getStyleCode());
                        String key = buyerWise
                                        ? buyerName
                                        : buyerName + "\u0000" + styleCode;

                        GroupAccumulator group = groups.computeIfAbsent(
                                        key,
                                        ignored -> new GroupAccumulator(
                                                        buyerName,
                                                        buyerWise ? null : styleCode));

                        group.totalInspections++;

                        if (!styleCode.isBlank()) {
                                group.styleCodes.add(styleCode);
                        }

                        group.totalQuantityInspect += valueOrZero(inspection.getInspectQuantity());

                        group.totalTransferQuantity += valueOrZero(inspection.getTransferQuantity());

                        for (InspectionDefect defect : safeDefects(inspection)) {
                                group.totalDefects += valueOrZero(defect.getQuantity());
                        }
                }

                List<InspectionGroupedReportResponse.GroupSummary> summaries = new ArrayList<>();

                for (GroupAccumulator group : groups.values()) {
                        summaries.add(
                                        new InspectionGroupedReportResponse.GroupSummary(
                                                        group.buyerName,
                                                        group.styleCode,
                                                        group.styleCodes.size(),
                                                        group.totalInspections,
                                                        group.totalQuantityInspect,
                                                        group.totalTransferQuantity,
                                                        group.totalDefects,
                                                        calculatePercentage(
                                                                        group.totalDefects,
                                                                        group.totalQuantityInspect)));
                }

                return new InspectionGroupedReportResponse(
                                reportType,
                                buyerWise ? "BUYER" : "STYLE",
                                startDate,
                                endDate,
                                summaries);
        }

        private boolean canViewInspection(Inspection inspection, User currentUser) {
                if (currentUser == null) {
                        return true;
                }

                if (currentUser.getRole() == null) {
                        throw new AccessDeniedException("User tidak memiliki akses");
                }

                String role = currentUser.getRole().name();

                if ("USER".equals(role)) {

                        User submittedBy = inspection.getSubmittedBy();

                        return submittedBy != null
                                        && currentUser.getId() != null
                                        && currentUser.getId().equals(submittedBy.getId());
                }

                return true;
        }

        private LocalDate getMonthStart(int year, int month) {
                try {
                        return LocalDate.of(year, month, 1);
                } catch (java.time.DateTimeException ex) {
                        throw new BadRequestException("Tahun atau bulan laporan tidak valid");
                }
        }

        private List<Inspection> findInspections(LocalDate startDate, LocalDate endDate) {
                Instant start = startDate
                                .atStartOfDay(ZONE_ID)
                                .toInstant();

                Instant endExclusive = endDate
                                .plusDays(1)
                                .atStartOfDay(ZONE_ID)
                                .toInstant();

                return inspectionRepository
                                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                                start, endExclusive);
        }

        private List<InspectionDefect> safeDefects(Inspection inspection) {
                if (inspection.getInspectionDefects() == null) {
                        return List.of();
                }

                return inspection.getInspectionDefects();
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
                        throw new BadRequestException("Section harus SEWING, FINISHING, WASHING, atau ALL");
                }
                return normalized;
        }

        private String normalizeText(String value) {
                return value == null ? "" : value.trim();
        }

        private void validateDate(LocalDate date) {
                if (date == null) {
                        throw new BadRequestException("Tanggal laporan wajib diisi");
                }
        }

        private long valueOrZero(Integer value) {
                return value == null ? 0L : value.longValue();
        }

        private double calculatePercentage(long totalDefects, long quantityInspect) {
                if (quantityInspect <= 0) {
                        return 0.0;
                }
                double percentage = (double) totalDefects / quantityInspect * 100.0;
                return Math.round(percentage * 100.0) / 100.0;
        }

        private static class GroupAccumulator {
                private final String buyerName;
                private final String styleCode;
                private final Set<String> styleCodes = new LinkedHashSet<>();

                private long totalInspections;
                private long totalQuantityInspect;
                private long totalTransferQuantity;
                private long totalDefects;

                private GroupAccumulator(String buyerName, String styleCode) {
                        this.buyerName = buyerName;
                        this.styleCode = styleCode;
                }
        }

        private static class SectionAccumulator {
                private long totalInspections;
                private long totalQuantityInspect;
                private long totalTransferQuantity;
                private long totalDefects;
        }

        private static class LineAccumulator {
                private final String lineId;
                private final String lineName;
                private final String section;

                private long totalInspections;
                private long totalQuantityInspect;
                private long totalTransferQuantity;
                private long totalDefects;

                private LineAccumulator(String lineId, String lineName, String section) {
                        this.lineId = lineId;
                        this.lineName = lineName;
                        this.section = section;
                }
        }

        private static class DefectAccumulator {
                private final String defectTypeId;
                private final String defectTypeName;
                private long totalDefects;

                private DefectAccumulator(String defectTypeId, String defectTypeName) {
                        this.defectTypeId = defectTypeId;
                        this.defectTypeName = defectTypeName;
                }
        }

        private List<InspectionReportResponse.TimeSlotSummary> buildTimeSlotSummaries(List<Inspection> inspections) {
                Map<String, List<Inspection>> groupedByTimeSlot = inspections.stream()
                                .filter(i -> i.getInspectionTimeSlot() != null
                                                && !i.getInspectionTimeSlot().isBlank())
                                .collect(Collectors.groupingBy(
                                                Inspection::getInspectionTimeSlot,
                                                TreeMap::new,
                                                Collectors.toList()));

                return groupedByTimeSlot.entrySet().stream()
                                .map(entry -> {
                                        List<Inspection> slotInspections = entry.getValue();
                                        long slotTotalInspections = slotInspections.size();

                                        long slotTotalQuantityInspect = slotInspections.stream()
                                                        .mapToLong(i -> valueOrZero(i.getInspectQuantity()))
                                                        .sum();

                                        long slotTotalTransferQuantity = slotInspections.stream()
                                                        .mapToLong(i -> valueOrZero(i.getTransferQuantity()))
                                                        .sum();

                                        long slotTotalDefects = slotInspections.stream()
                                                        .flatMap(i -> safeDefects(i).stream())
                                                        .mapToLong(d -> valueOrZero(d.getQuantity()))
                                                        .sum();

                                        return new InspectionReportResponse.TimeSlotSummary(
                                                        entry.getKey(),
                                                        slotTotalInspections,
                                                        slotTotalQuantityInspect,
                                                        slotTotalTransferQuantity,
                                                        slotTotalDefects,
                                                        calculatePercentage(
                                                                        slotTotalDefects,
                                                                        slotTotalQuantityInspect));
                                })
                                .toList();
        }
}
