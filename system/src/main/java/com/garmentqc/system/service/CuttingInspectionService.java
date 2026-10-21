
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
import com.garmentqc.system.enums.CuttingPanel;
import com.garmentqc.system.enums.CuttingProcessCheck;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.CuttingDefectType;
import com.garmentqc.system.model.CuttingInspection;
import com.garmentqc.system.model.CuttingInspectionDefect;
import com.garmentqc.system.model.CuttingInspectionItem;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.CuttingDefectTypeRepository;
import com.garmentqc.system.repository.CuttingInspectionDefectRepository;
import com.garmentqc.system.repository.CuttingInspectionItemRepository;
import com.garmentqc.system.repository.CuttingInspectionRepository;
import com.garmentqc.system.repository.LineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuttingInspectionService {

        private final CuttingInspectionRepository inspectionRepository;
        private final CuttingInspectionItemRepository itemRepository;
        private final CuttingInspectionDefectRepository defectRepository;
        private final CuttingDefectTypeRepository defectTypeRepository;
        private final LineRepository lineRepository;

        @Transactional
        public CuttingInspectionResponse create(CuttingInspectionRequest request, User submittedBy) {

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

                String role = currentUser.getRole().name();
                if ("USER".equals(role)) {
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
        public CuttingInspectionResponse getById(String id, User currentUser) {
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
        public CuttingInspectionResponse update(String id, CuttingInspectionRequest request, User currentUser) {
                if (id == null || id.isBlank()) {
                        throw new BadRequestException("ID laporan wajib diisi");
                }

                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Laporan cutting tidak ditemukan"));

                validateCanAccessInspection(inspection, currentUser);
                validateItems(request);
                Line line = getActiveCuttingLine(request.getLineId());

                validateItemContents(request);

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
                                .orElseThrow(() -> new ResourceNotFoundException("Laporan cutting tidak ditemukan"));

                validateCanAccessInspection(inspection, currentUser);
                deleteItems(inspection);
                inspectionRepository.delete(inspection);
        }

        @Transactional
        public CuttingInspectionResponse patchComment(String id, String comment, User currentUser) {
                if (id == null || id.isBlank()) {
                        throw new BadRequestException("ID laporan wajib diisi");
                }

                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Laporan cutting tidak ditemukan"));

                validateCanAccessInspection(inspection, currentUser);
                inspection.setComment(comment);
                return toResponse(inspectionRepository.save(inspection));
        }

        @Transactional(readOnly = true)
        public List<CuttingDefectType> searchDefectTypes(CuttingProcessCheck processCheck, String search,
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

        private void saveItems(CuttingInspection inspection, CuttingInspectionRequest request) {
                Set<String> usedItems = new HashSet<>();
                for (CuttingInspectionRequest.StyleItemRequest itemRequest : request.getItems()) {
                        String buyerName = itemRequest.getBuyerName().trim();
                        String styleCode = itemRequest.getStyleCode().trim();
                        String colorName = itemRequest.getColorName().trim();
                        String lot = itemRequest.getLot().trim();
                        String operatorName = itemRequest.getOperatorName().trim();
                        String itemKey = buyerName.toLowerCase(Locale.ROOT) + "|"
                                        + styleCode.toLowerCase(Locale.ROOT) + "|"
                                        + colorName.toLowerCase(Locale.ROOT) + "|"
                                        + lot.toLowerCase(Locale.ROOT);
                        if (!usedItems.add(itemKey)) {
                                throw new BadRequestException("Kombinasi buyer, style, color, dan lot "
                                                + "yang sama tidak boleh ditambahkan dua kali");
                        }

                        String defectTypeId = itemRequest.getDefectTypeId();
                        CuttingDefectType defectType = defectTypeRepository.findById(defectTypeId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Cutting defect type tidak ditemukan: " + defectTypeId));

                        validateDefectType(defectType, request.getProcessCheck());

                        CuttingInspectionItem item = new CuttingInspectionItem();
                        item.setCuttingInspection(inspection);
                        item.setBuyerName(buyerName);
                        item.setStyleCode(styleCode);
                        item.setColorName(colorName);
                        item.setDefectType(defectType);
                        item.setLot(lot);
                        item.setOperatorName(operatorName);
                        item.setQuantityInspect(itemRequest.getQuantityInspect());

                        item = itemRepository.save(item);
                        Set<CuttingPanel> usedPanels = new HashSet<>();
                        int totalDefect = 0;
                        if (itemRequest.getDefects() == null) {
                                continue;
                        }
                        for (CuttingInspectionRequest.DefectRequest defectRequest : itemRequest.getDefects()) {
                                if (defectRequest == null) {
                                        throw new BadRequestException("Data defect tidak boleh kosong");
                                }

                                CuttingPanel panel = defectRequest.getPanel();
                                Integer quantity = defectRequest.getQuantity();
                                if (panel == null) {
                                        throw new BadRequestException("Panel harus dipilih");
                                }

                                if (quantity == null || quantity <= 0) {
                                        throw new BadRequestException("Quantity defect harus lebih besar dari 0");
                                }

                                if (!usedPanels.add(panel)) {
                                        throw new BadRequestException(
                                                        "Panel " + panel + " tidak boleh dikirim dua kali "
                                                                        + "dalam item yang sama");
                                }

                                if (quantity > itemRequest.getQuantityInspect() - totalDefect) {
                                        throw new BadRequestException("Total quantity defect tidak boleh melebihi "
                                                        + "quantity inspect untuk style " + styleCode);
                                }

                                totalDefect += quantity;

                                CuttingInspectionDefect defect = new CuttingInspectionDefect();
                                defect.setInspectionItem(item);
                                defect.setPanel(panel);
                                defect.setQuantity(quantity);
                                defectRepository.save(defect);
                        }
                }
        }

        private void validateItemContents(CuttingInspectionRequest request) {
                Set<String> usedItems = new HashSet<>();
                for (CuttingInspectionRequest.StyleItemRequest item : request.getItems()) {
                        if (item == null) {
                                throw new BadRequestException("Item style tidak boleh kosong");
                        }
                        String buyerName = requireText(item.getBuyerName(), "Buyer wajib diisi");
                        String styleCode = requireText(item.getStyleCode(), "Style code wajib diisi");
                        String colorName = requireText(item.getColorName(), "Color wajib diisi");
                        String lot = requireText(item.getLot(), "Lot wajib diisi");

                        requireText(item.getOperatorName(), "Nama operator wajib diisi");
                        String itemKey = buyerName.toLowerCase(Locale.ROOT) + "|"
                                        + styleCode.toLowerCase(Locale.ROOT) + "|"
                                        + colorName.toLowerCase(Locale.ROOT) + "|"
                                        + lot.toLowerCase(Locale.ROOT);

                        if (!usedItems.add(itemKey)) {
                                throw new BadRequestException("Kombinasi buyer, style, color, dan lot "
                                                + "yang sama tidak boleh ditambahkan dua kali");
                        }

                        Integer quantityInspect = item.getQuantityInspect();
                        if (quantityInspect == null || quantityInspect <= 0) {
                                throw new BadRequestException("Quantity inspect harus lebih besar dari 0 "
                                                + "untuk style " + styleCode);
                        }

                        if (item.getDefectTypeId() == null || item.getDefectTypeId().isBlank()) {
                                throw new BadRequestException("Defect type wajib dipilih untuk style " + styleCode);
                        }

                        if (item.getDefects() == null) {
                                continue;
                        }

                        Set<CuttingPanel> usedPanels = new HashSet<>();
                        int totalDefect = 0;

                        for (CuttingInspectionRequest.DefectRequest defect : item.getDefects()) {
                                if (defect == null) {
                                        throw new BadRequestException("Data defect tidak boleh kosong");
                                }

                                if (defect.getPanel() == null) {
                                        throw new BadRequestException("Panel harus dipilih");
                                }

                                if (!usedPanels.add(defect.getPanel())) {
                                        throw new BadRequestException(
                                                        "Panel " + defect.getPanel()
                                                                        + " tidak boleh dikirim dua kali "
                                                                        + "dalam item yang sama");
                                }

                                Integer quantity = defect.getQuantity();

                                if (quantity == null || quantity <= 0) {
                                        throw new BadRequestException("Quantity defect harus lebih besar dari 0");
                                }

                                if (quantity > quantityInspect - totalDefect) {
                                        throw new BadRequestException("Total quantity defect tidak boleh melebihi "
                                                        + "quantity inspect untuk style "
                                                        + styleCode);
                                }
                                totalDefect += quantity;
                        }
                }
        }

        private void validateDefectType(CuttingDefectType defectType, CuttingProcessCheck processCheck) {
                if (!defectType.isActive()) {
                        throw new BadRequestException("Cutting defect type tidak aktif: " + defectType.getName());
                }
                if (defectType.getProcessCheck() != processCheck) {
                        throw new BadRequestException("Jenis defect " + defectType.getName()
                                        + " tidak sesuai dengan proses "
                                        + processCheck);
                }
        }

        private void deleteItems(CuttingInspection inspection) {
                List<CuttingInspectionItem> items = itemRepository.findByCuttingInspection(inspection);
                for (CuttingInspectionItem item : items) {
                        List<CuttingInspectionDefect> defects = defectRepository.findByInspectionItem(item);
                        defectRepository.deleteAll(defects);
                }
                itemRepository.deleteAll(items);
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
                if (user.getSection() == null || !"CUTTING".equals(user.getSection().name())) {
                        throw new UnauthorizedException("Akses hanya untuk pengguna section CUTTING");
                }
        }

        private void validateCanCreate(User user) {
                validateCuttingSection(user);
                if (!"USER".equals(user.getRole().name())) {
                        throw new UnauthorizedException("Hanya USER section CUTTING yang boleh membuat laporan");
                }
        }

        private void validateCanAccessInspection(CuttingInspection inspection, User currentUser) {
                validateCuttingSection(currentUser);
                String role = currentUser.getRole().name();
                if ("USER".equals(role)) {
                        User submittedBy = inspection.getSubmittedBy();
                        if (submittedBy == null || !submittedBy.getId().equals(currentUser.getId())) {
                                throw new UnauthorizedException(
                                                "Anda hanya dapat mengakses laporan "
                                                                + "yang anda input sendiri");
                        }
                }
        }

        private CuttingInspectionResponse toResponse(CuttingInspection inspection) {
                Line line = inspection.getLine();
                User submittedBy = inspection.getSubmittedBy();

                List<CuttingInspectionResponse.StyleItemResponse> itemResponses = new ArrayList<>();
                List<CuttingInspectionItem> items = itemRepository.findByCuttingInspection(inspection);
                for (CuttingInspectionItem item : items) {
                        CuttingDefectType defectType = item.getDefectType();
                        List<CuttingInspectionDefect> defects = defectRepository.findByInspectionItem(item);
                        List<CuttingInspectionResponse.DefectResponse> defectResponses = defects.stream()
                                        .map(defect -> new CuttingInspectionResponse.DefectResponse(
                                                        defect.getId(),
                                                        defect.getPanel(),
                                                        defect.getQuantity()))
                                        .toList();

                        int itemTotalDefect = defects.stream()
                                        .mapToInt(defect -> defect.getQuantity() == null
                                                        ? 0
                                                        : defect.getQuantity())
                                        .sum();

                        int itemQuantityInspect = item.getQuantityInspect() == null
                                        ? 0
                                        : item.getQuantityInspect();

                        double itemDefectPercentage = calculatePercentage(
                                        itemTotalDefect,
                                        itemQuantityInspect);

                        itemResponses.add(
                                        new CuttingInspectionResponse.StyleItemResponse(
                                                        item.getId(),
                                                        item.getBuyerName(),
                                                        item.getStyleCode(),
                                                        item.getColorName(),
                                                        defectType.getId(),
                                                        defectType.getName(),
                                                        item.getLot(),
                                                        item.getOperatorName(),
                                                        itemQuantityInspect,
                                                        itemTotalDefect,
                                                        itemDefectPercentage,
                                                        defectResponses));
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

        private double calculatePercentage(int totalDefect, int quantityInspect) {
                if (quantityInspect <= 0) {
                        return 0.0;
                }
                double percentage = (double) totalDefect / quantityInspect * 100;
                return Math.round(percentage * 100.0) / 100.0;
        }

        private Line getActiveCuttingLine(String lineId) {
                Line line = getActiveLine(lineId);
                if (line.getSection() == null || !"CUTTING".equals(line.getSection().name())) {
                        throw new BadRequestException("Laporan cutting hanya boleh menggunakan line CUTTING");
                }
                return line;
        }

        private Line getActiveLine(String lineId) {
                if (lineId == null || lineId.isBlank()) {
                        throw new BadRequestException("Line harus dipilih");
                }
                Line line = lineRepository.findById(lineId)
                                .orElseThrow(() -> new ResourceNotFoundException("Line tidak ditemukan"));
                if (!line.isActive()) {
                        throw new BadRequestException("Line tidak aktif");
                }
                return line;
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
                        throw new BadRequestException("Minimal harus ada satu item style");
                }
        }

        private String requireText(String value, String message) {
                if (value == null || value.isBlank()) {
                        throw new BadRequestException(message);
                }
                return value.trim();
        }
}
