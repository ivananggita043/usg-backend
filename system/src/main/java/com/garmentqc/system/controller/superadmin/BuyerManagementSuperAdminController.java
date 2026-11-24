
package com.garmentqc.system.controller.superadmin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.BuyerRequest;
import com.garmentqc.system.dto.BuyerResponse;
import com.garmentqc.system.model.Buyer;
import com.garmentqc.system.service.BuyerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("super-admin/buyers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BuyerManagementSuperAdminController {

    private final BuyerService buyerService;

    @PostMapping
    public ResponseEntity<BuyerResponse> createBuyer(@Valid @RequestBody BuyerRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(buyerService.createBuyer(request));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllBuyers() {
        return ResponseEntity.ok(buyerService.getAllBuyers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuyerResponse> getBuyerById(@PathVariable String id) {
        return ResponseEntity.ok(buyerService.getBuyerById(id));
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<BuyerResponse>> getAllBuyers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.sort(Buyer.class)
                        .by(Buyer::getName)
                        .ascending());

        return ResponseEntity.ok(buyerService.getAllBuyers(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BuyerResponse> updateBuyer(@PathVariable String id,
            @Valid @RequestBody BuyerRequest request) {
        return ResponseEntity.ok(buyerService.updateBuyer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuyer(@PathVariable String id) {
        buyerService.deleteBuyer(id);
        return ResponseEntity.noContent().build();
    }
}
