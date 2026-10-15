
package com.garmentqc.system.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.garmentqc.system.dto.CuttingReportResponse;
import com.garmentqc.system.dto.CuttingReportResponse.DetailRow;
import com.garmentqc.system.dto.CuttingReportResponse.ProcessSummary;
import com.garmentqc.system.enums.CuttingPanel;

@Service
public class CuttingReportExcelService {

        private static final int COLUMN_COUNT = 19;

        private static final int PROCESS_COL = 0;
        private static final int BUYER_COL = 1;
        private static final int STYLE_COL = 2;
        private static final int COLOR_COL = 3;
        private static final int LOT_COL = 4;
        private static final int FIRST_PANEL_COL = 5;
        private static final int QTY_INSPECT_COL = 14;
        private static final int QTY_DEFECT_COL = 15;
        private static final int PERCENTAGE_COL = 16;
        private static final int OPERATOR_COL = 17;
        private static final int DETAIL_DEFECT_COL = 18;

        public byte[] export(CuttingReportResponse report) throws IOException {

                try (Workbook workbook = new XSSFWorkbook();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                        Sheet sheet = workbook.createSheet("Cutting QC Report");

                        CellStyle titleStyle = createTitleStyle(workbook);
                        CellStyle periodStyle = createPeriodStyle(workbook);
                        CellStyle headerStyle = createHeaderStyle(workbook);
                        CellStyle textStyle = createBodyStyle(workbook);
                        CellStyle numberStyle = createNumberStyle(workbook);
                        CellStyle percentageStyle = createPercentageStyle(workbook);
                        CellStyle subtotalStyle = createTotalStyle(workbook);
                        CellStyle grandTotalStyle = createGrandTotalStyle(workbook);
                        CellStyle subtotalCenterStyle = createCenteredTotalStyle(workbook, subtotalStyle);
                        CellStyle grandTotalCenterStyle = createCenteredTotalStyle(workbook, grandTotalStyle);
                        CellStyle subtotalPercentageStyle = createPercentageTotalStyle(workbook, subtotalCenterStyle);
                        CellStyle grandTotalPercentageStyle = createPercentageTotalStyle(workbook, grandTotalCenterStyle);

                        Row titleRow = sheet.createRow(0);
                        titleRow.setHeightInPoints(34);

                        Cell titleCell = titleRow.createCell(0);
                        titleCell.setCellValue("CUTTING QC REPORT");
                        titleCell.setCellStyle(titleStyle);

                        sheet.addMergedRegion(
                                        new CellRangeAddress(
                                                        0, 0, 0, COLUMN_COUNT - 1));

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                        Row periodRow = sheet.createRow(1);
                        periodRow.setHeightInPoints(25);

                        Cell periodCell = periodRow.createCell(0);
                        periodCell.setCellValue(
                                        "Report: " + report.getReportType()
                                                        + " | Period: "
                                                        + report.getStartDate().format(formatter)
                                                        + " - "
                                                        + report.getEndDate().format(formatter));

                        periodCell.setCellStyle(periodStyle);
                        sheet.addMergedRegion(
                                        new CellRangeAddress(
                                                        1, 1, 0, COLUMN_COUNT - 1));

                        Row headerRow = sheet.createRow(3);
                        headerRow.setHeightInPoints(34);

                        String[] headers = {
                                        "PROCESS CHECK",
                                        "BUYER",
                                        "STYLE",
                                        "COLOR",
                                        "LOT",
                                        "FRONT",
                                        "BACK",
                                        "SLEEVE",
                                        "BKYK",
                                        "COLLAR",
                                        "CUFF",
                                        "PCT",
                                        "PLT",
                                        "TAB",
                                        "QTY INSPECT",
                                        "QTY DEFECT",
                                        "% DEFECT",
                                        "OPERATOR NAME",
                                        "DETAIL DEFECT"
                        };

                        for (int i = 0; i < headers.length; i++) {
                                Cell cell = headerRow.createCell(i);
                                cell.setCellValue(headers[i]);
                                cell.setCellStyle(headerStyle);
                        }

                        int rowIndex = 4;

                        Map<CuttingPanel, Long> grandPanelTotals = createEmptyPanelTotals();
                        long grandQuantityInspect = 0;
                        long grandDefects = 0;

                        for (ProcessSummary process : report.getProcessSummaries()) {

                                for (DetailRow detail : process.getDetailRows()) {

                                        Row row = sheet.createRow(rowIndex++);
                                        row.setHeightInPoints(22);

                                        setText(
                                                        row, PROCESS_COL,
                                                        process.getProcessCheck().name(),
                                                        textStyle);

                                        setText(
                                                        row, BUYER_COL,
                                                        detail.getBuyerName(),
                                                        textStyle);

                                        setText(
                                                        row, STYLE_COL,
                                                        detail.getStyleCode(),
                                                        textStyle);

                                        setText(
                                                        row, COLOR_COL,
                                                        detail.getColorName(),
                                                        textStyle);

                                        setText(
                                                        row, LOT_COL,
                                                        detail.getLot(),
                                                        textStyle);

                                        for (CuttingPanel panel : CuttingPanel.values()) {

                                                int column = FIRST_PANEL_COL + panel.ordinal();

                                                long quantity = detail.getPanelQuantities()
                                                                .getOrDefault(panel, 0L);

                                                if (quantity > 0) {
                                                        setNumber(
                                                                        row, column, quantity, numberStyle);
                                                } else {
                                                        setText(
                                                                        row, column, "", textStyle);
                                                }

                                                grandPanelTotals.merge(
                                                                panel, quantity, Long::sum);
                                        }

                                        setNumber(
                                                        row, QTY_INSPECT_COL,
                                                        detail.getQuantityInspect(),
                                                        numberStyle);

                                        setNumber(
                                                        row, QTY_DEFECT_COL,
                                                        detail.getTotalDefects(),
                                                        numberStyle);

                                        setPercentage(
                                                        row, PERCENTAGE_COL,
                                                        detail.getDefectPercentage(),
                                                        percentageStyle);

                                        setText(
                                                        row, OPERATOR_COL,
                                                        detail.getOperatorName(),
                                                        textStyle);

                                        setText(
                                                        row, DETAIL_DEFECT_COL,
                                                        detail.getDefectTypeName(),
                                                        textStyle);

                                        grandQuantityInspect += detail.getQuantityInspect();
                                        grandDefects += detail.getTotalDefects();
                                }

                                Row subtotalRow = sheet.createRow(rowIndex++);
                                subtotalRow.setHeightInPoints(25);

                                Cell subtotalLabel = subtotalRow.createCell(PROCESS_COL);

                                subtotalLabel.setCellValue(
                                                "TOTAL " + process.getProcessCheck().name());

                                subtotalLabel.setCellStyle(subtotalCenterStyle);

                                for (int col = BUYER_COL; col < FIRST_PANEL_COL; col++) {

                                        setText(
                                                        subtotalRow, col, "",
                                                        subtotalCenterStyle);
                                }

                                for (CuttingPanel panel : CuttingPanel.values()) {

                                        int column = FIRST_PANEL_COL + panel.ordinal();

                                        long quantity = process.getPanelSummaries()
                                                        .getOrDefault(panel, 0L);

                                        if (quantity > 0) {
                                                setNumber(
                                                                subtotalRow, column, quantity,
                                                                subtotalCenterStyle);
                                        } else {
                                                setText(
                                                                subtotalRow, column, "",
                                                                subtotalCenterStyle);
                                        }
                                }

                                setNumber(
                                                subtotalRow, QTY_INSPECT_COL,
                                                process.getTotalQuantityInspect(),
                                                subtotalCenterStyle);

                                setNumber(
                                                subtotalRow, QTY_DEFECT_COL,
                                                process.getTotalDefects(),
                                                subtotalCenterStyle);

                                setPercentage(
                                                subtotalRow, PERCENTAGE_COL,
                                                process.getDefectPercentage(),
                                                subtotalPercentageStyle);

                                for (int col = OPERATOR_COL; col < COLUMN_COUNT; col++) {

                                        setText(
                                                        subtotalRow, col, "",
                                                        subtotalCenterStyle);
                                }

                                rowIndex++;
                        }

                        Row grandRow = sheet.createRow(rowIndex);
                        grandRow.setHeightInPoints(28);

                        Cell grandLabel = grandRow.createCell(PROCESS_COL);
                        grandLabel.setCellValue("GRAND TOTAL");
                        grandLabel.setCellStyle(grandTotalCenterStyle);

                        for (int col = BUYER_COL; col < FIRST_PANEL_COL; col++) {

                                setText(
                                                grandRow, col, "",
                                                grandTotalCenterStyle);
                        }

                        for (CuttingPanel panel : CuttingPanel.values()) {

                                int column = FIRST_PANEL_COL + panel.ordinal();

                                long quantity = grandPanelTotals
                                                .getOrDefault(panel, 0L);

                                if (quantity > 0) {
                                        setNumber(
                                                        grandRow, column, quantity,
                                                        grandTotalCenterStyle);
                                } else {
                                        setText(
                                                        grandRow, column, "",
                                                        grandTotalCenterStyle);
                                }
                        }

                        setNumber(
                                        grandRow, QTY_INSPECT_COL,
                                        grandQuantityInspect,
                                        grandTotalCenterStyle);

                        setNumber(
                                        grandRow, QTY_DEFECT_COL,
                                        grandDefects,
                                        grandTotalCenterStyle);

                        double grandPercentage = grandQuantityInspect <= 0
                                        ? 0.0
                                        : (double) grandDefects
                                                        / grandQuantityInspect * 100.0;

                        setPercentage(
                                        grandRow, PERCENTAGE_COL,
                                        grandPercentage,
                                        grandTotalPercentageStyle);

                        for (int col = OPERATOR_COL; col < COLUMN_COUNT; col++) {

                                setText(
                                                grandRow, col, "",
                                                grandTotalCenterStyle);
                        }

                        int[] widths = {
                                        24, 18, 20, 16, 16,
                                        11, 11, 11, 11, 11, 11, 11, 11, 11,
                                        15, 13, 12, 20, 24
                        };

                        for (int i = 0; i < widths.length; i++) {
                                sheet.setColumnWidth(i, widths[i] * 256);
                        }

                        sheet.createFreezePane(5, 4);

                        if (rowIndex > 4) {
                                sheet.setAutoFilter(
                                                new CellRangeAddress(
                                                                3, rowIndex - 1,
                                                                0, COLUMN_COUNT - 1));
                        }

                        sheet.setFitToPage(true);
                        sheet.getPrintSetup().setLandscape(true);
                        sheet.getPrintSetup().setFitWidth((short) 1);
                        sheet.getPrintSetup().setFitHeight((short) 0);
                        sheet.setRepeatingRows(
                                        new CellRangeAddress(3, 3, -1, -1));

                        workbook.write(output);
                        return output.toByteArray();
                }
        }

        private Map<CuttingPanel, Long> createEmptyPanelTotals() {
                Map<CuttingPanel, Long> totals = new EnumMap<>(CuttingPanel.class);
                for (CuttingPanel panel : CuttingPanel.values()) {
                        totals.put(panel, 0L);
                }

                return totals;
        }

        private void setText(Row row, int column, String value, CellStyle style) {
                Cell cell = row.createCell(column);
                cell.setCellValue(value == null ? "" : value);
                cell.setCellStyle(style);
        }

        private void setNumber(Row row, int column, long value, CellStyle style) {
                Cell cell = row.createCell(column);
                cell.setCellValue(value);
                cell.setCellStyle(style);
        }

        private void setPercentage(Row row, int column, double percentage, CellStyle style) {
                Cell cell = row.createCell(column);
                cell.setCellValue(percentage / 100.0);
                cell.setCellStyle(style);
        }

        private CellStyle createTitleStyle(Workbook workbook) {
                CellStyle style = createBodyStyle(workbook);

                Font font = workbook.createFont();
                font.setBold(true);
                font.setFontHeightInPoints((short) 16);
                font.setColor(IndexedColors.WHITE.getIndex());

                style.setFont(font);

                setFillColor(style, 96, 125, 107);

                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);

                return style;
        }

        private CellStyle createPeriodStyle(Workbook workbook) {
                CellStyle style = createBodyStyle(workbook);

                Font font = workbook.createFont();
                font.setFontHeightInPoints((short) 10);
                font.setColor(IndexedColors.DARK_BLUE.getIndex());

                style.setFont(font);

                setFillColor(style, 238, 243, 238);

                style.setAlignment(HorizontalAlignment.LEFT);
                style.setVerticalAlignment(VerticalAlignment.CENTER);

                return style;
        }

        private CellStyle createHeaderStyle(Workbook workbook) {
                CellStyle style = createBodyStyle(workbook);

                Font font = workbook.createFont();
                font.setBold(true);
                font.setFontHeightInPoints((short) 10);
                font.setColor(IndexedColors.DARK_BLUE.getIndex());

                style.setFont(font);

                setFillColor(style, 220, 230, 239);

                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setWrapText(true);

                return style;
        }

        private CellStyle createBodyStyle(Workbook workbook) {
                CellStyle style = workbook.createCellStyle();

                style.setVerticalAlignment(VerticalAlignment.CENTER);

                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
                style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
                style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
                style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
                style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

                return style;
        }

        private CellStyle createNumberStyle(Workbook workbook) {
                CellStyle style = createBodyStyle(workbook);
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

                return style;
        }

        private CellStyle createPercentageStyle(Workbook workbook) {
                CellStyle style = createBodyStyle(workbook);
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

                return style;
        }

        private CellStyle createTotalStyle(Workbook workbook) {
                CellStyle style = createBodyStyle(workbook);

                Font font = workbook.createFont();
                font.setBold(true);
                font.setColor(IndexedColors.DARK_BLUE.getIndex());

                style.setFont(font);
                setFillColor(style, 245, 240, 230);

                return style;
        }

        private CellStyle createGrandTotalStyle(Workbook workbook) {
                CellStyle style = createBodyStyle(workbook);

                Font font = workbook.createFont();
                font.setBold(true);
                font.setColor(IndexedColors.DARK_GREEN.getIndex());

                style.setFont(font);

                setFillColor(style, 220, 232, 223);

                return style;
        }

        private CellStyle createCenteredTotalStyle(Workbook workbook, CellStyle baseStyle) {
                CellStyle style = workbook.createCellStyle();
                style.cloneStyleFrom(baseStyle);

                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);

                return style;
        }

        private CellStyle createPercentageTotalStyle(Workbook workbook, CellStyle baseStyle) {
                CellStyle style = workbook.createCellStyle();
                style.cloneStyleFrom(baseStyle);

                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);

                style.setDataFormat(
                                workbook.createDataFormat().getFormat("0.00%"));

                return style;
        }

        private void setFillColor(CellStyle style, int red, int green, int blue) {
                XSSFColor color = new XSSFColor(
                                new Color(red, green, blue),
                                new DefaultIndexedColorMap());

                ((XSSFCellStyle) style).setFillForegroundColor(color);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
}
