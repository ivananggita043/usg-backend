
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
                validateItems(request);

                Line line = getActiveLine(request.getLineId());
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
        public List<CuttingInspectionResponse> getAll() {
                return inspectionRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public CuttingInspectionResponse getById(String id) {
                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Laporan cutting tidak ditemukan"));

                return toResponse(inspection);
        }

        @Transactional
        public CuttingInspectionResponse update(String id, CuttingInspectionRequest request) {
                validateItems(request);

                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Laporan cutting tidak ditemukan"));

                Line line = getActiveLine(request.getLineId());
                inspection.setProcessCheck(request.getProcessCheck());
                inspection.setLine(line);
                inspection.setComment(request.getComment());
                inspection = inspectionRepository.save(inspection);

                deleteItems(inspection);
                saveItems(inspection, request);
                return toResponse(inspection);
        }

        @Transactional
        public void delete(String id) {
                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Laporan cutting tidak ditemukan"));
                deleteItems(inspection);
                inspectionRepository.delete(inspection);
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

                        if (!defectType.isActive()) {
                                throw new BadRequestException(
                                                "Cutting defect type tidak aktif: " + defectType.getName());
                        }

                        if (defectType.getProcessCheck() != request.getProcessCheck()) {
                                throw new BadRequestException("Jenis defect " + defectType.getName()
                                                + " tidak sesuai dengan proses " + request.getProcessCheck());
                        }

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

                        if (itemRequest.getDefects() == null) {
                                continue;
                        }
                        for (CuttingInspectionRequest.DefectRequest defectRequest : itemRequest.getDefects()) {
                                CuttingPanel panel = defectRequest.getPanel();
                                Integer quantity = defectRequest.getQuantity();

                                if (panel == null) {
                                        throw new BadRequestException("Panel harus dipilih");
                                }

                                if (quantity == null || quantity <= 0) {
                                        throw new BadRequestException("Quantity defect harus lebih besar dari 0");
                                }

                                if (!usedPanels.add(panel)) {
                                        throw new BadRequestException("Panel " + panel
                                                        + " tidak boleh dikirim dua kali " + "dalam item yang sama");
                                }

                                CuttingInspectionDefect defect = new CuttingInspectionDefect();
                                defect.setInspectionItem(item);
                                defect.setPanel(panel);
                                defect.setQuantity(quantity);
                                defectRepository.save(defect);
                        }
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

        @Transactional
        public CuttingInspectionResponse patchComment(String id, String comment) {
                CuttingInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Laporan cutting tidak ditemukan"));
                inspection.setComment(comment);
                inspection = inspectionRepository.save(inspection);
                return toResponse(inspection);
        }

        @Transactional(readOnly = true)
        public List<CuttingDefectType> searchDefectTypes(CuttingProcessCheck processCheck,String search) {
                if (processCheck == null) {
                        throw new BadRequestException("Proses cutting harus dipilih");
                }
                if (search == null || search.isBlank()) {
                        return defectTypeRepository.findByProcessCheckAndActiveTrue(processCheck);
                }
                return defectTypeRepository
                                .findByProcessCheckAndActiveTrueAndNameContainingIgnoreCase(
                                                processCheck,
                                                search.trim());
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

        private Line getActiveLine(String lineId) {
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
}
