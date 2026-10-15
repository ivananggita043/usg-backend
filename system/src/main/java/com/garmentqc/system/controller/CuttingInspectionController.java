package com.garmentqc.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.CuttingInspectionRequest;
import com.garmentqc.system.dto.CuttingInspectionResponse;
import com.garmentqc.system.enums.CuttingProcessCheck;
import com.garmentqc.system.model.CuttingDefectType;
import com.garmentqc.system.model.CuttingInspection;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.CuttingInspectionRepository;
import com.garmentqc.system.service.CuttingInspectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cutting-inspections")
@RequiredArgsConstructor
@Validated
public class CuttingInspectionController {

    private final CuttingInspectionService cuttingInspectionService;
    private final CuttingInspectionRepository cuttingInspectionRepository;

    @PostMapping
    public ResponseEntity<CuttingInspectionResponse> create(@Valid @RequestBody CuttingInspectionRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User belum terautentikasi");
        }
        CuttingInspectionResponse response = cuttingInspectionService.create(request, currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CuttingInspectionResponse>> getAll() {
        return ResponseEntity.ok(cuttingInspectionService.getAll());
    }

    @GetMapping("/pagination")
    public Page<CuttingInspection> getAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(CuttingInspection::getCreatedAt).descending());
        return cuttingInspectionRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuttingInspectionResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(cuttingInspectionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CuttingInspectionResponse> update(
            @PathVariable String id,
            @RequestBody CuttingInspectionRequest request) {
        return ResponseEntity.ok(cuttingInspectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        cuttingInspectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/comment")
    public ResponseEntity<CuttingInspectionResponse> patchComment(@PathVariable String id,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                cuttingInspectionService.patchComment(id, request.get("comment")));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CuttingDefectType>> searchDefectTypes(@RequestParam CuttingProcessCheck processCheck,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(cuttingInspectionService.searchDefectTypes(processCheck, search));
    }
}
