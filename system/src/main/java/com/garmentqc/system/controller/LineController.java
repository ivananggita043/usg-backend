package com.garmentqc.system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.LineRequest;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.service.LineService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/lines")
@RequiredArgsConstructor
public class LineController {

    private final LineService lineService;

    @PostMapping
    public ResponseEntity<Line> createLine(@Valid @RequestBody LineRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lineService.createLine(request));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllLines() {
        return ResponseEntity.ok(lineService.getAllLines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Line> getLineById(@PathVariable String id) {
        return ResponseEntity.ok(lineService.getLineById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Line> updateLine(@PathVariable String id, @Valid @RequestBody LineRequest request) {
        return ResponseEntity.ok(lineService.updateLine(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Line> updateLineStatus(@PathVariable String id, @RequestParam boolean active) {
        return ResponseEntity.ok(lineService.updateLineStatus(id, active));
    }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> deleteLine(@PathVariable String id) {

    // lineService.deleteLine(id);

    // return ResponseEntity.noContent().build();
    // }

}
