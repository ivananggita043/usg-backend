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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PageMargin;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.enums.DefectCategory;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.InspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinishingPeriodReportExcelService {

        private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");
        private static final String FINISHING = "FINISHING";

        private static final String ALL = "ALL";
        private static final String BUYERWISE = "BUYERWISE";
        private static final String STYLEWISE = "STYLEWISE";

        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy",
                        Locale.ENGLISH);

        private final InspectionRepository inspectionRepository;
        private final InspectionReportService inspectionReportService;

        public byte[] exportWeeklyFinishingExcel(
                        LocalDate date,
                        String reportType,
                        String buyer,
                        String style,
                        User currentUser) throws IOException {

                if (date == null) {
                        throw new BadRequestException(
                                        "Tanggal laporan wajib diisi.");
                }

                LocalDate start = date.with(
                                TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));

                LocalDate end = start.plusDays(6);

                return exportPeriod(
                                start,
                                end,
                                reportType,
                                buyer,
                                style,
                                currentUser,
                                "WEEKLY");
        }

        public byte[] exportMonthlyFinishingExcel(
                        int month,
                        int year,
                        String reportType,
                        String buyer,
                        String style,
                        User currentUser) throws IOException {

                final YearMonth period;

                try {
                        period = YearMonth.of(year, month);
                } catch (java.time.DateTimeException ex) {
                        throw new BadRequestException(
                                        "Bulan atau tahun laporan tidak valid.");
                }

                return exportPeriod(
                                period.atDay(1),
                                period.atEndOfMonth(),
                                reportType,
                                buyer,
                                style,
                                currentUser,
                                "MONTHLY");
        }

        private byte[] exportPeriod(
                        LocalDate start,
                        LocalDate end,
                        String reportTypeValue,
                        String buyerValue,
                        String styleValue,
                        User currentUser,
                        String periodType) throws IOException {

                String section = inspectionReportService.resolveSectionForUser(
                                currentUser,
                                FINISHING);

                if (section == null
                                || !FINISHING.equalsIgnoreCase(section)) {
                        throw new UnauthorizedException(
                                        "Tidak memiliki akses laporan Finishing.");
                }

                String reportType = normalizeReportType(reportTypeValue);
                String buyerFilter = normalizeFilter(buyerValue);
                String styleFilter = normalizeFilter(styleValue);

                if (BUYERWISE.equals(reportType) && ALL.equalsIgnoreCase(buyerFilter)) {
                        throw new BadRequestException(
                                        "Untuk laporan BUYERWISE, pilih satu buyer terlebih dahulu.");
                }

                if (STYLEWISE.equals(reportType) && ALL.equalsIgnoreCase(styleFilter)) {
                        throw new BadRequestException(
                                        "Untuk laporan STYLEWISE, pilih satu style terlebih dahulu.");
                }

                Instant startInstant = start.atStartOfDay(ZONE).toInstant();

                Instant endExclusive = end.plusDays(1)
                                .atStartOfDay(ZONE)
                                .toInstant();

                List<Inspection> inspections = inspectionRepository
                                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                                startInstant,
                                                endExclusive)
                                .stream()
                                .filter(i -> i.getCreatedAt() != null)
                                .filter(i -> i.getLine() != null
                                                && i.getLine().getSection() != null
                                                && FINISHING.equalsIgnoreCase(
                                                                i.getLine().getSection().name()))
                                .filter(i -> canViewInspection(i, currentUser))
                                .filter(i -> matches(
                                                buyerFilter,
                                                i.getBuyerName()))
                                .filter(i -> matches(
                                                styleFilter,
                                                i.getStyleCode()))
                                .sorted(
                                                Comparator.comparing(
                                                                Inspection::getCreatedAt))
                                .toList();

                List<LocalDate> dates = new ArrayList<>();

                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                        dates.add(d);
                }

                /*
                 * ALL, BUYERWISE, dan STYLEWISE memakai struktur
                 * laporan yang sama, dengan kolom tanggal.
                 *
                 * Perbedaannya hanya filter data yang diterapkan:
                 * - ALL: semua data sesuai filter buyer/style.
                 * - BUYERWISE: buyer tertentu atau semua buyer.
                 * - STYLEWISE: style tertentu atau semua style.
                 *
                 * Filter buyer dan style dapat digunakan bersamaan.
                 */
                return buildWorkbook(
                                start,
                                end,
                                dates,
                                inspections,
                                reportType,
                                buyerFilter,
                                styleFilter,
                                periodType);
        }

        private byte[] buildWorkbook(
                        LocalDate start,
                        LocalDate end,
                        List<LocalDate> dates,
                        List<Inspection> inspections,
                        String reportType,
                        String buyer,
                        String style,
                        String periodType) throws IOException {

                try (Workbook workbook = new XSSFWorkbook();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                        Sheet sheet = workbook.createSheet(
                                        "Finishing " + periodType);

                        Styles styles = new Styles(workbook);

                        int dateCount = dates.size();
                        int totalColumn = dateCount + 1;
                        int percentColumn = totalColumn + 1;
                        int lastColumn = percentColumn;

                        mergedRow(
                                        sheet, 0, 0, lastColumn,
                                        "PT UNGARAN SARI GARMENT",
                                        styles.title);

                        mergedRow(
                                        sheet, 1, 0, lastColumn,
                                        periodType + " REPORT FINISHING SECTION - "
                                                        + reportType,
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
                                        "PERIOD: "
                                                        + start.format(DATE_FORMAT)
                                                        + " - "
                                                        + end.format(DATE_FORMAT),
                                        styles.period);

                        mergedRow(
                                        sheet, 5, 0, lastColumn,
                                        "REPORT TYPE: " + reportType
                                                        + "     BUYER FILTER: " + buyer
                                                        + "     STYLE FILTER: " + style,
                                        styles.period);

                        /*
                         * Header tanggal.
                         * Struktur ini sama untuk ALL, BUYERWISE,
                         * dan STYLEWISE.
                         */
                        Row headerGroup = sheet.createRow(6);
                        Row headerDates = sheet.createRow(7);

                        for (int c = 0; c <= lastColumn; c++) {
                                Cell top = headerGroup.createCell(c);

                                top.setCellStyle(
                                                c == 0
                                                                ? styles.headerLabel
                                                                : styles.header);

                                Cell bottom = headerDates.createCell(c);

                                bottom.setCellStyle(
                                                c == 0
                                                                ? styles.headerLabel
                                                                : styles.header);
                        }

                        headerGroup.getCell(0).setCellValue("DEFECT");
                        headerGroup.getCell(1).setCellValue("DATE");
                        headerGroup.getCell(totalColumn).setCellValue("TOTAL");
                        headerGroup.getCell(percentColumn).setCellValue("% DEF");

                        for (int c = 0; c < dateCount; c++) {
                                headerDates.getCell(c + 1).setCellValue(
                                                String.valueOf(
                                                                dates.get(c).getDayOfMonth()));
                        }

                        sheet.addMergedRegion(
                                        new CellRangeAddress(6, 7, 0, 0));

                        sheet.addMergedRegion(
                                        new CellRangeAddress(
                                                        6, 6, 1, dateCount));

                        sheet.addMergedRegion(
                                        new CellRangeAddress(
                                                        6, 7, totalColumn, totalColumn));

                        sheet.addMergedRegion(
                                        new CellRangeAddress(
                                                        6, 7, percentColumn, percentColumn));

                        Map<LocalDate, DayData> dailyData = new LinkedHashMap<>();

                        for (LocalDate date : dates) {
                                dailyData.put(date, new DayData());
                        }

                        for (Inspection inspection : inspections) {
                                LocalDate inspectionDate = inspection.getCreatedAt()
                                                .atZone(ZONE)
                                                .toLocalDate();

                                DayData day = dailyData.get(inspectionDate);

                                if (day != null) {
                                        day.add(inspection);
                                }
                        }

                        int[] inspectedByDay = new int[dateCount];
                        int[] sewingByDay = new int[dateCount];
                        int[] finishingByDay = new int[dateCount];
                        int[] totalDefectByDay = new int[dateCount];
                        int[] goodByDay = new int[dateCount];

                        Map<DefectKey, int[]> sewingRows = newDefectMap();
                        Map<DefectKey, int[]> finishingRows = newDefectMap();

                        for (int c = 0; c < dateCount; c++) {
                                DayData day = dailyData.get(dates.get(c));

                                inspectedByDay[c] = day.inspectQuantity;
                                sewingByDay[c] = day.sewingDefectTotal;
                                finishingByDay[c] = day.finishingDefectTotal;

                                totalDefectByDay[c] = day.sewingDefectTotal
                                                + day.finishingDefectTotal;

                                goodByDay[c] = Math.max(
                                                0,
                                                inspectedByDay[c] - totalDefectByDay[c]);

                                for (Map.Entry<DefectKey, Integer> entry : day.sewingDefects.entrySet()) {

                                        sewingRows.computeIfAbsent(
                                                        entry.getKey(),
                                                        ignored -> new int[dateCount])[c] += entry.getValue();
                                }

                                for (Map.Entry<DefectKey, Integer> entry : day.finishingDefects.entrySet()) {

                                        finishingRows.computeIfAbsent(
                                                        entry.getKey(),
                                                        ignored -> new int[dateCount])[c] += entry.getValue();
                                }
                        }

                        int totalInspect = sum(inspectedByDay);

                        int rowIndex = 8;

                        rowIndex = writeDefectSection(
                                        sheet,
                                        rowIndex,
                                        "DEFECT DETAILS - SEWING",
                                        sewingRows,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        totalInspect,
                                        lastColumn,
                                        styles);

                        rowIndex = writeSummaryRow(
                                        sheet,
                                        rowIndex,
                                        "TOTAL DEF SEWING",
                                        sewingByDay,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        styles);

                        rowIndex = writePercentageRow(
                                        sheet,
                                        rowIndex,
                                        "% DEFECT SEWING",
                                        sewingByDay,
                                        inspectedByDay,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        styles);

                        rowIndex = writeDefectSection(
                                        sheet,
                                        rowIndex,
                                        "DEFECT DETAILS - FINISHING",
                                        finishingRows,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        totalInspect,
                                        lastColumn,
                                        styles);

                        rowIndex = writeSummaryRow(
                                        sheet,
                                        rowIndex,
                                        "TOTAL DEF FINISHING",
                                        finishingByDay,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        styles);

                        rowIndex = writePercentageRow(
                                        sheet,
                                        rowIndex,
                                        "% DEFECT FINISHING",
                                        finishingByDay,
                                        inspectedByDay,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        styles);

                        rowIndex = writeSummaryRow(
                                        sheet,
                                        rowIndex,
                                        "TOTAL DEFECT",
                                        totalDefectByDay,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        styles);

                        rowIndex = writePercentageRow(
                                        sheet,
                                        rowIndex,
                                        "% TOTAL DEFECT",
                                        totalDefectByDay,
                                        inspectedByDay,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        styles);

                        rowIndex = writeSummaryRow(
                                        sheet,
                                        rowIndex,
                                        "QTY GOOD",
                                        goodByDay,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        styles);

                        writeSummaryRow(
                                        sheet,
                                        rowIndex,
                                        "QTY INSPECT",
                                        inspectedByDay,
                                        dateCount,
                                        totalColumn,
                                        percentColumn,
                                        styles);

                        sheet.setColumnWidth(0, 36 * 256);
                        for (int c = 1; c <= dateCount; c++) {
                                sheet.setColumnWidth(c, 7 * 256);
                        }

                        sheet.setColumnWidth(totalColumn, 12 * 256);
                        sheet.setColumnWidth(percentColumn, 12 * 256);
                        sheet.createFreezePane(1, 8);

                        configurePrint(sheet);

                        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                                Row row = sheet.getRow(r);

                                if (row != null) {
                                        row.setHeightInPoints(r < 8 ? 22 : 17);
                                }
                        }

                        workbook.write(output);

                        return output.toByteArray();
                }
        }

        private Map<DefectKey, int[]> newDefectMap() {
                return new TreeMap<>(
                                Comparator.comparing(
                                                DefectKey::area,
                                                String.CASE_INSENSITIVE_ORDER)
                                                .thenComparing(
                                                                DefectKey::name,
                                                                String.CASE_INSENSITIVE_ORDER));
        }

        private int writeDefectSection(
                        Sheet sheet,
                        int rowIndex,
                        String title,
                        Map<DefectKey, int[]> defectRows,
                        int dateCount,
                        int totalColumn,
                        int percentColumn,
                        int totalInspect,
                        int lastColumn,
                        Styles styles) {

                mergedRow(
                                sheet,
                                rowIndex++,
                                0,
                                lastColumn,
                                title,
                                styles.section);

                String currentArea = null;

                for (Map.Entry<DefectKey, int[]> entry : defectRows.entrySet()) {

                        DefectKey key = entry.getKey();
                        int[] quantities = entry.getValue();

                        if (!key.area().isBlank()
                                        && !key.area().equals(currentArea)) {

                                currentArea = key.area();

                                Row areaRow = sheet.createRow(rowIndex++);

                                setCell(
                                                areaRow, 0,
                                                currentArea,
                                                styles.area);

                                for (int c = 1; c <= lastColumn; c++) {
                                        setCell(
                                                        areaRow, c,
                                                        "",
                                                        styles.area);
                                }
                        }

                        Row row = sheet.createRow(rowIndex++);

                        setCell(
                                        row, 0,
                                        "    " + key.name(),
                                        styles.body);

                        for (int c = 0; c < dateCount; c++) {
                                setCell(
                                                row,
                                                c + 1,
                                                quantities[c],
                                                styles.number);
                        }

                        int rowTotal = sum(quantities);

                        setCell(
                                        row,
                                        totalColumn,
                                        rowTotal,
                                        styles.total);

                        setCell(
                                        row,
                                        percentColumn,
                                        ratio(rowTotal, totalInspect),
                                        styles.percentBody);
                }

                return rowIndex;
        }

        private int writeSummaryRow(
                        Sheet sheet,
                        int rowIndex,
                        String label,
                        int[] values,
                        int dateCount,
                        int totalColumn,
                        int percentColumn,
                        Styles styles) {

                Row row = sheet.createRow(rowIndex++);

                setCell(row, 0, label, styles.summary);

                for (int c = 0; c < dateCount; c++) {
                        setCell(
                                        row,
                                        c + 1,
                                        values[c],
                                        styles.numberSummary);
                }

                setCell(
                                row,
                                totalColumn,
                                sum(values),
                                styles.total);

                setCell(
                                row,
                                percentColumn,
                                "",
                                styles.total);

                return rowIndex;
        }

        private int writePercentageRow(
                        Sheet sheet,
                        int rowIndex,
                        String label,
                        int[] defectByDay,
                        int[] inspectedByDay,
                        int dateCount,
                        int totalColumn,
                        int percentColumn,
                        Styles styles) {

                Row row = sheet.createRow(rowIndex++);

                setCell(row, 0, label, styles.summary);

                for (int c = 0; c < dateCount; c++) {
                        setCell(
                                        row,
                                        c + 1,
                                        ratio(
                                                        defectByDay[c],
                                                        inspectedByDay[c]),
                                        styles.percentSummary);
                }

                double overall = ratio(
                                sum(defectByDay),
                                sum(inspectedByDay));

                setCell(
                                row,
                                totalColumn,
                                overall,
                                styles.percentTotal);

                setCell(
                                row,
                                percentColumn,
                                "",
                                styles.percentTotal);

                return rowIndex;
        }

        private boolean canViewInspection(
                        Inspection inspection,
                        User currentUser) {

                if (currentUser == null
                                || currentUser.getRole() == null) {
                        return false;
                }

                String role = currentUser.getRole().name();

                if ("SUPER_ADMIN".equals(role)
                                || "ADMIN".equals(role)) {
                        return true;
                }

                return "USER".equals(role)
                                && inspection.getSubmittedBy() != null
                                && currentUser.getId() != null
                                && currentUser.getId().equals(
                                                inspection.getSubmittedBy().getId());
        }

        private String normalizeReportType(String value) {
                String normalized = value == null || value.isBlank()
                                ? ALL
                                : value.trim().toUpperCase(Locale.ROOT);

                if (!List.of(ALL, BUYERWISE, STYLEWISE)
                                .contains(normalized)) {

                        throw new BadRequestException(
                                        "Tipe laporan harus ALL, BUYERWISE, atau STYLEWISE.");
                }

                return normalized;
        }

        private String normalizeFilter(String value) {
                if (value == null
                                || value.isBlank()
                                || ALL.equalsIgnoreCase(value.trim())) {
                        return ALL;
                }

                return value.trim();
        }

        private boolean matches(String filter, String value) {
                return ALL.equalsIgnoreCase(filter)
                                || (value != null
                                                && filter.equalsIgnoreCase(value.trim()));
        }

        private int sum(int[] values) {
                int total = 0;

                for (int value : values) {
                        total += value;
                }

                return total;
        }

        private double ratio(int numerator, int denominator) {
                return denominator <= 0
                                ? 0.0
                                : (double) numerator / denominator;
        }

        private void mergedRow(
                        Sheet sheet,
                        int rowIndex,
                        int firstColumn,
                        int lastColumn,
                        String value,
                        CellStyle style) {

                Row row = sheet.createRow(rowIndex);

                for (int c = firstColumn; c <= lastColumn; c++) {
                        Cell cell = row.createCell(c);
                        cell.setCellStyle(style);
                }

                row.getCell(firstColumn).setCellValue(value);

                if (firstColumn < lastColumn) {
                        sheet.addMergedRegion(
                                        new CellRangeAddress(
                                                        rowIndex,
                                                        rowIndex,
                                                        firstColumn,
                                                        lastColumn));
                }
        }

        private void setCell(
                        Row row,
                        int column,
                        String value,
                        CellStyle style) {

                Cell cell = row.createCell(column);
                cell.setCellValue(value == null ? "" : value);
                cell.setCellStyle(style);
        }

        private void setCell(
                        Row row,
                        int column,
                        int value,
                        CellStyle style) {

                Cell cell = row.createCell(column);

                if (value != 0) {
                        cell.setCellValue(value);
                }

                cell.setCellStyle(style);
        }

        private void setCell(
                        Row row,
                        int column,
                        double value,
                        CellStyle style) {

                Cell cell = row.createCell(column);

                if (value != 0.0) {
                        cell.setCellValue(value);
                }

                cell.setCellStyle(style);
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
                sheet.setHorizontallyCenter(true);

                sheet.setMargin(PageMargin.LEFT, 0.4);
                sheet.setMargin(PageMargin.RIGHT, 0.4);
                sheet.setMargin(PageMargin.TOP, 0.5);
                sheet.setMargin(PageMargin.BOTTOM, 0.5);
        }

        private record DefectKey(
                        String area,
                        String name) {
        }

        private static class DayData {

                private int inspectQuantity;
                private int transferQuantity;

                private int sewingDefectTotal;
                private int finishingDefectTotal;

                private final Map<DefectKey, Integer> sewingDefects = new LinkedHashMap<>();

                private final Map<DefectKey, Integer> finishingDefects = new LinkedHashMap<>();

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

                        for (InspectionDefect defect : inspection.getInspectionDefects()) {

                                DefectType type = defect.getDefectType();

                                if (type == null
                                                || defect.getQuantity() == null
                                                || defect.getQuantity() <= 0) {
                                        continue;
                                }

                                int quantity = defect.getQuantity();

                                String area = type.getDefectArea() == null
                                                || type.getDefectArea().getName() == null
                                                                ? ""
                                                                : type.getDefectArea().getName().trim();

                                String name = type.getName() == null
                                                ? "UNKNOWN DEFECT"
                                                : type.getName().trim();

                                DefectKey key = new DefectKey(area, name);

                                if (type.getCategory() == DefectCategory.DEFECT_FINISHING) {

                                        finishingDefects.merge(
                                                        key,
                                                        quantity,
                                                        Integer::sum);

                                        finishingDefectTotal += quantity;

                                } else {
                                        sewingDefects.merge(
                                                        key,
                                                        quantity,
                                                        Integer::sum);

                                        sewingDefectTotal += quantity;
                                }
                        }
                }
        }

        private static class Styles {

                private static final Color GREEN_HEADER = new Color(96, 125, 107);
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
                final CellStyle numberSummary;
                final CellStyle summary;
                final CellStyle total;
                final CellStyle percentBody;
                final CellStyle percentSummary;
                final CellStyle percentTotal;

                Styles(Workbook workbook) {
                        title = style(
                                        workbook,
                                        TITLE_PASTEL,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        14,
                                        true,
                                        HorizontalAlignment.CENTER);

                        subtitle = style(
                                        workbook,
                                        SUBTITLE_PASTEL,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.CENTER);

                        period = style(
                                        workbook,
                                        GREEN_LIGHT,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9,
                                        false,
                                        HorizontalAlignment.CENTER);

                        header = style(
                                        workbook,
                                        BLUE_HEADER,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.CENTER);

                        headerLabel = style(
                                        workbook,
                                        BLUE_HEADER,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.LEFT);

                        section = style(
                                        workbook,
                                        BLUE_HEADER,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.LEFT);

                        area = style(
                                        workbook,
                                        YELLOW_LIGHT,
                                        IndexedColors.BLACK.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.LEFT);

                        body = style(
                                        workbook,
                                        Color.WHITE,
                                        IndexedColors.BLACK.getIndex(),
                                        9,
                                        false,
                                        HorizontalAlignment.LEFT);

                        number = style(
                                        workbook,
                                        Color.WHITE,
                                        IndexedColors.BLACK.getIndex(),
                                        9,
                                        false,
                                        HorizontalAlignment.CENTER);

                        numberSummary = style(
                                        workbook,
                                        BEIGE,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.CENTER);

                        summary = style(
                                        workbook,
                                        BEIGE,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.LEFT);

                        total = style(
                                        workbook,
                                        GREEN_TOTAL,
                                        IndexedColors.DARK_GREEN.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.CENTER);

                        percentBody = style(
                                        workbook,
                                        Color.WHITE,
                                        IndexedColors.BLACK.getIndex(),
                                        9,
                                        false,
                                        HorizontalAlignment.CENTER);

                        percentSummary = style(
                                        workbook,
                                        BEIGE,
                                        IndexedColors.DARK_BLUE.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.CENTER);

                        percentTotal = style(
                                        workbook,
                                        GREEN_TOTAL,
                                        IndexedColors.DARK_GREEN.getIndex(),
                                        9,
                                        true,
                                        HorizontalAlignment.CENTER);

                        DataFormat format = workbook.createDataFormat();

                        percentBody.setDataFormat(
                                        format.getFormat("0.00%"));

                        percentSummary.setDataFormat(
                                        format.getFormat("0.00%"));

                        percentTotal.setDataFormat(
                                        format.getFormat("0.00%"));
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
                                        new XSSFColor(
                                                        background,
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