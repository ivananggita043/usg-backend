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
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WashingReportExcelService {

        private static final ZoneId ZONE_ID = ZoneId.of("Asia/Jakarta");
        private static final String WASHING = "WASHING";
        private static final String BULK = "BULK";
        private static final String REPAIR = "REPAIR";

        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy",
                        Locale.ENGLISH);
        private static final int LABEL_COLUMN = 0;

        private final InspectionRepository inspectionRepository;
        private final InspectionReportService inspectionReportService;

        public byte[] exportDailyWashingExcel(LocalDate date, User currentUser) throws IOException {
                if (date == null) {
                        throw new com.garmentqc.system.exception.BadRequestException("Tanggal laporan wajib diisi");
                }

                String section = inspectionReportService.resolveSectionForUser(currentUser, WASHING);
                Instant start = date.atStartOfDay(ZONE_ID).toInstant();
                Instant end = date.plusDays(1).atStartOfDay(ZONE_ID).toInstant();
                List<Inspection> inspections = inspectionRepository
                                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)
                                .stream()
                                .filter(i -> i.getLine() != null && i.getLine().getSection() != null)
                                .filter(i -> WASHING.equals(i.getLine().getSection().name()))
                                .filter(i -> section == null || section.isBlank() || section.equalsIgnoreCase(WASHING))
                                .filter(i -> canViewInspection(i, currentUser))
                                .sorted(Comparator.comparing(
                                                Inspection::getCreatedAt,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                .collect(Collectors.toList());

                Map<ProductionKey, InspectionData> bulkGrouped = groupProduction(inspections, BULK);
                Map<ProductionKey, InspectionData> repairGrouped = groupProduction(inspections, REPAIR);
                List<InspectionData> bulkColumns = buildColumns(bulkGrouped);
                List<InspectionData> repairColumns = buildColumns(repairGrouped);

                Layout layout = new Layout(bulkColumns.size(), repairColumns.size());
                try (
                                Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                        Sheet sheet = workbook.createSheet("Daily Washing Report");
                        Styles styles = new Styles(workbook);
                        createMergedRow(
                                        sheet, 0, 0, layout.lastColumn(),
                                        "PT UNGARAN SARI GARMENT", styles.title);
                        createMergedRow(
                                        sheet, 1, 0, layout.lastColumn(),
                                        "DAILY REPORT WASHING SECTION", styles.subtitle);
                        createMergedRow(
                                        sheet, 2, 0, layout.lastColumn(),
                                        "QC DEPARTEMENT", styles.subtitle);
                        createMergedRow(
                                        sheet, 3, 0, layout.lastColumn(),
                                        "DEPT: DRESS 2", styles.subtitle);
                        createMergedRow(
                                        sheet, 4, 0, layout.lastColumn(),
                                        "REPORT DATE: " + date.format(DATE_FORMAT),
                                        styles.period);

                        int rowIndex = 5;
                        int infoStartRow = rowIndex;
                        rowIndex = writeInfoRow(
                                        sheet, rowIndex, "BUYER",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getBuyers, styles);

                        rowIndex = writeInfoRow(
                                        sheet, rowIndex, "STYLE",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getStyles, styles);

                        rowIndex = writeInfoRow(
                                        sheet, rowIndex, "COLOR",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getColors, styles);

                        rowIndex = writeInfoRow(
                                        sheet, rowIndex, "HANDFEEL",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getHandfeels, styles);

                        rowIndex = writeInfoRow(
                                        sheet, rowIndex, "GRADE",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getGrades, styles);

                        mergeTotalInfoColumns(
                                        sheet, infoStartRow, rowIndex - 1, layout, styles);

                        rowIndex = writeDefectSection(
                                        sheet, rowIndex,
                                        "DEFECT DETAILS - SEWING",
                                        bulkColumns, repairColumns, layout, styles,
                                        InspectionData::getSewingDefects);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "TOTAL DEF SEWING",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getSewingDefectTotal, styles);

                        rowIndex = writePercentageRow(
                                        sheet, rowIndex,
                                        "% DEFECT SEWING",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getSewingDefectTotal, styles);

                        rowIndex = writeDefectSection(
                                        sheet, rowIndex,
                                        "DEFECT DETAILS - WASHING",
                                        bulkColumns, repairColumns, layout, styles,
                                        InspectionData::getWashingDefects);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "TOTAL DEF WASHING",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getWashingDefectTotal, styles);

                        rowIndex = writePercentageRow(
                                        sheet, rowIndex,
                                        "% DEFECT WASHING",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getWashingDefectTotal, styles);

                        rowIndex = writeDefectSection(
                                        sheet, rowIndex,
                                        "DEFECT DETAILS - FINISHING",
                                        bulkColumns, repairColumns, layout, styles,
                                        InspectionData::getFinishingDefects);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "TOTAL DEF FINISHING",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getFinishingDefectTotal, styles);

                        rowIndex = writePercentageRow(
                                        sheet, rowIndex,
                                        "% DEFECT FINISHING",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getFinishingDefectTotal, styles);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "TOTAL DEFECT",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getTotalDefect, styles);

                        rowIndex = writePercentageRow(
                                        sheet, rowIndex,
                                        "% TOTAL DEFECT",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getTotalDefect, styles);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "QTY INSPECT",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getInspectQuantity, styles);

                        rowIndex = writePercentageRow(
                                        sheet, rowIndex,
                                        "% DEFECT",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getTotalDefect, styles);

                        rowIndex = writeSummaryRow(
                                        sheet, rowIndex,
                                        "GOOD QC/INSP-DEF",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getGoodQuantity, styles);

                        writeSummaryRow(
                                        sheet, rowIndex,
                                        "QTY TRANSFER",
                                        bulkColumns, repairColumns, layout,
                                        InspectionData::getTransferQuantity, styles);

                        sheet.setColumnWidth(LABEL_COLUMN, 30 * 256);
                        for (int c = layout.bulkStart(); c <= layout.lastColumn(); c++) {
                                sheet.setColumnWidth(c, 14 * 256);
                        }

                        sheet.createFreezePane(1, 5);

                        sheet.getPrintSetup().setLandscape(true);
                        sheet.getPrintSetup().setFitWidth((short) 1);
                        sheet.getPrintSetup().setFitHeight((short) 0);

                        sheet.setFitToPage(true);
                        sheet.setPrintGridlines(false);
                        sheet.setHorizontallyCenter(true);

                        sheet.setMargin(PageMargin.LEFT, 0.25);
                        sheet.setMargin(PageMargin.RIGHT, 0.25);
                        sheet.setMargin(PageMargin.TOP, 0.5);
                        sheet.setMargin(PageMargin.BOTTOM, 0.5);

                        workbook.write(output);
                        return output.toByteArray();
                }
        }

        private Map<ProductionKey, InspectionData> groupProduction(List<Inspection> inspections, String washingType) {
                Map<ProductionKey, InspectionData> grouped = new LinkedHashMap<>();
                for (Inspection inspection : inspections) {
                        if (inspection.getWashingType() == null
                                        || !washingType.equals(inspection.getWashingType().name())) {
                                continue;
                        }

                        ProductionKey key = new ProductionKey(
                                        safe(inspection.getBuyerName()),
                                        safe(inspection.getStyleCode()),
                                        safe(inspection.getColorName()));

                        InspectionData data = grouped.computeIfAbsent(
                                        key, ignored -> new InspectionData());
                        data.add(InspectionData.from(inspection));
                }
                return grouped;
        }

        private List<InspectionData> buildColumns(Map<ProductionKey, InspectionData> grouped) {
                List<InspectionData> columns = new ArrayList<>(grouped.values());
                if (columns.isEmpty()) {
                        columns.add(new InspectionData());
                }
                return columns;
        }

        private boolean canViewInspection(Inspection inspection, User currentUser) {
                if (currentUser == null || currentUser.getRole() == null) {
                        return false;
                }

                String role = currentUser.getRole().name();
                if ("SUPER_ADMIN".equals(role) || "ADMIN".equals(role)) {
                        return true;
                }

                return "USER".equals(role)
                                && inspection.getSubmittedBy() != null
                                && currentUser.getId() != null
                                && currentUser.getId().equals(
                                                inspection.getSubmittedBy().getId());
        }

        private void mergeTotalInfoColumns(Sheet sheet, int firstRow, int lastRow, Layout layout, Styles styles) {
                mergeTotalColumn(
                                sheet, firstRow, lastRow,
                                layout.bulkTotal(), "TOTAL BULK", styles);

                mergeTotalColumn(
                                sheet, firstRow, lastRow,
                                layout.bulkPct(), "% BULK", styles);

                mergeTotalColumn(
                                sheet, firstRow, lastRow,
                                layout.repairTotal(), "TOTAL REPAIR", styles);

                mergeTotalColumn(
                                sheet, firstRow, lastRow,
                                layout.repairPct(), "% REPAIR", styles);

                mergeTotalColumn(
                                sheet, firstRow, lastRow,
                                layout.grandTotal(), "TOTAL", styles);
        }

        private void mergeTotalColumn(Sheet sheet, int firstRow, int lastRow, int column, String label, Styles styles) {
                for (int r = firstRow; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) {
                                row = sheet.createRow(r);
                        }

                        Cell cell = row.getCell(column);
                        if (cell == null) {
                                cell = row.createCell(column);
                        }

                        cell.setCellStyle(styles.header);
                        cell.setCellValue("");
                }

                if (firstRow < lastRow) {
                        sheet.addMergedRegion(new CellRangeAddress(
                                        firstRow, lastRow, column, column));
                }

                sheet.getRow(firstRow)
                                .getCell(column)
                                .setCellValue(label);
        }

        private int writeInfoRow(Sheet sheet, int rowIndex, String label, List<InspectionData> bulkColumns,
                        List<InspectionData> repairColumns, Layout layout, Function<InspectionData, String> getter,
                        Styles styles) {
                Row row = sheet.createRow(rowIndex++);
                setCell(row, LABEL_COLUMN, label, styles.headerLabel);
                for (int i = 0; i < bulkColumns.size(); i++) {
                        setCell(
                                        row, layout.bulkStart() + i,
                                        getter.apply(bulkColumns.get(i)),
                                        styles.header);
                }

                for (int i = 0; i < repairColumns.size(); i++) {
                        setCell(
                                        row, layout.repairStart() + i,
                                        getter.apply(repairColumns.get(i)),
                                        styles.header);
                }

                setCell(row, layout.bulkTotal(), "", styles.header);
                setCell(row, layout.bulkPct(), "", styles.header);
                setCell(row, layout.repairTotal(), "", styles.header);
                setCell(row, layout.repairPct(), "", styles.header);
                setCell(row, layout.grandTotal(), "", styles.header);
                return rowIndex;
        }

        private int writeDefectSection(Sheet sheet, int rowIndex, String title, List<InspectionData> bulkColumns,
                        List<InspectionData> repairColumns, Layout layout, Styles styles,
                        Function<InspectionData, Map<DefectKey, Integer>> defectGetter) {
                createMergedRow(
                                sheet, rowIndex++, LABEL_COLUMN, layout.lastColumn(),
                                title, styles.section);

                Comparator<DefectKey> comparator = Comparator
                                .comparing(
                                                DefectKey::area,
                                                String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                                DefectKey::name,
                                                String.CASE_INSENSITIVE_ORDER);

                Set<DefectKey> keys = new TreeSet<>(comparator);

                for (InspectionData data : bulkColumns) {
                        keys.addAll(defectGetter.apply(data).keySet());
                }

                for (InspectionData data : repairColumns) {
                        keys.addAll(defectGetter.apply(data).keySet());
                }

                long bulkInspect = totalInspect(bulkColumns);
                long repairInspect = totalInspect(repairColumns);
                String currentArea = null;
                for (DefectKey key : keys) {
                        if (!key.area().isBlank()
                                        && !key.area().equals(currentArea)) {

                                currentArea = key.area();

                                Row areaRow = sheet.createRow(rowIndex++);

                                setCell(
                                                areaRow, LABEL_COLUMN,
                                                currentArea, styles.area);

                                for (int c = layout.bulkStart(); c <= layout.lastColumn(); c++) {
                                        setCell(areaRow, c, "", styles.area);
                                }
                        }

                        Row row = sheet.createRow(rowIndex++);
                        setCell(
                                        row, LABEL_COLUMN,
                                        "    " + key.name(), styles.body);

                        int bulkTotal = 0;
                        int repairTotal = 0;

                        for (int i = 0; i < bulkColumns.size(); i++) {
                                int quantity = defectGetter.apply(
                                                bulkColumns.get(i)).getOrDefault(key, 0);

                                bulkTotal += quantity;
                                setCell(
                                                row, layout.bulkStart() + i,
                                                quantity, styles.number);
                        }

                        for (int i = 0; i < repairColumns.size(); i++) {
                                int quantity = defectGetter.apply(
                                                repairColumns.get(i)).getOrDefault(key, 0);
                                repairTotal += quantity;
                                setCell(
                                                row, layout.repairStart() + i,
                                                quantity, styles.number);
                        }
                        setCell(row, layout.bulkTotal(), bulkTotal, styles.total);
                        setCell(
                                        row, layout.bulkPct(),
                                        percentage(bulkTotal, bulkInspect),
                                        styles.percentTotal);
                        setCell(row, layout.repairTotal(), repairTotal, styles.total);
                        setCell(
                                        row, layout.repairPct(),
                                        percentage(repairTotal, repairInspect),
                                        styles.percentTotal);
                        setCell(
                                        row, layout.grandTotal(),
                                        bulkTotal + repairTotal, styles.total);
                }
                return rowIndex;
        }

        private int writeSummaryRow(Sheet sheet, int rowIndex, String label, List<InspectionData> bulkColumns,
                        List<InspectionData> repairColumns, Layout layout, ToIntFunction<InspectionData> getter,
                        Styles styles) {
                Row row = sheet.createRow(rowIndex++);

                setCell(row, LABEL_COLUMN, label, styles.summaryLabel);

                int bulkGrandTotal = 0;
                int repairGrandTotal = 0;
                long bulkInspect = totalInspect(bulkColumns);
                long repairInspect = totalInspect(repairColumns);

                for (int i = 0; i < bulkColumns.size(); i++) {
                        int value = getter.applyAsInt(bulkColumns.get(i));
                        bulkGrandTotal += value;
                        setCell(
                                        row, layout.bulkStart() + i,
                                        value, styles.summaryValue);
                }

                for (int i = 0; i < repairColumns.size(); i++) {
                        int value = getter.applyAsInt(repairColumns.get(i));
                        repairGrandTotal += value;
                        setCell(
                                        row, layout.repairStart() + i,
                                        value, styles.summaryValue);
                }

                setCell(
                                row, layout.bulkTotal(),
                                bulkGrandTotal, styles.total);

                setCell(
                                row, layout.bulkPct(),
                                percentage(bulkGrandTotal, bulkInspect),
                                styles.percentTotal);

                setCell(
                                row, layout.repairTotal(),
                                repairGrandTotal, styles.total);

                setCell(
                                row, layout.repairPct(),
                                percentage(repairGrandTotal, repairInspect),
                                styles.percentTotal);

                setCell(
                                row, layout.grandTotal(),
                                bulkGrandTotal + repairGrandTotal, styles.total);

                return rowIndex;
        }

        private int writePercentageRow(Sheet sheet, int rowIndex, String label, List<InspectionData> bulkColumns,
                        List<InspectionData> repairColumns, Layout layout, ToIntFunction<InspectionData> defectGetter,
                        Styles styles) {
                Row row = sheet.createRow(rowIndex++);
                setCell(row, LABEL_COLUMN, label, styles.summaryLabel);

                long bulkInspect = 0;
                long bulkDefect = 0;
                long repairInspect = 0;
                long repairDefect = 0;

                for (int i = 0; i < bulkColumns.size(); i++) {
                        InspectionData data = bulkColumns.get(i);
                        int inspect = data.getInspectQuantity();
                        int defect = defectGetter.applyAsInt(data);
                        bulkInspect += inspect;
                        bulkDefect += defect;

                        setCell(
                                        row, layout.bulkStart() + i,
                                        percentage(defect, inspect), styles.percent);
                }

                for (int i = 0; i < repairColumns.size(); i++) {
                        InspectionData data = repairColumns.get(i);
                        int inspect = data.getInspectQuantity();
                        int defect = defectGetter.applyAsInt(data);

                        repairInspect += inspect;
                        repairDefect += defect;

                        setCell(
                                        row, layout.repairStart() + i,
                                        percentage(defect, inspect), styles.percent);
                }
                setCell(
                                row, layout.bulkTotal(),
                                percentage(bulkDefect, bulkInspect),
                                styles.percentTotal);
                setCell(row, layout.bulkPct(), "", styles.total);
                setCell(
                                row, layout.repairTotal(),
                                percentage(repairDefect, repairInspect),
                                styles.percentTotal);
                setCell(row, layout.repairPct(), "", styles.total);
                setCell(
                                row, layout.grandTotal(),
                                percentage(
                                                bulkDefect + repairDefect,
                                                bulkInspect + repairInspect),
                                styles.percentTotal);
                return rowIndex;
        }

        private double percentage(long defect, long inspect) {
                return inspect <= 0 ? 0.0 : (double) defect / inspect;
        }

        private long totalInspect(List<InspectionData> columns) {
                long total = 0;

                for (InspectionData data : columns) {
                        total += data.getInspectQuantity();
                }
                return total;
        }

        private void createMergedRow(Sheet sheet, int rowIndex, int firstCol, int lastCol, String value,
                        CellStyle style) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                        row = sheet.createRow(rowIndex);
                }

                Cell cell = row.getCell(firstCol);
                if (cell == null) {
                        cell = row.createCell(firstCol);
                }

                cell.setCellValue(value);
                for (int col = firstCol; col <= lastCol; col++) {
                        Cell currentCell = row.getCell(col);

                        if (currentCell == null) {
                                currentCell = row.createCell(col);
                        }
                        currentCell.setCellStyle(style);
                }

                CellRangeAddress targetRegion = new CellRangeAddress(rowIndex, rowIndex, firstCol, lastCol);

                boolean alreadyMerged = false;

                for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
                        CellRangeAddress existingRegion = sheet.getMergedRegion(i);

                        if (existingRegion.formatAsString()
                                        .equals(targetRegion.formatAsString())) {
                                alreadyMerged = true;
                                break;
                        }
                }

                if (!alreadyMerged && firstCol < lastCol) {
                        sheet.addMergedRegion(targetRegion);
                }
        }

        private void setCell(Row row, int column, String value, CellStyle style) {
                Cell cell = row.createCell(column);
                cell.setCellValue(value == null ? "" : value);
                cell.setCellStyle(style);
        }

        private void setCell(Row row, int column, int value, CellStyle style) {
                Cell cell = row.createCell(column);
                if (value != 0) {
                        cell.setCellValue(value);
                }
                cell.setCellStyle(style);
        }

        private void setCell(Row row, int column, double value, CellStyle style) {
                Cell cell = row.createCell(column);
                if (value != 0.0) {
                        cell.setCellValue(value);
                }
                cell.setCellStyle(style);
        }

        private static String safe(String value) {
                return value == null ? "" : value.trim();
        }

        private record Layout(int bulkCount, int repairCount) {
                int bulkStart() {
                        return 1;
                }

                int bulkTotal() {
                        return bulkStart() + bulkCount;
                }

                int bulkPct() {
                        return bulkTotal() + 1;
                }

                int repairStart() {
                        return bulkPct() + 1;
                }

                int repairTotal() {
                        return repairStart() + repairCount;
                }

                int repairPct() {
                        return repairTotal() + 1;
                }

                int grandTotal() {
                        return repairPct() + 1;
                }

                int lastColumn() {
                        return grandTotal();
                }
        }

        private record ProductionKey(String buyer, String style, String color) {

        }

        private record DefectKey(String area, String name) {

        }

        private static class InspectionData {
                private int inspectQuantity;
                private int transferQuantity;
                private int sewingDefectTotal;
                private int washingDefectTotal;
                private int finishingDefectTotal;

                private final Set<String> buyers = new LinkedHashSet<>();
                private final Set<String> styles = new LinkedHashSet<>();
                private final Set<String> colors = new LinkedHashSet<>();
                private final Set<String> processes = new LinkedHashSet<>();
                private final Set<String> handfeels = new LinkedHashSet<>();
                private final Set<String> grades = new LinkedHashSet<>();

                private final Map<DefectKey, Integer> sewingDefects = new LinkedHashMap<>();
                private final Map<DefectKey, Integer> washingDefects = new LinkedHashMap<>();
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
                        addIfPresent(data.handfeels, inspection.getHandfeel());
                        addIfPresent(data.grades, inspection.getGrade());

                        if (inspection.getInspectionDefects() == null) {
                                return data;
                        }

                        for (InspectionDefect inspectionDefect : inspection.getInspectionDefects()) {

                                if (inspectionDefect == null) {
                                        continue;
                                }

                                DefectType type = inspectionDefect.getDefectType();

                                if (type == null || type.getCategory() == null) {
                                        continue;
                                }

                                int quantity = inspectionDefect.getQuantity() == null
                                                ? 0
                                                : inspectionDefect.getQuantity();

                                if (quantity <= 0) {
                                        continue;
                                }

                                String area = "";

                                if (type.getDefectArea() != null
                                                && type.getDefectArea().getName() != null) {
                                        area = type.getDefectArea().getName().trim();
                                }

                                DefectKey key = new DefectKey(
                                                area, safe(type.getName()));

                                DefectCategory category = type.getCategory();

                                if (isSewingCategory(category)) {
                                        data.sewingDefects.merge(
                                                        key, quantity, Integer::sum);

                                        data.sewingDefectTotal += quantity;

                                } else if (category == DefectCategory.DEFECT_WASHING) {
                                        data.washingDefects.merge(
                                                        key, quantity, Integer::sum);

                                        data.washingDefectTotal += quantity;

                                } else if (category == DefectCategory.DEFECT_FINISHING) {
                                        data.finishingDefects.merge(
                                                        key, quantity, Integer::sum);

                                        data.finishingDefectTotal += quantity;
                                }
                        }
                        return data;
                }

                private static boolean isSewingCategory(DefectCategory category) {
                        return category == DefectCategory.DEFECT_BACK_TO_LINE
                                        || category == DefectCategory.DEFECT_TRIMMING
                                        || category == DefectCategory.DEFECT_STAIN
                                        || category == DefectCategory.DEFECT_FABRIC
                                        || category == DefectCategory.DEFECT_OTHERS;
                }

                void add(InspectionData other) {
                        inspectQuantity += other.inspectQuantity;
                        transferQuantity += other.transferQuantity;

                        sewingDefectTotal += other.sewingDefectTotal;
                        washingDefectTotal += other.washingDefectTotal;
                        finishingDefectTotal += other.finishingDefectTotal;

                        buyers.addAll(other.buyers);
                        styles.addAll(other.styles);
                        colors.addAll(other.colors);
                        processes.addAll(other.processes);
                        handfeels.addAll(other.handfeels);
                        grades.addAll(other.grades);

                        mergeDefects(sewingDefects, other.sewingDefects);
                        mergeDefects(washingDefects, other.washingDefects);
                        mergeDefects(finishingDefects, other.finishingDefects);
                }

                private static void mergeDefects(Map<DefectKey, Integer> target, Map<DefectKey, Integer> source) {
                        source.forEach(
                                        (key, value) -> target.merge(
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

                String getHandfeels() {
                        return String.join(", ", handfeels);
                }

                String getGrades() {
                        return String.join(", ", grades);
                }

                Map<DefectKey, Integer> getSewingDefects() {
                        return sewingDefects;
                }

                Map<DefectKey, Integer> getWashingDefects() {
                        return washingDefects;
                }

                Map<DefectKey, Integer> getFinishingDefects() {
                        return finishingDefects;
                }

                int getSewingDefectTotal() {
                        return sewingDefectTotal;
                }

                int getWashingDefectTotal() {
                        return washingDefectTotal;
                }

                int getFinishingDefectTotal() {
                        return finishingDefectTotal;
                }

                int getTotalDefect() {
                        return sewingDefectTotal
                                        + washingDefectTotal
                                        + finishingDefectTotal;
                }

                int getInspectQuantity() {
                        return inspectQuantity;
                }

                int getTransferQuantity() {
                        return transferQuantity;
                }

                int getGoodQuantity() {
                        return Math.max(
                                        0, inspectQuantity - getTotalDefect());
                }
        }

        private static class Styles {
                private static final Color GREEN_HEADER = new Color(96, 125, 107);
                private static final Color GREEN_LIGHT = new Color(238, 243, 238);
                private static final Color BLUE_HEADER = new Color(220, 230, 239);
                private static final Color BEIGE = new Color(245, 240, 230);
                private static final Color GREEN_TOTAL = new Color(220, 232, 223);
                private static final Color YELLOW_LIGHT = new Color(255, 249, 196);

                final CellStyle title;
                final CellStyle subtitle;
                final CellStyle period;
                final CellStyle header;
                final CellStyle headerLabel;
                final CellStyle section;
                final CellStyle area;
                final CellStyle body;
                final CellStyle number;
                final CellStyle summaryLabel;
                final CellStyle summaryValue;
                final CellStyle total;
                final CellStyle percent;
                final CellStyle percentTotal;

                Styles(Workbook workbook) {
                        title = style(
                                        workbook, GREEN_HEADER,
                                        IndexedColors.WHITE.getIndex(),
                                        14, true, HorizontalAlignment.CENTER);

                        subtitle = style(
                                        workbook, GREEN_HEADER,
                                        IndexedColors.WHITE.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        period = style(
                                        workbook, GREEN_LIGHT,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        header = style(
                                        workbook, BLUE_HEADER,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        headerLabel = style(
                                        workbook, BLUE_HEADER,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.LEFT);

                        section = style(
                                        workbook, BLUE_HEADER,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.LEFT);

                        area = style(
                                        workbook, YELLOW_LIGHT,
                                        IndexedColors.BLACK.getIndex(),
                                        9, true, HorizontalAlignment.LEFT);

                        body = style(
                                        workbook, Color.WHITE,
                                        IndexedColors.BLACK.getIndex(),
                                        9, false, HorizontalAlignment.LEFT);

                        number = style(
                                        workbook, Color.WHITE,
                                        IndexedColors.BLACK.getIndex(),
                                        9, false, HorizontalAlignment.CENTER);

                        summaryLabel = style(
                                        workbook, BEIGE,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.LEFT);

                        summaryValue = style(
                                        workbook, BEIGE,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        total = style(
                                        workbook, GREEN_TOTAL,
                                        IndexedColors.DARK_GREEN.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        percent = style(
                                        workbook, BEIGE,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        percentTotal = style(
                                        workbook, GREEN_TOTAL,
                                        IndexedColors.DARK_GREEN.getIndex(),
                                        9, true, HorizontalAlignment.CENTER);

                        DataFormat format = workbook.createDataFormat();

                        percent.setDataFormat(format.getFormat("0.00%"));
                        percentTotal.setDataFormat(format.getFormat("0.00%"));
                }

                private CellStyle style(Workbook workbook, Color background, short fontColor, int fontSize,
                                boolean bold, HorizontalAlignment alignment) {
                        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
                        style.setFillForegroundColor(
                                        new XSSFColor(
                                                        background,
                                                        new DefaultIndexedColorMap()));

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
                        font.setFontHeightInPoints((short) fontSize);
                        font.setBold(bold);
                        font.setColor(fontColor);
                        style.setFont(font);
                        return style;
                }
        }
}