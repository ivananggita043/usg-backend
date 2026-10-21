package com.garmentqc.system.service;

import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.InspectionRepository;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackingReportExcelService {

    private static final String PACKING = "PACKING";
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    private static final List<String> DEFECT_TYPES = List.of(
            "WRONG QTY",
            "WRONG SIZE",
            "WRONG GARMENT",
            "POOR FOLDING",
            "WRONG HANGTAG",
            "MISSING HANGTAG",
            "WRONG STICKER P/B",
            "MISSING STICKER P/B",
            "DAMAGE POLYBAG",
            "WRONG POLYBAG",
            "HANGER",
            "ACCESSORIES");

    private static final int HEADER_ROW = 5;
    private static final int FIRST_DATA_ROW = 6;
    private final InspectionRepository inspectionRepository;
    private final InspectionReportService inspectionReportService;

    public byte[] exportDailyPackingExcel(LocalDate date, User currentUser) throws IOException {
        String section = inspectionReportService.resolveSectionForUser(currentUser, PACKING);
        Instant start = date.atStartOfDay(ZONE_ID).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZONE_ID).toInstant();

        List<Inspection> inspections = inspectionRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)
                .stream()
                .filter(i -> i.getLine() != null)
                .filter(i -> i.getLine().getSection() != null)
                .filter(i -> PACKING.equals(i.getLine().getSection().name()))
                .filter(i -> section == null || section.isBlank() || PACKING.equalsIgnoreCase(section))
                .filter(i -> canViewInspection(i, currentUser))
                .sorted(Comparator.comparing(Inspection::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Packing QC Report");
            Styles styles = new Styles(workbook);

            int defectStartCol = 8;
            int notesCol = defectStartCol + (DEFECT_TYPES.size() * 2);
            int lastCol = notesCol;

            createTitle(sheet, 0, lastCol, "PT UNGARAN SARI GARMENT", styles.title, 30);
            createTitle(sheet, 1, lastCol, "DAILY REPORT QC COUNT & ACCURACY", styles.subtitle, 18);
            createTitle(sheet, 2, lastCol, "QC DEPARTMENT", styles.subtitle, 18);
            createTitle(sheet, 3, lastCol, "DEPT: DRESS 2", styles.subtitle, 18);
            createTitle(sheet, 4, lastCol, "REPORT DATE: " + date.format(DATE_FORMAT), styles.date, 22);
            createHeaders(sheet, styles, defectStartCol, notesCol);

            int rowIndex = FIRST_DATA_ROW;
            long totalInspect = 0;
            long totalGood = 0;
            long totalReject = 0;

            Map<String, Long> defectTotals = new LinkedHashMap<>();
            DEFECT_TYPES.forEach(name -> defectTotals.put(name, 0L));

            for (int i = 0; i < inspections.size(); i++) {
                Inspection inspection = inspections.get(i);
                int inspectQty = valueOrZero(inspection.getInspectQuantity());
                Map<String, Integer> defects = getDefects(inspection);

                int totalDefects = defects.values().stream()
                        .mapToInt(Integer::intValue)
                        .sum();

                int goodQty = Math.max(0, inspectQty - totalDefects);
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(20);
                int col = 0;

                setCell(row, col++, i + 1, styles.bodyNumber);
                setCell(row, col++, safe(inspection.getBuyerName()), styles.bodyNumber);
                setCell(row, col++, safe(inspection.getStyleCode()), styles.bodyNumber);
                setCell(row, col++, safe(inspection.getColorName()), styles.bodyNumber);
                setCell(row, col++, inspectQty, styles.bodyNumber);
                setCell(row, col++, goodQty, styles.bodyNumber);
                setCell(row, col++, totalDefects, styles.bodyNumber);

                double rejectPercentage = inspectQty <= 0
                        ? 0.0
                        : (double) totalDefects / inspectQty;
                setCell(row, col++, rejectPercentage, styles.percent);

                for (String defectName : DEFECT_TYPES) {
                    int qty = defects.getOrDefault(defectName, 0);
                    double percentage = inspectQty <= 0
                            ? 0.0
                            : (double) qty / inspectQty;

                    if (qty > 0) {
                        setCell(row, col++, qty, styles.bodyNumber);
                        setCell(row, col++, percentage, styles.percent);
                    } else {
                        setCell(row, col++, "", styles.bodyNumber);
                        setCell(row, col++, "", styles.percent);
                    }

                    defectTotals.merge(defectName, (long) qty, Long::sum);
                }

                setCell(row, col, safe(inspection.getComment()), styles.bodyNumber);

                totalInspect += inspectQty;
                totalGood += goodQty;
                totalReject += totalDefects;
            }

            Row totalRow = sheet.createRow(rowIndex);
            totalRow.setHeightInPoints(24);
            int col = 0;

            setCell(totalRow, col++, "TOTAL", styles.total);
            setCell(totalRow, col++, "", styles.total);
            setCell(totalRow, col++, "", styles.total);
            setCell(totalRow, col++, "", styles.total);

            setCell(totalRow, col++, totalInspect, styles.total);
            setCell(totalRow, col++, totalGood, styles.total);
            setCell(totalRow, col++, totalReject, styles.total);

            double totalRejectPercentage = totalInspect <= 0
                    ? 0.0
                    : (double) totalReject / totalInspect;
            setCell(totalRow, col++, totalRejectPercentage, styles.percentTotal);

            for (String defectName : DEFECT_TYPES) {
                long qty = defectTotals.getOrDefault(defectName, 0L);
                double percentage = totalInspect <= 0
                        ? 0.0
                        : (double) qty / totalInspect;

                if (qty > 0) {
                    setCell(totalRow, col++, qty, styles.total);
                    setCell(totalRow, col++, percentage, styles.percentTotal);
                } else {
                    setCell(totalRow, col++, "", styles.total);
                    setCell(totalRow, col++, "", styles.percentTotal);
                }
            }

            setCell(totalRow, col, "", styles.total);

            sheet.addMergedRegion(
                    new CellRangeAddress(rowIndex, rowIndex, 0, 3));

            sheet.setColumnWidth(0, 7 * 256);
            sheet.setColumnWidth(1, 16 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            sheet.setColumnWidth(3, 15 * 256);
            sheet.setColumnWidth(4, 11 * 256);
            sheet.setColumnWidth(5, 11 * 256);
            sheet.setColumnWidth(6, 11 * 256);
            sheet.setColumnWidth(7, 13 * 256);

            for (int c = defectStartCol; c < notesCol; c++) {
                sheet.setColumnWidth(c, 12 * 256);
            }

            sheet.setColumnWidth(notesCol, 30 * 256);

            sheet.createFreezePane(7, 0);

            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.setFitToPage(true);
            sheet.setHorizontallyCenter(true);

            sheet.setMargin(PageMargin.LEFT, 0.25);
            sheet.setMargin(PageMargin.RIGHT, 0.25);
            sheet.setMargin(PageMargin.TOP, 0.4);
            sheet.setMargin(PageMargin.BOTTOM, 0.4);

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void createHeaders(Sheet sheet, Styles styles, int defectStartCol, int notesCol) {
        Row header = sheet.createRow(HEADER_ROW);
        String[] fixedHeaders = {
                "NO", "BUYER", "STYLE", "COLOR", "QTY INSPECT", "QTY GOOD", "QTY REJECT", "% QTY REJECT"
        };

        for (int c = 0; c < fixedHeaders.length; c++) {
            setCell(header, c, fixedHeaders[c], styles.header);
        }

        int col = defectStartCol;

        for (String defectName : DEFECT_TYPES) {
            setCell(header, col++, defectName, styles.header);
            setCell(header, col++, "% " + defectName, styles.header);
        }

        setCell(header, notesCol, "NOTES", styles.header);
        header.setHeightInPoints(40);
    }

    private Map<String, Integer> getDefects(Inspection inspection) {
        Map<String, Integer> result = new LinkedHashMap<>();
        DEFECT_TYPES.forEach(name -> result.put(name, 0));
        if (inspection.getInspectionDefects() == null) {
            return result;
        }

        for (InspectionDefect item : inspection.getInspectionDefects()) {
            DefectType type = item.getDefectType();
            if (type == null || type.getName() == null) {
                continue;
            }

            String name = type.getName().trim().toUpperCase(Locale.ROOT);
            if (!result.containsKey(name)) {
                continue;
            }

            int quantity = valueOrZero(item.getQuantity());
            result.merge(name, quantity, Integer::sum);
        }

        return result;
    }

    private boolean canViewInspection(Inspection inspection, User currentUser) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }

        String role = currentUser.getRole().name();
        if ("SUPER_ADMIN".equals(role) || "ADMIN".equals(role)) {
            return true;
        }

        return "USER".equals(role) && inspection.getSubmittedBy() != null
                && currentUser.getId().equals(inspection.getSubmittedBy().getId());
    }

    private void createTitle(Sheet sheet, int rowIndex, int lastCol, String value, CellStyle style, float height) {
        Row row = sheet.createRow(rowIndex);
        for (int c = 0; c <= lastCol; c++) {
            setCell(row, c, "", style);
        }

        row.getCell(0).setCellValue(value);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastCol));
        row.setHeightInPoints(height);
    }

    private void setCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void setCell(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCell(Row row, int column, int value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCell(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static class Styles {
        private static final XSSFColor GREEN_DARK = rgb(94, 123, 108);
        private static final XSSFColor GREEN_PALE = rgb(238, 245, 238);
        private static final XSSFColor BLUE_HEADER = rgb(221, 235, 247);
        private static final XSSFColor BLUE_TEXT = rgb(20, 40, 160);
        private static final XSSFColor TOTAL_BG = rgb(220, 232, 224);
        private static final XSSFColor WHITE = rgb(255, 255, 255);
        private static final XSSFColor BLACK = rgb(0, 0, 0);
        private static final XSSFColor BORDER = rgb(191, 191, 191);
        private static final XSSFColor BORDER_LIGHT = rgb(235, 235, 235);

        final CellStyle title;
        final CellStyle subtitle;
        final CellStyle date;
        final CellStyle header;
        final CellStyle bodyNumber;
        final CellStyle total;
        final CellStyle percent;
        final CellStyle percentTotal;

        Styles(XSSFWorkbook workbook) {
            title = createStyle(workbook, GREEN_DARK, WHITE, 16, true, BORDER_LIGHT);
            subtitle = createStyle(workbook, GREEN_DARK, WHITE, 10, true, BORDER_LIGHT);
            date = createStyle(workbook, GREEN_PALE, BLUE_TEXT, 10, true, BORDER);
            header = createStyle(workbook, BLUE_HEADER, BLUE_TEXT, 9, true, BORDER);
            bodyNumber = createStyle(workbook, WHITE, BLACK, 10, false, BORDER);
            total = createStyle(workbook, TOTAL_BG, BLUE_TEXT, 10, true, BORDER);
            percent = createStyle(workbook, WHITE, BLACK, 10, false, BORDER);
            percentTotal = createStyle(workbook, TOTAL_BG, BLUE_TEXT, 10, true, BORDER);

            DataFormat format = workbook.createDataFormat();
            percent.setDataFormat(format.getFormat("0.00%"));
            percentTotal.setDataFormat(format.getFormat("0.00%"));
        }

        private static XSSFColor rgb(int r, int g, int b) {
            return new XSSFColor(
                    new byte[] { (byte) r, (byte) g, (byte) b }, null);
        }

        private static XSSFCellStyle createStyle(XSSFWorkbook workbook, XSSFColor background, XSSFColor fontColor,
                int fontSize, boolean bold, XSSFColor borderColor) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(background);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(true);

            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(borderColor);
            style.setBottomBorderColor(borderColor);
            style.setLeftBorderColor(borderColor);
            style.setRightBorderColor(borderColor);

            XSSFFont font = workbook.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) fontSize);
            font.setBold(bold);
            font.setColor(fontColor);
            style.setFont(font);

            return style;
        }
    }
}