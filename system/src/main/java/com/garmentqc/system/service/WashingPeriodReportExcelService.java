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
import java.util.function.Function;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PageMargin;
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
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.BuyerRepository;
import com.garmentqc.system.repository.InspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WashingPeriodReportExcelService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");
    private static final String WASHING = "WASHING";
    private static final String BULK = "BULK";
    private static final String REPAIR = "REPAIR";
    private static final String GRAND_TOTAL = "GRAND TOTAL";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM", Locale.ENGLISH);
    private static final int LABEL_COLUMN = 0;

    private final InspectionRepository inspectionRepository;
    private final InspectionReportService inspectionReportService;
    private final BuyerRepository buyerRepository;

    public byte[] exportWeeklyWashingExcel(LocalDate date, String reportType, String buyer, String style,
            User currentUser) throws IOException {

        if (date == null) {
            throw new BadRequestException("Tanggal laporan wajib diisi.");
        }

        LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        LocalDate end = start.plusDays(6);

        return exportPeriod(start, end, reportType, buyer, style, currentUser, "WEEKLY");
    }

    public byte[] exportMonthlyWashingExcel(int month, int year, String reportType, String buyer, String style,
            User currentUser) throws IOException {
        final YearMonth period;

        try {
            period = YearMonth.of(year, month);
        } catch (java.time.DateTimeException ex) {
            throw new ResourceNotFoundException("Bulan atau tahun laporan tidak valid.");
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

    private byte[] exportPeriod(LocalDate start, LocalDate end, String reportTypeValue, String buyerValue,
            String styleValue, User currentUser, String periodType) throws IOException {
        String section = inspectionReportService.resolveSectionForUser(currentUser, WASHING);

        if (section != null && !section.isBlank() && !WASHING.equalsIgnoreCase(section)) {
            throw new UnauthorizedException("Tidak memiliki akses laporan Washing.");
        }

        String reportType = normalizeReportType(reportTypeValue);
        String buyerFilter = normalizeFilter(buyerValue);
        String styleFilter = normalizeFilter(styleValue);

        if ("BUYERWISE".equals(reportType)) {
            validateBuyerExists(buyerFilter);
        }

        if ("STYLEWISE".equals(reportType) && "ALL".equalsIgnoreCase(styleFilter)) {
            throw new BadRequestException(
                    "Untuk laporan STYLEWISE, pilih satu style terlebih dahulu.");
        }

        Instant startInstant = start.atStartOfDay(ZONE).toInstant();
        Instant endExclusive = end.plusDays(1).atStartOfDay(ZONE).toInstant();

        List<Inspection> inspections = inspectionRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startInstant, endExclusive)
                .stream()
                .filter(i -> i.getCreatedAt() != null)
                .filter(i -> i.getLine() != null && i.getLine().getSection() != null
                        && WASHING.equalsIgnoreCase(i.getLine().getSection().name()))
                .filter(i -> i.getWashingType() != null)
                .filter(i -> canViewInspection(i, currentUser))
                .filter(i -> matches(buyerFilter, i.getBuyerName()))
                .filter(i -> matches(styleFilter, i.getStyleCode()))
                .sorted(Comparator.comparing(Inspection::getCreatedAt))
                .toList();

        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dates.add(d);
        }

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Styles styles = new Styles(workbook);

            createWashingSheet(
                    workbook, styles, BULK, BULK, start, end,
                    dates, inspections, reportType, buyerFilter,
                    styleFilter, periodType);

            createWashingSheet(
                    workbook, styles, REPAIR, REPAIR, start, end,
                    dates, inspections, reportType, buyerFilter,
                    styleFilter, periodType);

            createWashingSheet(
                    workbook, styles, GRAND_TOTAL, null, start, end,
                    dates, inspections, reportType, buyerFilter,
                    styleFilter, periodType);

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void createWashingSheet(Workbook workbook, Styles styles, String sheetName, String washingType,
            LocalDate start, LocalDate end, List<LocalDate> dates, List<Inspection> allInspections, String reportType,
            String buyerFilter, String styleFilter, String periodType) {
        Sheet sheet = workbook.createSheet(sheetName);
        List<Inspection> inspections = washingType == null
                ? allInspections
                : allInspections.stream()
                        .filter(i -> washingType.equalsIgnoreCase(
                                i.getWashingType().name()))
                        .toList();

        String titleSuffix = washingType == null
                ? "GRAND TOTAL (BULK + REPAIR)"
                : washingType;

        Map<String, List<Inspection>> groups = groupInspections(inspections, reportType);

        int dateCount = dates.size();
        int totalColumn = dateCount + 1;
        int percentColumn = totalColumn + 1;
        int lastColumn = percentColumn;

        mergedRow(sheet, 0, 0, lastColumn,
                "PT UNGARAN SARI GARMENT", styles.title);

        mergedRow(sheet, 1, 0, lastColumn,
                periodType + " REPORT WASHING SECTION - " + titleSuffix,
                styles.subtitle);

        mergedRow(sheet, 2, 0, lastColumn,
                "QC DEPARTMENT", styles.subtitle);

        mergedRow(sheet, 3, 0, lastColumn,
                "DEPT: DRESS 2", styles.subtitle);

        mergedRow(sheet, 4, 0, lastColumn,
                "PERIOD: " + start.format(DATE_FORMAT)
                        + " - " + end.format(DATE_FORMAT),
                styles.period);

        mergedRow(sheet, 5, 0, lastColumn, "    BUYER: " + buyerFilter
                + "    STYLE: " + styleFilter,
                styles.period);

        int rowIndex = 6;

        List<SectionDef> sectionDefs = List.of(
                new SectionDef(
                        "DEFECT DETAILS - SEWING",
                        "TOTAL DEF SEWING",
                        "% DEFECT SEWING",
                        DayData::getSewing),
                new SectionDef(
                        "DEFECT DETAILS - WASHING",
                        "TOTAL DEF WASHING",
                        "% DEFECT WASHING",
                        DayData::getWashing),
                new SectionDef(
                        "DEFECT DETAILS - FINISHING",
                        "TOTAL DEF FINISHING",
                        "% DEFECT FINISHING",
                        DayData::getFinishing));

        for (Map.Entry<String, List<Inspection>> groupEntry : groups.entrySet()) {

            List<Inspection> groupInspections = groupEntry.getValue();

            String heading;

            if ("BUYERWISE".equals(reportType)) {
                heading = "BUYER: " + buyerFilter;
            } else if ("STYLEWISE".equals(reportType)) {
                heading = "STYLE: " + styleFilter;
            } else {
                heading = "WASHING DATA - " + titleSuffix;
            }

            mergedRow(sheet, rowIndex++, 0, lastColumn,
                    heading, styles.groupTitle);

            Row topHeader = sheet.createRow(rowIndex++);
            Row dateHeader = sheet.createRow(rowIndex++);

            for (int c = 0; c <= lastColumn; c++) {
                topHeader.createCell(c).setCellStyle(styles.header);
                dateHeader.createCell(c).setCellStyle(styles.header);
            }

            topHeader.getCell(LABEL_COLUMN).setCellValue("DEFECT");
            topHeader.getCell(1).setCellValue("DAILY DEFECT");
            topHeader.getCell(totalColumn).setCellValue("TOTAL");
            topHeader.getCell(percentColumn).setCellValue("% DEFECT");

            for (int c = 0; c < dateCount; c++) {
                LocalDate d = dates.get(c);
                dateHeader.getCell(c + 1).setCellValue(
                        "WEEKLY".equals(periodType)
                                ? d.format(SHORT_DATE_FORMAT)
                                : String.valueOf(d.getDayOfMonth()));
            }

            int topRow = topHeader.getRowNum();
            int dateRow = dateHeader.getRowNum();
            sheet.addMergedRegion(new CellRangeAddress(
                    topRow, dateRow, LABEL_COLUMN, LABEL_COLUMN));

            if (dateCount > 1) {
                sheet.addMergedRegion(new CellRangeAddress(
                        topRow, topRow, 1, dateCount));
            }

            sheet.addMergedRegion(new CellRangeAddress(
                    topRow, dateRow, totalColumn, totalColumn));

            sheet.addMergedRegion(new CellRangeAddress(
                    topRow, dateRow, percentColumn, percentColumn));

            Map<LocalDate, DayData> dailyData = new LinkedHashMap<>();

            for (LocalDate date : dates) {
                dailyData.put(date, new DayData());
            }

            for (Inspection inspection : groupInspections) {
                LocalDate inspectionDate = inspection.getCreatedAt()
                        .atZone(ZONE).toLocalDate();

                DayData day = dailyData.get(inspectionDate);

                if (day != null) {
                    day.add(inspection);
                }
            }

            int[] inspectByDay = new int[dateCount];
            int[] transferByDay = new int[dateCount];
            int[] totalDefectByDay = new int[dateCount];

            for (int c = 0; c < dateCount; c++) {
                DayData day = dailyData.get(dates.get(c));
                inspectByDay[c] = day.inspectQuantity;
                transferByDay[c] = day.transferQuantity;
            }

            int totalInspect = sum(inspectByDay);

            for (SectionDef def : sectionDefs) {

                int[] sectionDefectByDay = new int[dateCount];

                rowIndex = writeDefectSection(
                        sheet, rowIndex, def, dates, dailyData,
                        sectionDefectByDay, totalInspect,
                        totalColumn, percentColumn, lastColumn, styles);

                int sectionTotal = sum(sectionDefectByDay);

                rowIndex = writeSummaryRow(
                        sheet, rowIndex, def.totalLabel(),
                        sectionDefectByDay, totalColumn, sectionTotal,
                        percentColumn, percentage(sectionTotal, totalInspect),
                        styles);

                rowIndex = writePercentageRow(
                        sheet, rowIndex, def.percentLabel(),
                        sectionDefectByDay, inspectByDay,
                        totalColumn, percentColumn, styles);

                for (int c = 0; c < dateCount; c++) {
                    totalDefectByDay[c] += sectionDefectByDay[c];
                }
            }

            int totalDefect = sum(totalDefectByDay);

            rowIndex = writeSummaryRow(
                    sheet, rowIndex, "TOTAL DEFECT",
                    totalDefectByDay, totalColumn, totalDefect,
                    percentColumn, percentage(totalDefect, totalInspect),
                    styles);

            rowIndex = writePercentageRow(
                    sheet, rowIndex, "% TOTAL DEFECT",
                    totalDefectByDay, inspectByDay,
                    totalColumn, percentColumn, styles);

            rowIndex = writeSummaryRow(
                    sheet, rowIndex, "QTY INSPECT",
                    inspectByDay, totalColumn, totalInspect,
                    percentColumn, null, styles);

            rowIndex = writePercentageRow(
                    sheet, rowIndex, "% DEFECT",
                    totalDefectByDay, inspectByDay,
                    totalColumn, percentColumn, styles);

            int[] goodByDay = new int[dateCount];

            for (int c = 0; c < dateCount; c++) {
                goodByDay[c] = Math.max(
                        0, inspectByDay[c] - totalDefectByDay[c]);
            }

            rowIndex = writeSummaryRow(
                    sheet, rowIndex, "GOOD QC",
                    goodByDay, totalColumn, sum(goodByDay),
                    percentColumn, null, styles);

            // rowIndex = writeSummaryRow(
            //         sheet, rowIndex, "QTY TRANSFER",
            //         transferByDay, totalColumn, sum(transferByDay),
            //         percentColumn, null, styles);

            rowIndex += 2;
        }

        sheet.setColumnWidth(LABEL_COLUMN, 38 * 256);

        for (int c = 1; c <= dateCount; c++) {
            sheet.setColumnWidth(c, 9 * 256);
        }

        sheet.setColumnWidth(totalColumn, 14 * 256);
        sheet.setColumnWidth(percentColumn, 12 * 256);

        sheet.createFreezePane(1, 6);

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
    }

    private int writeDefectSection(Sheet sheet, int rowIndex, SectionDef def, List<LocalDate> dates,
            Map<LocalDate, DayData> dailyData, int[] sectionDefectByDay, int totalInspect, int totalColumn,
            int percentColumn, int lastColumn, Styles styles) {
        int dateCount = dates.size();

        mergedRow(sheet, rowIndex++, LABEL_COLUMN, lastColumn, def.title(), styles.section);

        Map<DefectKey, int[]> defectRows = new TreeMap<>(
                Comparator.comparing(DefectKey::area, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DefectKey::name, String.CASE_INSENSITIVE_ORDER));

        for (int c = 0; c < dateCount; c++) {
            DayData day = dailyData.get(dates.get(c));
            Map<DefectKey, Integer> defects = def.getter().apply(day);

            for (Map.Entry<DefectKey, Integer> entry : defects.entrySet()) {
                defectRows.computeIfAbsent(
                        entry.getKey(),
                        ignored -> new int[dateCount])[c] += entry.getValue();

                sectionDefectByDay[c] += entry.getValue();
            }
        }

        String currentArea = null;

        for (Map.Entry<DefectKey, int[]> entry : defectRows.entrySet()) {
            DefectKey key = entry.getKey();
            int[] quantities = entry.getValue();

            if (!key.area().isBlank() && !key.area().equals(currentArea)) {
                currentArea = key.area();

                Row areaRow = sheet.createRow(rowIndex++);
                setCell(areaRow, LABEL_COLUMN, currentArea, styles.area);

                for (int c = 1; c <= lastColumn; c++) {
                    setCell(areaRow, c, "", styles.area);
                }
            }

            Row row = sheet.createRow(rowIndex++);
            setCell(row, LABEL_COLUMN, "    " + key.name(), styles.body);

            for (int c = 0; c < dateCount; c++) {
                setCell(row, c + 1, quantities[c], styles.number);
            }

            int rowTotal = sum(quantities);

            setCell(row, totalColumn, rowTotal, styles.total);
            setCell(row, percentColumn,
                    percentage(rowTotal, totalInspect),
                    styles.percentTotal);
        }

        return rowIndex;
    }

    private int writeSummaryRow(Sheet sheet, int rowIndex, String label, int[] values, int totalColumn, int grandTotal,
            int percentColumn, Double percentValue, Styles styles) {
        Row row = sheet.createRow(rowIndex++);

        setCell(row, LABEL_COLUMN, label, styles.summaryLabel);

        for (int c = 0; c < values.length; c++) {
            setCell(row, c + 1, values[c], styles.summaryValue);
        }

        setCell(row, totalColumn, grandTotal, styles.total);

        if (percentValue == null) {
            setCell(row, percentColumn, "", styles.total);
        } else {
            setCell(row, percentColumn, percentValue.doubleValue(), styles.percentTotal);
        }

        return rowIndex;
    }

    private int writePercentageRow(Sheet sheet, int rowIndex, String label, int[] defectByDay, int[] inspectByDay,
            int totalColumn, int percentColumn, Styles styles) {
        Row row = sheet.createRow(rowIndex++);

        setCell(row, LABEL_COLUMN, label, styles.summaryLabel);

        for (int c = 0; c < defectByDay.length; c++) {
            setCell(row, c + 1, percentage(defectByDay[c], inspectByDay[c]), styles.percent);
        }

        setCell(row, totalColumn, percentage(sum(defectByDay), sum(inspectByDay)), styles.percentTotal);
        setCell(row, percentColumn, "", styles.total);

        return rowIndex;
    }

    private Map<String, List<Inspection>> groupInspections(
            List<Inspection> inspections,
            String reportType) {

        Map<String, List<Inspection>> groups = new LinkedHashMap<>();

        // Hanya satu grup data per sheet.
        // Data sudah difilter berdasarkan buyer/style dan kemudian
        // diakumulasikan per tanggal.
        String key;

        if ("BUYERWISE".equals(reportType)) {
            key = "SELECTED BUYER";
        } else if ("STYLEWISE".equals(reportType)) {
            key = "SELECTED STYLE";
        } else {
            key = "ALL DATA";
        }

        groups.put(key, inspections);
        return groups;
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
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())) {
            return "ALL";
        }

        return value.trim();
    }

    private boolean matches(String filter, String value) {
        return "ALL".equalsIgnoreCase(filter) || (value != null && filter.equalsIgnoreCase(value.trim()));
    }

    private String safeLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private double percentage(long defect, long inspect) {
        return inspect <= 0 ? 0.0 : (double) defect / inspect;
    }

    private int sum(int[] values) {
        int total = 0;

        for (int value : values) {
            total += value;
        }

        return total;
    }

    private void mergedRow(Sheet sheet, int rowIndex, int firstColumn, int lastColumn, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);

        for (int c = firstColumn; c <= lastColumn; c++) {
            Cell cell = row.createCell(c);
            cell.setCellStyle(style);
        }

        row.getCell(firstColumn).setCellValue(value);

        if (firstColumn < lastColumn) {
            sheet.addMergedRegion(new CellRangeAddress(
                    rowIndex, rowIndex, firstColumn, lastColumn));
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

    private void validateBuyerExists(String buyer) {
        if (buyer == null || buyer.isBlank()
                || "ALL".equalsIgnoreCase(buyer.trim())) {
            throw new BadRequestException(
                    "Untuk laporan BUYERWISE, pilih satu buyer terlebih dahulu.");
        }

        String buyerName = buyer.trim();

        boolean exists = inspectionRepository
                .existsByBuyerNameIgnoreCase(buyerName);

        if (!exists) {
            throw new BadRequestException(
                    "Buyer '" + buyerName + "' tidak ditemukan.");
        }
    }

    private record DefectKey(String area, String name) {

    }

    private record SectionDef(String title, String totalLabel, String percentLabel,
            Function<DayData, Map<DefectKey, Integer>> getter) {

    }

    private static class DayData {
        private int inspectQuantity;
        private int transferQuantity;

        private final Map<DefectKey, Integer> sewing = new LinkedHashMap<>();
        private final Map<DefectKey, Integer> washing = new LinkedHashMap<>();
        private final Map<DefectKey, Integer> finishing = new LinkedHashMap<>();

        Map<DefectKey, Integer> getSewing() {
            return sewing;
        }

        Map<DefectKey, Integer> getWashing() {
            return washing;
        }

        Map<DefectKey, Integer> getFinishing() {
            return finishing;
        }

        void add(Inspection inspection) {
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
                if (defect == null || defect.getQuantity() == null || defect.getQuantity() <= 0) {
                    continue;
                }

                DefectType type = defect.getDefectType();

                if (type == null || type.getCategory() == null) {
                    continue;
                }

                DefectCategory category = type.getCategory();

                String area = type.getDefectArea() == null
                        || type.getDefectArea().getName() == null
                                ? ""
                                : type.getDefectArea().getName().trim();

                DefectKey key = new DefectKey(area, safe(type.getName()));

                if (isSewingCategory(category)) {
                    sewing.merge(key, defect.getQuantity(), Integer::sum);
                } else if (category == DefectCategory.DEFECT_WASHING) {
                    washing.merge(key, defect.getQuantity(), Integer::sum);
                } else if (category == DefectCategory.DEFECT_FINISHING) {
                    finishing.merge(key, defect.getQuantity(), Integer::sum);
                }
            }
        }

        private static boolean isSewingCategory(DefectCategory category) {
            return category == DefectCategory.DEFECT_BACK_TO_LINE
                    || category == DefectCategory.DEFECT_TRIMMING
                    || category == DefectCategory.DEFECT_STAIN
                    || category == DefectCategory.DEFECT_FABRIC
                    || category == DefectCategory.DEFECT_OTHERS;
        }
    }

    private static class Styles {
        private static final Color GREEN_HEADER = new Color(96, 125, 107);
        private static final Color GREEN_LIGHT = new Color(238, 243, 238);
        private static final Color GREEN_GROUP = new Color(198, 215, 202);
        private static final Color BLUE_HEADER = new Color(220, 230, 239);
        private static final Color BEIGE = new Color(245, 240, 230);
        private static final Color GREEN_TOTAL = new Color(220, 232, 223);
        private static final Color YELLOW_LIGHT = new Color(255, 249, 196);
        private static final Color TITLE_PASTEL = new Color(168, 200, 178);
        private static final Color SUBTITLE_PASTEL = new Color(198, 215, 202);

        final CellStyle title;
        final CellStyle subtitle;
        final CellStyle period;
        final CellStyle groupTitle;
        final CellStyle header;
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
            title = create(workbook, TITLE_PASTEL,
                    IndexedColors.DARK_BLUE.getIndex(), 14, true,
                    HorizontalAlignment.CENTER);

            subtitle = create(workbook, SUBTITLE_PASTEL,
                    IndexedColors.DARK_BLUE.getIndex(), 10, true,
                    HorizontalAlignment.CENTER);

            period = create(workbook, GREEN_LIGHT,
                    IndexedColors.DARK_BLUE.getIndex(), 10, false,
                    HorizontalAlignment.CENTER);

            groupTitle = create(workbook, GREEN_GROUP,
                    IndexedColors.DARK_BLUE.getIndex(), 10, true,
                    HorizontalAlignment.LEFT);

            header = create(workbook, BLUE_HEADER,
                    IndexedColors.DARK_BLUE.getIndex(), 9, true,
                    HorizontalAlignment.CENTER);

            section = create(workbook, BLUE_HEADER,
                    IndexedColors.DARK_BLUE.getIndex(), 9, true,
                    HorizontalAlignment.LEFT);

            area = create(workbook, YELLOW_LIGHT,
                    IndexedColors.BLACK.getIndex(), 9, true,
                    HorizontalAlignment.LEFT);

            body = create(workbook, Color.WHITE,
                    IndexedColors.BLACK.getIndex(), 9, false,
                    HorizontalAlignment.LEFT);

            number = create(workbook, Color.WHITE,
                    IndexedColors.BLACK.getIndex(), 9, false,
                    HorizontalAlignment.CENTER);

            summaryLabel = create(workbook, BEIGE,
                    IndexedColors.DARK_BLUE.getIndex(), 9, true,
                    HorizontalAlignment.LEFT);

            summaryValue = create(workbook, BEIGE,
                    IndexedColors.DARK_BLUE.getIndex(), 9, true,
                    HorizontalAlignment.CENTER);

            total = create(workbook, GREEN_TOTAL,
                    IndexedColors.DARK_GREEN.getIndex(), 9, true,
                    HorizontalAlignment.CENTER);

            percent = create(workbook, BEIGE,
                    IndexedColors.DARK_BLUE.getIndex(), 9, true,
                    HorizontalAlignment.CENTER);

            percentTotal = create(workbook, GREEN_TOTAL,
                    IndexedColors.DARK_GREEN.getIndex(), 9, true,
                    HorizontalAlignment.CENTER);

            DataFormat format = workbook.createDataFormat();
            percent.setDataFormat(format.getFormat("0.00%"));
            percentTotal.setDataFormat(format.getFormat("0.00%"));
        }

        private CellStyle create(
                Workbook workbook,
                Color background,
                short fontColor,
                int fontSize,
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