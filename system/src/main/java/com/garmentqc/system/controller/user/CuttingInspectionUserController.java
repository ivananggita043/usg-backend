
package com.garmentqc.system.controller.user;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.CuttingInspectionRequest;
import com.garmentqc.system.dto.CuttingInspectionResponse;
import com.garmentqc.system.enums.CuttingProcessCheck;
import com.garmentqc.system.model.CuttingDefectType;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.CuttingInspectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/cutting-inspections")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('USER')")
public class CuttingInspectionUserController {

        private final CuttingInspectionService cuttingInspectionService;

        @PostMapping
        public ResponseEntity<CuttingInspectionResponse> create(@Valid @RequestBody CuttingInspectionRequest request,
                        @AuthenticationPrincipal User currentUser) {
                CuttingInspectionResponse response = cuttingInspectionService.create(request, currentUser);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/all")
        public ResponseEntity<List<CuttingInspectionResponse>> getAll(@AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(cuttingInspectionService.getAll(currentUser));
        }

        @GetMapping("/{id}")
        public ResponseEntity<CuttingInspectionResponse> getById(@PathVariable String id,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(cuttingInspectionService.getById(id, currentUser));
        }

        @PutMapping("/{id}")
        public ResponseEntity<CuttingInspectionResponse> update(@PathVariable String id,
                        @Valid @RequestBody CuttingInspectionRequest request,
                        @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(cuttingInspectionService.update(id, request, currentUser));
        }

        @PatchMapping("/{id}/comment")
        public ResponseEntity<CuttingInspectionResponse> patchComment(@PathVariable String id,
                       @Valid @RequestBody Map<String, String> request, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity
                                .ok(cuttingInspectionService.patchComment(id, request.get("comment"), currentUser));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal User currentUser) {
                cuttingInspectionService.delete(id, currentUser);
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/search")
        public ResponseEntity<List<CuttingDefectType>> searchDefectTypes(@RequestParam CuttingProcessCheck processCheck,
                        @RequestParam(required = false) String search, @AuthenticationPrincipal User currentUser) {
                return ResponseEntity.ok(cuttingInspectionService.searchDefectTypes(processCheck, search, currentUser));
        }
}
