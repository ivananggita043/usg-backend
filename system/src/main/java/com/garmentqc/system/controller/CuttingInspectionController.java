package com.garmentqc.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.CuttingInspectionRequest;
import com.garmentqc.system.dto.CuttingInspectionResponse;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.CuttingInspectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cutting-inspections")
@RequiredArgsConstructor
@Validated
public class CuttingInspectionController {

    private final CuttingInspectionService cuttingInspectionService;

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

    @GetMapping("/{id}")
    public ResponseEntity<CuttingInspectionResponse> getById(
            @PathVariable String id) {
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
                cuttingInspectionService.patchComment(
                        id,
                        request.get("comment")));
    }
}
