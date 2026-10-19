
package com.garmentqc.system.service;

import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.InspectionRepository;

import lombok.RequiredArgsConstructor;

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
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SewingReportExcelService {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Jakarta");
    private static final String SEWING = "SEWING";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private final InspectionRepository inspectionRepository;
    private final InspectionReportService inspectionReportService;

    public byte[] exportDailySewingExcel(LocalDate date, User currentUser) throws IOException {
        String section = inspectionReportService.resolveSectionForUser(currentUser, SEWING);

        Instant start = date.atStartOfDay(ZONE_ID).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZONE_ID).toInstant();

        List<Inspection> inspections = inspectionRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)
                .stream()
                .filter(inspection -> inspection.getLine() != null)
                .filter(inspection -> inspection.getLine().getSection() != null)
                .filter(inspection -> SEWING.equals(inspection.getLine().getSection().name()))
                .filter(inspection -> section == null
                        || section.isBlank()
                        || section.equalsIgnoreCase(SEWING))
                .filter(inspection -> canViewInspection(inspection, currentUser))
                .sorted(
                        Comparator
                                .comparing(
                                        (Inspection i) -> i.getLine().getName(),
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER))
                                .thenComparing(
                                        Inspection::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder())))
                .collect(Collectors.toList());

        Map<String, LineData> lineDataMap = new LinkedHashMap<>();
        for (Inspection inspection : inspections) {
            Line line = inspection.getLine();
            String lineId = line.getId();
            LineData data = lineDataMap.computeIfAbsent(
                    lineId,
                    id -> new LineData(line.getName()));
            data.addInspection(inspection);
        }

        List<LineData> lines = new ArrayList<>(lineDataMap.values());

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Daily Sewing Report");
            ReportStyles styles = new ReportStyles(workbook);
            int lastColumn = lines.size() + 1;

            createReportHeader(sheet, styles, date, lastColumn);

            int rowIndex = 5;
            int infoStartRow = rowIndex;

            rowIndex = writeLineInfoRow(
                    sheet,
                    rowIndex++,
                    "LINE",
                    lines,
                    LineData::getLineName,
                    lastColumn,
                    styles);

            rowIndex = writeLineInfoRow(
                    sheet,
                    rowIndex++,
                    "BUYER",
                    lines,
                    LineData::getBuyers,
                    lastColumn,
                    styles);

            rowIndex = writeLineInfoRow(
                    sheet,
                    rowIndex++,
                    "STYLE",
                    lines,
                    LineData::getStyles,
                    lastColumn,
                    styles);

            sheet.getRow(infoStartRow)
                    .getCell(lastColumn)
                    .setCellValue("TOTAL");

            sheet.addMergedRegion(
                    new org.apache.poi.ss.util.CellRangeAddress(
                            infoStartRow,
                            rowIndex - 1,
                            lastColumn,
                            lastColumn));

            rowIndex = writeDefectDetails(
                    sheet,
                    rowIndex,
                    lines,
                    lastColumn,
                    styles);

            rowIndex = writeSummaryRow(
                    sheet,
                    rowIndex,
                    "QTY DEFECT",
                    lines,
                    LineData::getTotalDefect,
                    lastColumn,
                    styles,
                    styles.summaryStyle,
                    styles.numberStyle);

            rowIndex = writeSummaryRow(
                    sheet,
                    rowIndex,
                    "QTY INSPECT",
                    lines,
                    LineData::getInspectQuantity,
                    lastColumn,
                    styles,
                    styles.summaryStyle,
                    styles.numberStyle);

            rowIndex = writePercentageRow(
                    sheet,
                    rowIndex,
                    lines,
                    lastColumn,
                    styles);

            rowIndex = writeSummaryRow(
                    sheet,
                    rowIndex,
                    "GOOD QC/INSP-DEF",
                    lines,
                    LineData::getGoodQuantity,
                    lastColumn,
                    styles,
                    styles.summaryStyle,
                    styles.numberStyle);

            writeSummaryRow(
                    sheet,
                    rowIndex,
                    "QTY TRANSFER",
                    lines,
                    LineData::getTransferQuantity,
                    lastColumn,
                    styles,
                    styles.summaryStyle,
                    styles.numberStyle);

            configurePrint(sheet, lastColumn);
            configureColumnWidths(sheet, lines.size());
            configureRowHeights(sheet);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private boolean canViewInspection(Inspection inspection, User currentUser) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }

        String role = currentUser.getRole().name();

        if ("SUPER_ADMIN".equals(role) || "ADMIN".equals(role)) {
            return true;
        }

        if ("USER".equals(role)) {
            if (inspection.getSubmittedBy() == null) {
                return false;
            }

            return currentUser.getId().equals(
                    inspection.getSubmittedBy().getId());
        }

        return false;
    }

    private void createReportHeader(Sheet sheet, ReportStyles styles, LocalDate date, int lastColumn) {
        createMergedRow(
                sheet, 0, 0, lastColumn,
                "PT UNGARAN SARI GARMENT",
                styles.titleStyle);

        createMergedRow(
                sheet, 1, 0, lastColumn,
                "DAILY REPORT SEWING SECTION",
                styles.subtitleStyle);

        createMergedRow(
                sheet, 2, 0, lastColumn,
                "QC DEPARTMENT",
                styles.subtitleStyle);

        createMergedRow(
                sheet, 3, 0, lastColumn,
                "DEPT: DRESS 2",
                styles.subtitleStyle);

        createMergedRow(
                sheet, 4, 0, lastColumn,
                "REPORT DATE: " + date.format(DATE_FORMAT),
                styles.periodStyle);
    }

    private void createMergedRow(Sheet sheet, int rowIndex, int firstColumn, int lastColumn, String value,
            CellStyle style) {
        Row row = sheet.createRow(rowIndex);

        for (int column = firstColumn; column <= lastColumn; column++) {

            Cell cell = row.createCell(column);
            cell.setCellStyle(style);
        }

        row.getCell(firstColumn).setCellValue(value);

        if (firstColumn < lastColumn) {
            sheet.addMergedRegion(
                    new org.apache.poi.ss.util.CellRangeAddress(
                            rowIndex,
                            rowIndex,
                            firstColumn,
                            lastColumn));
        }
    }

    private int writeLineInfoRow(Sheet sheet, int rowIndex, String label, List<LineData> lines,
            java.util.function.Function<LineData, String> valueFunction, int lastColumn, ReportStyles styles) {
        Row row = sheet.createRow(rowIndex);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.headerLabelStyle);

        int column = 1;

        for (LineData line : lines) {
            Cell cell = row.createCell(column++);
            cell.setCellValue(
                    nullToEmpty(valueFunction.apply(line)));
            cell.setCellStyle(styles.headerCenterStyle);
        }

        Cell totalCell = row.createCell(lastColumn);
        totalCell.setCellValue("");
        totalCell.setCellStyle(styles.headerCenterStyle);

        return rowIndex + 1;
    }

    private int writeDefectDetails(Sheet sheet, int rowIndex, List<LineData> lines, int lastColumn,
            ReportStyles styles) {
        createMergedRow(
                sheet,
                rowIndex,
                0,
                lastColumn,
                "DEFECT DETAILS",
                styles.headerStyle);
        rowIndex++;

        Map<String, Map<String, Map<String, Integer>>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (LineData line : lines) {
            for (Map.Entry<DefectKey, Integer> entry : line.getDefectQuantities().entrySet()) {
                DefectKey key = entry.getKey();

                grouped
                        .computeIfAbsent(
                                key.category(),
                                ignored -> new TreeMap<>(
                                        String.CASE_INSENSITIVE_ORDER))
                        .computeIfAbsent(
                                key.area(),
                                ignored -> new TreeMap<>(
                                        String.CASE_INSENSITIVE_ORDER))
                        .merge(
                                key.defectName(),
                                entry.getValue(),
                                Integer::sum);
            }
        }

        for (Map.Entry<String, Map<String, Map<String, Integer>>> categoryEntry : grouped.entrySet()) {
            String categoryName = categoryEntry.getKey();

            createMergedRow(
                    sheet,
                    rowIndex,
                    0,
                    lastColumn,
                    categoryName,
                    styles.categoryStyle);

            rowIndex++;

            for (Map.Entry<String, Map<String, Integer>> areaEntry : categoryEntry.getValue().entrySet()) {
                String areaName = areaEntry.getKey();
                if (!areaName.isBlank()) {
                    Row areaRow = sheet.createRow(rowIndex++);

                    Cell areaCell = areaRow.createCell(0);
                    areaCell.setCellValue("  " + areaName);
                    areaCell.setCellStyle(styles.areaStyle);

                    for (int column = 1; column <= lastColumn; column++) {

                        areaRow.createCell(column)
                                .setCellStyle(styles.areaStyle);
                    }
                }

                for (String defectName : areaEntry.getValue().keySet()) {
                    Row row = sheet.createRow(rowIndex);

                    Cell nameCell = row.createCell(0);
                    nameCell.setCellValue("      " + defectName);
                    nameCell.setCellStyle(styles.bodyStyle);

                    int totalQuantity = 0;

                    for (int column = 0; column < lines.size(); column++) {

                        LineData line = lines.get(column);

                        int quantity = line.getDefectQuantity(
                                categoryName,
                                areaName,
                                defectName);

                        totalQuantity += quantity;

                        Cell cell = row.createCell(column + 1);
                        if (quantity != 0) {
                            cell.setCellValue(quantity);
                        }
                        cell.setCellStyle(styles.defectNumberStyle);
                    }

                    Cell totalCell = row.createCell(lastColumn);

                    if (totalQuantity != 0) {
                        totalCell.setCellValue(totalQuantity);
                    }

                    totalCell.setCellStyle(styles.defectTotalStyle);
                    rowIndex++;
                }
            }
        }

        return rowIndex;
    }

    private int writeSummaryRow(Sheet sheet, int rowIndex, String label, List<LineData> lines,
            java.util.function.ToIntFunction<LineData> valueFunction, int lastColumn, ReportStyles styles,
            CellStyle rowStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(rowStyle);

        int grandTotal = 0;
        int column = 1;

        for (LineData line : lines) {
            int value = valueFunction.applyAsInt(line);
            grandTotal += value;
            Cell cell = row.createCell(column++);

            if (value != 0) {
                cell.setCellValue(value);
            }

            cell.setCellStyle(valueStyle);
        }

        Cell totalCell = row.createCell(lastColumn);
        if (grandTotal != 0) {
            totalCell.setCellValue(grandTotal);
        }

        totalCell.setCellStyle(styles.grandTotalStyle);
        return rowIndex + 1;
    }

    private int writePercentageRow(Sheet sheet, int rowIndex, List<LineData> lines, int lastColumn,
            ReportStyles styles) {
        Row row = sheet.createRow(rowIndex);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue("% DEFECT");
        labelCell.setCellStyle(styles.summaryStyle);

        int totalInspect = 0;
        int totalDefect = 0;
        int column = 1;

        for (LineData line : lines) {
            int inspect = line.getInspectQuantity();
            int defect = line.getTotalDefect();

            double percentage = inspect == 0
                    ? 0.0
                    : (double) defect / inspect;

            totalInspect += inspect;
            totalDefect += defect;

            Cell cell = row.createCell(column++);

            if (percentage != 0.0) {
                cell.setCellValue(percentage);
            }

            cell.setCellStyle(styles.percentageStyle);
        }

        double grandPercentage = totalInspect == 0
                ? 0.0
                : (double) totalDefect / totalInspect;

        Cell totalCell = row.createCell(lastColumn);

        if (grandPercentage != 0.0) {
            totalCell.setCellValue(grandPercentage);
        }

        totalCell.setCellStyle(styles.grandPercentageStyle);

        return rowIndex + 1;
    }

    private void configureColumnWidths(Sheet sheet, int lineCount) {
        sheet.setColumnWidth(0, 30 * 256);

        for (int column = 1; column <= lineCount; column++) {

            sheet.setColumnWidth(column, 14 * 256);
        }

        sheet.setColumnWidth(lineCount + 1, 14 * 256);
    }

    private void configureRowHeights(Sheet sheet) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            if (i == 0) {
                row.setHeightInPoints(24);
            } else if (i >= 1 && i <= 4) {
                row.setHeightInPoints(16);
            } else if (i >= 5 && i <= 7) {
                row.setHeightInPoints(24);
            } else {
                row.setHeightInPoints(16);
            }
        }
    }

    @SuppressWarnings("unused")
    private static void applyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    @SuppressWarnings("deprecation")
    private void configurePrint(Sheet sheet, int lastColumn) {
        PrintSetup printSetup = sheet.getPrintSetup();

        printSetup.setPaperSize((short) 9);
        printSetup.setLandscape(true);

        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 0);

        sheet.setFitToPage(true);
        sheet.setAutobreaks(true);
        sheet.setPrintGridlines(false);
        sheet.setDisplayGridlines(true);

        sheet.setMargin(Sheet.LeftMargin, 0.5);
        sheet.setMargin(Sheet.RightMargin, 0.5);
        sheet.setMargin(Sheet.TopMargin, 0.9);
        sheet.setMargin(Sheet.BottomMargin, 0.7);
        sheet.setMargin(Sheet.HeaderMargin, 0.3);
        sheet.setMargin(Sheet.FooterMargin, 0.3);

        sheet.setHorizontallyCenter(true);
        sheet.setVerticallyCenter(false);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class LineData {
        private final String lineName;
        private int inspectQuantity;
        private int transferQuantity;
        private int totalDefect;

        private final Set<String> buyers = new LinkedHashSet<>();
        private final Set<String> styles = new LinkedHashSet<>();

        private final Map<DefectKey, Integer> defectQuantities = new LinkedHashMap<>();

        private LineData(String lineName) {
            this.lineName = lineName;
        }

        private void addInspection(Inspection inspection) {
            if (inspection.getInspectQuantity() != null) {
                inspectQuantity += inspection.getInspectQuantity();
            }

            if (inspection.getTransferQuantity() != null) {
                transferQuantity += inspection.getTransferQuantity();
            }

            if (inspection.getBuyerName() != null
                    && !inspection.getBuyerName().isBlank()) {

                buyers.add(inspection.getBuyerName());
            }

            if (inspection.getStyleCode() != null
                    && !inspection.getStyleCode().isBlank()) {

                styles.add(inspection.getStyleCode());
            }

            if (inspection.getInspectionDefects() == null) {
                return;
            }

            for (InspectionDefect inspectionDefect : inspection.getInspectionDefects()) {

                if (inspectionDefect.getDefectType() == null) {
                    continue;
                }

                DefectType defectType = inspectionDefect.getDefectType();

                int quantity = inspectionDefect.getQuantity() == null
                        ? 0
                        : inspectionDefect.getQuantity();

                if (quantity <= 0) {
                    continue;
                }

                String category = defectType.getCategory() == null
                        ? "OTHER DEFECT"
                        : defectType.getCategory().name();

                String area = "";

                if (defectType.getDefectArea() != null
                        && defectType.getDefectArea().getName() != null) {

                    area = defectType.getDefectArea().getName();
                }

                String defectName = defectType.getName();

                DefectKey key = new DefectKey(
                        formatCategoryStatic(category),
                        area,
                        defectName);

                defectQuantities.merge(
                        key,
                        quantity,
                        Integer::sum);

                totalDefect += quantity;
            }
        }

        private String getLineName() {
            return lineName;
        }

        private String getBuyers() {
            return String.join(", ", buyers);
        }

        private String getStyles() {
            return String.join(", ", styles);
        }

        private int getInspectQuantity() {
            return inspectQuantity;
        }

        private int getTransferQuantity() {
            return transferQuantity;
        }

        private int getTotalDefect() {
            return totalDefect;
        }

        private int getGoodQuantity() {
            return Math.max(0, inspectQuantity - totalDefect);
        }

        private Map<DefectKey, Integer> getDefectQuantities() {
            return defectQuantities;
        }

        private int getDefectQuantity(String category, String area, String defectName) {
            return defectQuantities.getOrDefault(
                    new DefectKey(category, area, defectName),
                    0);
        }

        private static String formatCategoryStatic(String category) {
            if (category == null || category.isBlank()) {
                return "OTHER DEFECT";
            }

            return category
                    .replace("DEFECT_", "")
                    .replace('_', ' ');
        }
    }

    private record DefectKey(String category, String area, String defectName) {

    }

    private static class ReportStyles {

        private final CellStyle titleStyle;
        private final CellStyle subtitleStyle;
        private final CellStyle periodStyle;
        private final CellStyle headerStyle;
        private final CellStyle headerLabelStyle;
        private final CellStyle headerCenterStyle;
        private final CellStyle categoryStyle;
        private final CellStyle areaStyle;
        private final CellStyle bodyStyle;
        private final CellStyle numberStyle;
        private final CellStyle defectNumberStyle;
        private final CellStyle defectTotalStyle;
        private final CellStyle summaryStyle;
        private final CellStyle grandTotalStyle;
        private final CellStyle percentageStyle;
        private final CellStyle grandPercentageStyle;

        private static final Color GREEN_HEADER = new Color(96, 125, 107);
        private static final Color GREEN_LIGHT = new Color(238, 243, 238);
        private static final Color BLUE_HEADER = new Color(220, 230, 239);
        private static final Color BEIGE = new Color(245, 240, 230);
        private static final Color GREEN_TOTAL = new Color(220, 232, 223);
        private static final Color YELLOW_LIGHT = new Color(255, 249, 196);

        private ReportStyles(Workbook workbook) {
            titleStyle = createStyle(
                    workbook,
                    GREEN_HEADER,
                    IndexedColors.WHITE.getIndex(),
                    (short) 14,
                    true,
                    HorizontalAlignment.CENTER);

            subtitleStyle = createStyle(
                    workbook,
                    GREEN_HEADER,
                    IndexedColors.WHITE.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.CENTER);

            periodStyle = createStyle(
                    workbook,
                    GREEN_LIGHT,
                    IndexedColors.DARK_BLUE.getIndex(),
                    (short) 8,
                    false,
                    HorizontalAlignment.LEFT);

            headerStyle = createStyle(
                    workbook,
                    BLUE_HEADER,
                    IndexedColors.DARK_BLUE.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.CENTER);

            headerLabelStyle = createStyle(
                    workbook,
                    BLUE_HEADER,
                    IndexedColors.DARK_BLUE.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.LEFT);

            headerCenterStyle = createStyle(
                    workbook,
                    BLUE_HEADER,
                    IndexedColors.DARK_BLUE.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.CENTER);

            headerCenterStyle.setWrapText(true);

            categoryStyle = createStyle(
                    workbook,
                    BLUE_HEADER,
                    IndexedColors.DARK_BLUE.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.LEFT);

            areaStyle = createStyle(
                    workbook,
                    YELLOW_LIGHT,
                    IndexedColors.BLACK.getIndex(),
                    (short) 9,
                    false,
                    HorizontalAlignment.LEFT);

            bodyStyle = createStyle(
                    workbook,
                    Color.WHITE,
                    IndexedColors.BLACK.getIndex(),
                    (short) 9,
                    false,
                    HorizontalAlignment.LEFT);

            numberStyle = createStyle(
                    workbook,
                    BEIGE,
                    IndexedColors.DARK_BLUE.getIndex(),
                    (short) 9,
                    false,
                    HorizontalAlignment.CENTER);

            defectNumberStyle = createStyle(
                    workbook,
                    Color.WHITE,
                    IndexedColors.BLACK.getIndex(),
                    (short) 9,
                    false,
                    HorizontalAlignment.CENTER);

            defectTotalStyle = createStyle(
                    workbook,
                    GREEN_TOTAL,
                    IndexedColors.DARK_GREEN.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.CENTER);

            summaryStyle = createStyle(
                    workbook,
                    BEIGE,
                    IndexedColors.DARK_BLUE.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.LEFT);

            grandTotalStyle = createStyle(
                    workbook,
                    GREEN_TOTAL,
                    IndexedColors.DARK_GREEN.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.CENTER);

            percentageStyle = createStyle(
                    workbook,
                    BEIGE,
                    IndexedColors.DARK_BLUE.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.CENTER);

            grandPercentageStyle = createStyle(
                    workbook,
                    GREEN_TOTAL,
                    IndexedColors.DARK_GREEN.getIndex(),
                    (short) 9,
                    true,
                    HorizontalAlignment.CENTER);

            percentageStyle.setDataFormat(
                    workbook.createDataFormat().getFormat("0.00%"));

            grandPercentageStyle.setDataFormat(
                    workbook.createDataFormat().getFormat("0.00%"));
        }

        private CellStyle createStyle(Workbook workbook, Color background, short fontColor, short fontSize,
                boolean bold, HorizontalAlignment alignment) {
            XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();

            style.setFillForegroundColor(new XSSFColor(background, new DefaultIndexedColorMap()));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(alignment);
            style.setVerticalAlignment(VerticalAlignment.CENTER);

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
            font.setFontHeightInPoints(fontSize);
            font.setBold(bold);
            font.setColor(fontColor);

            style.setFont(font);
            return style;
        }
    }
}
