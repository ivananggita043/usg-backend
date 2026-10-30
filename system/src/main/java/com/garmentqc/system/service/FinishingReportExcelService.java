
package com.garmentqc.system.service;

import com.garmentqc.system.enums.DefectCategory;
import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.InspectionRepository;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinishingReportExcelService {

        private static final ZoneId ZONE_ID = ZoneId.of("Asia/Jakarta");
        private static final String FINISHING = "FINISHING";
        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy",
                        Locale.ENGLISH);

        private final InspectionRepository inspectionRepository;
        private final InspectionReportService inspectionReportService;

        public byte[] exportDailyFinishingExcel(LocalDate date, User currentUser)
                        throws IOException {

                String section = inspectionReportService.resolveSectionForUser(currentUser, FINISHING);

                Instant start = date.atStartOfDay(ZONE_ID).toInstant();
                Instant end = date.plusDays(1).atStartOfDay(ZONE_ID).toInstant();

                List<Inspection> inspections = inspectionRepository
                                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)
                                .stream()
                                .filter(i -> i.getLine() != null && i.getLine().getSection() != null)
                                .filter(i -> FINISHING.equals(i.getLine().getSection().name()))
                                .filter(i -> section == null || section.isBlank()
                                                || section.equalsIgnoreCase(FINISHING))
                                .filter(i -> canViewInspection(i, currentUser))
                                .sorted(Comparator.comparing(
                                                Inspection::getCreatedAt,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                .collect(Collectors.toList());

                Map<ProductionKey, InspectionData> groupedData = new LinkedHashMap<>();

                for (Inspection inspection : inspections) {
                        ProductionKey key = new ProductionKey(
                                        safe(inspection.getBuyerName()),
                                        safe(inspection.getStyleCode()),
                                        safe(inspection.getColorName()));

                        InspectionData data = groupedData.computeIfAbsent(
                                        key, ignored -> new InspectionData());

                        data.add(InspectionData.from(inspection));
                }

                List<InspectionData> allData = new ArrayList<>(groupedData.values());
                List<InspectionData> columns = new ArrayList<>();

                for (int i = 0; i < 5; i++) {
                        columns.add(i < allData.size()
                                        ? allData.get(i)
                                        : new InspectionData());
                }

                InspectionData dll = new InspectionData();

                for (int i = 5; i < allData.size(); i++) {
                        dll.add(allData.get(i));
                }

                columns.add(dll);

                try (Workbook workbook = new XSSFWorkbook();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                        Sheet sheet = workbook.createSheet("Daily Finishing Report");
                        Styles styles = new Styles(workbook);

                        final int totalColumn = 7;
                        final int percentColumn = 8;

                        createMergedRow(sheet, 0, 0, percentColumn,
                                        "PT UNGARAN SARI GARMENT", styles.title);

                        createMergedRow(sheet, 1, 0, percentColumn,
                                        "DAILY REPORT FINISHING SECTION", styles.subtitle);

                        createMergedRow(sheet, 2, 0, percentColumn,
                                        "QC DEPARTEMENT", styles.subtitle);

                        createMergedRow(sheet, 3, 0, percentColumn,
                                        "DEPT: DRESS 2", styles.subtitle);

                        createMergedRow(sheet, 4, 0, percentColumn,
                                        "REPORT DATE: " + date.format(DATE_FORMAT), styles.period);

                        int rowIndex = 5;
                        int infoStartRow = rowIndex;

                        rowIndex = writeInfoRow(
                                        sheet, rowIndex, "BUYER", columns,
                                        InspectionData::getBuyers,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writeInfoRow(
                                        sheet, rowIndex, "STYLE", columns,
                                        InspectionData::getStyles,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writeInfoRow(
                                        sheet, rowIndex, "COLOR", columns,
                                        InspectionData::getColors,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writeInfoRow(
                                        sheet, rowIndex, "PROCESS", columns,
                                        InspectionData::getProcesses,
                                        totalColumn, percentColumn, styles);

                        sheet.getRow(infoStartRow)
                                        .getCell(totalColumn)
                                        .setCellValue("TOTAL");

                        sheet.getRow(infoStartRow)
                                        .getCell(percentColumn)
                                        .setCellValue("% TOTAL DEFECT");

                        sheet.addMergedRegion(new CellRangeAddress(
                                        infoStartRow, rowIndex - 1,
                                        totalColumn, totalColumn));

                        sheet.addMergedRegion(new CellRangeAddress(
                                        infoStartRow, rowIndex - 1,
                                        percentColumn, percentColumn));

                        rowIndex = writeDefectSection(
                                        sheet, rowIndex,
                                        "DEFECT DETAILS - SEWING",
                                        columns, totalColumn, percentColumn,
                                        styles, false);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "TOTAL DEF SEWING",
                                        columns, InspectionData::getSewingDefectTotal,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writePercentageRow(
                                        sheet, rowIndex,
                                        "% DEFECT SEWING",
                                        columns, InspectionData::getSewingDefectTotal,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writeDefectSection(
                                        sheet, rowIndex,
                                        "DEFECT DETAILS - FINISHING",
                                        columns, totalColumn, percentColumn,
                                        styles, true);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "TOTAL DEF FINISHING",
                                        columns, InspectionData::getFinishingDefectTotal,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writePercentageRow(
                                        sheet, rowIndex,
                                        "% DEFECT FINISHING",
                                        columns, InspectionData::getFinishingDefectTotal,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "TOTAL DEFECT",
                                        columns, InspectionData::getTotalDefect,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writePercentageRow(
                                        sheet, rowIndex,
                                        "% TOTAL DEFECT",
                                        columns, InspectionData::getTotalDefect,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "QTY INSPECT",
                                        columns, InspectionData::getInspectQuantity,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writePercentageRow(
                                        sheet, rowIndex,
                                        "% DEFECT",
                                        columns, InspectionData::getTotalDefect,
                                        totalColumn, percentColumn, styles);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "GOOD QC/INSP-DEF",
                                        columns, InspectionData::getGoodQuantity,
                                        totalColumn, percentColumn, styles);

                        writeSummaryRow(
                                        sheet, rowIndex,
                                        "QTY TRANSFER",
                                        columns, InspectionData::getTransferQuantity,
                                        totalColumn, percentColumn, styles);

                        sheet.setColumnWidth(0, 36 * 256);

                        for (int c = 1; c <= 6; c++) {
                                sheet.setColumnWidth(c, 16 * 256);
                        }

                        sheet.setColumnWidth(totalColumn, 16 * 256);
                        sheet.setColumnWidth(percentColumn, 18 * 256);

                        sheet.createFreezePane(1, 5);
                        sheet.getPrintSetup().setLandscape(true);
                        sheet.getPrintSetup().setFitWidth((short) 1);
                        sheet.getPrintSetup().setFitHeight((short) 0);
                        sheet.setFitToPage(true);
                        sheet.setPrintGridlines(false);
                        sheet.setHorizontallyCenter(true);
                        sheet.setMargin(PageMargin.LEFT, 0.35);
                        sheet.setMargin(PageMargin.RIGHT, 0.35);
                        sheet.setMargin(PageMargin.TOP, 0.5);
                        sheet.setMargin(PageMargin.BOTTOM, 0.5);

                        workbook.write(output);
                        return output.toByteArray();
                }
        }

        private boolean canViewInspection(
                        Inspection inspection, User currentUser) {

                if (currentUser == null || currentUser.getRole() == null) {
                        return false;
                }

                String role = currentUser.getRole().name();

                if ("SUPER_ADMIN".equals(role) || "ADMIN".equals(role)) {
                        return true;
                }

                return "USER".equals(role)
                                && inspection.getSubmittedBy() != null
                                && currentUser.getId().equals(
                                                inspection.getSubmittedBy().getId());
        }

        private int writeInfoRow(
                        Sheet sheet,
                        int rowIndex,
                        String label,
                        List<InspectionData> columns,
                        java.util.function.Function<InspectionData, String> getter,
                        int totalColumn,
                        int percentColumn,
                        Styles styles) {

                Row row = sheet.createRow(rowIndex++);

                setCell(row, 0, label, styles.headerLabel);

                for (int c = 0; c < 6; c++) {
                        String value = c < columns.size()
                                        ? getter.apply(columns.get(c))
                                        : "";

                        setCell(row, c + 1, value, styles.header);
                }

                setCell(row, totalColumn, "", styles.header);
                setCell(row, percentColumn, "", styles.header);

                return rowIndex;
        }

        private int writeDefectSection(
                        Sheet sheet,
                        int rowIndex,
                        String title,
                        List<InspectionData> columns,
                        int totalColumn,
                        int percentColumn,
                        Styles styles,
                        boolean finishingOnly) {

                createMergedRow(
                                sheet, rowIndex++, 0, percentColumn,
                                title, styles.section);

                Comparator<DefectKey> comparator = Comparator
                                .comparing(
                                                DefectKey::area,
                                                String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                                DefectKey::name,
                                                String.CASE_INSENSITIVE_ORDER);

                Set<DefectKey> keys = new TreeSet<>(comparator);

                for (InspectionData data : columns) {
                        Map<DefectKey, Integer> defects = finishingOnly
                                        ? data.finishingDefects
                                        : data.sewingDefects;

                        keys.addAll(defects.keySet());
                }

                int totalInspect = columns.stream()
                                .mapToInt(InspectionData::getInspectQuantity)
                                .sum();

                String currentArea = null;

                for (DefectKey key : keys) {
                        if (!key.area().isBlank()
                                        && !key.area().equals(currentArea)) {

                                currentArea = key.area();
                                Row areaRow = sheet.createRow(rowIndex++);

                                setCell(areaRow, 0, currentArea, styles.areaSummary);

                                for (int c = 1; c <= percentColumn; c++) {
                                        setCell(areaRow, c, "", styles.area);
                                }
                        }

                        Row row = sheet.createRow(rowIndex++);
                        setCell(row, 0, "    " + key.name(), styles.body);

                        int rowTotal = 0;

                        for (int c = 0; c < 6; c++) {
                                int quantity = 0;

                                if (c < columns.size()) {
                                        InspectionData data = columns.get(c);

                                        Map<DefectKey, Integer> defects = finishingOnly
                                                        ? data.finishingDefects
                                                        : data.sewingDefects;

                                        quantity = defects.getOrDefault(key, 0);
                                }

                                rowTotal += quantity;
                                setCell(row, c + 1, quantity, styles.number);
                        }

                        setCell(row, totalColumn, rowTotal, styles.total);

                        double percentage = totalInspect <= 0
                                        ? 0.0
                                        : (double) rowTotal / totalInspect;

                        setCell(
                                        row, percentColumn,
                                        percentage, styles.percentTotal);
                }

                return rowIndex;
        }

        private int writeSummaryRow(
                        Sheet sheet,
                        int rowIndex,
                        String label,
                        List<InspectionData> columns,
                        ToIntFunction<InspectionData> getter,
                        int totalColumn,
                        int percentColumn,
                        Styles styles) {

                Row row = sheet.createRow(rowIndex++);
                setCell(row, 0, label, styles.summary);

                int grandTotal = 0;

                for (int c = 0; c < 6; c++) {
                        int value = c < columns.size()
                                        ? getter.applyAsInt(columns.get(c))
                                        : 0;

                        grandTotal += value;
                        setCell(row, c + 1, value, styles.numberSummary);
                }

                setCell(row, totalColumn, grandTotal, styles.total);

                // Kolom % TOTAL DEFECT di kanan selalu kosong pada baris summary.
                setCell(row, percentColumn, "", styles.total);

                return rowIndex;
        }

        private int writePercentageRow(
                        Sheet sheet,
                        int rowIndex,
                        String label,
                        List<InspectionData> columns,
                        ToIntFunction<InspectionData> defectGetter,
                        int totalColumn,
                        int percentColumn,
                        Styles styles) {

                Row row = sheet.createRow(rowIndex++);
                setCell(row, 0, label, styles.summary);

                int totalInspect = 0;
                int totalDefect = 0;

                for (int c = 0; c < 6; c++) {
                        InspectionData data = c < columns.size()
                                        ? columns.get(c)
                                        : new InspectionData();

                        int inspect = data.getInspectQuantity();
                        int defect = defectGetter.applyAsInt(data);

                        totalInspect += inspect;
                        totalDefect += defect;

                        double percentage = inspect <= 0
                                        ? 0.0
                                        : (double) defect / inspect;

                        setCell(
                                        row, c + 1,
                                        percentage, styles.percentSummary);
                }

                double grandPercentage = totalInspect <= 0
                                ? 0.0
                                : (double) totalDefect / totalInspect;

                setCell(
                                row, totalColumn,
                                grandPercentage, styles.percentTotal);

                // Kolom persentase tambahan tetap kosong pada baris persentase summary.
                setCell(row, percentColumn, "", styles.total);

                return rowIndex;
        }

        private void createMergedRow(
                        Sheet sheet,
                        int rowIndex,
                        int firstCol,
                        int lastCol,
                        String value,
                        CellStyle style) {

                Row row = sheet.createRow(rowIndex);

                for (int c = firstCol; c <= lastCol; c++) {
                        Cell cell = row.createCell(c);
                        cell.setCellStyle(style);
                }

                row.getCell(firstCol).setCellValue(value);

                if (firstCol < lastCol) {
                        sheet.addMergedRegion(new CellRangeAddress(
                                        rowIndex, rowIndex, firstCol, lastCol));
                }
        }

        private void setCell(
                        Row row, int column, String value, CellStyle style) {

                Cell cell = row.createCell(column);
                cell.setCellValue(value == null ? "" : value);
                cell.setCellStyle(style);
        }

        private void setCell(
                        Row row, int column, int value, CellStyle style) {

                Cell cell = row.createCell(column);

                if (value != 0) {
                        cell.setCellValue(value);
                }

                cell.setCellStyle(style);
        }

        private void setCell(
                        Row row, int column, double value, CellStyle style) {

                Cell cell = row.createCell(column);

                if (value != 0) {
                        cell.setCellValue(value);
                }

                cell.setCellStyle(style);
        }

        private static String safe(String value) {
                return value == null ? "" : value.trim();
        }

        private record ProductionKey(
                        String buyer, String style, String color) {
        }

        private record DefectKey(
                        String area, String name) {
        }

        private static class InspectionData {

                private int inspectQuantity;
                private int transferQuantity;
                private int sewingDefectTotal;
                private int finishingDefectTotal;

                private final Set<String> buyers = new LinkedHashSet<>();
                private final Set<String> styles = new LinkedHashSet<>();
                private final Set<String> colors = new LinkedHashSet<>();
                private final Set<String> processes = new LinkedHashSet<>();

                private final Map<DefectKey, Integer> sewingDefects = new LinkedHashMap<>();

                private final Map<DefectKey, Integer> finishingDefects = new LinkedHashMap<>();

                static InspectionData from(Inspection inspection) {
                        InspectionData data = new InspectionData();

                        if (inspection.getInspectQuantity() != null) {
                                data.inspectQuantity = inspection.getInspectQuantity();
                        }

                        if (inspection.getTransferQuantity() != null) {
                                data.transferQuantity = inspection.getTransferQuantity();
                        }

                        addIfPresent(data.buyers, inspection.getBuyerName());
                        addIfPresent(data.styles, inspection.getStyleCode());
                        addIfPresent(data.colors, inspection.getColorName());
                        addIfPresent(data.processes, inspection.getProcess());

                        if (inspection.getInspectionDefects() != null) {
                                for (InspectionDefect inspectionDefect : inspection.getInspectionDefects()) {

                                        DefectType type = inspectionDefect.getDefectType();

                                        if (type == null) {
                                                continue;
                                        }

                                        int quantity = inspectionDefect.getQuantity() == null
                                                        ? 0
                                                        : inspectionDefect.getQuantity();

                                        if (quantity <= 0) {
                                                continue;
                                        }

                                        DefectCategory category = type.getCategory();

                                        String area = type.getDefectArea() == null
                                                        || type.getDefectArea().getName() == null
                                                                        ? ""
                                                                        : type.getDefectArea().getName().trim();

                                        String name = safe(type.getName());
                                        DefectKey key = new DefectKey(area, name);

                                        if (category == DefectCategory.DEFECT_FINISHING) {
                                                data.finishingDefects.merge(
                                                                key, quantity, Integer::sum);

                                                data.finishingDefectTotal += quantity;
                                        } else {
                                                data.sewingDefects.merge(
                                                                key, quantity, Integer::sum);

                                                data.sewingDefectTotal += quantity;
                                        }
                                }
                        }

                        return data;
                }

                void add(InspectionData other) {
                        inspectQuantity += other.inspectQuantity;
                        transferQuantity += other.transferQuantity;
                        sewingDefectTotal += other.sewingDefectTotal;
                        finishingDefectTotal += other.finishingDefectTotal;

                        buyers.addAll(other.buyers);
                        styles.addAll(other.styles);
                        colors.addAll(other.colors);
                        processes.addAll(other.processes);

                        other.sewingDefects.forEach(
                                        (key, value) -> sewingDefects.merge(
                                                        key, value, Integer::sum));

                        other.finishingDefects.forEach(
                                        (key, value) -> finishingDefects.merge(
                                                        key, value, Integer::sum));
                }

                private static void addIfPresent(Set<String> set, String value) {
                        if (value != null && !value.isBlank()) {
                                set.add(value.trim());
                        }
                }

                String getBuyers() {
                        return String.join(", ", buyers);
                }

                String getStyles() {
                        return String.join(", ", styles);
                }

                String getColors() {
                        return String.join(", ", colors);
                }

                String getProcesses() {
                        return String.join(", ", processes);
                }

                int getSewingDefectTotal() {
                        return sewingDefectTotal;
                }

                int getFinishingDefectTotal() {
                        return finishingDefectTotal;
                }

                int getTotalDefect() {
                        return sewingDefectTotal + finishingDefectTotal;
                }

                int getInspectQuantity() {
                        return inspectQuantity;
                }

                int getTransferQuantity() {
                        return transferQuantity;
                }

                int getGoodQuantity() {
                        return Math.max(0, inspectQuantity - getTotalDefect());
                }
        }

        private static class Styles {

                private static final Color GREEN_LIGHT = new Color(238, 243, 238);
                private static final Color BLUE_HEADER = new Color(220, 230, 239);
                private static final Color BEIGE = new Color(245, 240, 230);
                private static final Color GREEN_TOTAL = new Color(220, 232, 223);
                private static final Color YELLOW_LIGHT = new Color(255, 249, 196);
                private static final Color TITLE_PASTEL = new Color(168, 200, 178);
                private static final Color SUBTITLE_PASTEL = new Color(198, 215, 202);

                final CellStyle title;
                final CellStyle subtitle;
                final CellStyle period;
                final CellStyle header;
                final CellStyle headerLabel;
                final CellStyle section;
                final CellStyle area;
                final CellStyle body;
                final CellStyle number;
                final CellStyle summary;
                final CellStyle total;
                final CellStyle percent;
                final CellStyle percentTotal;
                final CellStyle areaSummary;
                final CellStyle numberSummary;
                final CellStyle percentSummary;

                Styles(Workbook workbook) {
                        title = style(workbook, TITLE_PASTEL,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        14, true, HorizontalAlignment.CENTER);

                        subtitle = style(workbook, SUBTITLE_PASTEL,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        period = style(workbook, GREEN_LIGHT,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, false, HorizontalAlignment.LEFT);

                        header = style(workbook, BLUE_HEADER,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        headerLabel = style(workbook, BLUE_HEADER,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.LEFT);

                        section = style(workbook, BLUE_HEADER,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.LEFT);

                        area = style(workbook, YELLOW_LIGHT,
                                        IndexedColors.BLACK.getIndex(),
                                        9, true, HorizontalAlignment.LEFT);

                        body = style(workbook, Color.WHITE,
                                        IndexedColors.BLACK.getIndex(),
                                        9, false, HorizontalAlignment.LEFT);

                        number = style(workbook, Color.WHITE,
                                        IndexedColors.BLACK.getIndex(),
                                        9, false, HorizontalAlignment.CENTER);

                        summary = style(workbook, BEIGE,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.LEFT);

                        total = style(workbook, GREEN_TOTAL,
                                        IndexedColors.DARK_GREEN.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        percent = style(workbook, BEIGE,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        percentTotal = style(workbook, GREEN_TOTAL,
                                        IndexedColors.DARK_GREEN.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        areaSummary = style(workbook, BEIGE,
                                        IndexedColors.BLACK.getIndex(),
                                        9, true, HorizontalAlignment.LEFT);

                        numberSummary = style(workbook, BEIGE,
                                        IndexedColors.BLACK.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        percentSummary = style(workbook, BEIGE,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        DataFormat format = workbook.createDataFormat();
                        percent.setDataFormat(format.getFormat("0.00%"));
                        percentTotal.setDataFormat(format.getFormat("0.00%"));
                        percentSummary.setDataFormat(format.getFormat("0.00%"));
                }

                private CellStyle style(
                                Workbook workbook,
                                Color background,
                                short fontColor,
                                int fontSize,
                                boolean bold,
                                HorizontalAlignment alignment) {

                        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();

                        style.setFillForegroundColor(
                                        new XSSFColor(background, new DefaultIndexedColorMap()));

                        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        style.setAlignment(alignment);
                        style.setVerticalAlignment(VerticalAlignment.CENTER);
                        style.setWrapText(true);

                        style.setBorderTop(BorderStyle.THIN);
                        style.setBorderBottom(BorderStyle.THIN);
                        style.setBorderLeft(BorderStyle.THIN);
                        style.setBorderRight(BorderStyle.THIN);

                        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());

                        XSSFFont font = (XSSFFont) workbook.createFont();
                        font.setFontName("Calibri");
                        font.setFontHeightInPoints((short) fontSize);
                        font.setBold(bold);
                        font.setColor(fontColor);
                        style.setFont(font);

                        return style;
                }
        }
}
