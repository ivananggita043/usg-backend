package com.garmentqc.system.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.DefectSearchResponse;
import com.garmentqc.system.dto.InspectionDefectRequest;
import com.garmentqc.system.dto.InspectionRequest;
import com.garmentqc.system.dto.InspectionResponse;
import com.garmentqc.system.enums.WashingType;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.DefectArea;
import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.DefectAreaRepository;
import com.garmentqc.system.repository.DefectTypeRepository;
import com.garmentqc.system.repository.InspectionRepository;
import com.garmentqc.system.repository.LineRepository;
import com.garmentqc.system.repository.UserLineRepository;
import com.garmentqc.system.repository.UserRepository;

@Service
public class InspectionService {

        private static final Set<String> SUPPORTED_SECTIONS = Set.of(
                        "SEWING", "FINISHING", "WASHING", "PACKING");

        private static final String SUPER_ADMIN = "SUPER_ADMIN";
        private static final String ADMIN = "ADMIN";
        private static final String USER = "USER";

        private final InspectionRepository inspectionRepository;
        private final LineRepository lineRepository;
        private final DefectTypeRepository defectTypeRepository;
        private final UserRepository userRepository;
        private final DefectAreaRepository defectAreaRepository;
        private final UserLineRepository userLineRepository;

        public InspectionService(InspectionRepository inspectionRepository, LineRepository lineRepository,
                        DefectTypeRepository defectTypeRepository, UserRepository userRepository,
                        DefectAreaRepository defectAreaRepository, UserLineRepository userLineRepository) {
                this.inspectionRepository = inspectionRepository;
                this.lineRepository = lineRepository;
                this.defectTypeRepository = defectTypeRepository;
                this.userRepository = userRepository;
                this.defectAreaRepository = defectAreaRepository;
                this.userLineRepository = userLineRepository;
        }

        @Transactional
        public InspectionResponse create(InspectionRequest request) {
                validateRequest(request);

                User submittedBy = getCurrentUser();

                Line line = findLine(request.getLineId());

                validateLineAccess(submittedBy, line);
                validateSupportedSection(line);

                String section = line.getSection().name();

                Integer qtyCheckCartonToday = request.getQtyCheckCartonToday();

                validatePackingQuantity(section, qtyCheckCartonToday);

                String handfeel = normalizeOptional(request.getHandfeel());
                String grade = normalizeOptional(request.getGrade());
                String process = normalizeOptional(request.getProcess());

                WashingType washingType = request.getWashingType();

                String inspectionTimeSlot = normalizeOptional(request.getInspectionTimeSlot());

                validateSectionFields(
                                section,
                                handfeel,
                                grade,
                                process,
                                washingType,
                                inspectionTimeSlot);

                int inspectedQuantity = validateInspectedQuantity(request.getInspectedQuantity());

                int transferQuantity = validateTransferQuantity(
                                request.getTransferQuantity(),
                                inspectedQuantity,
                                section);

                Inspection inspection = new Inspection();

                applyRequestFields(
                                inspection,
                                request,
                                line,
                                inspectedQuantity,
                                transferQuantity,
                                qtyCheckCartonToday,
                                section,
                                handfeel,
                                grade,
                                process,
                                washingType);

                inspection.setSubmittedBy(submittedBy);

                replaceDefects(
                                inspection,
                                request.getDefects(),
                                inspectedQuantity);

                Inspection saved = inspectionRepository.save(inspection);

                return toResponse(saved);
        }

        @Transactional(readOnly = true)
        public InspectionResponse getById(String id) {
                Inspection inspection = findInspection(id);

                User currentUser = getCurrentUser();

                validateInspectionAccess(currentUser, inspection);

                return toResponse(inspection);
        }

        @Transactional(readOnly = true)
        public List<InspectionResponse> getAll(String section) {
                User currentUser = getCurrentUser();

                String selectedSection = normalizeOptional(section);
                String role = currentUser.getRole().name();

                if (SUPER_ADMIN.equals(role) && selectedSection == null) {
                        throw new BadRequestException(
                                        "Super Admin wajib memilih section: " + String.join(", ", SUPPORTED_SECTIONS));
                }

                if (selectedSection != null
                                && !SUPPORTED_SECTIONS.contains(selectedSection)) {
                        throw new BadRequestException(
                                        "Section tidak valid. Pilihan: " + String.join(", ", SUPPORTED_SECTIONS));
                }

                return inspectionRepository.findAll()
                                .stream()
                                .filter(inspection -> canAccessInspection(currentUser, inspection))
                                .filter(inspection -> {
                                        if (selectedSection == null) {
                                                return true;
                                        }

                                        return inspection.getLine() != null
                                                        && inspection.getLine().getSection() != null
                                                        && selectedSection.equals(
                                                                        inspection.getLine()
                                                                                        .getSection()
                                                                                        .name());
                                })
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional
        public InspectionResponse update(String id, InspectionRequest request) {
                validateRequest(request);

                User currentUser = getCurrentUser();

                Inspection inspection = findInspection(id);

                validateInspectionAccess(currentUser, inspection);

                Line line = findLine(request.getLineId());

                validateLineAccess(currentUser, line);
                validateSupportedSection(line);

                String section = line.getSection().name();

                Integer qtyCheckCartonToday = request.getQtyCheckCartonToday();

                validatePackingQuantity(section, qtyCheckCartonToday);

                String handfeel = normalizeOptional(request.getHandfeel());
                String grade = normalizeOptional(request.getGrade());
                String process = normalizeOptional(request.getProcess());

                WashingType washingType = request.getWashingType();

                String inspectionTimeSlot = normalizeOptional(request.getInspectionTimeSlot());

                validateSectionFields(
                                section,
                                handfeel,
                                grade,
                                process,
                                washingType,
                                inspectionTimeSlot);

                int inspectedQuantity = validateInspectedQuantity(request.getInspectedQuantity());

                int transferQuantity = validateTransferQuantity(
                                request.getTransferQuantity(),
                                inspectedQuantity,
                                section);

                applyRequestFields(
                                inspection,
                                request,
                                line,
                                inspectedQuantity,
                                transferQuantity,
                                qtyCheckCartonToday,
                                section,
                                handfeel,
                                grade,
                                process,
                                washingType);

                replaceDefects(
                                inspection,
                                request.getDefects(),
                                inspectedQuantity);

                Inspection saved = inspectionRepository.save(inspection);

                return toResponse(saved);
        }

        @Transactional
        public void delete(String id) {
                User currentUser = getCurrentUser();

                Inspection inspection = findInspection(id);

                validateInspectionAccess(currentUser, inspection);

                inspectionRepository.delete(inspection);
        }

        private User getCurrentUser() {
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication instanceof AnonymousAuthenticationToken
                                || authentication.getName() == null
                                || authentication.getName().isBlank()
                                || "anonymousUser".equals(authentication.getName())) {

                        throw new UnauthorizedException("User harus login terlebih dahulu");
                }

                return userRepository
                                .findByUsername(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("User yang login tidak ditemukan"));
        }

        private void validateLineAccess(User user, Line line) {
                String role = user.getRole().name();

                if (SUPER_ADMIN.equals(role)) {
                        return;
                }

                if (ADMIN.equals(role)) {
                        if (user.getSection() == null
                                        || line.getSection() == null
                                        || !user.getSection().equals(line.getSection())) {

                                throw new AccessDeniedException(
                                                "Admin hanya dapat menggunakan line " + "dari section miliknya");
                        }
                        return;
                }

                if (USER.equals(role)) {
                        boolean assigned = userLineRepository.findByUser(user)
                                        .stream()
                                        .anyMatch(userLine -> userLine.getLine() != null
                                                        && userLine.getLine().getId()
                                                                        .equals(line.getId()));

                        if (!assigned) {
                                throw new UnauthorizedException("Kamu tidak memiliki akses ke line tersebut");
                        }

                        return;
                }

                throw new UnauthorizedException("Role tidak memiliki izin untuk membuat " + "atau mengubah inspection");
        }

        private void validateInspectionAccess(User user, Inspection inspection) {
                if (!canAccessInspection(user, inspection)) {
                        throw new AccessDeniedException(
                                        "Anda tidak memiliki akses ke data inspection ini");
                }
        }

        private boolean canAccessInspection(User user, Inspection inspection) {
                if (user == null || inspection == null) {
                        return false;
                }

                String role = user.getRole().name();

                if (SUPER_ADMIN.equals(role)) {
                        return true;
                }

                Line line = inspection.getLine();

                if (line == null || line.getSection() == null) {
                        return false;
                }

                if (ADMIN.equals(role)) {
                        return user.getSection() != null
                                        && user.getSection().equals(line.getSection());
                }

                if (USER.equals(role)) {
                        User submittedBy = inspection.getSubmittedBy();

                        return submittedBy != null
                                        && submittedBy.getId() != null
                                        && submittedBy.getId().equals(user.getId());
                }

                return false;
        }

        private Line findLine(String lineId) {
                if (lineId == null || lineId.isBlank()) {
                        throw new BadRequestException("Field 'lineId' wajib diisi");
                }

                return lineRepository.findById(lineId.trim())
                                .orElseThrow(() -> new ResourceNotFoundException("Line tidak ditemukan"));
        }

        private Inspection findInspection(String id) {
                if (id == null || id.isBlank()) {
                        throw new BadRequestException("ID inspection wajib diisi");
                }

                return inspectionRepository.findById(id.trim())
                                .orElseThrow(() -> new ResourceNotFoundException("Inspection tidak ditemukan"));
        }

        private void validateSupportedSection(Line line) {
                if (line.getSection() == null) {
                        throw new BadRequestException("Section pada line belum ditentukan");
                }

                if (!SUPPORTED_SECTIONS.contains(line.getSection().name())) {
                        throw new BadRequestException("Inspection hanya tersedia untuk section "
                                        + "SEWING, FINISHING, WASHING, dan PACKING");
                }
        }

        private void validatePackingQuantity(String section, Integer qtyCheckCartonToday) {
                if (!"PACKING".equals(section)) {
                        return;
                }

                if (qtyCheckCartonToday == null) {
                        throw new BadRequestException(
                                        "Field 'qtyCheckCartonToday' wajib diisi " + "untuk section PACKING");
                }

                if (qtyCheckCartonToday < 0) {
                        throw new BadRequestException("Field 'qtyCheckCartonToday' tidak boleh negatif");
                }
        }

        private void validateRequest(InspectionRequest request) {
                if (request == null) {
                        throw new BadRequestException("Request body wajib diisi");
                }

                if (request.getBuyerName() == null || request.getBuyerName().isBlank()) {
                        throw new BadRequestException("Field 'buyerName' wajib diisi");
                }

                if (request.getStyleCode() == null || request.getStyleCode().isBlank()) {
                        throw new BadRequestException("Field 'styleCode' wajib diisi");
                }

                if (request.getColorName() == null || request.getColorName().isBlank()) {
                        throw new BadRequestException("Field 'colorName' wajib diisi");
                }

                if (request.getLineId() == null || request.getLineId().isBlank()) {
                        throw new BadRequestException("Field 'lineId' wajib diisi");
                }

                if (request.getBuyerName().trim().length() > 255) {
                        throw new BadRequestException("Field 'buyerName' maksimal 255 karakter");
                }

                if (request.getStyleCode().trim().length() > 255) {
                        throw new BadRequestException("Field 'styleCode' maksimal 255 karakter");
                }

                if (request.getColorName().trim().length() > 255) {
                        throw new BadRequestException("Field 'colorName' maksimal 255 karakter");
                }
        }

        private int validateInspectedQuantity(Integer quantity) {
                if (quantity == null) {
                        throw new BadRequestException("Field 'inspectedQuantity' wajib diisi");
                }

                if (quantity < 1) {
                        throw new BadRequestException("Field 'inspectedQuantity' minimal 1");
                }
                return quantity;
        }

        private int validateTransferQuantity(Integer transferQuantity, int inspectedQuantity, String section) {
                if (transferQuantity == null) {
                        if ("SEWING".equals(section)) {
                                throw new BadRequestException(
                                                "Field 'transferQuantity' wajib diisi " + "untuk section SEWING");
                        }
                        return 0;
                }

                if (transferQuantity < 0) {
                        throw new BadRequestException("Field 'transferQuantity' tidak boleh negatif");
                }

                if (transferQuantity > inspectedQuantity) {
                        throw new BadRequestException(
                                        "Field 'transferQuantity' tidak boleh melebihi " + "inspectedQuantity");
                }
                return transferQuantity;
        }

        private void validateSectionFields(String section, String handfeel, String grade, String process,
                        WashingType washingType, String inspectionTimeSlot) {
                switch (section) {
                        case "SEWING":
                                if (handfeel != null || grade != null || process != null || washingType != null) {
                                        throw new BadRequestException(
                                                        "Section SEWING tidak menggunakan "
                                                                        + "handfeel, grade, process, "
                                                                        + "atau washingType");
                                }

                                if (inspectionTimeSlot == null) {
                                        throw new BadRequestException(
                                                        "Field 'inspectionTimeSlot' wajib diisi "
                                                                        + "untuk section SEWING");
                                }
                                break;

                        case "WASHING":
                                if (handfeel == null) {
                                        throw new BadRequestException(
                                                        "Field 'handfeel' wajib diisi "
                                                                        + "untuk section WASHING");
                                }

                                if (grade == null) {
                                        throw new BadRequestException(
                                                        "Field 'grade' wajib diisi "
                                                                        + "untuk section WASHING");
                                }

                                if (washingType == null) {
                                        throw new BadRequestException(
                                                        "Field 'washingType' wajib diisi "
                                                                        + "untuk section WASHING. "
                                                                        + "Pilihan: BULK atau REPAIR");
                                }

                                if (inspectionTimeSlot == null) {
                                        throw new BadRequestException(
                                                        "Field 'inspectionTimeSlot' wajib diisi "
                                                                        + "untuk section WASHING");
                                }

                                if (process != null) {
                                        throw new BadRequestException(
                                                        "Section WASHING tidak menggunakan process");
                                }

                                break;

                        case "FINISHING":
                                if (handfeel != null
                                                || grade != null
                                                || washingType != null) {

                                        throw new BadRequestException(
                                                        "Section FINISHING tidak menggunakan "
                                                                        + "handfeel, grade, atau washingType");
                                }

                                if (process != null
                                                && !process.equals("WASH")
                                                && !process.equals("NON_WASH")) {

                                        throw new BadRequestException(
                                                        "Field 'process' untuk FINISHING "
                                                                        + "hanya boleh WASH atau NON_WASH");
                                }

                                break;

                        case "PACKING":
                                if (handfeel != null || grade != null || process != null || washingType != null) {
                                        throw new BadRequestException(
                                                        "Section PACKING tidak menggunakan "
                                                                        + "handfeel, grade, process, "
                                                                        + "atau washingType");
                                }

                                if (inspectionTimeSlot != null) {
                                        throw new BadRequestException(
                                                        "Section PACKING tidak menggunakan " + "inspectionTimeSlot");
                                }
                                break;

                        default:
                                throw new BadRequestException("Section '" + section + "' tidak didukung");
                }
        }

        private void applyRequestFields(Inspection inspection, InspectionRequest request, Line line,
                        int inspectedQuantity, int transferQuantity, Integer qtyCheckCartonToday, String section,
                        String handfeel, String grade, String process, WashingType washingType) {
                inspection.setBuyerName(request.getBuyerName().trim());
                inspection.setStyleCode(request.getStyleCode().trim());
                inspection.setColorName(request.getColorName().trim());

                inspection.setLine(line);
                inspection.setInspectQuantity(inspectedQuantity);
                inspection.setTransferQuantity(transferQuantity);

                inspection.setComment(request.getComment());

                inspection.setInspectionTimeSlot(
                                "PACKING".equals(section)
                                                ? null
                                                : normalizeOptional(request.getInspectionTimeSlot()));

                inspection.setHandfeel(handfeel);
                inspection.setGrade(grade);

                inspection.setProcess(
                                "FINISHING".equals(section)
                                                ? process
                                                : null);

                inspection.setWashingType(
                                "WASHING".equals(section)
                                                ? washingType
                                                : null);

                inspection.setQtyCheckCartonToday(
                                "PACKING".equals(section)
                                                ? qtyCheckCartonToday
                                                : null);
        }

        private void replaceDefects(Inspection inspection, List<InspectionDefectRequest> defects,
                        int inspectedQuantity) {
                inspection.getInspectionDefects().clear();

                if (defects == null || defects.isEmpty()) {
                        return;
                }

                int totalDefect = 0;
                for (int i = 0; i < defects.size(); i++) {
                        InspectionDefectRequest item = defects.get(i);

                        if (item == null) {
                                throw new BadRequestException("Item defects pada indeks " + i + " tidak boleh null");
                        }

                        String defectTypeId = item.getDefectTypeId();

                        if (defectTypeId == null || defectTypeId.isBlank()) {
                                throw new BadRequestException(
                                                "Field 'defectTypeId' wajib diisi " + "pada defects indeks " + i);
                        }

                        Integer quantity = item.getQuantity();
                        if (quantity == null) {
                                throw new BadRequestException(
                                                "Field 'quantity' wajib diisi " + "pada defects indeks " + i);
                        }

                        if (quantity < 1) {
                                throw new BadRequestException("Quantity defect minimal 1 pada defects indeks " + i);
                        }

                        DefectType defectType = defectTypeRepository
                                        .findById(defectTypeId.trim())
                                        .orElseThrow(() -> new ResourceNotFoundException("Defect type dengan ID "
                                                        + defectTypeId
                                                        + " tidak ditemukan"));

                        if (!defectType.isActive()) {
                                throw new BadRequestException(
                                                "Defect type dengan ID "
                                                                + defectTypeId
                                                                + " sudah tidak aktif");
                        }

                        try {
                                totalDefect = Math.addExact(totalDefect, quantity);
                        } catch (ArithmeticException ex) {
                                throw new BadRequestException(
                                                "Total quantity defect melebihi " + "batas angka yang diperbolehkan");
                        }

                        InspectionDefect defect = new InspectionDefect();

                        defect.setInspection(inspection);
                        defect.setDefectType(defectType);
                        defect.setQuantity(quantity);

                        inspection.getInspectionDefects().add(defect);
                }

                if (totalDefect > inspectedQuantity) {
                        throw new BadRequestException(
                                        "Total quantity defect (" + totalDefect
                                                        + ") tidak boleh melebihi inspectedQuantity ("
                                                        + inspectedQuantity + ")");
                }
        }

        private String normalizeOptional(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }

                return value.trim().toUpperCase(Locale.ROOT);
        }

        // ===================== SEARCH DEFECT =====================

        @Transactional(readOnly = true)
        public List<DefectArea> searchDefectAreas(String keyword) {
                if (keyword == null || keyword.isBlank()) {
                        return defectAreaRepository
                                        .findByActiveTrueOrderByNameAsc();
                }

                return defectAreaRepository
                                .findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(
                                                keyword.trim());
        }

        @Transactional(readOnly = true)
        public List<DefectType> searchDefectTypes(
                        String defectAreaId,
                        String keyword) {

                if (defectAreaId == null || defectAreaId.isBlank()) {
                        throw new BadRequestException("Field 'defectAreaId' wajib diisi terlebih dahulu");
                }

                DefectArea area = defectAreaRepository
                                .findById(defectAreaId.trim())
                                .orElseThrow(() -> new ResourceNotFoundException("Defect area tidak ditemukan"));

                if (!area.isActive()) {
                        throw new BadRequestException("Defect area tidak aktif");
                }

                if (keyword == null || keyword.isBlank()) {
                        return defectTypeRepository
                                        .findByDefectAreaAndActiveTrueOrderByNameAsc(area);
                }

                return defectTypeRepository
                                .findByDefectArea_IdAndNameContainingIgnoreCaseAndActiveTrue(
                                                defectAreaId.trim(),
                                                keyword.trim());
        }

        @Transactional(readOnly = true)
        public List<DefectSearchResponse> searchDefects(String defectAreaId, String keyword) {
                String areaId = (defectAreaId == null || defectAreaId.isBlank())
                                ? null
                                : defectAreaId.trim();

                String key = (keyword == null || keyword.isBlank())
                                ? null
                                : keyword.trim();

                List<DefectType> types;

                if (areaId != null) {
                        DefectArea area = defectAreaRepository
                                        .findById(areaId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Defect area tidak ditemukan"));

                        if (!area.isActive()) {
                                throw new BadRequestException("Defect area tidak aktif");
                        }

                        types = key == null
                                        ? defectTypeRepository.findByActiveTrueAndDefectArea_Id(areaId)
                                        : defectTypeRepository
                                                        .findByDefectArea_IdAndNameContainingIgnoreCaseAndActiveTrue(
                                                                        areaId, key);
                } else {
                        types = key == null
                                        ? defectTypeRepository.findByActiveTrue()
                                        : defectTypeRepository.findByActiveTrueAndNameContainingIgnoreCase(key);
                }

                return types.stream()
                                .filter(t -> t.getDefectArea() == null || t.getDefectArea().isActive())
                                .sorted(Comparator
                                                .comparing((DefectType t) -> t.getDefectArea() == null
                                                                ? ""
                                                                : t.getDefectArea().getName(),
                                                                String.CASE_INSENSITIVE_ORDER)
                                                .thenComparing(DefectType::getName,
                                                                String.CASE_INSENSITIVE_ORDER))
                                .map(DefectSearchResponse::from)
                                .toList();
        }

        // =========================================================

        private double calculatePercentage(int totalDefect, int inspectQuantity) {
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

                int goodQc = inspectQuantity - totalDefect;

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
                                goodQc,
                                inspection.getQtyCheckCartonToday(),
                                inspection.getTransferQuantity(),
                                inspection.getComment(),
                                inspection.getInspectionTimeSlot(),
                                inspection.getHandfeel(),
                                inspection.getGrade(),
                                inspection.getProcess(),
                                inspection.getWashingType(),
                                defectItems,
                                submittedBy != null ? submittedBy.getId() : null,
                                submittedBy != null ? submittedBy.getNama() : null,
                                inspection.getCreatedAt(),
                                inspection.getUpdatedAt());
        }
}