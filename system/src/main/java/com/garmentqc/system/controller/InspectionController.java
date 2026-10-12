package com.garmentqc.system.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.garmentqc.system.dto.InspectionRequest;
import com.garmentqc.system.dto.InspectionResponse;
import com.garmentqc.system.service.InspectionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inspections")
public class InspectionController {

    private final InspectionService inspectionService;

    public InspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InspectionResponse create(@Valid @RequestBody InspectionRequest request) {
        return inspectionService.create(request);
    }

    @GetMapping
    public List<InspectionResponse> getAll() {
        return inspectionService.getAll();
    }

    @GetMapping("/{id}")
    public InspectionResponse getById(@PathVariable String id) {
        return inspectionService.getById(id);
    }
}
