package com.garmentqc.system.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.InspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WashingGroupedReportExcelService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");
    private static final String WASHING = "WASHING";
    private static final String REPAIR = "REPAIR";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy",
            Locale.forLanguageTag("id-ID"));

    private final InspectionRepository inspectionRepository;
    private final InspectionReportService inspectionReportService;

    public byte[] exportWeeklyWashingExcel(LocalDate date, String reportType, String buyer, String style,
            User currentUser) throws IOException {
        validateDate(date);

        LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));

        return exportPeriod(
                "WEEKLY",
                start,
                start.plusDays(6),
                reportType,
                buyer,
                style,
                currentUser);
    }

    public byte[] exportMonthlyWashingExcel(int month, int year, String reportType, String buyer, String style,
            User currentUser) throws IOException {
        YearMonth period;

        try {
            period = YearMonth.of(year, month);
        } catch (java.time.DateTimeException ex) {
            throw new BadRequestException("Bulan atau tahun laporan tidak valid.");
        }

        return exportPeriod(
                "MONTHLY",
                period.atDay(1),
                period.atEndOfMonth(),
                reportType,
                buyer,
                style,
                currentUser);
    }

    private byte[] exportPeriod(String periodType, LocalDate startDate, LocalDate endDate, String reportTypeValue,
            String buyerValue, String styleValue, User currentUser) throws IOException {
        String authorizedSection = inspectionReportService.resolveSectionForUser(currentUser, WASHING);

        if (authorizedSection != null && !authorizedSection.isBlank() && !WASHING.equalsIgnoreCase(authorizedSection)) {
            throw new UnauthorizedException("Laporan ini hanya dapat diakses untuk section Washing.");
        }

        String reportType = normalizeReportType(reportTypeValue);
        String buyerFilter = normalizeFilter(buyerValue);
        String styleFilter = normalizeFilter(styleValue);

        if ("ALL".equals(reportType)) {
            throw new BadRequestException("Service ini hanya digunakan untuk BUYERWISE atau STYLEWISE.");
        }

        Instant start = startDate.atStartOfDay(ZONE).toInstant();
        Instant endExclusive = endDate.plusDays(1).atStartOfDay(ZONE).toInstant();

        List<Inspection> inspections = inspectionRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, endExclusive)
                .stream()
                .filter(i -> i.getCreatedAt() != null)
                .filter(i -> i.getLine() != null)
                .filter(i -> i.getLine().getSection() != null)
                .filter(i -> WASHING.equalsIgnoreCase(i.getLine().getSection().name()))
                .filter(i -> i.getWashingType() != null)
                .filter(i -> canViewInspection(i, currentUser))
                .filter(i -> matches(buyerFilter, i.getBuyerName()))
                .filter(i -> "BUYERWISE".equals(reportType) || matches(styleFilter, i.getStyleCode()))
                .sorted(
                        Comparator.comparing(
                                (Inspection i) -> text(i.getBuyerName()),
                                String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                        i -> text(i.getStyleCode()),
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                        Inspection::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder())))
                .toList();

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Styles styles = new Styles(workbook);
            Sheet sheet = workbook.createSheet("Washing QC Report");

            boolean buyerwise = "BUYERWISE".equals(reportType);
            int firstQtyColumn = buyerwise ? 4 : 5;
            int lastColumn = firstQtyColumn + 3;

            writeReportHeader(
                    sheet, styles, lastColumn, periodType,
                    startDate, endDate, reportType,
                    buyerFilter, styleFilter);

            int rowIndex = 6;

            mergedRow(sheet, rowIndex++, lastColumn, "SUMMARY", styles.sectionTitle);

            rowIndex = writeSummary(
                    sheet, styles, rowIndex, lastColumn,
                    inspections, buyerwise);

            rowIndex++;

            mergedRow(sheet, rowIndex++, lastColumn,
                    "DETAIL DEFECT", styles.sectionTitle);

            writeDetailDefects(
                    sheet, styles, rowIndex, lastColumn,
                    firstQtyColumn, inspections, buyerwise);

            configureColumns(sheet, lastColumn);
            sheet.createFreezePane(0, 6);
            configurePrint(sheet);

            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);

                if (row != null) {
                    row.setHeightInPoints(r < 6 ? 22 : 19);
                }
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void writeReportHeader(Sheet sheet, Styles styles, int lastColumn, String periodType, LocalDate startDate,
            LocalDate endDate, String reportType, String buyerFilter, String styleFilter) {
        mergedRow(sheet, 0, lastColumn,
                "PT UNGARAN SARI GARMENT", styles.title);

        mergedRow(sheet, 1, lastColumn,
                periodType + " REPORT WASHING - " + reportType,
                styles.subtitle);

        mergedRow(sheet, 2, lastColumn,
                "QC DEPARTMENT", styles.subtitle);

        mergedRow(sheet, 3, lastColumn,
                "DEPT: DRESS 2", styles.subtitle);

        mergedRow(sheet, 4, lastColumn,
                "PERIODE: "
                        + startDate.format(DATE_FORMAT)
                        + " - "
                        + endDate.format(DATE_FORMAT),
                styles.period);

        String filterText = "BUYER: " + displayFilter(buyerFilter);

        if (!"BUYERWISE".equals(reportType)) {
            filterText += "     STYLE: " + displayFilter(styleFilter);
        }

        mergedRow(sheet, 5, lastColumn, filterText, styles.period);
    }

    private int writeSummary(Sheet sheet, Styles styles, int rowIndex, int lastColumn, List<Inspection> inspections,
            boolean buyerwise) {
        int inspectCol = buyerwise ? 2 : 3;
        int bulkCol = inspectCol + 1;
        int repairCol = inspectCol + 2;
        int grandCol = inspectCol + 3;
        int goodCol = inspectCol + 4;
        int percentCol = inspectCol + 5;

        Row top = sheet.createRow(rowIndex++);
        Row sub = sheet.createRow(rowIndex++);

        for (int c = 0; c <= lastColumn; c++) {
            cell(top, c, "", styles.header);
            cell(sub, c, "", styles.header);
        }

        top.getCell(0).setCellValue("NO");
        top.getCell(1).setCellValue("BUYER");

        if (!buyerwise) {
            top.getCell(2).setCellValue("STYLE");
        }

        top.getCell(inspectCol).setCellValue("QTY INSPECT");
        top.getCell(bulkCol).setCellValue("QTY DEFECT");
        sub.getCell(bulkCol).setCellValue("BULK");
        sub.getCell(repairCol).setCellValue("REPAIR");
        sub.getCell(grandCol).setCellValue("GRAND TOTAL");
        top.getCell(goodCol).setCellValue("GOOD QC");
        top.getCell(percentCol).setCellValue("% DEFECT");

        int topRow = top.getRowNum();
        int subRow = sub.getRowNum();

        for (int c = 0; c <= lastColumn; c++) {
            if (c >= bulkCol && c <= grandCol) {
                continue;
            }
            sheet.addMergedRegion(new CellRangeAddress(topRow, subRow, c, c));
        }

        sheet.addMergedRegion(new CellRangeAddress(topRow, topRow, bulkCol, grandCol));

        Map<GroupKey, Summary> summaries = summarize(inspections, buyerwise);
        long totalInspect = 0;
        long totalBulk = 0;
        long totalRepair = 0;
        int no = 1;

        for (Map.Entry<GroupKey, Summary> entry : summaries.entrySet()) {
            GroupKey key = entry.getKey();
            Summary s = entry.getValue();

            long defect = s.bulkDefects + s.repairDefects;

            Row row = sheet.createRow(rowIndex++);

            cell(row, 0, String.valueOf(no++), styles.body);
            cell(row, 1, key.buyer(), styles.body);

            if (!buyerwise) {
                cell(row, 2, key.style(), styles.body);
            }

            numberCell(row, inspectCol, s.inspectQuantity, styles.number);
            numberCell(row, bulkCol, s.bulkDefects, styles.number);
            numberCell(row, repairCol, s.repairDefects, styles.number);
            numberCell(row, grandCol, defect, styles.number);
            numberCell(row, goodCol, Math.max(0, s.inspectQuantity - defect), styles.number);
            percentageCell(row, percentCol, percentage(defect, s.inspectQuantity), styles.percentage);

            totalInspect += s.inspectQuantity;
            totalBulk += s.bulkDefects;
            totalRepair += s.repairDefects;
        }

        long totalDefect = totalBulk + totalRepair;

        Row totalRow = sheet.createRow(rowIndex++);

        for (int c = 0; c <= lastColumn; c++) {
            cell(totalRow, c, "", styles.total);
        }

        totalRow.getCell(0).setCellValue("TOTAL");
        sheet.addMergedRegion(new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, inspectCol - 1));

        numberCell(totalRow, inspectCol, totalInspect, styles.total);
        numberCell(totalRow, bulkCol, totalBulk, styles.total);
        numberCell(totalRow, repairCol, totalRepair, styles.total);
        numberCell(totalRow, grandCol, totalDefect, styles.total);
        numberCell(totalRow, goodCol, Math.max(0, totalInspect - totalDefect), styles.total);
        percentageCell(totalRow, percentCol, percentage(totalDefect, totalInspect), styles.totalPercentage);

        return rowIndex;
    }

    private int writeDetailDefects(Sheet sheet, Styles styles, int rowIndex, int lastColumn, int firstQtyColumn,
            List<Inspection> inspections, boolean buyerwise) {
        int bulkCol = firstQtyColumn;
        int repairCol = firstQtyColumn + 1;
        int grandCol = firstQtyColumn + 2;
        int percentCol = firstQtyColumn + 3;

        Row top = sheet.createRow(rowIndex++);
        Row sub = sheet.createRow(rowIndex++);

        for (int c = 0; c <= lastColumn; c++) {
            cell(top, c, "", styles.header);
            cell(sub, c, "", styles.header);
        }

        int col = 0;
        top.getCell(col++).setCellValue("BUYER");

        if (!buyerwise) {
            top.getCell(col++).setCellValue("STYLE");
        }

        top.getCell(col++).setCellValue("CATEGORY");
        top.getCell(col++).setCellValue("AREA");
        top.getCell(col).setCellValue("DEFECT TYPE");

        top.getCell(bulkCol).setCellValue("QTY DEFECT");
        sub.getCell(bulkCol).setCellValue("BULK");
        sub.getCell(repairCol).setCellValue("REPAIR");
        sub.getCell(grandCol).setCellValue("GRAND TOTAL");
        top.getCell(percentCol).setCellValue("% DEFECT");

        int topRow = top.getRowNum();
        int subRow = sub.getRowNum();

        for (int c = 0; c <= lastColumn; c++) {
            if (c >= bulkCol && c <= grandCol) {
                continue;
            }
            sheet.addMergedRegion(new CellRangeAddress(topRow, subRow, c, c));
        }

        sheet.addMergedRegion(new CellRangeAddress(topRow, topRow, bulkCol, grandCol));

        Map<GroupKey, DetailGroup> groups = buildDetailGroups(inspections, buyerwise);

        if (groups.isEmpty()) {
            mergedRow(sheet, rowIndex++, lastColumn, "NO DEFECT DATA", styles.empty);
            return rowIndex;
        }

        for (Map.Entry<GroupKey, DetailGroup> entry : groups.entrySet()) {
            GroupKey groupKey = entry.getKey();
            DetailGroup group = entry.getValue();

            Row groupRow = sheet.createRow(rowIndex++);

            for (int c = 0; c <= lastColumn; c++) {
                cell(groupRow, c, "", styles.groupHeader);
            }

            groupRow.getCell(0).setCellValue(groupKey.buyer());

            if (!buyerwise) {
                groupRow.getCell(1).setCellValue(groupKey.style());
            }

            long groupBulk = 0;
            long groupRepair = 0;

            for (Map.Entry<DetailKey, long[]> detailEntry : group.details.entrySet()) {
                DetailKey detail = detailEntry.getKey();
                long bulk = detailEntry.getValue()[0];
                long repair = detailEntry.getValue()[1];
                long grand = bulk + repair;

                groupBulk += bulk;
                groupRepair += repair;

                Row row = sheet.createRow(rowIndex++);
                int currentCol = 0;

                cell(row, currentCol++, "", styles.body);

                if (!buyerwise) {
                    cell(row, currentCol++, "", styles.body);
                }

                cell(row, currentCol++, detail.category(), styles.body);
                cell(row, currentCol++, detail.area(), styles.body);
                cell(row, currentCol, detail.defectType(), styles.body);

                numberCell(row, bulkCol, bulk, styles.number);
                numberCell(row, repairCol, repair, styles.number);
                numberCell(row, grandCol, grand, styles.number);
                percentageCell(row, percentCol,
                        percentage(grand, group.inspectQuantity),
                        styles.percentage);
            }

            long groupGrand = groupBulk + groupRepair;

            Row totalRow = sheet.createRow(rowIndex++);

            for (int c = 0; c <= lastColumn; c++) {
                cell(totalRow, c, "", styles.total);
            }

            String label = buyerwise
                    ? "TOTAL DEFECT " + groupKey.buyer()
                    : "TOTAL DEFECT " + groupKey.buyer() + " / " + groupKey.style();

            totalRow.getCell(0).setCellValue(label);

            sheet.addMergedRegion(
                    new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, firstQtyColumn - 1));

            numberCell(totalRow, bulkCol, groupBulk, styles.total);
            numberCell(totalRow, repairCol, groupRepair, styles.total);
            numberCell(totalRow, grandCol, groupGrand, styles.total);
            percentageCell(totalRow, percentCol, percentage(groupGrand, group.inspectQuantity), styles.totalPercentage);
        }
        return rowIndex;
    }

    private Map<GroupKey, Summary> summarize(List<Inspection> inspections, boolean buyerwise) {
        Map<GroupKey, Summary> summaries = new TreeMap<>(groupComparator());

        for (Inspection inspection : inspections) {
            GroupKey key = new GroupKey(
                    text(inspection.getBuyerName()),
                    buyerwise ? "" : text(inspection.getStyleCode()));

            Summary summary = summaries.computeIfAbsent(
                    key, ignored -> new Summary());

            summary.inspectQuantity += number(inspection.getInspectQuantity());

            long defects = totalDefects(inspection);

            if (isRepair(inspection)) {
                summary.repairDefects += defects;
            } else {
                summary.bulkDefects += defects;
            }
        }

        return summaries;
    }

    private Map<GroupKey, DetailGroup> buildDetailGroups(List<Inspection> inspections, boolean buyerwise) {
        Map<GroupKey, DetailGroup> groups = new TreeMap<>(groupComparator());

        for (Inspection inspection : inspections) {
            GroupKey groupKey = new GroupKey(
                    text(inspection.getBuyerName()),
                    buyerwise ? "" : text(inspection.getStyleCode()));

            DetailGroup group = groups.computeIfAbsent(
                    groupKey, ignored -> new DetailGroup());

            group.inspectQuantity += number(inspection.getInspectQuantity());

            boolean repair = isRepair(inspection);

            for (InspectionDefect defect : safeDefects(inspection)) {
                if (defect == null || defect.getDefectType() == null) {
                    continue;
                }

                long quantity = number(defect.getQuantity());

                if (quantity <= 0) {
                    continue;
                }

                String category = defect.getDefectType().getCategory() == null
                        ? ""
                        : defect.getDefectType()
                                .getCategory()
                                .name()
                                .replace("DEFECT_", "")
                                .replace('_', ' ');

                String area = defect.getDefectType().getDefectArea() == null
                        ? ""
                        : text(defect.getDefectType().getDefectArea().getName());

                String defectName = text(defect.getDefectType().getName());

                DetailKey detailKey = new DetailKey(category, area, defectName);

                long[] values = group.details.computeIfAbsent(
                        detailKey, ignored -> new long[2]);

                values[repair ? 1 : 0] += quantity;
            }
        }

        return groups;
    }

    private Comparator<GroupKey> groupComparator() {
        return Comparator.comparing(
                GroupKey::buyer, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(
                        GroupKey::style, String.CASE_INSENSITIVE_ORDER);
    }

    private boolean isRepair(Inspection inspection) {
        return inspection.getWashingType() != null
                && REPAIR.equalsIgnoreCase(inspection.getWashingType().name());
    }

    private long totalDefects(Inspection inspection) {
        long total = 0;

        for (InspectionDefect defect : safeDefects(inspection)) {
            if (defect == null || defect.getDefectType() == null) {
                continue;
            }

            total += number(defect.getQuantity());
        }

        return total;
    }

    private boolean canViewInspection(Inspection inspection, User currentUser) {

        if (currentUser == null || currentUser.getRole() == null) {
            throw new AccessDeniedException("User tidak memiliki akses laporan.");
        }

        String role = currentUser.getRole().name();

        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }

        if ("ADMIN".equals(role)) {
            return currentUser.getSection() != null && inspection.getLine() != null
                    && currentUser.getSection().equals(
                            inspection.getLine().getSection());
        }

        if ("USER".equals(role)) {
            return inspection.getSubmittedBy() != null && currentUser.getId() != null
                    && currentUser.getId().equals(
                            inspection.getSubmittedBy().getId());
        }

        return false;
    }

    private String normalizeReportType(String value) {
        String normalized = value == null || value.isBlank()
                ? "ALL"
                : value.trim().toUpperCase(Locale.ROOT);

        if (!List.of("ALL", "BUYERWISE", "STYLEWISE")
                .contains(normalized)) {

            throw new BadRequestException("Tipe laporan harus ALL, BUYERWISE, atau STYLEWISE.");
        }

        return normalized;
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())
                ? "ALL"
                : value.trim();
    }

    private boolean matches(String filter, String value) {
        return "ALL".equalsIgnoreCase(filter) || (value != null
                && filter.equalsIgnoreCase(value.trim()));
    }

    private String displayFilter(String value) {
        return value == null || value.isBlank()
                ? "ALL"
                : value.trim();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private long number(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private double percentage(long defects, long inspect) {
        return inspect <= 0
                ? 0.0
                : (double) defects / inspect;
    }

    private List<InspectionDefect> safeDefects(Inspection inspection) {
        return inspection.getInspectionDefects() == null
                ? List.of()
                : inspection.getInspectionDefects();
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("Tanggal laporan wajib diisi.");
        }
    }

    private void mergedRow(Sheet sheet, int rowIndex, int lastColumn, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);

        for (int col = 0; col <= lastColumn; col++) {
            Cell cell = row.createCell(col);
            cell.setCellStyle(style);
        }

        row.getCell(0).setCellValue(value);

        if (lastColumn > 0) {
            sheet.addMergedRegion(
                    new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
        }
    }

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void numberCell(Row row, int col, long value, CellStyle style) {
        Cell cell = row.createCell(col);

        if (value != 0) {
            cell.setCellValue(value);
        }

        cell.setCellStyle(style);
    }

    private void percentageCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);

        if (value != 0.0) {
            cell.setCellValue(value);
        }

        cell.setCellStyle(style);
    }

    private void configureColumns(Sheet sheet, int lastColumn) {
        sheet.setColumnWidth(0, 20 * 256);

        for (int c = 1; c <= lastColumn; c++) {
            sheet.setColumnWidth(c, 18 * 256);
        }

        sheet.setColumnWidth(lastColumn, 14 * 256);
    }

    private void configurePrint(Sheet sheet) {
        PrintSetup setup = sheet.getPrintSetup();

        setup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        setup.setLandscape(true);
        setup.setFitWidth((short) 1);
        setup.setFitHeight((short) 0);

        sheet.setFitToPage(true);
        sheet.setAutobreaks(true);
        sheet.setPrintGridlines(false);
        sheet.setDisplayGridlines(true);

        sheet.setMargin(Sheet.LeftMargin, 0.4);
        sheet.setMargin(Sheet.RightMargin, 0.4);
        sheet.setMargin(Sheet.TopMargin, 0.6);
        sheet.setMargin(Sheet.BottomMargin, 0.6);

        sheet.setHorizontallyCenter(true);
    }

    private record GroupKey(String buyer, String style) {

    }

    private record DetailKey(String category, String area, String defectType) {

    }

    private static class Summary {
        private long inspectQuantity;
        private long bulkDefects;
        private long repairDefects;
    }

    private static class DetailGroup {
        private long inspectQuantity;

        private final Map<DetailKey, long[]> details = new TreeMap<>(
                Comparator.comparing(
                        DetailKey::category,
                        String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                DetailKey::area,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                DetailKey::defectType,
                                String.CASE_INSENSITIVE_ORDER));
    }

    private static class Styles {
        private final CellStyle title;
        private final CellStyle subtitle;
        private final CellStyle period;
        private final CellStyle sectionTitle;
        private final CellStyle header;
        private final CellStyle body;
        private final CellStyle number;
        private final CellStyle percentage;
        private final CellStyle total;
        private final CellStyle totalPercentage;
        private final CellStyle groupHeader;
        private final CellStyle empty;

        private Styles(Workbook workbook) {

            Color green = new Color(96, 125, 107);
            Color lightGreen = new Color(238, 243, 238);
            Color lightBlue = new Color(220, 230, 239);
            Color totalGreen = new Color(220, 232, 223);
            Color groupBlue = new Color(235, 240, 245);

            short numberFormat = workbook.createDataFormat().getFormat("#,##0");
            short percentFormat = workbook.createDataFormat().getFormat("0.00%");

            title = style(
                    workbook, green,
                    IndexedColors.WHITE.getIndex(),
                    14, true, HorizontalAlignment.CENTER);

            subtitle = style(
                    workbook, green,
                    IndexedColors.WHITE.getIndex(),
                    9, true, HorizontalAlignment.CENTER);

            period = style(
                    workbook, lightGreen,
                    IndexedColors.DARK_BLUE.getIndex(),
                    9, false, HorizontalAlignment.CENTER);

            sectionTitle = style(
                    workbook, lightBlue,
                    IndexedColors.DARK_BLUE.getIndex(),
                    10, true, HorizontalAlignment.CENTER);

            header = style(
                    workbook, lightBlue,
                    IndexedColors.DARK_BLUE.getIndex(),
                    9, true, HorizontalAlignment.CENTER);

            body = style(
                    workbook, Color.WHITE,
                    IndexedColors.BLACK.getIndex(),
                    9, false, HorizontalAlignment.CENTER);

            number = style(
                    workbook, Color.WHITE,
                    IndexedColors.BLACK.getIndex(),
                    9, false, HorizontalAlignment.CENTER);

            percentage = style(
                    workbook, Color.WHITE,
                    IndexedColors.BLACK.getIndex(),
                    9, false, HorizontalAlignment.CENTER);

            total = style(
                    workbook, totalGreen,
                    IndexedColors.DARK_GREEN.getIndex(),
                    9, true, HorizontalAlignment.CENTER);

            totalPercentage = style(
                    workbook, totalGreen,
                    IndexedColors.DARK_GREEN.getIndex(),
                    9, true, HorizontalAlignment.CENTER);

            groupHeader = style(
                    workbook, groupBlue,
                    IndexedColors.DARK_BLUE.getIndex(),
                    9, true, HorizontalAlignment.LEFT);

            empty = style(
                    workbook, Color.WHITE,
                    IndexedColors.GREY_50_PERCENT.getIndex(),
                    9, false, HorizontalAlignment.CENTER);

            number.setDataFormat(numberFormat);
            total.setDataFormat(numberFormat);

            percentage.setDataFormat(percentFormat);
            totalPercentage.setDataFormat(percentFormat);
        }

        private CellStyle style(
                Workbook workbook,
                Color background,
                short fontColor,
                int size,
                boolean bold,
                HorizontalAlignment alignment) {

            XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();

            style.setFillForegroundColor(new XSSFColor(background, new DefaultIndexedColorMap()));

            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(alignment);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(true);

            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);

            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

            XSSFFont font = (XSSFFont) workbook.createFont();

            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            font.setColor(fontColor);

            style.setFont(font);

            return style;
        }
    }
}