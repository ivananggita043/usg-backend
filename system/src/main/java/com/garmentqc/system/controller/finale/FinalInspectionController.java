package com.garmentqc.system.controller.finale;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.FinalInspectionLookupResponse;
import com.garmentqc.system.dto.FinalInspectionProgressResponse;
import com.garmentqc.system.dto.FinalInspectionReportResponse;
import com.garmentqc.system.dto.FinalInspectionRequest;
import com.garmentqc.system.dto.FinalInspectionResponse;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.BuyerService;
import com.garmentqc.system.service.FinalInspectionExcelService;
import com.garmentqc.system.service.FinalInspectionReportService;
import com.garmentqc.system.service.FinalInspectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/final/inspections")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'USER')")
public class FinalInspectionController {

    private final FinalInspectionService finalInspectionService;
    private final FinalInspectionReportService finalInspectionReportService;
    private final FinalInspectionExcelService finalInspectionExcelService;
    private final BuyerService buyerService;

    @PostMapping
    public ResponseEntity<FinalInspectionResponse> create(@Valid @RequestBody FinalInspectionRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(finalInspectionService.create(request, currentUser));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FinalInspectionResponse>> getAll(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(finalInspectionService.getAll(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinalInspectionResponse> getById(@PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(finalInspectionService.getById(id, currentUser));
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<FinalInspectionResponse>> getAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @AuthenticationPrincipal User currentUser) {

        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy));

        return ResponseEntity.ok(
                finalInspectionService.getAllPaginated(currentUser, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinalInspectionResponse> update(@PathVariable String id,
            @Valid @RequestBody FinalInspectionRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(finalInspectionService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal User currentUser) {
        finalInspectionService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/accessories")
    public ResponseEntity<List<FinalInspectionLookupResponse>> getAccessories(
            @RequestParam(required = false) String keyword, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(finalInspectionService.getAccessories(keyword, currentUser));
    }

    @GetMapping("/search/defects")
    public ResponseEntity<List<FinalInspectionLookupResponse>> getFinalDefects(
            @RequestParam(required = false) String keyword, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(finalInspectionService.getFinalDefects(keyword, currentUser));
    }

    @GetMapping("/progress")
    public ResponseEntity<FinalInspectionProgressResponse> getProgress(@RequestParam String poNumber,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(finalInspectionService.getProgressByPoNumber(poNumber, currentUser));
    }

    @GetMapping("/search/buyers")
    public ResponseEntity<List<FinalInspectionLookupResponse>> getBuyers(
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                finalInspectionService.getBuyers(keyword, currentUser));
    }

    @GetMapping("/search/styles")
    public ResponseEntity<List<FinalInspectionLookupResponse>> getStyles(
            @RequestParam String buyerId,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                finalInspectionService.getStyles(buyerId, keyword, currentUser));
    }

    @GetMapping("/search/colors")
    public ResponseEntity<List<FinalInspectionLookupResponse>> getColors(
            @RequestParam String styleId,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                finalInspectionService.getColors(styleId, keyword, currentUser));
    }

    @GetMapping("/reports/daily")
    public ResponseEntity<FinalInspectionReportResponse> getDailyReport(@RequestParam LocalDate date) {
        return ResponseEntity.ok(finalInspectionReportService.getDailyReport(date));
    }

    @GetMapping("/reports/weekly")
    public ResponseEntity<FinalInspectionReportResponse> getWeeklyReport(@RequestParam LocalDate date) {
        return ResponseEntity.ok(finalInspectionReportService.getWeeklyReport(date));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<FinalInspectionReportResponse> getMonthlyReport(@RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(finalInspectionReportService.getMonthlyReport(year, month));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportExcel(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) throws IOException {

        FinalInspectionResponse inspection = finalInspectionService.getById(id, currentUser);

        byte[] excel = finalInspectionExcelService.generateExcel(inspection);

        String poNumber = inspection.getPoNumber() != null
                ? inspection.getPoNumber().trim()
                : "";

        String safePo = poNumber.replaceAll("[\\\\/:*?\"<>|\\s]+", "-");

        String fileName = safePo.isBlank()
                ? "final-inspection.xlsx"
                : "Final-Inspection-" + safePo + ".xlsx";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}