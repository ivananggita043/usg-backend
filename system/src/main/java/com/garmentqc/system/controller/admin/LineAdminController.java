package com.garmentqc.system.controller.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.LineRequest;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.LineService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/lines")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LineControllerAdminController {

    private final LineService lineService;

    @PostMapping
    public ResponseEntity<Line> createLine(@Valid @RequestBody LineRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lineService.createLineForAdmin(request, currentUser));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllLines(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                lineService.getLinesForAdmin(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Line> getLineById(@PathVariable String id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(lineService.getLineForAdmin(id, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Line> updateLine(@PathVariable String id, @Valid @RequestBody LineRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(lineService.updateLineForAdmin(id, request, currentUser));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Line> updateLineStatus(@PathVariable String id, @RequestParam boolean active,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                lineService.updateLineStatusForAdmin(
                        id, active, currentUser));
    }
}
