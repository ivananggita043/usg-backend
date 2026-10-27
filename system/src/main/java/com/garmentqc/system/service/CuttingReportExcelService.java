
package com.garmentqc.system.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.garmentqc.system.enums.CuttingProcessCheck;

@Service
public class CuttingReportExcelService {

        private static final int COLUMN_COUNT = 10;

        private static final int PROCESS_COL = 0;
        private static final int BUYER_COL = 1;
        private static final int STYLE_COL = 2;
        private static final int COLOR_COL = 3;
        private static final int LOT_COL = 4;
        private static final int QTY_INSPECT_COL = 5;
        private static final int QTY_DEFECT_COL = 6;
        private static final int PERCENTAGE_COL = 7;
        private static final int OPERATOR_COL = 8;
        private static final int DETAIL_DEFECT_COL = 9;

        private static final int HEADER_ROW_INDEX = 5;
        private static final int FIRST_DATA_ROW_INDEX = HEADER_ROW_INDEX + 1;

        public byte[] export(CuttingReportResponse report) throws IOException {
                if (!"DAILY".equalsIgnoreCase(report.getReportType())) {
                        return exportPeriodReport(report);
                }

                try (Workbook workbook = new XSSFWorkbook();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                        Sheet sheet = workbook.createSheet("Cutting QC Report");

                        CellStyle titleStyle = createTitleStyle(workbook);
                        CellStyle subtitleStyle = createSubtitleStyle(workbook);
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
                        CellStyle grandTotalPercentageStyle = createPercentageTotalStyle(workbook,
                                        grandTotalCenterStyle);

                        createMergedRow(
                                        sheet, 0, COLUMN_COUNT,
                                        "PT UNGARAN SARI GARMENT", titleStyle, 34);

                        createMergedRow(
                                        sheet, 1, COLUMN_COUNT,
                                        "DAILY REPORT CUTTING SECTION", subtitleStyle, 22);

                        createMergedRow(
                                        sheet, 2, COLUMN_COUNT,
                                        "QC DEPARTMENT", subtitleStyle, 20);

                        createMergedRow(
                                        sheet, 3, COLUMN_COUNT,
                                        "DEPT: DRESS 2", subtitleStyle, 20);

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

                        String reportDate = report.getStartDate().equals(report.getEndDate())
                                        ? report.getStartDate().format(formatter)
                                        : report.getStartDate().format(formatter)
                                                        + " - "
                                                        + report.getEndDate().format(formatter);

                        createMergedRow(
                                        sheet, 4, COLUMN_COUNT,
                                        "REPORT DATE: " + reportDate, periodStyle, 24);

                        Row headerRow = sheet.createRow(HEADER_ROW_INDEX);
                        headerRow.setHeightInPoints(34);

                        String[] headers = {
                                        "PROCESS CHECK", "BUYER", "STYLE", "COLOR", "LOT",
                                        "QTY INSPECT", "QTY DEFECT", "% DEFECT",
                                        "OPERATOR NAME", "DETAIL DEFECT"
                        };

                        for (int i = 0; i < headers.length; i++) {
                                Cell cell = headerRow.createCell(i);
                                cell.setCellValue(headers[i]);
                                cell.setCellStyle(headerStyle);
                        }

                        int rowIndex = FIRST_DATA_ROW_INDEX;
                        long grandQuantityInspect = 0;
                        long grandDefects = 0;

                        for (ProcessSummary process : report.getProcessSummaries()) {
                                String processName = getProcessName(process.getProcessCheck());

                                for (DetailRow detail : process.getDetailRows()) {
                                        Row row = sheet.createRow(rowIndex++);
                                        row.setHeightInPoints(22);

                                        setText(row, PROCESS_COL, processName, textStyle);
                                        setText(row, BUYER_COL, detail.getBuyerName(), textStyle);
                                        setText(row, STYLE_COL, detail.getStyleCode(), textStyle);
                                        setText(row, COLOR_COL, detail.getColorName(), textStyle);
                                        setText(row, LOT_COL, detail.getLot(), textStyle);

                                        setNumber(row, QTY_INSPECT_COL,
                                                        detail.getQuantityInspect(), numberStyle);
                                        setNumber(row, QTY_DEFECT_COL,
                                                        detail.getQuantityDefect(), numberStyle);
                                        setPercentage(row, PERCENTAGE_COL,
                                                        detail.getDefectPercentage(), percentageStyle);

                                        setText(row, OPERATOR_COL,
                                                        detail.getOperatorName(), textStyle);
                                        setText(row, DETAIL_DEFECT_COL,
                                                        detail.getDefectTypeName(), textStyle);

                                        grandQuantityInspect += detail.getQuantityInspect();
                                        grandDefects += detail.getQuantityDefect();
                                }

                                Row subtotalRow = sheet.createRow(rowIndex++);
                                subtotalRow.setHeightInPoints(25);

                                Cell subtotalLabel = subtotalRow.createCell(PROCESS_COL);
                                subtotalLabel.setCellValue("TOTAL " + processName);
                                subtotalLabel.setCellStyle(subtotalCenterStyle);

                                for (int col = BUYER_COL; col < QTY_INSPECT_COL; col++) {
                                        setText(subtotalRow, col, "", subtotalCenterStyle);
                                }

                                setNumber(subtotalRow, QTY_INSPECT_COL,
                                                process.getTotalQuantityInspect(), subtotalCenterStyle);
                                setNumber(subtotalRow, QTY_DEFECT_COL,
                                                process.getTotalDefects(), subtotalCenterStyle);
                                setPercentage(subtotalRow, PERCENTAGE_COL,
                                                process.getDefectPercentage(), subtotalPercentageStyle);

                                for (int col = OPERATOR_COL; col < COLUMN_COUNT; col++) {
                                        setText(subtotalRow, col, "", subtotalCenterStyle);
                                }
                        }

                        int grandTotalRowIndex = rowIndex;
                        Row grandRow = sheet.createRow(grandTotalRowIndex);
                        grandRow.setHeightInPoints(28);

                        Cell grandLabel = grandRow.createCell(PROCESS_COL);
                        grandLabel.setCellValue("GRAND TOTAL");
                        grandLabel.setCellStyle(grandTotalCenterStyle);

                        for (int col = BUYER_COL; col < QTY_INSPECT_COL; col++) {
                                setText(grandRow, col, "", grandTotalCenterStyle);
                        }

                        setNumber(grandRow, QTY_INSPECT_COL,
                                        grandQuantityInspect, grandTotalCenterStyle);
                        setNumber(grandRow, QTY_DEFECT_COL,
                                        grandDefects, grandTotalCenterStyle);
                        setPercentage(grandRow, PERCENTAGE_COL,
                                        calculatePercentage(grandDefects, grandQuantityInspect),
                                        grandTotalPercentageStyle);

                        for (int col = OPERATOR_COL; col < COLUMN_COUNT; col++) {
                                setText(grandRow, col, "", grandTotalCenterStyle);
                        }

                        int[] widths = {
                                        24, 18, 20, 16, 16, 15, 13, 12, 20, 24
                        };

                        for (int i = 0; i < widths.length; i++) {
                                sheet.setColumnWidth(i, widths[i] * 256);
                        }

                        sheet.autoSizeColumn(DETAIL_DEFECT_COL);

                        int currentWidth = sheet.getColumnWidth(DETAIL_DEFECT_COL);
                        sheet.setColumnWidth(
                                        DETAIL_DEFECT_COL,
                                        Math.min(currentWidth + 2 * 256, 60 * 256));

                        sheet.createFreezePane(QTY_INSPECT_COL, FIRST_DATA_ROW_INDEX);

                        if (grandTotalRowIndex > HEADER_ROW_INDEX) {
                                sheet.setAutoFilter(new CellRangeAddress(
                                                HEADER_ROW_INDEX,
                                                grandTotalRowIndex - 1,
                                                0,
                                                COLUMN_COUNT - 1));
                        }

                        sheet.setFitToPage(true);
                        sheet.getPrintSetup().setLandscape(true);
                        sheet.getPrintSetup().setFitWidth((short) 1);
                        sheet.getPrintSetup().setFitHeight((short) 0);
                        sheet.setRepeatingRows(new CellRangeAddress(
                                        HEADER_ROW_INDEX, HEADER_ROW_INDEX, -1, -1));

                        workbook.write(output);
                        return output.toByteArray();
                }
        }

        private byte[] exportPeriodReport(CuttingReportResponse report)
                        throws IOException {

                try (Workbook workbook = new XSSFWorkbook();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                        Sheet sheet = workbook.createSheet("Cutting QC Report");

                        CellStyle titleStyle = createTitleStyle(workbook);
                        CellStyle subtitleStyle = createSubtitleStyle(workbook);
                        CellStyle periodStyle = createPeriodStyle(workbook);
                        CellStyle headerStyle = createHeaderStyle(workbook);
                        CellStyle bodyStyle = createBodyStyle(workbook);
                        CellStyle numberStyle = createNumberStyle(workbook);
                        CellStyle percentageStyle = createPercentageStyle(workbook);

                        CellStyle subtotalStyle = createCenteredTotalStyle(workbook, createTotalStyle(workbook));
                        CellStyle subtotalPercentageStyle = createPercentageTotalStyle(workbook, subtotalStyle);
                        CellStyle grandStyle = createCenteredTotalStyle(workbook, createGrandTotalStyle(workbook));
                        CellStyle grandPercentageStyle = createPercentageTotalStyle(workbook, grandStyle);

                        List<LocalDate> dates = new ArrayList<>();

                        for (LocalDate date = report.getStartDate(); !date.isAfter(report.getEndDate()); date = date
                                        .plusDays(1)) {
                                dates.add(date);
                        }

                        int dateStartCol = 2;
                        int totalCol = dateStartCol + dates.size();
                        int percentageCol = totalCol + 1;
                        int columnCount = percentageCol + 1;

                        createMergedRow(
                                        sheet, 0, columnCount,
                                        "PT UNGARAN SARI GARMENT", titleStyle, 34);

                        createMergedRow(
                                        sheet, 1, columnCount,
                                        report.getReportType() + " REPORT CUTTING SECTION",
                                        subtitleStyle, 22);

                        createMergedRow(
                                        sheet, 2, columnCount,
                                        "QC DEPARTMENT",
                                        subtitleStyle, 20);

                        createMergedRow(
                                        sheet, 3, columnCount,
                                        "DEPT: DRESS 2", subtitleStyle, 20);

                        createMergedRow(
                                        sheet, 4, columnCount,
                                        "PERIODE: " + report.getStartDate()
                                                        + " - " + report.getEndDate(),
                                        periodStyle, 24);

                        int headerRowIndex = 5;
                        int dateRowIndex = 6;
                        int firstDataRow = 7;

                        Row header = sheet.createRow(headerRowIndex);
                        Row dateHeader = sheet.createRow(dateRowIndex);

                        header.setHeightInPoints(28);
                        dateHeader.setHeightInPoints(24);

                        for (int col = 0; col < 2; col++) {
                                Cell top = header.createCell(col);
                                top.setCellValue(col == 0 ? "PROCESS CHECK" : "DETAIL DEFECT");
                                top.setCellStyle(headerStyle);

                                Cell bottom = dateHeader.createCell(col);
                                bottom.setCellStyle(headerStyle);

                                sheet.addMergedRegion(new CellRangeAddress(
                                                headerRowIndex, dateRowIndex, col, col));
                        }

                        Cell dateTitle = header.createCell(dateStartCol);
                        dateTitle.setCellValue("DATE");
                        dateTitle.setCellStyle(headerStyle);

                        for (int col = dateStartCol + 1; col < totalCol; col++) {
                                header.createCell(col).setCellStyle(headerStyle);
                        }

                        sheet.addMergedRegion(new CellRangeAddress(
                                        headerRowIndex, headerRowIndex,
                                        dateStartCol, totalCol - 1));

                        for (int i = 0; i < dates.size(); i++) {
                                Cell cell = dateHeader.createCell(dateStartCol + i);
                                cell.setCellValue(dates.get(i).getDayOfMonth());
                                cell.setCellStyle(headerStyle);
                        }

                        Cell totalHeader = header.createCell(totalCol);
                        totalHeader.setCellValue("TOTAL");
                        totalHeader.setCellStyle(headerStyle);
                        dateHeader.createCell(totalCol).setCellStyle(headerStyle);

                        sheet.addMergedRegion(new CellRangeAddress(
                                        headerRowIndex, dateRowIndex, totalCol, totalCol));

                        Cell percentageHeader = header.createCell(percentageCol);
                        percentageHeader.setCellValue("% DEFECT");
                        percentageHeader.setCellStyle(headerStyle);
                        dateHeader.createCell(percentageCol).setCellStyle(headerStyle);

                        sheet.addMergedRegion(new CellRangeAddress(
                                        headerRowIndex, dateRowIndex, percentageCol, percentageCol));

                        /*
                         * Struktur:
                         * process check -> defect type -> tanggal -> jumlah defect.
                         */
                        Map<CuttingProcessCheck, Map<String, Map<LocalDate, Long>>> processDefects = new LinkedHashMap<>();

                        Map<LocalDate, Long> grandInspectByDate = new TreeMap<>();

                        for (ProcessSummary process : report.getProcessSummaries()) {
                                Map<String, Map<LocalDate, Long>> defectMap = processDefects.computeIfAbsent(
                                                process.getProcessCheck(),
                                                key -> new LinkedHashMap<>());

                                for (DetailRow detail : process.getDetailRows()) {
                                        LocalDate date = detail.getInspectionDate();

                                        if (date == null
                                                        || date.isBefore(report.getStartDate())
                                                        || date.isAfter(report.getEndDate())) {
                                                continue;
                                        }

                                        String defectName = detail.getDefectTypeName() == null
                                                        || detail.getDefectTypeName().isBlank()
                                                                        ? "UNSPECIFIED"
                                                                        : detail.getDefectTypeName();

                                        Map<LocalDate, Long> dailyDefects = defectMap.computeIfAbsent(
                                                        defectName, key -> new TreeMap<>());

                                        dailyDefects.merge(
                                                        date, detail.getQuantityDefect(), Long::sum);

                                        grandInspectByDate.merge(
                                                        date, detail.getQuantityInspect(), Long::sum);
                                }
                        }

                        int rowIndex = firstDataRow;

                        Map<LocalDate, Long> grandDefectTotalsByDate = new TreeMap<>();
                        long grandDefectTotal = 0;

                        for (ProcessSummary process : report.getProcessSummaries()) {
                                Map<String, Map<LocalDate, Long>> defectMap = processDefects
                                                .get(process.getProcessCheck());

                                if (defectMap == null || defectMap.isEmpty()) {
                                        continue;
                                }

                                int processStartRow = rowIndex;
                                String processName = getProcessName(process.getProcessCheck());

                                long subtotalFromDates = 0;

                                for (Map.Entry<String, Map<LocalDate, Long>> entry : defectMap.entrySet()) {

                                        Row row = sheet.createRow(rowIndex++);
                                        row.setHeightInPoints(22);

                                        setText(row, 1, entry.getKey(), bodyStyle);

                                        long defectTotal = 0;

                                        for (int i = 0; i < dates.size(); i++) {
                                                LocalDate date = dates.get(i);
                                                long quantity = entry.getValue().getOrDefault(date, 0L);

                                                Cell cell = row.createCell(dateStartCol + i);

                                                if (quantity != 0) {
                                                        cell.setCellValue(quantity);
                                                }

                                                cell.setCellStyle(numberStyle);
                                                defectTotal += quantity;
                                        }

                                        setNumber(row, totalCol, defectTotal, numberStyle);

                                        setPercentage(
                                                        row,
                                                        percentageCol,
                                                        calculatePercentage(
                                                                        defectTotal, process.getTotalQuantityInspect()),
                                                        percentageStyle);

                                        subtotalFromDates += defectTotal;
                                }

                                int subtotalRowIndex = rowIndex;
                                Row subtotal = sheet.createRow(rowIndex++);
                                subtotal.setHeightInPoints(25);

                                setText(
                                                subtotal, 1,
                                                "TOTAL DEFECT " + processName,
                                                subtotalStyle);

                                long processTotalForAllDates = 0;

                                for (int i = 0; i < dates.size(); i++) {
                                        LocalDate date = dates.get(i);
                                        long dailyTotal = 0;

                                        for (Map<LocalDate, Long> values : defectMap.values()) {
                                                dailyTotal += values.getOrDefault(date, 0L);
                                        }

                                        if (dailyTotal != 0) {
                                                setNumber(
                                                                subtotal, dateStartCol + i,
                                                                dailyTotal, subtotalStyle);
                                        } else {
                                                setText(
                                                                subtotal, dateStartCol + i,
                                                                "", subtotalStyle);
                                        }

                                        grandDefectTotalsByDate.merge(
                                                        date, dailyTotal, Long::sum);

                                        processTotalForAllDates += dailyTotal;
                                }

                                setNumber(
                                                subtotal, totalCol,
                                                processTotalForAllDates, subtotalStyle);

                                setPercentage(
                                                subtotal, percentageCol,
                                                calculatePercentage(
                                                                processTotalForAllDates,
                                                                process.getTotalQuantityInspect()),
                                                subtotalPercentageStyle);

                                /*
                                 * PERBAIKAN UTAMA:
                                 * Isi sel paling atas sebelum melakukan merge.
                                 * Excel menampilkan nilai dari sel kiri-atas pada area merge.
                                 */
                                Row firstProcessRow = sheet.getRow(processStartRow);

                                if (firstProcessRow == null) {
                                        firstProcessRow = sheet.createRow(processStartRow);
                                }

                                Cell processCell = firstProcessRow.getCell(PROCESS_COL);

                                if (processCell == null) {
                                        processCell = firstProcessRow.createCell(PROCESS_COL);
                                }

                                processCell.setCellValue(processName);
                                processCell.setCellStyle(bodyStyle);

                                for (int r = processStartRow; r <= subtotalRowIndex; r++) {
                                        Row currentRow = sheet.getRow(r);

                                        if (currentRow == null) {
                                                currentRow = sheet.createRow(r);
                                        }

                                        Cell currentCell = currentRow.getCell(PROCESS_COL);

                                        if (currentCell == null) {
                                                currentCell = currentRow.createCell(PROCESS_COL);
                                        }

                                        currentCell.setCellStyle(bodyStyle);
                                }

                                if (subtotalRowIndex >= processStartRow) {
                                        sheet.addMergedRegion(new CellRangeAddress(
                                                        processStartRow, subtotalRowIndex,
                                                        PROCESS_COL, PROCESS_COL));
                                }

                                grandDefectTotal += subtotalFromDates;
                        }

                        Row grandDefectRow = sheet.createRow(rowIndex++);
                        grandDefectRow.setHeightInPoints(27);

                        setText(grandDefectRow, 0, "", grandStyle);
                        setText(grandDefectRow, 1, "GRAND TOTAL DEFECT", grandStyle);

                        for (int i = 0; i < dates.size(); i++) {
                                setNumber(
                                                grandDefectRow, dateStartCol + i,
                                                grandDefectTotalsByDate.getOrDefault(dates.get(i), 0L),
                                                grandStyle);
                        }

                        setNumber(grandDefectRow, totalCol, grandDefectTotal, grandStyle);

                        setPercentage(
                                        grandDefectRow, percentageCol,
                                        calculatePercentage(
                                                        grandDefectTotal, report.getTotalQuantityInspect()),
                                        grandPercentageStyle);

                        Row grandInspectRow = sheet.createRow(rowIndex++);
                        grandInspectRow.setHeightInPoints(27);

                        setText(grandInspectRow, 0, "", grandStyle);
                        setText(grandInspectRow, 1, "GRAND TOTAL INSPECT", grandStyle);

                        long grandInspectTotal = 0;

                        for (int i = 0; i < dates.size(); i++) {
                                long value = grandInspectByDate.getOrDefault(dates.get(i), 0L);

                                setNumber(
                                                grandInspectRow, dateStartCol + i,
                                                value, grandStyle);

                                grandInspectTotal += value;
                        }

                        setNumber(grandInspectRow, totalCol, grandInspectTotal, grandStyle);
                        setText(grandInspectRow, percentageCol, "", grandStyle);

                        Row percentageRow = sheet.createRow(rowIndex);
                        percentageRow.setHeightInPoints(27);

                        setText(percentageRow, 0, "", grandStyle);
                        setText(percentageRow, 1, "% DEFECT", grandStyle);

                        for (int i = 0; i < dates.size(); i++) {
                                LocalDate date = dates.get(i);

                                double percentage = calculatePercentage(
                                                grandDefectTotalsByDate.getOrDefault(date, 0L),
                                                grandInspectByDate.getOrDefault(date, 0L));

                                setPercentage(
                                                percentageRow, dateStartCol + i,
                                                percentage, grandPercentageStyle);
                        }

                        setPercentage(
                                        percentageRow, totalCol,
                                        calculatePercentage(grandDefectTotal, grandInspectTotal),
                                        grandPercentageStyle);

                        setText(percentageRow, percentageCol, "", grandStyle);

                        sheet.setColumnWidth(0, 24 * 256);
                        sheet.setColumnWidth(1, 30 * 256);

                        for (int col = dateStartCol; col < totalCol; col++) {
                                sheet.setColumnWidth(col, 10 * 256);
                        }

                        sheet.setColumnWidth(totalCol, 14 * 256);
                        sheet.setColumnWidth(percentageCol, 14 * 256);

                        sheet.createFreezePane(dateStartCol, firstDataRow);

                        sheet.setFitToPage(true);
                        sheet.getPrintSetup().setLandscape(true);
                        sheet.getPrintSetup().setFitWidth((short) 1);
                        sheet.getPrintSetup().setFitHeight((short) 0);

                        sheet.setRepeatingRows(new CellRangeAddress(
                                        headerRowIndex, dateRowIndex, -1, -1));

                        workbook.write(output);
                        return output.toByteArray();
                }
        }

        private String getProcessName(CuttingProcessCheck processCheck) {
                return processCheck == null ? "UNSPECIFIED" : processCheck.name();
        }

        private void createMergedRow(
                        Sheet sheet,
                        int rowIndex,
                        int columnCount,
                        String value,
                        CellStyle style,
                        float height) {

                Row row = sheet.createRow(rowIndex);
                row.setHeightInPoints(height);

                for (int col = 0; col < columnCount; col++) {
                        Cell cell = row.createCell(col);

                        if (col == 0) {
                                cell.setCellValue(value);
                        }

                        cell.setCellStyle(style);
                }

                sheet.addMergedRegion(new CellRangeAddress(
                                rowIndex, rowIndex, 0, columnCount - 1));
        }

        private void setText(
                        Row row,
                        int column,
                        String value,
                        CellStyle style) {

                Cell cell = row.getCell(column);

                if (cell == null) {
                        cell = row.createCell(column);
                }

                cell.setCellValue(value == null ? "" : value);
                cell.setCellStyle(style);
        }

        private void setNumber(
                        Row row,
                        int column,
                        long value,
                        CellStyle style) {

                Cell cell = row.getCell(column);

                if (cell == null) {
                        cell = row.createCell(column);
                }

                cell.setCellValue(value);
                cell.setCellStyle(style);
        }

        private void setPercentage(
                        Row row,
                        int column,
                        double percentage,
                        CellStyle style) {

                Cell cell = row.getCell(column);

                if (cell == null) {
                        cell = row.createCell(column);
                }

                cell.setCellValue(percentage / 100.0);
                cell.setCellStyle(style);
        }

        private double calculatePercentage(
                        long totalDefects,
                        long totalQuantityInspect) {

                if (totalQuantityInspect <= 0) {
                        return 0.0;
                }

                double percentage = (double) totalDefects / totalQuantityInspect * 100.0;

                return Math.round(percentage * 100.0) / 100.0;
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

        private CellStyle createSubtitleStyle(Workbook workbook) {
                CellStyle style = createBodyStyle(workbook);

                Font font = workbook.createFont();
                font.setBold(true);
                font.setFontHeightInPoints((short) 10);
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
                font.setBold(true);
                font.setFontHeightInPoints((short) 10);
                font.setColor(IndexedColors.DARK_BLUE.getIndex());

                style.setFont(font);
                setFillColor(style, 238, 243, 238);
                style.setAlignment(HorizontalAlignment.CENTER);
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

                style.setAlignment(HorizontalAlignment.CENTER);
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

                style.setDataFormat(
                                workbook.createDataFormat().getFormat("#,##0"));

                return style;
        }

        private CellStyle createPercentageStyle(Workbook workbook) {
                CellStyle style = createBodyStyle(workbook);

                style.setDataFormat(
                                workbook.createDataFormat().getFormat("0.00%"));

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

        private CellStyle createCenteredTotalStyle(
                        Workbook workbook,
                        CellStyle baseStyle) {

                CellStyle style = workbook.createCellStyle();
                style.cloneStyleFrom(baseStyle);

                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);

                return style;
        }

        private CellStyle createPercentageTotalStyle(
                        Workbook workbook,
                        CellStyle baseStyle) {

                CellStyle style = workbook.createCellStyle();
                style.cloneStyleFrom(baseStyle);

                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setDataFormat(
                                workbook.createDataFormat().getFormat("0.00%"));

                return style;
        }

        private void setFillColor(
                        CellStyle style,
                        int red,
                        int green,
                        int blue) {

                XSSFColor color = new XSSFColor(
                                new Color(red, green, blue),
                                new DefaultIndexedColorMap());

                ((XSSFCellStyle) style).setFillForegroundColor(color);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
}
