package com.garmentqc.system.controller.visitor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.FinalInspectionProgressResponse;
import com.garmentqc.system.dto.FinalInspectionResponse;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.FinalInspectionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("visitor/final/inspections")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VISITOR')")
public class FinalInspectionVisitorController {

    private final FinalInspectionService finalInspectionService;

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
    public ResponseEntity<Page<FinalInspectionResponse>> getAllPaginated(@AuthenticationPrincipal User currentUser,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                finalInspectionService.getAllPaginated(currentUser, pageable));
    }

    @GetMapping("/progress")
    public ResponseEntity<FinalInspectionProgressResponse> getProgress(@RequestParam String poNumber,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(finalInspectionService.getProgressByPoNumber(poNumber, currentUser));
    }
}