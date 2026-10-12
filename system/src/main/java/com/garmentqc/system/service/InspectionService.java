
package com.garmentqc.system.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.garmentqc.system.dto.InspectionDefectRequest;
import com.garmentqc.system.dto.InspectionRequest;
import com.garmentqc.system.dto.InspectionResponse;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.DefectTypeRepository;
import com.garmentqc.system.repository.InspectionRepository;
import com.garmentqc.system.repository.LineRepository;
import com.garmentqc.system.repository.UserRepository;

@Service
public class InspectionService {

        private final InspectionRepository inspectionRepository;
        private final LineRepository lineRepository;
        private final DefectTypeRepository defectTypeRepository;
        private final UserRepository userRepository;

        public InspectionService(
                        InspectionRepository inspectionRepository,
                        LineRepository lineRepository,
                        DefectTypeRepository defectTypeRepository,
                        UserRepository userRepository) {

                this.inspectionRepository = inspectionRepository;
                this.lineRepository = lineRepository;
                this.defectTypeRepository = defectTypeRepository;
                this.userRepository = userRepository;
        }

        @Transactional
        public InspectionResponse create(InspectionRequest request) {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication.getName() == null
                                || authentication.getName().equals("anonymousUser")) {

                        throw new UnauthorizedException(
                                        "User harus login terlebih dahulu");
                }

                String username = authentication.getName();

                User submittedBy = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User yang login tidak ditemukan"));

                if (request.getBuyerName() == null
                                || request.getBuyerName().isBlank()) {

                        throw new BadRequestException(
                                        "Nama buyer wajib diisi");
                }

                if (request.getStyleCode() == null
                                || request.getStyleCode().isBlank()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Kode style wajib diisi");
                }

                if (request.getColorName() == null
                                || request.getColorName().isBlank()) {

                        throw new BadRequestException(
                                        "Color wajib diisi");
                }

                if (request.getLineId() == null
                                || request.getLineId().isBlank()) {

                        throw new BadRequestException(
                                        "Line wajib dipilih");
                }

                Line line = lineRepository.findById(request.getLineId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Line tidak ditemukan"));

                if (line.getSection() == null
                                || !List.of("SEWING", "FINISHING", "WASHING")
                                                .contains(line.getSection().name())) {

                        throw new BadRequestException(
                                        "Inspection hanya tersedia untuk sewing, finishing, dan washing");
                }

                if (request.getInspectedQuantity() == null
                                || request.getInspectedQuantity() < 1) {

                        throw new BadRequestException(
                                        "Jumlah inspect minimal 1");
                }

                int transferQuantity = request.getTransferQuantity() == null
                                ? 0
                                : request.getTransferQuantity();

                if (transferQuantity < 0
                                || transferQuantity > request.getInspectedQuantity()) {

                        throw new BadRequestException(
                                        "Quantity transfer harus antara 0 dan jumlah inspect");
                }

                Inspection inspection = new Inspection();

                inspection.setBuyerName(request.getBuyerName().trim());
                inspection.setStyleCode(request.getStyleCode().trim());
                inspection.setColorName(request.getColorName().trim());
                inspection.setLine(line);
                inspection.setInspectQuantity(request.getInspectedQuantity());
                inspection.setTransferQuantity(transferQuantity);
                inspection.setComment(request.getComment());

                inspection.setSubmittedBy(submittedBy);

                if (request.getDefects() != null) {

                        for (InspectionDefectRequest item : request.getDefects()) {

                                if (item.getDefectTypeId() == null
                                                || item.getDefectTypeId().isBlank()) {

                                        throw new BadRequestException(
                                                        "Defect type wajib dipilih");
                                }

                                if (item.getQuantity() == null
                                                || item.getQuantity() < 1) {

                                        throw new BadRequestException(
                                                        "Quantity defect minimal 1");
                                }

                                DefectType defectType = defectTypeRepository
                                                .findById(item.getDefectTypeId())
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Defect type tidak ditemukan"));

                                InspectionDefect defect = new InspectionDefect();
                                defect.setInspection(inspection);
                                defect.setDefectType(defectType);
                                defect.setQuantity(item.getQuantity());

                                inspection.getInspectionDefects().add(defect);
                        }
                }

                Inspection saved = inspectionRepository.save(inspection);

                return toResponse(saved);
        }

        @Transactional(readOnly = true)
        public InspectionResponse getById(String id) {

                Inspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Inspection tidak ditemukan"));

                return toResponse(inspection);
        }

        @Transactional(readOnly = true)
        public List<InspectionResponse> getAll() {

                return inspectionRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        private double calculatePercentage(
                        int totalDefect,
                        int inspectQuantity) {

                if (inspectQuantity <= 0) {
                        return 0.0;
                }

                double percentage = (double) totalDefect / inspectQuantity * 100;

                return Math.round(percentage * 100.0) / 100.0;
        }

        private InspectionResponse toResponse(Inspection inspection) {

                List<InspectionResponse.DefectItem> defectItems = inspection.getInspectionDefects()
                                .stream()
                                .map(d -> new InspectionResponse.DefectItem(
                                                d.getId(),
                                                d.getDefectType().getId(),
                                                d.getQuantity()))
                                .toList();

                int totalDefect = defectItems.stream()
                                .mapToInt(item -> item.getQuantity() == null
                                                ? 0
                                                : item.getQuantity())
                                .sum();

                int inspectQuantity = inspection.getInspectQuantity() == null
                                ? 0
                                : inspection.getInspectQuantity();

                double defectPercentage = calculatePercentage(
                                totalDefect,
                                inspectQuantity);

                User submittedBy = inspection.getSubmittedBy();

                return new InspectionResponse(
                                inspection.getId(),
                                inspection.getBuyerName(),
                                inspection.getStyleCode(),
                                inspection.getColorName(),
                                inspection.getLine().getName(),
                                inspection.getLine().getSection().name(),
                                inspection.getInspectQuantity(),
                                totalDefect,
                                defectPercentage,
                                inspection.getTransferQuantity(),
                                inspection.getComment(),
                                defectItems,
                                submittedBy != null ? submittedBy.getId() : null,
                                submittedBy != null ? submittedBy.getNama() : null,
                                inspection.getCreatedAt(),
                                inspection.getUpdatedAt());
        }
}