package com.garmentqc.system.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
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
import org.springframework.stereotype.Service;

import com.garmentqc.system.dto.FinalInspectionResponse;
import com.garmentqc.system.dto.FinalInspectionResponse.AccessoryItem;
import com.garmentqc.system.dto.FinalInspectionResponse.ColorItem;
import com.garmentqc.system.dto.FinalInspectionResponse.DefectItem;
import com.garmentqc.system.dto.FinalInspectionResponse.StyleItem;
import com.garmentqc.system.model.Accessory;
import com.garmentqc.system.repository.AccessoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinalInspectionExcelService {

        private final AccessoryRepository accessoryRepository;

        private static final String DARK_GREEN = "355E4B";
        private static final String TITLE_GREEN = "A8C8B2";
        private static final String LIGHT_GREEN = "C6D7CA";
        private static final String PALE_GREEN = "EAF1EC";

        private static final int COLS = 24;
        private static final int LAST_COL = COLS - 1;
        private static final int COL_WIDTH = (int) (5.3 * 256);
        private static final String DOC_NUMBER_INSPECTION = "NO. DOC : 005 / BAG / USG / UNG / QA / FORM / PRE & FINAL INSPECTION / 10 / 2026 ";
        private static final String DOC_NUMBER_IMPROVEMENT = "NO. DOC : 005 / BAG / USG / UNG / QA / FORM / PRE & FINAL INSPECTION / 10 / 2026 ";

        private static final List<String> STATUS_V = List.of(
                        "V", "OK", "PASS", "PASSED", "YES", "Y", "TRUE",
                        "CHECKED", "AVAILABLE", "COMPLETE");

        private static final List<String> STATUS_X = List.of(
                        "X", "NG", "NOT_OK", "NOT OK", "FAIL", "FAILED", "NO", "N",
                        "FALSE", "MISSING", "NOT_AVAILABLE");

        private static final class Styles {
                CellStyle company;
                CellStyle sectionTitle;
                CellStyle title;
                CellStyle section;
                CellStyle header;
                CellStyle label;
                CellStyle normal;
                CellStyle center;
                CellStyle boldCenter;
                CellStyle category;
                CellStyle defectArea;
                CellStyle release;
                CellStyle reject;
                CellStyle box;
                CellStyle docNo;
                CellStyle signTitle;
                CellStyle signValue;
                CellStyle signName;
        }

        public byte[] generateExcel(FinalInspectionResponse data) throws IOException {

                if (data == null) {
                        throw new IllegalArgumentException("Final inspection data cannot be null");
                }

                try (
                                Workbook workbook = new XSSFWorkbook();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                        Styles s = new Styles();

                        s.company = createPlainStyle(
                                        workbook, 20, true, HorizontalAlignment.CENTER);

                        s.sectionTitle = createPlainStyle(
                                        workbook, 11, true, HorizontalAlignment.LEFT);

                        s.title = createStyle(
                                        workbook, TITLE_GREEN, true, 15,
                                        HorizontalAlignment.CENTER);

                        s.section = createStyle(
                                        workbook, DARK_GREEN, true, 10,
                                        HorizontalAlignment.CENTER);

                        s.header = createStyle(
                                        workbook, LIGHT_GREEN, true, 9,
                                        HorizontalAlignment.CENTER);

                        s.label = createStyle(
                                        workbook, PALE_GREEN, true, 9,
                                        HorizontalAlignment.LEFT);
                        s.label.setWrapText(false);
                        s.label.setShrinkToFit(true);

                        s.normal = createStyle(
                                        workbook, "FFFFFF", false, 9,
                                        HorizontalAlignment.LEFT);

                        s.center = createStyle(
                                        workbook, "FFFFFF", false, 9,
                                        HorizontalAlignment.CENTER);

                        s.boldCenter = createStyle(
                                        workbook, "FFFFFF", true, 10,
                                        HorizontalAlignment.CENTER);

                        s.category = createStyle(
                                        workbook, PALE_GREEN, true, 9,
                                        HorizontalAlignment.CENTER);
                        s.category.setRotation((short) 90);

                        s.defectArea = createStyle(
                                        workbook, "FFFFFF", true, 9,
                                        HorizontalAlignment.CENTER);
                        s.defectArea.setVerticalAlignment(VerticalAlignment.CENTER);

                        s.release = createStyle(
                                        workbook, "D9EAD3", true, 22,
                                        HorizontalAlignment.CENTER);
                        setFontColor(s.release, "1E8E3E");

                        s.reject = createStyle(
                                        workbook, "F4CCCC", true, 22,
                                        HorizontalAlignment.CENTER);
                        setFontColor(s.reject, "C00000");

                        s.box = createStyle(
                                        workbook, "FFFFFF", true, 9,
                                        HorizontalAlignment.LEFT);
                        s.box.setVerticalAlignment(VerticalAlignment.TOP);

                        s.signTitle = createStyle(
                                        workbook, PALE_GREEN, true, 9,
                                        HorizontalAlignment.CENTER);

                        s.signValue = createStyle(
                                        workbook, "FFFFFF", true, 10,
                                        HorizontalAlignment.CENTER);
                        s.signValue.setVerticalAlignment(VerticalAlignment.CENTER);

                        s.signName = createStyle(
                                        workbook, "FFFFFF", false, 9,
                                        HorizontalAlignment.CENTER);
                        s.signName.setVerticalAlignment(VerticalAlignment.BOTTOM);

                        s.docNo = createPlainStyle(
                                        workbook, 8, true, HorizontalAlignment.LEFT);

                        createSingleSheet(workbook, data, s);

                        workbook.write(output);

                        return output.toByteArray();
                }
        }

        private void createSingleSheet(
                        Workbook workbook,
                        FinalInspectionResponse data,
                        Styles s) {

                Sheet sheet = workbook.createSheet("Inspection Report");

                for (int i = 0; i < COLS; i++) {
                        sheet.setColumnWidth(i, COL_WIDTH);
                }

                sheet.setDisplayGridlines(true);
                sheet.setDefaultRowHeightInPoints(18);

                int row = 0;

                mergeText(sheet, row, 0, row, LAST_COL,
                                DOC_NUMBER_INSPECTION, s.docNo);
                row++;

                mergeText(sheet, row, 0, row, LAST_COL,
                                "PT UNGARAN SARI GARMENTS", s.company);
                setHeight(sheet, row, 36);
                row++;

                mergeText(sheet, row, 0, row, 11,
                                "INSPECTION REPORT", s.title);
                mergeText(sheet, row, 12, row, 16,
                                "PURPOSE", s.header);
                mergeText(sheet, row, 17, row, 19,
                                "AQL LEVEL", s.header);
                mergeText(sheet, row, 20, row + 1, 23,
                                "SINGLE SAMPLING\nNORMAL INSPECTION", s.header);
                setHeight(sheet, row, 26);
                row++;

                mergeText(sheet, row, 0, row, 2,
                                "DATE", s.label);
                mergeText(sheet, row, 3, row, 11,
                                formatDate(data.getInspectionDate()), s.normal);
                mergeText(sheet, row, 12, row, 16,
                                value(data.getPurposeLabel()), s.boldCenter);
                mergeText(sheet, row, 17, row, 19,
                                formatAql(data.getAqlLevel()), s.boldCenter);
                setHeight(sheet, row, 24);
                row++;

                writeInfoRow(sheet, row++,
                                "BUYER", value(data.getBuyerName()),
                                "QTY ORDER", getTotalOrder(data),
                                "PO", value(data.getPoNumber()),
                                s);

                writeInfoRow(sheet, row++,
                                "STYLE", getStyles(data),
                                "READY QTY", getTotalReady(data),
                                "DELIVERY", formatDate(data.getDeliveryDate()),
                                s);

                writeInfoRow(sheet, row++,
                                "LOT / BATCH", value(data.getLotBatchSize()),
                                "SAMPLE SIZE", value(data.getSamplingSampleSize()),
                                "SAMPLING CODE", value(data.getSamplingCodeLetter()),
                                s);

                mergeText(sheet, row, 0, row, 2,
                                "CONTENT FABRIC", s.label);
                mergeText(sheet, row, 3, row, LAST_COL,
                                value(data.getContentFabric()), s.normal);
                row++;

                mergeText(sheet, row, 0, row, 1, "NO", s.header);
                mergeText(sheet, row, 2, row, 5, "COLOR", s.header);
                mergeText(sheet, row, 6, row, 8, "QTY ORDER", s.header);
                mergeText(sheet, row, 9, row, 11, "QTY READY", s.header);

                mergeText(sheet, row, 12, row, 12, "NO", s.header);
                mergeText(sheet, row, 13, row, 17, "COLOR", s.header);
                mergeText(sheet, row, 18, row, 20, "QTY ORDER", s.header);
                mergeText(sheet, row, 21, row, 23, "QTY READY", s.header);
                row++;

                List<ColorItem> colorList = new ArrayList<>();

                if (data.getStyles() != null) {
                        for (StyleItem style : data.getStyles()) {
                                if (style.getColors() != null) {
                                        colorList.addAll(style.getColors());
                                }
                        }
                }

                if (colorList.isEmpty() && data.getColors() != null) {
                        colorList.addAll(data.getColors());
                }

                int colorRows = Math.max(3, (colorList.size() + 1) / 2);

                for (int r = 0; r < colorRows; r++) {

                        ColorItem left = r < colorList.size()
                                        ? colorList.get(r)
                                        : null;

                        int rightIndex = colorRows + r;

                        ColorItem right = rightIndex < colorList.size()
                                        ? colorList.get(rightIndex)
                                        : null;

                        mergeText(sheet, row, 0, row, 1, r + 1, s.center);
                        mergeText(sheet, row, 2, row, 5,
                                        left != null ? value(left.getColorName()) : "", s.center);
                        mergeText(sheet, row, 6, row, 8,
                                        left != null ? (Object) number(left.getQtyOrder()) : "",
                                        s.center);
                        mergeText(sheet, row, 9, row, 11,
                                        left != null ? (Object) number(left.getQtyReady()) : "",
                                        s.center);

                        mergeText(sheet, row, 12, row, 12, rightIndex + 1, s.center);
                        mergeText(sheet, row, 13, row, 17,
                                        right != null ? value(right.getColorName()) : "", s.center);
                        mergeText(sheet, row, 18, row, 20,
                                        right != null ? (Object) number(right.getQtyOrder()) : "",
                                        s.center);
                        mergeText(sheet, row, 21, row, 23,
                                        right != null ? (Object) number(right.getQtyReady()) : "",
                                        s.center);

                        row++;
                }

                mergeText(sheet, row, 0, row, 1, "ART", s.header);
                mergeText(sheet, row, 2, row, 7, "ACCESSORIES", s.header);
                mergeText(sheet, row, 8, row, 9, "V", s.header);
                mergeText(sheet, row, 10, row, 11, "X", s.header);

                mergeText(sheet, row, 12, row, 12, "NO", s.header);
                mergeText(sheet, row, 13, row, 16, "DEFECT AREA", s.header);
                mergeText(sheet, row, 17, row, 19, "DEFECT TYPE", s.header);
                mergeText(sheet, row, 20, row, 21, "MAJOR", s.header);
                mergeText(sheet, row, 22, row, 23, "MINOR", s.header);
                row++;

                List<AccessoryItem> accessories = getAllAccessories(data);

                List<DefectItem> defects = data.getDefects() != null
                                ? new ArrayList<>(data.getDefects())
                                : new ArrayList<>();

                defects.sort(
                                Comparator.comparing(
                                                DefectItem::getDefectAreaName,
                                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                                                .thenComparing(
                                                                DefectItem::getDefectName,
                                                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

                int maxRows = Math.max(accessories.size(), defects.size());

                int tableStartRow = row;

                String previousCategory = null;
                int categoryNo = 0;

                for (int i = 0; i < maxRows; i++) {

                        if (i < accessories.size()) {

                                AccessoryItem accessory = accessories.get(i);

                                String category = value(accessory.getCategory());

                                boolean newCategory = !category.equals(previousCategory);

                                if (newCategory) {
                                        previousCategory = category;
                                        categoryNo = 0;
                                }

                                categoryNo++;

                                mergeText(sheet, row, 0, row, 0,
                                                newCategory ? category : "", s.category);

                                mergeText(sheet, row, 1, row, 1,
                                                categoryNo, s.center);

                                mergeText(sheet, row, 2, row, 7,
                                                value(accessory.getAccessoryName()), s.normal);

                                writeStatus(sheet, row, accessory.getStatus(), s.center);

                        } else {

                                mergeText(sheet, row, 0, row, 0, "", s.category);
                                mergeText(sheet, row, 1, row, 1, "", s.center);
                                mergeText(sheet, row, 2, row, 7, "", s.normal);
                                mergeText(sheet, row, 8, row, 9, "", s.center);
                                mergeText(sheet, row, 10, row, 11, "", s.center);
                        }

                        mergeText(sheet, row, 12, row, 12, i + 1, s.center);

                        if (i < defects.size()) {

                                DefectItem defect = defects.get(i);

                                String severity = value(defect.getSeverity());
                                int quantity = number(defect.getQuantity());

                                mergeText(sheet, row, 17, row, 19,
                                                value(defect.getDefectName()), s.normal);

                                mergeText(sheet, row, 20, row, 21,
                                                "MAJOR".equals(severity) ? (Object) quantity : "",
                                                s.center);

                                mergeText(sheet, row, 22, row, 23,
                                                "MINOR".equals(severity) ? (Object) quantity : "",
                                                s.center);

                        } else {

                                mergeText(sheet, row, 13, row, 16, "", s.normal);

                                mergeText(sheet, row, 17, row, 19, "", s.normal);
                                mergeText(sheet, row, 20, row, 21, "", s.center);
                                mergeText(sheet, row, 22, row, 23, "", s.center);
                        }

                        row++;
                }

                mergeCategoryRuns(sheet, accessories, tableStartRow);
                mergeDefectAreaRuns(sheet, defects, tableStartRow, s);
                String cartonSampling = value(data.getCartonSampling()).trim();

                mergeText(sheet, row, 0, row, 7,
                                ("CARTON SAMPLING : " + cartonSampling).trim(),
                                s.header);
                String totalInspected = value(data.getTotalInspected()).trim();

                mergeText(sheet, row, 8, row, 12,
                                ("TOTAL INSPECT : " + totalInspected).trim(),
                                s.header);

                mergeText(sheet, row, 12, row, 12,
                                value(data.getTotalInspected()), s.center);

                mergeText(sheet, row, 13, row, 19,
                                "TOTAL DEFECT", s.header);

                mergeText(sheet, row, 20, row, 21,
                                value(data.getTotalMajor()), s.center);

                mergeText(sheet, row, 22, row, 23,
                                value(data.getTotalMinor()), s.center);

                setHeight(sheet, row, 22);
                row++;

                mergeText(sheet, row, 0, row, 16,
                                "ADDITIONAL COMMENT", s.header);

                mergeText(sheet, row, 17, row, 23,
                                "INSPECTION RESULT", s.header);

                row++;

                mergeText(sheet, row, 0, row + 2, 16,
                                value(data.getAdditionalComment()), s.normal);

                String result = value(data.getInspectionResult());

                CellStyle resultStyle;
                String resultLabel;

                switch (result) {

                        case "SHIPMENT_RELEASE":
                                resultStyle = s.release;
                                resultLabel = "SHIPMENT RELEASE";
                                break;

                        case "SHIPMENT_REJECT":
                                resultStyle = s.reject;
                                resultLabel = "SHIPMENT REJECTED";
                                break;

                        default:
                                resultStyle = s.normal;
                                resultLabel = "PENDING";
                                break;
                }

                mergeText(sheet, row, 17, row + 2, 23,
                                resultLabel, resultStyle);

                for (int r = row; r <= row + 2; r++) {
                        setHeight(sheet, r, 26);
                }

                row += 3;

                String inspectedBy = value(data.getInspectedBy()).trim();
                String finishingIncharge = value(data.getFinishingIncharge()).trim();

                mergeText(sheet, row, 0, row, 8, "INSPECTED BY", s.signTitle);
                mergeText(sheet, row, 9, row, 16, "FINISHING INCHARGE", s.signTitle);
                mergeText(sheet, row, 17, row, 23, "QA DEPT", s.signTitle);
                setHeight(sheet, row, 20);

                mergeText(sheet, row + 1, 0, row + 2, 8, inspectedBy, s.signValue);
                mergeText(sheet, row + 1, 9, row + 2, 16, finishingIncharge, s.signValue);
                mergeText(sheet, row + 1, 17, row + 2, 23, "DRESS 2", s.signValue);

                setHeight(sheet, row + 1, 22);
                setHeight(sheet, row + 2, 22);

                row += 3;

                sheet.setRowBreak(row - 1);

                writeImprovementForm(sheet, data, row, s);

                sheet.setFitToPage(true);
                sheet.setHorizontallyCenter(true);

                PrintSetup printSetup = sheet.getPrintSetup();
                printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
                printSetup.setLandscape(false);
                printSetup.setFitWidth((short) 1);
                printSetup.setFitHeight((short) 0);

                sheet.setMargin(Sheet.LeftMargin, 0.4);
                sheet.setMargin(Sheet.RightMargin, 0.4);
                sheet.setMargin(Sheet.TopMargin, 0.5);
                sheet.setMargin(Sheet.BottomMargin, 0.5);
        }

        private void mergeDefectAreaRuns(
                        Sheet sheet,
                        List<DefectItem> defects,
                        int startRow,
                        Styles s) {

                if (defects == null || defects.isEmpty()) {
                        return;
                }

                int runStart = 0;

                while (runStart < defects.size()) {

                        int runEnd = runStart;

                        while (runEnd + 1 < defects.size()
                                        && sameDefectArea(
                                                        defects.get(runStart),
                                                        defects.get(runEnd + 1))) {
                                runEnd++;
                        }

                        int firstRow = startRow + runStart;
                        int lastRow = startRow + runEnd;

                        mergeText(
                                        sheet,
                                        firstRow, 13,
                                        lastRow, 16,
                                        value(defects.get(runStart).getDefectAreaName()),
                                        s.defectArea);

                        runStart = runEnd + 1;
                }
        }

        private boolean sameDefectArea(
                        DefectItem first,
                        DefectItem second) {

                String firstId = value(first.getDefectAreaId());
                String secondId = value(second.getDefectAreaId());

                if (!firstId.isBlank() && !secondId.isBlank()) {
                        return firstId.equals(secondId);
                }

                String firstName = value(first.getDefectAreaName());
                String secondName = value(second.getDefectAreaName());

                if (firstName.isBlank() || secondName.isBlank()) {
                        return false;
                }

                return firstName.equalsIgnoreCase(secondName);
        }

        private int writeImprovementForm(
                        Sheet sheet,
                        FinalInspectionResponse data,
                        int row,
                        Styles s) {

                mergeText(sheet, row, 0, row, LAST_COL,
                                DOC_NUMBER_IMPROVEMENT, s.docNo);
                row++;

                mergeText(sheet, row, 0, row, LAST_COL,
                                "IMPROVEMENT FORM", s.company);
                setHeight(sheet, row, 36);
                row++;

                String[][] meta = {
                                { "BUYER", value(data.getBuyerName()) },
                                { "STYLE", getStyles(data) },
                                { "PO", value(data.getPoNumber()) },
                                { "COLOR", getColors(data) },
                                { "INSPECTION DATE", formatDate(data.getInspectionDate()) }
                };

                for (String[] item : meta) {

                        mergeText(sheet, row, 0, row, 2, item[0], s.label);
                        mergeText(sheet, row, 3, row, 9, item[1], s.normal);

                        if ("COLOR".equals(item[0])) {
                                setHeight(sheet, row, 32);
                        }

                        row++;
                }

                row++;

                mergeText(sheet, row, 0, row, LAST_COL,
                                "A. DETAIL QTY", s.sectionTitle);
                row++;

                mergeText(sheet, row, 0, row, 5, "SIZE QTY", s.header);

                for (int c = 6; c <= 17; c++) {
                        mergeText(sheet, row, c, row, c, "", s.header);
                }

                mergeText(sheet, row, 18, row, 23, "TOTAL", s.header);
                setHeight(sheet, row, 26);
                row++;

                mergeText(sheet, row, 0, row, 5, "QTY ORDER", s.label);

                for (int c = 6; c <= 17; c++) {
                        mergeText(sheet, row, c, row, c, "", s.center);
                }

                mergeText(sheet, row, 18, row, 23, "", s.center);
                row++;

                mergeText(sheet, row, 0, row + 1, 3, "QTY PACK", s.label);
                mergeText(sheet, row, 4, row, 5, "PCS", s.label);
                mergeText(sheet, row + 1, 4, row + 1, 5, "%", s.label);

                for (int r = row; r <= row + 1; r++) {

                        for (int c = 6; c <= 17; c++) {
                                mergeText(sheet, r, c, r, c, "", s.center);
                        }

                        mergeText(sheet, r, 18, r, 23, "", s.center);
                }

                row += 3;

                mergeText(sheet, row, 0, row, LAST_COL,
                                "B. DETAIL IMPROVEMENT", s.sectionTitle);
                row++;

                mergeText(sheet, row, 0, row + 1, 5, "DETAIL", s.header);
                mergeText(sheet, row, 6, row, 17, "DATE INSPECTION", s.header);
                mergeText(sheet, row, 18, row + 1, 23, "IMPROVEMENT", s.header);
                row++;

                for (int k = 0; k < 4; k++) {
                        mergeText(sheet, row, 6 + (k * 3), row, 8 + (k * 3),
                                        "", s.header);
                }

                row++;

                writeImprovementRow(sheet, row++, "Qty Inspect", s.normal, s);
                writeImprovementRow(sheet, row++, "Qty Defect", s.normal, s);
                writeImprovementRow(sheet, row++, "% Defect", s.normal, s);

                mergeText(sheet, row, 0, row, LAST_COL,
                                "Detail Defect :", s.normal);
                row++;

                for (int n = 1; n <= 10; n++) {
                        writeImprovementRow(sheet, row++, "     " + n + ".", s.normal, s);
                }

                writeImprovementRow(sheet, row++, "", s.normal, s);
                writeImprovementRow(sheet, row++, "", s.normal, s);

                writeImprovementRow(sheet, row++, "TOTAL", s.label, s);

                row += 2;

                mergeText(sheet, row, 0, row, 11, "INSPECTED BY", s.header);
                mergeText(sheet, row, 12, row, 23, "QA DEPT", s.header);
                row++;

                String signLine = "(                                        )\n";

                mergeText(sheet, row, 0, row + 3, 11, signLine, s.signName);
                mergeText(sheet, row, 12, row + 3, 23, signLine, s.signName);

                for (int r = row; r <= row + 3; r++) {
                        setHeight(sheet, r, 26);
                }

                return row + 4;
        }

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.US);

        private String formatDate(Object date) {

                if (date == null) {
                        return "";
                }

                if (date instanceof LocalDate localDate) {
                        return localDate.format(DATE_FORMATTER);
                }

                if (date instanceof LocalDateTime localDateTime) {
                        return localDateTime.toLocalDate().format(DATE_FORMATTER);
                }

                if (date instanceof OffsetDateTime offsetDateTime) {
                        return offsetDateTime.toLocalDate().format(DATE_FORMATTER);
                }

                String text = date.toString().trim();

                if (text.isEmpty()) {
                        return "";
                }

                try {
                        return LocalDate.parse(text.substring(0, 10))
                                        .format(DATE_FORMATTER);
                } catch (Exception e) {
                        return text;
                }
        }

        private String formatAql(Object aql) {

                if (aql == null) {
                        return "";
                }

                String text = aql.toString().trim();

                if (text.isEmpty()) {
                        return "";
                }

                try {
                        return new BigDecimal(text)
                                        .stripTrailingZeros()
                                        .toPlainString();
                } catch (NumberFormatException e) {
                        return text;
                }
        }

        private List<AccessoryItem> getAllAccessories(
                        FinalInspectionResponse data) {

                List<AccessoryItem> selectedAccessories = data.getAccessories() != null
                                ? data.getAccessories()
                                : List.of();

                Map<String, AccessoryItem> selectedMap = new HashMap<>();

                for (AccessoryItem item : selectedAccessories) {
                        if (item.getAccessoryId() != null) {
                                selectedMap.put(item.getAccessoryId(), item);
                        }
                }

                List<Accessory> masterAccessories = accessoryRepository
                                .findByActiveTrueOrderByCategoryAscSortOrderAscNameAsc();

                List<AccessoryItem> result = new ArrayList<>();

                Set<String> processedIds = new HashSet<>();

                for (Accessory master : masterAccessories) {

                        AccessoryItem selected = selectedMap.get(master.getId());

                        AccessoryItem item = new AccessoryItem();

                        item.setId(selected != null ? selected.getId() : null);
                        item.setAccessoryId(master.getId());
                        item.setAccessoryName(master.getName());

                        item.setCategory(
                                        master.getCategory() != null
                                                        ? master.getCategory().name()
                                                        : null);

                        item.setStatus(
                                        selected != null
                                                        ? selected.getStatus()
                                                        : null);

                        result.add(item);

                        processedIds.add(master.getId());
                }

                for (AccessoryItem selected : selectedAccessories) {

                        if (selected.getAccessoryId() == null
                                        || processedIds.contains(selected.getAccessoryId())) {
                                continue;
                        }

                        result.add(selected);
                }

                return result;
        }

        private void writeImprovementRow(
                        Sheet sheet,
                        int row,
                        String label,
                        CellStyle labelCellStyle,
                        Styles s) {

                mergeText(sheet, row, 0, row, 5, label, labelCellStyle);

                for (int k = 0; k < 4; k++) {
                        mergeText(sheet, row, 6 + (k * 3), row, 8 + (k * 3),
                                        "", s.normal);
                }

                mergeText(sheet, row, 18, row, 23, "", s.normal);
        }

        private CellStyle createStyle(
                        Workbook workbook,
                        String color,
                        boolean bold,
                        int fontSize,
                        HorizontalAlignment alignment) {

                XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();

                Font font = workbook.createFont();

                font.setFontName("Arial");
                font.setFontHeightInPoints((short) fontSize);
                font.setBold(bold);

                if (DARK_GREEN.equals(color)) {
                        font.setColor(IndexedColors.WHITE.getIndex());
                }

                style.setFont(font);
                style.setAlignment(alignment);
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setWrapText(true);

                XSSFColor fillColor = new XSSFColor(
                                java.awt.Color.decode("#" + color),
                                new DefaultIndexedColorMap());

                style.setFillForegroundColor(fillColor);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                BorderStyle border = BorderStyle.THIN;

                style.setBorderTop(border);
                style.setBorderBottom(border);
                style.setBorderLeft(border);
                style.setBorderRight(border);

                return style;
        }

        private void setFontColor(CellStyle style, String hex) {

                XSSFFont font = ((XSSFCellStyle) style).getFont();

                font.setColor(new XSSFColor(
                                java.awt.Color.decode("#" + hex),
                                new DefaultIndexedColorMap()));
        }

        private CellStyle createPlainStyle(
                        Workbook workbook,
                        int fontSize,
                        boolean bold,
                        HorizontalAlignment alignment) {

                CellStyle style = workbook.createCellStyle();

                Font font = workbook.createFont();

                font.setFontName("Arial");
                font.setFontHeightInPoints((short) fontSize);
                font.setBold(bold);

                style.setFont(font);
                style.setAlignment(alignment);
                style.setVerticalAlignment(VerticalAlignment.CENTER);

                return style;
        }

        private void writeInfoRow(
                        Sheet sheet,
                        int row,
                        String label1, String value1,
                        String label2, String value2,
                        String label3, String value3,
                        Styles s) {

                mergeText(sheet, row, 0, row, 2, label1, s.label);
                mergeText(sheet, row, 3, row, 7, value1, s.normal);

                mergeText(sheet, row, 8, row, 10, label2, s.label);
                mergeText(sheet, row, 11, row, 15, value2, s.normal);

                mergeText(sheet, row, 16, row, 18, label3, s.label);
                mergeText(sheet, row, 19, row, 23, value3, s.normal);
        }

        private void writeStatus(
                        Sheet sheet,
                        int row,
                        Object status,
                        CellStyle style) {

                String text = value(status);

                String key = text.trim().toUpperCase();

                if (key.isEmpty()) {

                        mergeText(sheet, row, 8, row, 9, "", style);
                        mergeText(sheet, row, 10, row, 11, "", style);

                } else if (STATUS_V.contains(key)) {

                        mergeText(sheet, row, 8, row, 9, "V", style);
                        mergeText(sheet, row, 10, row, 11, "", style);

                } else if (STATUS_X.contains(key)) {

                        mergeText(sheet, row, 8, row, 9, "", style);
                        mergeText(sheet, row, 10, row, 11, "X", style);

                } else {

                        mergeText(sheet, row, 8, row, 11, text, style);
                }
        }

        private void mergeCategoryRuns(
                        Sheet sheet,
                        List<AccessoryItem> accessories,
                        int startRow) {

                int runStart = 0;

                for (int i = 1; i <= accessories.size(); i++) {

                        boolean endOfRun = i == accessories.size()
                                        || !value(accessories.get(i).getCategory())
                                                        .equals(value(accessories.get(runStart).getCategory()));

                        if (endOfRun) {

                                int firstRow = startRow + runStart;
                                int lastRow = startRow + i - 1;

                                if (lastRow > firstRow) {
                                        sheet.addMergedRegion(
                                                        new CellRangeAddress(firstRow, lastRow, 0, 0));
                                }

                                runStart = i;
                        }
                }
        }

        private void setHeight(Sheet sheet, int rowIndex, float points) {
                getRow(sheet, rowIndex).setHeightInPoints(points);
        }

        private void mergeText(
                        Sheet sheet,
                        int firstRow,
                        int firstCol,
                        int lastRow,
                        int lastCol,
                        Object value,
                        CellStyle style) {

                if (firstRow != lastRow || firstCol != lastCol) {

                        sheet.addMergedRegion(
                                        new CellRangeAddress(
                                                        firstRow,
                                                        lastRow,
                                                        firstCol,
                                                        lastCol));
                }

                for (int r = firstRow; r <= lastRow; r++) {

                        Row row = getRow(sheet, r);

                        for (int c = firstCol; c <= lastCol; c++) {

                                Cell cell = row.getCell(c);

                                if (cell == null) {
                                        cell = row.createCell(c);
                                }

                                cell.setCellStyle(style);
                        }
                }

                setCell(
                                getRow(sheet, firstRow),
                                firstCol,
                                value == null ? "" : value,
                                style);
        }

        private void setCell(
                        Row row,
                        int column,
                        Object value,
                        CellStyle style) {

                Cell cell = row.getCell(column);

                if (cell == null) {
                        cell = row.createCell(column);
                }

                cell.setCellStyle(style);

                if (value == null) {

                        cell.setBlank();

                } else if (value instanceof Number number) {

                        cell.setCellValue(number.doubleValue());

                } else {

                        cell.setCellValue(value.toString());
                }
        }

        private Row getRow(Sheet sheet, int rowIndex) {

                Row row = sheet.getRow(rowIndex);

                return row != null
                                ? row
                                : sheet.createRow(rowIndex);
        }

        private String value(Object value) {

                return value == null
                                ? ""
                                : value.toString();
        }

        private int number(Integer value) {

                return value == null
                                ? 0
                                : value;
        }

        private String getTotalOrder(
                        FinalInspectionResponse data) {

                int total = 0;

                if (data.getStyles() != null) {

                        for (StyleItem style : data.getStyles()) {

                                if (style.getColors() == null) {
                                        continue;
                                }

                                for (ColorItem color : style.getColors()) {
                                        total += number(color.getQtyOrder());
                                }
                        }

                } else if (data.getColors() != null) {

                        for (ColorItem color : data.getColors()) {
                                total += number(color.getQtyOrder());
                        }
                }

                return String.valueOf(total);
        }

        private String getTotalReady(
                        FinalInspectionResponse data) {

                int total = 0;

                if (data.getStyles() != null) {

                        for (StyleItem style : data.getStyles()) {

                                if (style.getColors() == null) {
                                        continue;
                                }

                                for (ColorItem color : style.getColors()) {
                                        total += number(color.getQtyReady());
                                }
                        }

                } else if (data.getColors() != null) {

                        for (ColorItem color : data.getColors()) {

                                total += number(color.getQtyReady());
                        }
                }

                return String.valueOf(total);
        }

        private String getStyles(
                        FinalInspectionResponse data) {

                List<String> values = new ArrayList<>();

                if (data.getStyles() != null) {

                        for (StyleItem style : data.getStyles()) {

                                String code = value(style.getStyleCode());

                                if (!code.isBlank() && !values.contains(code)) {
                                        values.add(code);
                                }
                        }
                }

                if (values.isEmpty()) {

                        String styleCode = value(data.getStyleCode());

                        if (!styleCode.isBlank()) {
                                values.add(styleCode);
                        }
                }

                return String.join(", ", values);
        }

        private String getColors(
                        FinalInspectionResponse data) {

                List<String> values = new ArrayList<>();

                if (data.getStyles() != null) {

                        for (StyleItem style : data.getStyles()) {

                                if (style.getColors() == null) {
                                        continue;
                                }

                                for (ColorItem color : style.getColors()) {

                                        String name = value(color.getColorName());

                                        if (!name.isBlank() && !values.contains(name)) {
                                                values.add(name);
                                        }
                                }
                        }
                }

                if (values.isEmpty() && data.getColors() != null) {

                        for (ColorItem color : data.getColors()) {

                                String name = value(color.getColorName());

                                if (!name.isBlank() && !values.contains(name)) {
                                        values.add(name);
                                }
                        }
                }

                return String.join(", ", values);
        }
}