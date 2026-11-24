
package com.garmentqc.system.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.BuyerRequest;
import com.garmentqc.system.dto.BuyerResponse;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.model.Buyer;
import com.garmentqc.system.repository.BuyerRepository;
import com.garmentqc.system.repository.StyleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuyerService {

    private final BuyerRepository buyerRepository;
    private final StyleRepository styleRepository;

    @Transactional
    public BuyerResponse createBuyer(BuyerRequest request) {

        String name = request.getName().trim();

        if (buyerRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Nama buyer sudah terdaftar");
        }

        Buyer buyer = new Buyer();
        buyer.setName(name);

        return toResponse(buyerRepository.save(buyer));
    }

    @Transactional(readOnly = true)
    public List<BuyerResponse> getAllBuyers() {

        return buyerRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BuyerResponse getBuyerById(String id) {

        return toResponse(findBuyer(id));
    }

    @Transactional(readOnly = true)
    public Page<BuyerResponse> getAllBuyers(Pageable pageable) {

        return buyerRepository.findAllByOrderByNameAsc(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public BuyerResponse updateBuyer(String id, BuyerRequest request) {

        Buyer buyer = findBuyer(id);
        String name = request.getName().trim();

        if (buyerRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BadRequestException("Nama buyer sudah digunakan");
        }

        buyer.setName(name);

        return toResponse(buyerRepository.save(buyer));
    }

    @Transactional
    public void deleteBuyer(String id) {
        Buyer buyer = findBuyer(id);

        if (styleRepository.existsByBuyer_Id(id)) {
            throw new BadRequestException("Buyer tidak dapat dihapus karena masih memiliki Style. "
                    + "Buyer yang sudah digunakan tidak boleh dihapus.");
        }

        buyerRepository.delete(buyer);
    }

    private Buyer findBuyer(String id) {
        return buyerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer dengan ID " + id + " tidak ditemukan"));
    }

    private BuyerResponse toResponse(Buyer buyer) {
        return new BuyerResponse(
                buyer.getId(),
                buyer.getName(),
                buyer.getCreatedAt(),
                buyer.getUpdatedAt());
    }
}
