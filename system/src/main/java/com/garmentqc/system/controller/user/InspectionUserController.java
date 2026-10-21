
package com.garmentqc.system.controller.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.InspectionRequest;
import com.garmentqc.system.dto.InspectionResponse;
import com.garmentqc.system.service.InspectionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/inspections")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class InspectionUserController {

    private final InspectionService inspectionService;

    @PostMapping
    public ResponseEntity<InspectionResponse> create(@RequestBody InspectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inspectionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<InspectionResponse>> getAll() {
        return ResponseEntity.ok(inspectionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(inspectionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InspectionResponse> update(@PathVariable String id, @RequestBody InspectionRequest request) {
        return ResponseEntity.ok(inspectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        inspectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
