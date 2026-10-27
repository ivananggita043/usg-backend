package com.garmentqc.system.service;

import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.InspectionRepository;
import com.garmentqc.system.repository.LineRepository;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import java.util.*;
import java.util.function.ToIntFunction;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SewingPeriodReportExcelService {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Jakarta");
    private static final String SEWING = "SEWING";

    private static final String ALL = "ALL";
    private static final String BUYERWISE = "BUYERWISE";
    private static final String STYLEWISE = "STYLEWISE";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    private final InspectionRepository inspectionRepository;
    private final InspectionReportService inspectionReportService;
    private final LineRepository lineRepository;

    public byte[] exportWeeklySewingExcel(
            LocalDate selectedDate,
            String buyer,
            String style,
            String line,
            String groupingType,
            User currentUser) throws IOException {

        if (selectedDate == null) {
            throw new BadRequestException("Tanggal laporan wajib diisi.");
        }

        LocalDate startDate = selectedDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));

        LocalDate endDate = startDate.plusDays(6);

        return exportPeriodExcel(
                "WEEKLY",
                startDate,
                endDate,
                buyer,
                style,
                line,
                groupingType,
                currentUser);
    }

    public byte[] exportMonthlySewingExcel(
            int month,
            int year,
            String buyer,
            String style,
            String line,
            String groupingType,
            User currentUser) throws IOException {

        YearMonth yearMonth;

        try {
            yearMonth = YearMonth.of(year, month);
        } catch (java.time.DateTimeException e) {
            throw new BadRequestException("Bulan atau tahun laporan tidak valid.");
        }

        return exportPeriodExcel(
                "MONTHLY",
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth(),
                buyer,
                style,
                line,
                groupingType,
                currentUser);
    }

    private byte[] exportPeriodExcel(
            String periodType,
            LocalDate startDate,
            LocalDate endDate,
            String buyerFilter,
            String styleFilter,
            String lineFilter,
            String groupingType,
            User currentUser) throws IOException {

        if (startDate == null
                || endDate == null
                || endDate.isBefore(startDate)) {
            throw new BadRequestException("Periode tanggal tidak valid.");
        }

        String grouping = normalizeGroupingType(groupingType);

        String section = inspectionReportService.resolveSectionForUser(
                currentUser, SEWING);

        if (section != null
                && !section.isBlank()
                && !SEWING.equalsIgnoreCase(section)) {
            throw new BadRequestException(
                    "User tidak memiliki akses ke laporan Sewing.");
        }

        Instant start = startDate.atStartOfDay(ZONE_ID).toInstant();
        Instant endExclusive = endDate.plusDays(1)
                .atStartOfDay(ZONE_ID)
                .toInstant();

        List<Inspection> inspections = inspectionRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        start, endExclusive)
                .stream()
                .filter(i -> i.getLine() != null)
                .filter(i -> i.getLine().getSection() != null)
                .filter(i -> SEWING.equals(
                        i.getLine().getSection().name()))
                .filter(i -> canViewInspection(i, currentUser))
                .filter(i -> matches(buyerFilter, i.getBuyerName()))
                .filter(i -> matches(styleFilter, i.getStyleCode()))
                .filter(i -> matches(lineFilter, i.getLine().getName()))
                .sorted(
                        Comparator.comparing(
                                (Inspection i) -> safe(i.getBuyerName()),
                                String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                        i -> safe(i.getStyleCode()),
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                        i -> safe(i.getLine().getName()),
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                        Inspection::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder())))
                .toList();

        /*
         * ALL:
         * - line spesifik -> kolom tanggal
         * - line ALL -> kolom semua line Sewing
         *
         * BUYERWISE:
         * - filter buyer -> kolom tanggal
         *
         * STYLEWISE:
         * - filter style -> kolom tanggal
         *
         * BUYERWISE dan STYLEWISE tidak membuat kolom
         * terpisah untuk setiap buyer atau style.
         */
        boolean oneLine = grouping.equals(ALL)
                && isSpecificFilter(lineFilter);

        boolean dateColumns = oneLine
                || BUYERWISE.equals(grouping)
                || STYLEWISE.equals(grouping);

        List<ColumnData> columns = dateColumns
                ? createDateColumns(startDate, endDate)
                : createLineColumns(lineFilter);

        Map<String, ColumnData> columnMap = new HashMap<>();

        for (ColumnData column : columns) {
            columnMap.put(column.key, column);
        }

        /*
         * Setiap inspection ditambahkan tepat satu kali.
         * createLineColumns hanya membuat kolom kosong.
         */
        for (Inspection inspection : inspections) {
            String key = dateColumns
                    ? localDate(inspection).toString()
                    : inspection.getLine().getId();

            ColumnData column = columnMap.get(key);

            if (column != null) {
                column.add(inspection);
            }
        }

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(
                    periodType.equals("WEEKLY")
                            ? "Weekly Sewing"
                            : "Monthly Sewing");

            Styles styles = new Styles(workbook);

            Map<Short, CellStyle> percentageStyleCache = new HashMap<>();

            int totalDefectColumn = columns.size() + 1;
            int percentageColumn = columns.size() + 2;
            int lastColumn = percentageColumn;

            mergedRow(
                    sheet, 0, 0, lastColumn,
                    "PT UNGARAN SARI GARMENT",
                    styles.title);

            mergedRow(
                    sheet, 1, 0, lastColumn,
                    periodType + " REPORT SEWING SECTION",
                    styles.subtitle);

            mergedRow(
                    sheet, 2, 0, lastColumn,
                    "QC DEPARTMENT",
                    styles.subtitle);

            mergedRow(
                    sheet, 3, 0, lastColumn,
                    "DEPT: DRESS 2",
                    styles.subtitle);

            mergedRow(
                    sheet, 4, 0, lastColumn,
                    "PERIODE: "
                            + startDate.format(DATE_FORMAT)
                            + " - "
                            + endDate.format(DATE_FORMAT),
                    styles.periodCenter);

            mergedRow(
                    sheet, 5, 0, lastColumn,
                    "BUYER: " + displayFilter(buyerFilter)
                            + "     STYLE: " + displayFilter(styleFilter)
                            + "     LINE: " + displayFilter(lineFilter)
                            + "     GROUP: " + grouping,
                    styles.periodCenter);

            int dataStartRow;

            if (oneLine) {
                Row topHeader = sheet.createRow(6);
                Row dateHeader = sheet.createRow(7);

                cell(topHeader, 0, "NO / DEFECT", styles.header);
                sheet.addMergedRegion(
                        new CellRangeAddress(6, 7, 0, 0));

                cell(topHeader, 1, "DATE", styles.header);

                for (int c = 1; c <= columns.size(); c++) {
                    Cell currentCell = topHeader.createCell(c);
                    currentCell.setCellStyle(styles.header);
                }

                if (columns.size() > 1) {
                    sheet.addMergedRegion(
                            new CellRangeAddress(
                                    6, 6, 1, columns.size()));
                }

                for (int i = 0; i < columns.size(); i++) {
                    cell(
                            dateHeader,
                            i + 1,
                            columns.get(i).label,
                            styles.header);
                }

                cell(
                        topHeader,
                        totalDefectColumn,
                        "TOTAL DEFECT",
                        styles.header);

                cell(
                        topHeader,
                        percentageColumn,
                        "% DEFECT",
                        styles.header);

                sheet.addMergedRegion(
                        new CellRangeAddress(
                                6, 7,
                                totalDefectColumn,
                                totalDefectColumn));

                sheet.addMergedRegion(
                        new CellRangeAddress(
                                6, 7,
                                percentageColumn,
                                percentageColumn));

                dataStartRow = 8;

            } else if (dateColumns) {
                Row topHeader = sheet.createRow(6);
                Row dateHeader = sheet.createRow(7);

                cell(topHeader, 0, "NO / DEFECT", styles.header);
                sheet.addMergedRegion(
                        new CellRangeAddress(6, 7, 0, 0));

                cell(topHeader, 1, "DATE", styles.header);

                for (int c = 1; c <= columns.size(); c++) {
                    Cell currentCell = topHeader.createCell(c);
                    currentCell.setCellStyle(styles.header);
                }

                if (columns.size() > 1) {
                    sheet.addMergedRegion(
                            new CellRangeAddress(
                                    6, 6, 1, columns.size()));
                }

                for (int i = 0; i < columns.size(); i++) {
                    cell(
                            dateHeader,
                            i + 1,
                            columns.get(i).label,
                            styles.header);
                }

                cell(
                        topHeader,
                        totalDefectColumn,
                        "TOTAL DEFECT",
                        styles.header);

                cell(
                        topHeader,
                        percentageColumn,
                        "% DEFECT",
                        styles.header);

                sheet.addMergedRegion(
                        new CellRangeAddress(
                                6, 7,
                                totalDefectColumn,
                                totalDefectColumn));

                sheet.addMergedRegion(
                        new CellRangeAddress(
                                6, 7,
                                percentageColumn,
                                percentageColumn));

                dataStartRow = 8;

            } else {
                Row header = sheet.createRow(6);

                cell(header, 0, "NO / DEFECT", styles.header);

                for (int i = 0; i < columns.size(); i++) {
                    cell(
                            header,
                            i + 1,
                            columns.get(i).label,
                            styles.header);
                }

                cell(
                        header,
                        totalDefectColumn,
                        "TOTAL DEFECT",
                        styles.header);

                cell(
                        header,
                        percentageColumn,
                        "% DEFECT",
                        styles.header);

                dataStartRow = 7;
            }

            Map<DefectKey, Map<String, Integer>> defects = new TreeMap<>(
                    Comparator
                            .comparing(
                                    DefectKey::category,
                                    String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(
                                    DefectKey::area,
                                    String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(
                                    DefectKey::name,
                                    String.CASE_INSENSITIVE_ORDER));

            for (ColumnData column : columns) {
                for (Map.Entry<DefectKey, Integer> entry : column.defects.entrySet()) {

                    defects.computeIfAbsent(
                            entry.getKey(),
                            ignored -> new LinkedHashMap<>())
                            .put(column.key, entry.getValue());
                }
            }

            int totalInspectForDefectPercentage = columns.stream()
                    .mapToInt(ColumnData::inspectQuantity)
                    .sum();

            int rowIndex = dataStartRow;
            String previousCategory = null;
            String previousArea = null;

            for (DefectKey key : defects.keySet()) {
                if (!Objects.equals(previousCategory, key.category())) {
                    Row categoryRow = sheet.createRow(rowIndex++);

                    for (int c = 0; c <= lastColumn; c++) {
                        Cell categoryCell = categoryRow.createCell(c);
                        categoryCell.setCellStyle(styles.category);
                    }

                    categoryRow.getCell(0).setCellValue(key.category());

                    previousCategory = key.category();
                    previousArea = null;
                }

                if (!key.area().isBlank()
                        && !Objects.equals(previousArea, key.area())) {

                    Row areaRow = sheet.createRow(rowIndex++);

                    for (int c = 0; c <= lastColumn; c++) {
                        Cell areaCell = areaRow.createCell(c);
                        areaCell.setCellStyle(styles.area);
                    }

                    areaRow.getCell(0).setCellValue(key.area());
                    previousArea = key.area();
                }

                Row row = sheet.createRow(rowIndex++);

                cell(row, 0, key.name(), styles.body);

                int total = 0;

                for (int i = 0; i < columns.size(); i++) {
                    int qty = columns.get(i).defects
                            .getOrDefault(key, 0);

                    total += qty;

                    Cell detailCell = row.createCell(i + 1);

                    if (qty != 0) {
                        detailCell.setCellValue(qty);
                    }

                    detailCell.setCellStyle(styles.detailNumber);
                }

                Cell totalCell = row.createCell(totalDefectColumn);

                if (total != 0) {
                    totalCell.setCellValue(total);
                }

                totalCell.setCellStyle(styles.total);

                Cell percentageCell = row.createCell(percentageColumn);

                if (total != 0
                        && totalInspectForDefectPercentage != 0) {
                    percentageCell.setCellValue(
                            (double) total
                                    / totalInspectForDefectPercentage);
                }

                percentageCell.setCellStyle(
                        styles.percentageStyleLike(
                                workbook,
                                totalCell.getCellStyle(),
                                styles.percentage,
                                percentageStyleCache));
            }

            rowIndex = summaryRow(
                    sheet,
                    rowIndex,
                    "QTY DEFECT",
                    columns,
                    ColumnData::totalDefect,
                    totalDefectColumn,
                    percentageColumn,
                    styles,
                    styles.summary,
                    styles.number,
                    true);

            int qtyInspectRowIndex = rowIndex;

            rowIndex = summaryRow(
                    sheet,
                    rowIndex,
                    "QTY INSPECT",
                    columns,
                    ColumnData::inspectQuantity,
                    totalDefectColumn,
                    percentageColumn,
                    styles,
                    styles.summary,
                    styles.number,
                    true);

            rowIndex = summaryRow(
                    sheet,
                    rowIndex,
                    "QTY PRODUKSI",
                    columns,
                    ColumnData::goodQuantity,
                    totalDefectColumn,
                    percentageColumn,
                    styles,
                    styles.summary,
                    styles.number,
                    true);

            Row percentageRow = sheet.createRow(rowIndex++);

            cell(
                    percentageRow,
                    0,
                    "% DEFECT",
                    styles.summary);

            Row qtyInspectRow = sheet.getRow(qtyInspectRowIndex);

            int totalInspect = 0;
            int totalDefect = 0;

            for (int i = 0; i < columns.size(); i++) {
                ColumnData data = columns.get(i);

                double percentage = data.inspectQuantity == 0
                        ? 0.0
                        : (double) data.totalDefect
                                / data.inspectQuantity;

                totalInspect += data.inspectQuantity;
                totalDefect += data.totalDefect;

                Cell percentageCell = percentageRow.createCell(i + 1);

                if (percentage != 0.0) {
                    percentageCell.setCellValue(percentage);
                }

                Cell inspectStyleCell = qtyInspectRow.getCell(i + 1);

                CellStyle inspectStyle = inspectStyleCell == null
                        ? styles.number
                        : inspectStyleCell.getCellStyle();

                percentageCell.setCellStyle(
                        styles.percentageStyleLike(
                                workbook,
                                inspectStyle,
                                styles.percentage,
                                percentageStyleCache));
            }

            Cell totalPercentageCell = percentageRow.createCell(totalDefectColumn);

            if (totalDefect != 0) {
                totalPercentageCell.setCellValue(totalDefect);
            }

            totalPercentageCell.setCellStyle(styles.total);

            Cell overallPercentageCell = percentageRow.createCell(percentageColumn);

            double overall = totalInspect == 0
                    ? 0.0
                    : (double) totalDefect / totalInspect;

            if (overall != 0.0) {
                overallPercentageCell.setCellValue(overall);
            }

            overallPercentageCell.setCellStyle(
                    styles.percentageStyleLike(
                            workbook,
                            styles.totalPercentage,
                            styles.percentage,
                            percentageStyleCache));

            sheet.setColumnWidth(0, 32 * 256);

            for (int c = 1; c <= columns.size(); c++) {
                sheet.setColumnWidth(
                        c,
                        dateColumns ? 12 * 256 : 18 * 256);
            }

            sheet.setColumnWidth(totalDefectColumn, 16 * 256);
            sheet.setColumnWidth(percentageColumn, 14 * 256);

            sheet.createFreezePane(1, dataStartRow);

            configurePrint(sheet);

            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row currentRow = sheet.getRow(r);

                if (currentRow != null) {
                    currentRow.setHeightInPoints(
                            r < dataStartRow ? 22 : 17);
                }
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String normalizeGroupingType(String groupingType) {
        if (groupingType == null || groupingType.isBlank()) {
            return ALL;
        }

        String normalized = groupingType.trim()
                .toUpperCase(Locale.ROOT);

        if (!ALL.equals(normalized)
                && !BUYERWISE.equals(normalized)
                && !STYLEWISE.equals(normalized)) {
            throw new BadRequestException(
                    "Tipe laporan tidak valid. Gunakan ALL, BUYERWISE, atau STYLEWISE.");
        }

        return normalized;
    }

    private boolean isSpecificFilter(String filter) {
        return filter != null
                && !filter.isBlank()
                && !ALL.equalsIgnoreCase(filter.trim());
    }

    /**
     * Membuat kolom untuk semua line Sewing dari database.
     * Data inspection akan ditambahkan di exportPeriodExcel(),
     * jadi method ini tidak mengisi quantity maupun defect.
     */
    private List<ColumnData> createLineColumns(String lineFilter) {
        Map<String, ColumnData> map = new LinkedHashMap<>();

        lineRepository.findAll()
                .stream()
                .filter(line -> line.getSection() != null)
                .filter(line -> SEWING.equals(
                        line.getSection().name()))
                .filter(line -> matches(
                        lineFilter,
                        line.getName()))
                .sorted(Comparator.comparing(
                        line -> safe(line.getName()),
                        String.CASE_INSENSITIVE_ORDER))
                .forEach(line -> map.put(
                        line.getId(),
                        new ColumnData(
                                line.getId(),
                                line.getName())));

        return new ArrayList<>(map.values());
    }

    private List<ColumnData> createDateColumns(
            LocalDate startDate,
            LocalDate endDate) {

        List<ColumnData> result = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

            result.add(new ColumnData(
                    date.toString(),
                    String.valueOf(date.getDayOfMonth())));
        }

        return result;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean canViewInspection(
            Inspection inspection,
            User currentUser) {

        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }

        String role = currentUser.getRole().name();

        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }

        if ("ADMIN".equals(role)) {
            return inspection.getLine() != null
                    && inspection.getLine().getSection() != null
                    && currentUser.getSection() != null
                    && currentUser.getSection().equals(
                            inspection.getLine().getSection());
        }

        if ("USER".equals(role)) {
            return inspection.getSubmittedBy() != null
                    && currentUser.getId().equals(
                            inspection.getSubmittedBy().getId());
        }

        return false;
    }

    private boolean matches(String filter, String value) {
        if (filter == null
                || filter.isBlank()
                || ALL.equalsIgnoreCase(filter.trim())) {
            return true;
        }

        return value != null
                && value.equalsIgnoreCase(filter.trim());
    }

    private String displayFilter(String value) {
        return value == null || value.isBlank()
                ? ALL
                : value.trim();
    }

    private LocalDate localDate(Inspection inspection) {
        return inspection.getCreatedAt()
                .atZone(ZONE_ID)
                .toLocalDate();
    }

    private int summaryRow(
            Sheet sheet,
            int rowIndex,
            String label,
            List<ColumnData> columns,
            ToIntFunction<ColumnData> getter,
            int totalDefectColumn,
            int percentageColumn,
            Styles styles,
            CellStyle labelStyle,
            CellStyle valueStyle,
            boolean totalRow) {

        Row row = sheet.createRow(rowIndex);

        cell(row, 0, label, labelStyle);

        int grandTotal = 0;

        for (int i = 0; i < columns.size(); i++) {
            int value = getter.applyAsInt(columns.get(i));
            grandTotal += value;

            Cell valueCell = row.createCell(i + 1);

            if (value != 0) {
                valueCell.setCellValue(value);
            }

            valueCell.setCellStyle(valueStyle);
        }

        Cell total = row.createCell(totalDefectColumn);

        if (grandTotal != 0) {
            total.setCellValue(grandTotal);
        }

        total.setCellStyle(totalRow ? styles.total : valueStyle);

        Cell percentage = row.createCell(percentageColumn);
        percentage.setCellStyle(styles.total);

        return rowIndex + 1;
    }

    private void mergedRow(
            Sheet sheet,
            int rowIndex,
            int firstCol,
            int lastCol,
            String value,
            CellStyle style) {

        Row row = sheet.getRow(rowIndex);

        if (row == null) {
            row = sheet.createRow(rowIndex);
        }

        for (int c = firstCol; c <= lastCol; c++) {
            Cell currentCell = row.getCell(c);

            if (currentCell == null) {
                currentCell = row.createCell(c);
            }

            currentCell.setCellStyle(style);
        }

        row.getCell(firstCol).setCellValue(value);

        if (firstCol < lastCol) {
            sheet.addMergedRegion(
                    new CellRangeAddress(
                            rowIndex,
                            rowIndex,
                            firstCol,
                            lastCol));
        }
    }

    private void cell(
            Row row,
            int col,
            String value,
            CellStyle style) {

        Cell currentCell = row.createCell(col);
        currentCell.setCellValue(value == null ? "" : value);
        currentCell.setCellStyle(style);
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

    private static class ColumnData {

        private final String key;
        private final String label;

        private int inspectQuantity;
        private int transferQuantity;
        private int totalDefect;

        private final Map<DefectKey, Integer> defects = new LinkedHashMap<>();

        private ColumnData(String key, String label) {
            this.key = key;
            this.label = label;
        }

        private void add(Inspection inspection) {
            if (inspection.getInspectQuantity() != null) {
                inspectQuantity += inspection.getInspectQuantity();
            }

            if (inspection.getTransferQuantity() != null) {
                transferQuantity += inspection.getTransferQuantity();
            }

            if (inspection.getInspectionDefects() == null) {
                return;
            }

            for (InspectionDefect inspectionDefect : inspection.getInspectionDefects()) {

                if (inspectionDefect.getDefectType() == null) {
                    continue;
                }

                DefectType type = inspectionDefect.getDefectType();

                int quantity = inspectionDefect.getQuantity() == null
                        ? 0
                        : inspectionDefect.getQuantity();

                if (quantity <= 0) {
                    continue;
                }

                String category = type.getCategory() == null
                        ? "OTHER DEFECT"
                        : type.getCategory().name()
                                .replace("DEFECT_", "")
                                .replace('_', ' ');

                String area = type.getDefectArea() == null
                        || type.getDefectArea().getName() == null
                                ? ""
                                : type.getDefectArea().getName();

                DefectKey key = new DefectKey(
                        category,
                        area,
                        type.getName() == null
                                ? "UNKNOWN DEFECT"
                                : type.getName());

                defects.merge(key, quantity, Integer::sum);
                totalDefect += quantity;
            }
        }

        private int inspectQuantity() {
            return inspectQuantity;
        }

        private int totalDefect() {
            return totalDefect;
        }

        private int goodQuantity() {
            return Math.max(0, inspectQuantity - totalDefect);
        }
    }

    private record DefectKey(
            String category,
            String area,
            String name) {
    }

    private static class Styles {

        private final CellStyle title;
        private final CellStyle subtitle;
        private final CellStyle header;
        private final CellStyle category;
        private final CellStyle area;
        private final CellStyle body;
        private final CellStyle detailNumber;
        private final CellStyle number;
        private final CellStyle total;
        private final CellStyle summary;
        private final CellStyle percentage;
        private final CellStyle totalPercentage;
        private final CellStyle periodCenter;

        private Styles(Workbook workbook) {
            title = style(
                    workbook, new Color(96, 125, 107),
                    IndexedColors.WHITE.getIndex(), 14, true,
                    HorizontalAlignment.CENTER);

            subtitle = style(
                    workbook, new Color(96, 125, 107),
                    IndexedColors.WHITE.getIndex(), 9, true,
                    HorizontalAlignment.CENTER);

            header = style(
                    workbook, new Color(220, 230, 239),
                    IndexedColors.DARK_BLUE.getIndex(), 9, true,
                    HorizontalAlignment.CENTER);

            category = style(
                    workbook, new Color(220, 230, 239),
                    IndexedColors.DARK_BLUE.getIndex(), 9, true,
                    HorizontalAlignment.LEFT);

            area = style(
                    workbook, new Color(255, 249, 196),
                    IndexedColors.BLACK.getIndex(), 9, false,
                    HorizontalAlignment.LEFT);
            area.setIndention((short) 1);

            body = style(
                    workbook, Color.WHITE,
                    IndexedColors.BLACK.getIndex(), 9, false,
                    HorizontalAlignment.LEFT);
            body.setIndention((short) 2);

            detailNumber = style(
                    workbook, Color.WHITE,
                    IndexedColors.BLACK.getIndex(), 9, false,
                    HorizontalAlignment.CENTER);

            number = style(
                    workbook, new Color(245, 240, 230),
                    IndexedColors.DARK_BLUE.getIndex(), 9, false,
                    HorizontalAlignment.CENTER);

            total = style(
                    workbook, new Color(220, 232, 223),
                    IndexedColors.DARK_GREEN.getIndex(), 9, true,
                    HorizontalAlignment.CENTER);

            summary = style(
                    workbook, new Color(245, 240, 230),
                    IndexedColors.DARK_BLUE.getIndex(), 9, true,
                    HorizontalAlignment.LEFT);

            percentage = style(
                    workbook, Color.WHITE,
                    IndexedColors.DARK_BLUE.getIndex(), 9, false,
                    HorizontalAlignment.CENTER);

            totalPercentage = style(
                    workbook, new Color(220, 232, 223),
                    IndexedColors.DARK_GREEN.getIndex(), 9, true,
                    HorizontalAlignment.CENTER);

            periodCenter = style(
                    workbook, new Color(238, 243, 238),
                    IndexedColors.DARK_BLUE.getIndex(), 9, false,
                    HorizontalAlignment.CENTER);

            DataFormat dataFormat = workbook.createDataFormat();

            percentage.setDataFormat(
                    dataFormat.getFormat("0.00%"));

            totalPercentage.setDataFormat(
                    dataFormat.getFormat("0.00%"));
        }

        private CellStyle percentageStyleLike(
                Workbook workbook,
                CellStyle baseStyle,
                CellStyle percentageStyle,
                Map<Short, CellStyle> cache) {

            short key = baseStyle.getIndex();

            return cache.computeIfAbsent(key, ignored -> {
                CellStyle result = workbook.createCellStyle();
                result.cloneStyleFrom(baseStyle);
                result.setDataFormat(
                        percentageStyle.getDataFormat());
                return result;
            });
        }

        private CellStyle style(
                Workbook workbook,
                Color bg,
                short fontColor,
                int size,
                boolean bold,
                HorizontalAlignment alignment) {

            XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();

            style.setFillForegroundColor(
                    new XSSFColor(
                            bg,
                            new DefaultIndexedColorMap()));

            style.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(alignment);
            style.setVerticalAlignment(
                    VerticalAlignment.CENTER);
            style.setWrapText(true);

            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);

            style.setTopBorderColor(
                    IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBottomBorderColor(
                    IndexedColors.GREY_25_PERCENT.getIndex());
            style.setLeftBorderColor(
                    IndexedColors.GREY_25_PERCENT.getIndex());
            style.setRightBorderColor(
                    IndexedColors.GREY_25_PERCENT.getIndex());

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