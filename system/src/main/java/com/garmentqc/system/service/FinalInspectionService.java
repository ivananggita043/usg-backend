
package com.garmentqc.system.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.FinalInspectionLookupResponse;
import com.garmentqc.system.dto.FinalInspectionProgressResponse;
import com.garmentqc.system.dto.FinalInspectionRequest;
import com.garmentqc.system.dto.FinalInspectionResponse;
import com.garmentqc.system.enums.DefectSeverity;
import com.garmentqc.system.enums.FinalInspectionPurpose;
import com.garmentqc.system.enums.FinalInspectionResult;
import com.garmentqc.system.enums.Section;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.Accessory;
import com.garmentqc.system.model.AqlLevel;
import com.garmentqc.system.model.AqlLotSizeCode;
import com.garmentqc.system.model.AqlSamplingPlan;
import com.garmentqc.system.model.Buyer;
import com.garmentqc.system.model.DefectArea;
import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.FinalInspection;
import com.garmentqc.system.model.FinalInspectionAccessory;
import com.garmentqc.system.model.FinalInspectionDefect;
import com.garmentqc.system.model.FinalInspectionStyle;
import com.garmentqc.system.model.FinalInspectionStyleColor;
import com.garmentqc.system.model.Style;
import com.garmentqc.system.model.StyleColor;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.AccessoryRepository;
import com.garmentqc.system.repository.AqlLevelRepository;
import com.garmentqc.system.repository.AqlLotSizeCodeRepository;
import com.garmentqc.system.repository.AqlSamplingPlanRepository;
import com.garmentqc.system.repository.BuyerRepository;
import com.garmentqc.system.repository.DefectTypeRepository;
import com.garmentqc.system.repository.FinalInspectionRepository;
import com.garmentqc.system.repository.StyleColorRepository;
import com.garmentqc.system.repository.StyleRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinalInspectionService {

        private static final String INSPECTION_LEVEL = "II";

        private final FinalInspectionRepository finalInspectionRepository;
        private final AqlLevelRepository aqlLevelRepository;
        private final AqlLotSizeCodeRepository aqlLotSizeCodeRepository;
        private final AqlSamplingPlanRepository aqlSamplingPlanRepository;
        private final AccessoryRepository accessoryRepository;
        private final DefectTypeRepository defectTypeRepository;
        private final BuyerRepository buyerRepository;
        private final StyleRepository styleRepository;
        private final StyleColorRepository styleColorRepository;
        private final EntityManager entityManager;

        @Transactional(readOnly = true)
        public List<FinalInspectionLookupResponse> getAccessories(String keyword, User currentUser) {
                validateFinalSection(currentUser);

                List<Accessory> accessories;
                if (keyword == null || keyword.isBlank()) {
                        accessories = accessoryRepository.findByActiveTrueOrderByCategoryAscSortOrderAscNameAsc();
                } else {
                        accessories = accessoryRepository
                                        .findByActiveTrueAndNameContainingIgnoreCaseOrderByCategoryAscSortOrderAscNameAsc(
                                                        keyword.trim());
                }

                return accessories.stream()
                                .map(item -> new FinalInspectionLookupResponse(
                                                item.getId(),
                                                item.getName(),
                                                null,
                                                null,
                                                item.getCategory() != null
                                                                ? item.getCategory().name()
                                                                : null))
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<FinalInspectionLookupResponse> getFinalDefects(String keyword, User currentUser) {
                validateFinalSection(currentUser);

                List<DefectType> defects = new ArrayList<>();
                if (keyword == null || keyword.isBlank()) {
                        // defects.addAll(defectTypeRepository.findBySectionAndActiveTrueOrderByNameAsc(Section.FINAL));
                        defects.addAll(defectTypeRepository
                                        .findBySectionAndActiveTrueOrderByNameAsc(Section.FINISHING));
                } else {
                        String normalizedKeyword = keyword.trim();

                        defects.addAll(
                                        defectTypeRepository
                                                        .findBySectionAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
                                                                        Section.FINAL,
                                                                        normalizedKeyword));

                        defects.addAll(
                                        defectTypeRepository
                                                        .findBySectionAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
                                                                        Section.FINISHING,
                                                                        normalizedKeyword));
                }

                return defects.stream()
                                .map(item -> {
                                        DefectArea area = item.getDefectArea();

                                        return new FinalInspectionLookupResponse(
                                                        item.getId(),
                                                        item.getName(),
                                                        area != null ? area.getId() : null,
                                                        area != null ? area.getName() : null,
                                                        item.getCategory() != null
                                                                        ? item.getCategory().name()
                                                                        : null);
                                })
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<FinalInspectionLookupResponse> getBuyers(String keyword, User currentUser) {
                validateFinalSection(currentUser);

                String normalizedKeyword = normalizeKeyword(keyword);

                return buyerRepository.findAll().stream()
                                .filter(buyer -> matches(buyer.getName(), normalizedKeyword))
                                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                                .map(buyer -> new FinalInspectionLookupResponse(
                                                buyer.getId(),
                                                buyer.getName(),
                                                null,
                                                null,
                                                null))
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<FinalInspectionLookupResponse> getStyles(String buyerId, String keyword, User currentUser) {
                validateFinalSection(currentUser);

                if (buyerId == null || buyerId.isBlank()) {
                        throw new BadRequestException("Buyer wajib dipilih");
                }

                Buyer buyer = validateBuyer(buyerId);

                String normalizedKeyword = normalizeKeyword(keyword);

                return styleRepository.findAll().stream()
                                .filter(style -> style.getBuyer() != null
                                                && style.getBuyer().getId().equals(buyer.getId()))
                                .filter(style -> matches(style.getStyleCode(), normalizedKeyword))
                                .sorted((a, b) -> a.getStyleCode().compareToIgnoreCase(b.getStyleCode()))
                                .map(style -> new FinalInspectionLookupResponse(
                                                style.getId(),
                                                style.getStyleCode(),
                                                null,
                                                null,
                                                null))
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<FinalInspectionLookupResponse> getColors(String styleId, String keyword, User currentUser) {
                validateFinalSection(currentUser);

                if (styleId == null || styleId.isBlank()) {
                        throw new BadRequestException("Style wajib dipilih");
                }

                Style style = styleRepository.findById(styleId.trim())
                                .orElseThrow(() -> new ResourceNotFoundException("Style tidak ditemukan"));

                String normalizedKeyword = normalizeKeyword(keyword);

                return styleColorRepository.findAll().stream()
                                .filter(color -> color.getStyle() != null
                                                && color.getStyle().getId().equals(style.getId()))
                                .filter(color -> matches(color.getColorName(), normalizedKeyword))
                                .sorted((a, b) -> a.getColorName().compareToIgnoreCase(b.getColorName()))
                                .map(color -> new FinalInspectionLookupResponse(
                                                color.getId(),
                                                color.getColorName(),
                                                null,
                                                null,
                                                null))
                                .toList();
        }

        private String normalizeKeyword(String keyword) {
                return keyword == null ? "" : keyword.trim();
        }

        private boolean matches(String value, String keyword) {
                return keyword == null
                                || keyword.isBlank()
                                || (value != null && value.toLowerCase(Locale.ROOT)
                                                .contains(keyword.toLowerCase(Locale.ROOT)));
        }

        @Transactional(readOnly = true)
        public FinalInspectionProgressResponse getProgressByPoNumber(String poNumber, User currentUser) {
                validateFinalSection(currentUser);

                if (poNumber == null || poNumber.isBlank()) {
                        throw new BadRequestException("PO Number wajib diisi");
                }

                String normalizedPoNumber = poNumber.trim();

                boolean pre20 = finalInspectionRepository.existsByPoNumberAndPurpose(
                                normalizedPoNumber,
                                FinalInspectionPurpose.PRE_FINAL_20_PERCENT);

                boolean re20 = finalInspectionRepository.existsByPoNumberAndPurpose(
                                normalizedPoNumber,
                                FinalInspectionPurpose.RE_FINAL_20_PERCENT);

                boolean pre60 = finalInspectionRepository.existsByPoNumberAndPurpose(
                                normalizedPoNumber,
                                FinalInspectionPurpose.PRE_FINAL_60_PERCENT);

                boolean re60 = finalInspectionRepository.existsByPoNumberAndPurpose(
                                normalizedPoNumber,
                                FinalInspectionPurpose.RE_FINAL_60_PERCENT);

                boolean pre80 = finalInspectionRepository.existsByPoNumberAndPurpose(
                                normalizedPoNumber,
                                FinalInspectionPurpose.PRE_FINAL_80_PERCENT);

                boolean re80 = finalInspectionRepository.existsByPoNumberAndPurpose(
                                normalizedPoNumber,
                                FinalInspectionPurpose.RE_FINAL_80_PERCENT);

                return new FinalInspectionProgressResponse(
                                normalizedPoNumber,
                                pre20,
                                re20,
                                pre60,
                                re60,
                                pre80,
                                re80);
        }

        @Transactional(readOnly = true)
        public Page<FinalInspectionResponse> getAllPaginated(User currentUser, Pageable pageable) {
                validateFinalSection(currentUser);

                return finalInspectionRepository.findAll(pageable).map(this::toResponse);
        }

        @Transactional
        public FinalInspectionResponse create(FinalInspectionRequest request, User currentUser) {
                validateRequest(request);
                validateFinalSection(currentUser);

                validateReFinalPrerequisite(request, null);
                validatePreFinalUnique(request, null);
                validateBuyerStyleColor(request);

                FinalInspection inspection = new FinalInspection();

                applyRequestFields(inspection, request);
                applyAqlSampling(inspection, request);

                replaceStyles(inspection, request.getStyles());
                replaceAccessories(inspection, request.getAccessories());
                replaceDefects(inspection, request.getDefects());

                applyInspectionResult(inspection);

                FinalInspection saved = finalInspectionRepository.save(inspection);

                return toResponse(saved);
        }

        @Transactional(readOnly = true)
        public FinalInspectionResponse getById(String id, User currentUser) {
                validateFinalSection(currentUser);

                return toResponse(findInspection(id));
        }

        @Transactional(readOnly = true)
        public List<FinalInspectionResponse> getAll(User currentUser) {
                validateFinalSection(currentUser);

                return finalInspectionRepository.findAll().stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional
        public FinalInspectionResponse update(String id, FinalInspectionRequest request, User currentUser) {
                validateFinalSection(currentUser);
                validateRequest(request);

                FinalInspection inspection = findInspection(id);

                validateReFinalPrerequisite(request, inspection.getId());
                validatePreFinalUnique(request, inspection.getId());
                validateBuyerStyleColor(request);

                applyRequestFields(inspection, request);
                applyAqlSampling(inspection, request);

                replaceStyles(inspection, request.getStyles());
                replaceAccessories(inspection, request.getAccessories());
                replaceDefects(inspection, request.getDefects());

                applyInspectionResult(inspection);

                FinalInspection saved = finalInspectionRepository.save(inspection);

                return toResponse(saved);
        }

        @Transactional
        public void delete(String id, User currentUser) {
                validateFinalSection(currentUser);

                FinalInspection inspection = findInspection(id);

                finalInspectionRepository.delete(inspection);
        }

        private void validateFinalSection(User user) {
                if (user == null || user.getRole() == null) {
                        throw new UnauthorizedException("Akses ditolak");
                }

                String role = user.getRole().name();

                if ("SUPER_ADMIN".equals(role) || "VISITOR".equals(role)) {
                        return;
                }

                if (!"USER".equals(role) && !"ADMIN".equals(role)) {
                        throw new UnauthorizedException("Akses ditolak");
                }

                if (user.getSection() == null || !"FINAL".equals(user.getSection().name())) {
                        throw new UnauthorizedException("Akses hanya untuk pengguna section FINAL");
                }
        }

        private void validateRequest(FinalInspectionRequest request) {
                if (request == null) {
                        throw new BadRequestException("Request body wajib diisi");
                }

                if (request.getInspectionDate() == null) {
                        throw new BadRequestException("Inspection date wajib diisi");
                }

                if (request.getBuyerId() == null || request.getBuyerId().isBlank()) {
                        throw new BadRequestException("Buyer wajib dipilih");
                }

                if (request.getPurpose() == null) {
                        throw new BadRequestException("Purpose wajib dipilih");
                }

                if (request.getAqlLevel() == null || request.getAqlLevel().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BadRequestException("AQL level wajib diisi dengan benar");
                }

                if (request.getCartonSampling() != null && request.getCartonSampling() < 0) {
                        throw new BadRequestException("Carton sampling tidak boleh negatif");
                }

                // if (request.getTotalInspected() != null
                // && request.getTotalInspected() < 0) {

                // throw new BadRequestException(
                // "Total inspected tidak boleh negatif");
                // }
        }

        private void validatePreFinalUnique(FinalInspectionRequest request, String currentInspectionId) {
                FinalInspectionPurpose purpose = request.getPurpose();

                if (purpose != FinalInspectionPurpose.PRE_FINAL_20_PERCENT
                                && purpose != FinalInspectionPurpose.PRE_FINAL_60_PERCENT
                                && purpose != FinalInspectionPurpose.PRE_FINAL_80_PERCENT) {
                        return;
                }

                String poNumber = request.getPoNumber();

                if (poNumber == null || poNumber.isBlank()) {
                        throw new BadRequestException("PO Number wajib diisi untuk proses Pre-Final");
                }

                String normalizedPoNumber = poNumber.trim();

                boolean alreadyExists;

                if (currentInspectionId == null) {
                        alreadyExists = finalInspectionRepository.existsByPoNumberAndPurpose(normalizedPoNumber,
                                        purpose);
                } else {
                        alreadyExists = finalInspectionRepository.existsByPoNumberAndPurposeAndIdNot(
                                        normalizedPoNumber,
                                        purpose,
                                        currentInspectionId);
                }

                if (alreadyExists) {
                        throw new BadRequestException(
                                        purpose.getLabel() + " untuk PO Number " + normalizedPoNumber
                                                        + " sudah pernah dibuat");
                }
        }

        private void validateReFinalPrerequisite(FinalInspectionRequest request, String currentInspectionId) {
                FinalInspectionPurpose requiredPreFinal;

                switch (request.getPurpose()) {
                        case RE_FINAL_20_PERCENT ->
                                requiredPreFinal = FinalInspectionPurpose.PRE_FINAL_20_PERCENT;

                        case RE_FINAL_60_PERCENT ->
                                requiredPreFinal = FinalInspectionPurpose.PRE_FINAL_60_PERCENT;

                        case RE_FINAL_80_PERCENT ->
                                requiredPreFinal = FinalInspectionPurpose.PRE_FINAL_80_PERCENT;

                        default -> {
                                return;
                        }
                }

                String poNumber = request.getPoNumber();

                if (poNumber == null || poNumber.isBlank()) {
                        throw new BadRequestException("PO Number wajib diisi untuk proses Re-Final");
                }

                String normalizedPoNumber = poNumber.trim();

                boolean preFinalExists;

                if (currentInspectionId == null) {
                        preFinalExists = finalInspectionRepository
                                        .existsByPoNumberAndPurpose(
                                                        normalizedPoNumber,
                                                        requiredPreFinal);
                } else {
                        preFinalExists = finalInspectionRepository
                                        .existsByPoNumberAndPurposeAndIdNot(
                                                        normalizedPoNumber,
                                                        requiredPreFinal,
                                                        currentInspectionId);
                }

                if (!preFinalExists) {
                        throw new BadRequestException(
                                        "Re-Final tidak dapat dilakukan karena " + requiredPreFinal.getLabel()
                                                        + " untuk PO Number " + normalizedPoNumber + " belum tersedia");
                }
        }

        private Integer calculateLotBatchSize(List<FinalInspectionRequest.StyleItem> styles) {
                if (styles == null || styles.isEmpty()) {
                        throw new BadRequestException("Style wajib dipilih");
                }

                int totalReadyQty = 0;

                try {
                        for (FinalInspectionRequest.StyleItem style : styles) {
                                if (style == null || style.getColors() == null) {
                                        continue;
                                }

                                for (FinalInspectionRequest.ColorItem color : style.getColors()) {

                                        if (color == null || color.getQtyReady() == null) {
                                                continue;
                                        }

                                        if (color.getQtyReady() < 0) {
                                                throw new BadRequestException("Qty ready tidak boleh negatif");
                                        }

                                        totalReadyQty = Math.addExact(
                                                        totalReadyQty,
                                                        color.getQtyReady());
                                }
                        }
                } catch (ArithmeticException e) {
                        throw new BadRequestException("Total Ready Qty melebihi batas angka yang diperbolehkan");
                }

                if (totalReadyQty <= 0) {
                        throw new BadRequestException(
                                        "Total Ready Qty harus lebih besar dari 0 untuk menentukan Lot/Batch Size");
                }

                return totalReadyQty;
        }

        private FinalInspection findInspection(String id) {
                if (id == null || id.isBlank()) {
                        throw new BadRequestException("ID Final Inspection wajib diisi");
                }

                return finalInspectionRepository.findById(id.trim())
                                .orElseThrow(() -> new ResourceNotFoundException("Final Inspection tidak ditemukan"));
        }

        private Buyer validateBuyer(String buyerId) {
                if (buyerId == null || buyerId.isBlank()) {
                        throw new BadRequestException("Buyer wajib dipilih");
                }

                return buyerRepository.findById(buyerId.trim())
                                .orElseThrow(() -> new ResourceNotFoundException("Buyer tidak ditemukan"));
        }

        private Style validateStyle(String styleId, Buyer buyer) {
                if (styleId == null || styleId.isBlank()) {
                        throw new BadRequestException("Style wajib dipilih");
                }

                Style style = styleRepository.findById(styleId.trim())
                                .orElseThrow(() -> new ResourceNotFoundException("Style tidak ditemukan"));

                if (style.getBuyer() == null || !style.getBuyer().getId().equals(buyer.getId())) {
                        throw new BadRequestException("Style tidak sesuai dengan buyer yang dipilih");
                }

                return style;
        }

        private StyleColor validateStyleColor(String styleColorId, Style style) {
                if (styleColorId == null || styleColorId.isBlank()) {
                        throw new BadRequestException("Color wajib dipilih");
                }

                StyleColor styleColor = styleColorRepository
                                .findById(styleColorId.trim())
                                .orElseThrow(() -> new ResourceNotFoundException("Style color tidak ditemukan"));

                if (styleColor.getStyle() == null || !styleColor.getStyle().getId().equals(style.getId())) {
                        throw new BadRequestException("Color tidak sesuai dengan style yang dipilih");
                }

                return styleColor;
        }

        private void validateBuyerStyleColor(FinalInspectionRequest request) {
                Buyer buyer = validateBuyer(request.getBuyerId());

                if (request.getStyles() == null || request.getStyles().size() != 1) {
                        throw new BadRequestException("Final Inspection hanya dapat menggunakan satu style");
                }

                FinalInspectionRequest.StyleItem item = request.getStyles().get(0);

                if (item == null) {
                        throw new BadRequestException("Style wajib dipilih");
                }

                Style style = validateStyle(item.getStyleId(), buyer);

                if (item.getColors() == null || item.getColors().isEmpty()) {
                        throw new BadRequestException(
                                        "Minimal satu warna wajib dipilih untuk style " + style.getStyleCode());
                }

                Set<String> usedColorIds = new HashSet<>();

                for (int i = 0; i < item.getColors().size(); i++) {
                        FinalInspectionRequest.ColorItem colorItem = item.getColors().get(i);

                        if (colorItem == null) {
                                throw new BadRequestException("Color pada indeks " + i + " tidak boleh null");
                        }

                        String colorId = requireId(
                                        colorItem.getStyleColorId(),
                                        "styleColorId",
                                        i);

                        if (!usedColorIds.add(colorId)) {
                                throw new BadRequestException(
                                                "Warna yang sama tidak boleh ditambahkan lebih dari satu kali");
                        }

                        validateStyleColor(colorId, style);

                        if (colorItem.getQtyOrder() != null && colorItem.getQtyOrder() < 0) {
                                throw new BadRequestException("Qty order tidak boleh negatif");
                        }

                        if (colorItem.getQtyReady() != null && colorItem.getQtyReady() < 0) {
                                throw new BadRequestException("Qty ready tidak boleh negatif");
                        }
                }
                calculateLotBatchSize(request.getStyles());
        }

        private void applyRequestFields(FinalInspection inspection, FinalInspectionRequest request) {
                inspection.setInspectionDate(request.getInspectionDate());
                inspection.setBuyer(validateBuyer(request.getBuyerId()));
                inspection.setPoNumber(normalizeOptional(request.getPoNumber()));
                inspection.setDeliveryDate(request.getDeliveryDate());
                inspection.setContentFabric(normalizeOptional(request.getContentFabric()));
                inspection.setPurpose(request.getPurpose());
                inspection.setLotBatchSize(calculateLotBatchSize(request.getStyles()));
                inspection.setAqlLevel(request.getAqlLevel());
                inspection.setCartonSampling(request.getCartonSampling());
                // inspection.setTotalInspected(request.getTotalInspected());
                inspection.setAdditionalComment(normalizeOptional(request.getAdditionalComment()));
                inspection.setInspectedBy(normalizeOptional(request.getInspectedBy()));
                inspection.setFinishingIncharge(normalizeOptional(request.getFinishingIncharge()));
        }

        private void applyAqlSampling(FinalInspection inspection, FinalInspectionRequest request) {

                AqlLevel aqlLevel = aqlLevelRepository
                                .findByAqlValueAndActiveTrue(request.getAqlLevel())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "AQL level tidak ditemukan atau sudah tidak aktif"));

                AqlLotSizeCode lotSizeCode = aqlLotSizeCodeRepository
                                .findByLotSizeAndInspectionLevel(
                                                inspection.getLotBatchSize(),
                                                INSPECTION_LEVEL)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Lot/batch size tidak memiliki sampling code"));

                AqlSamplingPlan samplingPlan = aqlSamplingPlanRepository
                                .findByLotSizeCode_IdAndAqlLevel_Id(
                                                lotSizeCode.getId(),
                                                aqlLevel.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "AQL sampling plan tidak ditemukan untuk kombinasi lot size dan AQL tersebut"));

                String effectiveCodeLetter = samplingPlan.getEffectiveCodeLetter();

                if (effectiveCodeLetter == null || effectiveCodeLetter.isBlank()) {
                        effectiveCodeLetter = lotSizeCode.getCodeLetter();
                }

                inspection.setAqlLevel(aqlLevel.getAqlValue());
                inspection.setLotBatchSize(calculateLotBatchSize(request.getStyles()));
                inspection.setSamplingCodeLetter(effectiveCodeLetter);
                inspection.setSamplingSampleSize(samplingPlan.getSampleSize());
                inspection.setAcceptanceNumber(samplingPlan.getAcceptanceNumber());
                inspection.setRejectionNumber(samplingPlan.getRejectionNumber());
                inspection.setTotalInspected(samplingPlan.getSampleSize());
        }

        private void applyInspectionResult(FinalInspection inspection) {
                Integer totalMajor = inspection.getTotalMajor();
                Integer acceptanceNumber = inspection.getAcceptanceNumber();
                Integer rejectionNumber = inspection.getRejectionNumber();

                if (totalMajor == null || acceptanceNumber == null || rejectionNumber == null) {
                        throw new BadRequestException("Total MAJOR dan batas AQL AC/RE wajib tersedia");
                }

                if (acceptanceNumber < 0 || rejectionNumber < 0 || rejectionNumber <= acceptanceNumber) {
                        throw new BadRequestException(
                                        "Konfigurasi AQL tidak valid: Rejection Number harus lebih besar dari Acceptance Number");
                }

                if (totalMajor >= rejectionNumber) {
                        inspection.setInspectionResult(FinalInspectionResult.SHIPMENT_REJECT);
                        return;
                }

                if (totalMajor <= acceptanceNumber) {
                        inspection.setInspectionResult(FinalInspectionResult.SHIPMENT_RELEASE);
                        return;
                }
                throw new BadRequestException("Jumlah defect MAJOR berada di antara batas ACCEPT dan REJECT. "
                                + "Periksa konfigurasi AQL Sampling Plan.");
        }

        private void replaceStyles(FinalInspection inspection, List<FinalInspectionRequest.StyleItem> items) {
                inspection.getStyles().clear();
                entityManager.flush();
                if (items == null || items.isEmpty()) {
                        throw new BadRequestException("Style wajib dipilih");
                }

                if (items.size() != 1) {
                        throw new BadRequestException("Final Inspection hanya dapat menggunakan satu style");
                }

                Buyer buyer = inspection.getBuyer();
                FinalInspectionRequest.StyleItem item = items.get(0);

                if (item == null) {
                        throw new BadRequestException("Style wajib dipilih");
                }

                Style style = validateStyle(item.getStyleId(), buyer);
                FinalInspectionStyle inspectionStyle = new FinalInspectionStyle();

                inspectionStyle.setFinalInspection(inspection);
                inspectionStyle.setStyle(style);
                inspectionStyle.setSortOrder(0);

                replaceStyleColors(
                                inspectionStyle,
                                style,
                                item.getColors());

                inspection.getStyles().add(inspectionStyle);
        }

        private void replaceStyleColors(FinalInspectionStyle inspectionStyle, Style style,
                        List<FinalInspectionRequest.ColorItem> items) {
                if (items == null || items.isEmpty()) {
                        throw new BadRequestException("Minimal satu warna wajib dipilih untuk setiap style");
                }

                Set<String> usedColorIds = new HashSet<>();
                for (int i = 0; i < items.size(); i++) {
                        FinalInspectionRequest.ColorItem item = items.get(i);
                        if (item == null) {
                                throw new BadRequestException("Color pada indeks " + i + " tidak boleh null");
                        }

                        String colorId = requireId(
                                        item.getStyleColorId(),
                                        "styleColorId",
                                        i);

                        if (!usedColorIds.add(colorId)) {
                                throw new BadRequestException(
                                                "Warna yang sama tidak boleh ditambahkan lebih dari satu kali pada style yang sama");
                        }

                        StyleColor styleColor = validateStyleColor(colorId, style);

                        if (item.getQtyOrder() != null && item.getQtyOrder() < 0) {
                                throw new BadRequestException("Qty order tidak boleh negatif");
                        }

                        if (item.getQtyReady() != null && item.getQtyReady() < 0) {
                                throw new BadRequestException("Qty ready tidak boleh negatif");
                        }

                        FinalInspectionStyleColor inspectionColor = new FinalInspectionStyleColor();

                        inspectionColor.setFinalInspectionStyle(inspectionStyle);
                        inspectionColor.setStyleColor(styleColor);
                        inspectionColor.setQtyOrder(item.getQtyOrder());
                        inspectionColor.setQtyReady(item.getQtyReady());
                        inspectionStyle.getColors().add(inspectionColor);
                }
        }

        private void replaceAccessories(FinalInspection inspection, List<FinalInspectionRequest.AccessoryItem> items) {

                inspection.getAccessories().clear();
                entityManager.flush();

                if (items == null || items.isEmpty()) {
                        return;
                }

                Set<String> usedAccessoryIds = new HashSet<>();
                for (int i = 0; i < items.size(); i++) {

                        FinalInspectionRequest.AccessoryItem item = items.get(i);

                        if (item == null) {
                                throw new BadRequestException("Accessory pada indeks " + i + " tidak boleh null");
                        }

                        String accessoryId = requireId(
                                        item.getAccessoryId(),
                                        "accessoryId",
                                        i);

                        if (!usedAccessoryIds.add(accessoryId)) {
                                throw new BadRequestException(
                                                "Accessory yang sama tidak boleh ditambahkan lebih dari satu kali");
                        }

                        Accessory accessory = accessoryRepository
                                        .findById(accessoryId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Accessory dengan ID " + accessoryId + " tidak ditemukan"));

                        if (!accessory.isActive()) {
                                throw new BadRequestException(
                                                "Accessory " + accessory.getName() + " sudah tidak aktif");
                        }

                        if (item.getStatus() == null) {
                                throw new BadRequestException("Status accessory wajib dipilih");
                        }

                        FinalInspectionAccessory inspectionAccessory = new FinalInspectionAccessory();

                        inspectionAccessory.setFinalInspection(inspection);
                        inspectionAccessory.setAccessory(accessory);
                        inspectionAccessory.setStatus(item.getStatus());
                        inspection.getAccessories().add(inspectionAccessory);
                }
        }

        private void replaceDefects(FinalInspection inspection, List<FinalInspectionRequest.DefectItem> items) {
                inspection.getDefects().clear();
                entityManager.flush();

                int totalMajor = 0;
                int totalMinor = 0;

                Set<String> usedDefects = new HashSet<>();

                if (items != null) {
                        for (int i = 0; i < items.size(); i++) {
                                FinalInspectionRequest.DefectItem item = items.get(i);
                                if (item == null) {
                                        throw new BadRequestException("Defect pada indeks " + i + " tidak boleh null");
                                }

                                String defectTypeId = requireId(
                                                item.getDefectTypeId(),
                                                "defectTypeId",
                                                i);

                                DefectSeverity severity = item.getSeverity();

                                if (severity == null) {
                                        throw new BadRequestException("Severity defect wajib dipilih");
                                }

                                Integer quantity = item.getQuantity();
                                if (quantity == null || quantity <= 0) {
                                        throw new BadRequestException("Quantity defect harus lebih besar dari 0");
                                }

                                String uniqueKey = defectTypeId + "|" + severity.name();
                                if (!usedDefects.add(uniqueKey)) {
                                        throw new BadRequestException(
                                                        "Kombinasi defect type dan severity tidak boleh duplikat");
                                }

                                DefectType defectType = defectTypeRepository
                                                .findById(defectTypeId)
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Defect type dengan ID " + defectTypeId
                                                                                + " tidak ditemukan"));

                                if (!defectType.isActive()) {
                                        throw new BadRequestException(
                                                        "Defect type " + defectType.getName() + " sudah tidak aktif");
                                }

                                if (defectType.getSection() != Section.FINISHING) {
                                        throw new BadRequestException(
                                                        "Final Inspection hanya dapat menggunakan defect dari section FINISHING");
                                }

                                try {
                                        switch (severity) {
                                                case MAJOR ->
                                                        totalMajor = Math.addExact(totalMajor, quantity);

                                                case MINOR ->
                                                        totalMinor = Math.addExact(totalMinor, quantity);
                                        }
                                } catch (ArithmeticException ex) {
                                        throw new BadRequestException(
                                                        "Total quantity defect melebihi batas angka yang diperbolehkan");
                                }

                                FinalInspectionDefect defect = new FinalInspectionDefect();

                                defect.setFinalInspection(inspection);
                                defect.setDefectType(defectType);
                                defect.setSeverity(severity);
                                defect.setQuantity(quantity);
                                inspection.getDefects().add(defect);
                        }
                }

                int totalDefect;

                try {
                        totalDefect = Math.addExact(totalMajor, totalMinor);
                } catch (ArithmeticException ex) {
                        throw new BadRequestException("Total defect melebihi batas angka yang diperbolehkan");
                }

                inspection.setTotalMajor(totalMajor);
                inspection.setTotalMinor(totalMinor);
                inspection.setTotalDefect(totalDefect);
        }

        private FinalInspectionResponse toResponse(FinalInspection inspection) {
                FinalInspectionResponse response = new FinalInspectionResponse();

                response.setId(inspection.getId());
                response.setInspectionDate(inspection.getInspectionDate());

                if (inspection.getBuyer() != null) {
                        response.setBuyerId(inspection.getBuyer().getId());
                        response.setBuyerName(inspection.getBuyer().getName());
                }

                response.setPoNumber(inspection.getPoNumber());
                response.setDeliveryDate(inspection.getDeliveryDate());
                response.setContentFabric(inspection.getContentFabric());
                response.setPurpose(inspection.getPurpose());
                response.setPurposeLabel(inspection.getPurpose() != null
                                ? inspection.getPurpose().getLabel()
                                : null);
                response.setLotBatchSize(inspection.getLotBatchSize());
                response.setAqlLevel(inspection.getAqlLevel());
                response.setSamplingCodeLetter(inspection.getSamplingCodeLetter());
                response.setSamplingSampleSize(inspection.getSamplingSampleSize());
                response.setAcceptanceNumber(inspection.getAcceptanceNumber());
                response.setRejectionNumber(inspection.getRejectionNumber());
                response.setCartonSampling(inspection.getCartonSampling());
                response.setTotalInspected(inspection.getTotalInspected());
                response.setTotalMajor(inspection.getTotalMajor());
                response.setTotalMinor(inspection.getTotalMinor());
                response.setTotalDefect(inspection.getTotalDefect());
                response.setAdditionalComment(inspection.getAdditionalComment());
                response.setInspectionResult(inspection.getInspectionResult());
                response.setInspectedBy(inspection.getInspectedBy());
                response.setFinishingIncharge(inspection.getFinishingIncharge());
                response.setStyles(toStyleResponses(inspection.getStyles()));
                response.setAccessories(toAccessoryResponses(inspection.getAccessories()));
                response.setDefects(toDefectResponses(inspection.getDefects()));

                return response;
        }

        private List<FinalInspectionResponse.StyleItem> toStyleResponses(List<FinalInspectionStyle> styles) {
                if (styles == null) {
                        return List.of();
                }

                return styles.stream()
                                .map(style -> {
                                        FinalInspectionResponse.StyleItem item = new FinalInspectionResponse.StyleItem();
                                        item.setId(style.getId());

                                        if (style.getStyle() != null) {
                                                item.setStyleId(style.getStyle().getId());
                                                item.setStyleCode(style.getStyle().getStyleCode());
                                        }

                                        item.setSortOrder(style.getSortOrder());
                                        List<FinalInspectionResponse.ColorItem> colors = style.getColors() == null
                                                        ? List.of()
                                                        : style.getColors().stream()
                                                                        .map(color -> {
                                                                                FinalInspectionResponse.ColorItem colorItem = new FinalInspectionResponse.ColorItem();
                                                                                colorItem.setId(color.getId());
                                                                                if (color.getStyleColor() != null) {
                                                                                        colorItem.setStyleColorId(color
                                                                                                        .getStyleColor()
                                                                                                        .getId());
                                                                                        colorItem.setColorName(color
                                                                                                        .getStyleColor()
                                                                                                        .getColorName());
                                                                                }
                                                                                colorItem.setQtyOrder(
                                                                                                color.getQtyOrder());
                                                                                colorItem.setQtyReady(
                                                                                                color.getQtyReady());

                                                                                return colorItem;
                                                                        })
                                                                        .toList();

                                        item.setColors(colors);

                                        return item;
                                })
                                .toList();
        }

        private List<FinalInspectionResponse.AccessoryItem> toAccessoryResponses(
                        List<FinalInspectionAccessory> accessories) {
                if (accessories == null) {
                        return List.of();
                }

                return accessories.stream()
                                .map(accessory -> {
                                        FinalInspectionResponse.AccessoryItem item = new FinalInspectionResponse.AccessoryItem();

                                        item.setId(accessory.getId());
                                        item.setAccessoryId(accessory.getAccessory().getId());
                                        item.setAccessoryName(accessory.getAccessory().getName());
                                        item.setCategory(
                                                        accessory.getAccessory().getCategory() != null
                                                                        ? accessory.getAccessory().getCategory().name()
                                                                        : null);
                                        item.setStatus(accessory.getStatus());
                                        return item;
                                })
                                .toList();
        }

        private List<FinalInspectionResponse.DefectItem> toDefectResponses(List<FinalInspectionDefect> defects) {
                if (defects == null) {
                        return List.of();
                }

                return defects.stream()
                                .map(defect -> {
                                        FinalInspectionResponse.DefectItem item = new FinalInspectionResponse.DefectItem();
                                        item.setId(defect.getId());
                                        item.setDefectTypeId(defect.getDefectType().getId());
                                        item.setDefectName(defect.getDefectType().getName());
                                        if (defect.getDefectType().getDefectArea() != null) {
                                                item.setDefectAreaId(defect.getDefectType().getDefectArea().getId());
                                                item.setDefectAreaName(
                                                                defect.getDefectType().getDefectArea().getName());
                                        }

                                        item.setSeverity(defect.getSeverity());
                                        item.setQuantity(defect.getQuantity());
                                        return item;
                                })
                                .toList();
        }

        private String requireId(String id, String fieldName, int index) {
                if (id == null || id.isBlank()) {
                        throw new BadRequestException("Field '" + fieldName + "' wajib diisi pada indeks " + index);
                }
                return id.trim();
        }

        private String normalizeOptional(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }
                return value.trim();
        }
}
