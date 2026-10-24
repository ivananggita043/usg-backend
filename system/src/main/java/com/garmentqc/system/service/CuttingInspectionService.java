
package com.garmentqc.system.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.CuttingInspectionRequest;
import com.garmentqc.system.dto.CuttingInspectionResponse;
import com.garmentqc.system.enums.CuttingProcessCheck;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.CuttingDefectType;
import com.garmentqc.system.model.CuttingInspection;
import com.garmentqc.system.model.CuttingInspectionItem;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.CuttingDefectTypeRepository;
import com.garmentqc.system.repository.CuttingInspectionItemRepository;
import com.garmentqc.system.repository.CuttingInspectionRepository;
import com.garmentqc.system.repository.LineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuttingInspectionService {

        private final CuttingInspectionRepository inspectionRepository;
        private final CuttingInspectionItemRepository itemRepository;
        private final CuttingDefectTypeRepository defectTypeRepository;
        private final LineRepository lineRepository;

        @Transactional
        public CuttingInspectionResponse create(
                        CuttingInspectionRequest request,
                        User submittedBy) {

                validateCanCreate(submittedBy);
                validateItems(request);

                Line line = getActiveCuttingLine(request.getLineId());

                CuttingInspection inspection = new CuttingInspection();
                inspection.setProcessCheck(request.getProcessCheck());
                inspection.setLine(line);
                inspection.setSubmittedBy(submittedBy);
                inspection.setComment(request.getComment());

                inspection = inspectionRepository.save(inspection);

                saveItems(inspection, request);

                return toResponse(inspection);
        }

        @Transactional(readOnly = true)
        public List<CuttingInspectionResponse> getAll(User currentUser) {

                validateCuttingSection(currentUser);

                if ("USER".equals(currentUser.getRole().name())) {
                        return inspectionRepository
                                        .findBySubmittedById(currentUser.getId())
                                        .stream()
                                        .map(this::toResponse)
                                        .toList();
                }

                return inspectionRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public CuttingInspectionResponse getById(
                        String id,
                        User currentUser) {

                if (id == null || id.isBlank()) {
                        throw new BadRequestException("ID laporan wajib diisi");
                }

                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Laporan cutting tidak ditemukan"));

                validateCanAccessInspection(inspection, currentUser);

                return toResponse(inspection);
        }

        @Transactional
        public CuttingInspectionResponse update(
                        String id,
                        CuttingInspectionRequest request,
                        User currentUser) {

                if (id == null || id.isBlank()) {
                        throw new BadRequestException("ID laporan wajib diisi");
                }

                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Laporan cutting tidak ditemukan"));

                validateCanAccessInspection(inspection, currentUser);
                validateItems(request);

                Line line = getActiveCuttingLine(request.getLineId());

                inspection.setProcessCheck(request.getProcessCheck());
                inspection.setLine(line);
                inspection.setComment(request.getComment());

                inspection = inspectionRepository.save(inspection);

                deleteItems(inspection);
                saveItems(inspection, request);

                return toResponse(inspection);
        }

        @Transactional
        public void delete(String id, User currentUser) {

                if (id == null || id.isBlank()) {
                        throw new BadRequestException("ID laporan wajib diisi");
                }

                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Laporan cutting tidak ditemukan"));

                validateCanAccessInspection(inspection, currentUser);

                deleteItems(inspection);
                inspectionRepository.delete(inspection);
        }

        @Transactional
        public CuttingInspectionResponse patchComment(
                        String id,
                        String comment,
                        User currentUser) {

                if (id == null || id.isBlank()) {
                        throw new BadRequestException("ID laporan wajib diisi");
                }

                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Laporan cutting tidak ditemukan"));

                validateCanAccessInspection(inspection, currentUser);

                inspection.setComment(comment);

                return toResponse(inspectionRepository.save(inspection));
        }

        @Transactional(readOnly = true)
        public List<CuttingDefectType> searchDefectTypes(
                        CuttingProcessCheck processCheck,
                        String search,
                        User currentUser) {

                validateCuttingSection(currentUser);

                if (processCheck == null) {
                        throw new BadRequestException("Proses cutting harus dipilih");
                }

                if (search == null || search.isBlank()) {
                        return defectTypeRepository
                                        .findByProcessCheckAndActiveTrue(processCheck);
                }

                return defectTypeRepository
                                .findByProcessCheckAndActiveTrueAndNameContainingIgnoreCase(
                                                processCheck,
                                                search.trim());
        }

        private void saveItems(
                        CuttingInspection inspection,
                        CuttingInspectionRequest request) {

                for (CuttingInspectionRequest.StyleItemRequest itemRequest : request.getItems()) {

                        String buyerName = itemRequest.getBuyerName().trim();
                        String styleCode = itemRequest.getStyleCode().trim();
                        String colorName = itemRequest.getColorName().trim();
                        String lot = itemRequest.getLot().trim();
                        String operatorName = itemRequest.getOperatorName().trim();
                        String defectTypeId = itemRequest.getDefectTypeId();

                        CuttingDefectType defectType = defectTypeRepository
                                        .findById(defectTypeId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Cutting defect type tidak ditemukan: "
                                                                        + defectTypeId));

                        validateDefectType(defectType, request.getProcessCheck());

                        CuttingInspectionItem item = new CuttingInspectionItem();
                        item.setCuttingInspection(inspection);
                        item.setBuyerName(buyerName);
                        item.setStyleCode(styleCode);
                        item.setColorName(colorName);
                        item.setLot(lot);
                        item.setOperatorName(operatorName);
                        item.setDefectType(defectType);
                        item.setQuantityInspect(itemRequest.getQuantityInspect());
                        item.setQuantityDefect(itemRequest.getQuantityDefect());

                        itemRepository.save(item);
                }
        }

        private void validateItems(CuttingInspectionRequest request) {

                if (request == null) {
                        throw new BadRequestException("Request tidak boleh kosong");
                }

                if (request.getProcessCheck() == null) {
                        throw new BadRequestException("Proses cutting harus dipilih");
                }

                if (request.getLineId() == null || request.getLineId().isBlank()) {
                        throw new BadRequestException("Line harus dipilih");
                }

                if (request.getItems() == null || request.getItems().isEmpty()) {
                        throw new BadRequestException(
                                        "Minimal harus ada satu item style");
                }

                Set<String> usedItems = new HashSet<>();

                for (CuttingInspectionRequest.StyleItemRequest item : request.getItems()) {

                        if (item == null) {
                                throw new BadRequestException(
                                                "Item style tidak boleh kosong");
                        }

                        String buyerName = requireText(
                                        item.getBuyerName(), "Buyer wajib diisi");

                        String styleCode = requireText(
                                        item.getStyleCode(), "Style code wajib diisi");

                        String colorName = requireText(
                                        item.getColorName(), "Color wajib diisi");

                        String lot = requireText(
                                        item.getLot(), "Lot wajib diisi");

                        requireText(
                                        item.getOperatorName(), "Nama operator wajib diisi");

                        String itemKey = buyerName.toLowerCase(Locale.ROOT) + "|"
                                        + styleCode.toLowerCase(Locale.ROOT) + "|"
                                        + colorName.toLowerCase(Locale.ROOT) + "|"
                                        + lot.toLowerCase(Locale.ROOT);

                        if (!usedItems.add(itemKey)) {
                                throw new BadRequestException(
                                                "Kombinasi buyer, style, color, dan lot "
                                                                + "yang sama tidak boleh ditambahkan dua kali");
                        }

                        Integer quantityInspect = item.getQuantityInspect();

                        if (quantityInspect == null || quantityInspect <= 0) {
                                throw new BadRequestException(
                                                "Quantity inspect harus lebih besar dari 0 "
                                                                + "untuk style " + styleCode);
                        }

                        Integer quantityDefect = item.getQuantityDefect();

                        if (quantityDefect == null || quantityDefect < 0) {
                                throw new BadRequestException(
                                                "Quantity defect tidak boleh kurang dari 0 "
                                                                + "untuk style " + styleCode);
                        }

                        if (quantityDefect > quantityInspect) {
                                throw new BadRequestException(
                                                "Quantity defect tidak boleh melebihi "
                                                                + "quantity inspect untuk style " + styleCode);
                        }

                        String defectTypeId = item.getDefectTypeId();

                        if (defectTypeId == null || defectTypeId.isBlank()) {
                                throw new BadRequestException(
                                                "Defect type wajib dipilih untuk style " + styleCode);
                        }
                }
        }

        private void validateDefectType(
                        CuttingDefectType defectType,
                        CuttingProcessCheck processCheck) {

                if (!defectType.isActive()) {
                        throw new BadRequestException(
                                        "Cutting defect type tidak aktif: "
                                                        + defectType.getName());
                }

                if (defectType.getProcessCheck() != processCheck) {
                        throw new BadRequestException(
                                        "Jenis defect " + defectType.getName()
                                                        + " tidak sesuai dengan proses " + processCheck);
                }
        }

        private void deleteItems(CuttingInspection inspection) {
                List<CuttingInspectionItem> items = itemRepository.findByCuttingInspection(inspection);

                itemRepository.deleteAll(items);
        }

        private CuttingInspectionResponse toResponse(
                        CuttingInspection inspection) {

                Line line = inspection.getLine();
                User submittedBy = inspection.getSubmittedBy();

                List<CuttingInspectionResponse.StyleItemResponse> itemResponses = new ArrayList<>();

                List<CuttingInspectionItem> items = itemRepository.findByCuttingInspection(inspection);

                for (CuttingInspectionItem item : items) {

                        CuttingDefectType defectType = item.getDefectType();

                        int quantityInspect = item.getQuantityInspect() == null
                                        ? 0
                                        : item.getQuantityInspect();

                        int quantityDefect = item.getQuantityDefect() == null
                                        ? 0
                                        : item.getQuantityDefect();

                        double defectPercentage = calculatePercentage(
                                        quantityDefect, quantityInspect);

                        itemResponses.add(
                                        new CuttingInspectionResponse.StyleItemResponse(
                                                        item.getId(),
                                                        item.getBuyerName(),
                                                        item.getStyleCode(),
                                                        item.getColorName(),
                                                        item.getLot(),
                                                        item.getOperatorName(),
                                                        quantityInspect,
                                                        quantityDefect,
                                                        defectType.getId(),
                                                        defectType.getName(),
                                                        defectPercentage));
                }

                return new CuttingInspectionResponse(
                                inspection.getId(),
                                inspection.getProcessCheck(),
                                line.getId(),
                                line.getName(),
                                submittedBy.getId(),
                                submittedBy.getNama(),
                                inspection.getComment(),
                                itemResponses,
                                inspection.getCreatedAt(),
                                inspection.getUpdatedAt());
        }

        private double calculatePercentage(
                        int totalDefect,
                        int quantityInspect) {

                if (quantityInspect <= 0) {
                        return 0.0;
                }

                double percentage = (double) totalDefect / quantityInspect * 100;

                return Math.round(percentage * 100.0) / 100.0;
        }

        private Line getActiveCuttingLine(String lineId) {

                Line line = getActiveLine(lineId);

                if (line.getSection() == null
                                || !"CUTTING".equals(line.getSection().name())) {
                        throw new BadRequestException(
                                        "Laporan cutting hanya boleh menggunakan line CUTTING");
                }

                return line;
        }

        private Line getActiveLine(String lineId) {

                if (lineId == null || lineId.isBlank()) {
                        throw new BadRequestException("Line harus dipilih");
                }

                Line line = lineRepository.findById(lineId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Line tidak ditemukan"));

                if (!line.isActive()) {
                        throw new BadRequestException("Line tidak aktif");
                }

                return line;
        }

        private void validateCuttingSection(User user) {

                if (user == null || user.getRole() == null) {
                        throw new UnauthorizedException("Akses ditolak");
                }

                String role = user.getRole().name();

                if ("SUPER_ADMIN".equals(role)) {
                        return;
                }

                if (!"USER".equals(role) && !"ADMIN".equals(role)) {
                        throw new UnauthorizedException("Akses ditolak");
                }

                if (user.getSection() == null
                                || !"CUTTING".equals(user.getSection().name())) {
                        throw new UnauthorizedException(
                                        "Akses hanya untuk pengguna section CUTTING");
                }
        }

        private void validateCanCreate(User user) {

                validateCuttingSection(user);

                if (!"USER".equals(user.getRole().name())) {
                        throw new UnauthorizedException(
                                        "Hanya USER section CUTTING yang boleh membuat laporan");
                }
        }

        private void validateCanAccessInspection(
                        CuttingInspection inspection,
                        User currentUser) {

                validateCuttingSection(currentUser);

                if ("USER".equals(currentUser.getRole().name())) {

                        User submittedBy = inspection.getSubmittedBy();

                        if (submittedBy == null
                                        || !submittedBy.getId().equals(currentUser.getId())) {
                                throw new UnauthorizedException(
                                                "Anda hanya dapat mengakses laporan yang anda input sendiri");
                        }
                }
        }

        private String requireText(String value, String message) {

                if (value == null || value.isBlank()) {
                        throw new BadRequestException(message);
                }

                return value.trim();
        }
}
