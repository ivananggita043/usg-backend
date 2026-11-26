
package com.garmentqc.system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.garmentqc.system.enums.Section;
import com.garmentqc.system.enums.WashingType;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.Attendance;
import com.garmentqc.system.model.Buyer;
import com.garmentqc.system.model.DefectArea;
import com.garmentqc.system.model.DefectType;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.InspectionDefect;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.Style;
import com.garmentqc.system.model.StyleColor;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.AttendanceRepository;
import com.garmentqc.system.repository.BuyerRepository;
import com.garmentqc.system.repository.DefectAreaRepository;
import com.garmentqc.system.repository.DefectTypeRepository;
import com.garmentqc.system.repository.InspectionRepository;
import com.garmentqc.system.repository.LineRepository;
import com.garmentqc.system.repository.StyleColorRepository;
import com.garmentqc.system.repository.StyleRepository;
import com.garmentqc.system.repository.UserLineRepository;
import com.garmentqc.system.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InspectionService {
        private static final Set<String> SUPPORTED_SECTIONS = Set.of("SEWING", "FINISHING", "WASHING", "PACKING");

        private static final String SUPER_ADMIN = "SUPER_ADMIN";
        private static final String ADMIN = "ADMIN";
        private static final String USER = "USER";
        private static final String VISITOR = "VISITOR";

        private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");

        private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

        private static final int GRACE_PERIOD_MINUTES = 30;

        private final InspectionRepository inspectionRepository;
        private final LineRepository lineRepository;
        private final DefectTypeRepository defectTypeRepository;
        private final UserRepository userRepository;
        private final DefectAreaRepository defectAreaRepository;
        private final UserLineRepository userLineRepository;
        private final AttendanceRepository attendanceRepository;
        private final BuyerRepository buyerRepository;
        private final StyleRepository styleRepository;
        private final StyleColorRepository styleColorRepository;
        private final DataSource dataSource;
        private final JdbcTemplate jdbcTemplate;

        @Transactional
        public InspectionResponse create(InspectionRequest request) {
                validateRequest(request);

                User submittedBy = getCurrentUser();

                validateQcAttendance(submittedBy);

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

                validateInspectionTimeSlot(section, inspectionTimeSlot);

                validateDuplicateTimeSlot(line, section, inspectionTimeSlot);

                int inspectedQuantity = validateInspectedQuantity(request.getInspectedQuantity());

                int transferQuantity = validateTransferQuantity(
                                request.getTransferQuantity(),
                                inspectedQuantity,
                                section);

                Inspection inspection = new Inspection();
                StyleColor styleColor = findOrCreateStyleColor(request);

                applyRequestFields(
                                inspection,
                                request,
                                line,
                                styleColor,
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

        private void validateQcAttendance(User user) {
                if (!USER.equals(user.getRole().name())) {
                        return;
                }

                LocalDate today = LocalDate.now(ZONE);

                Attendance attendance = attendanceRepository
                                .findByUser_IdAndAttendanceDate(user.getId(), today)
                                .orElseThrow(() -> new BadRequestException("Anda belum melakukan check-in hari ini. "
                                                + "Silakan check-in terlebih dahulu sebelum membuat inspeksi."));

                if (attendance.getCheckInAt() == null) {
                        throw new BadRequestException(
                                        "Anda belum melakukan check-in. " + "Silakan check-in terlebih dahulu.");
                }

                if (attendance.getCheckOutAt() != null) {
                        throw new BadRequestException(
                                        "Anda sudah melakukan check-out. " + "Tidak dapat menambahkan inspeksi baru.");
                }
        }

        private void validateInspectionTimeSlot(String section, String timeSlot) {
                if (!"SEWING".equals(section)
                                && !"FINISHING".equals(section)
                                && !"WASHING".equals(section)) {
                        return;
                }

                if (timeSlot == null || timeSlot.isBlank()) {
                        throw new BadRequestException("Inspection time slot wajib diisi.");
                }

                try {
                        String[] parts = timeSlot.split("-");

                        if (parts.length != 2) {
                                throw new BadRequestException("Format inspection time slot harus HH:mm-HH:mm.");
                        }

                        LocalTime start = LocalTime.parse(
                                        parts[0].trim(),
                                        SLOT_FORMAT);

                        LocalTime end = LocalTime.parse(
                                        parts[1].trim(),
                                        SLOT_FORMAT);

                        if (!end.equals(start.plusHours(1))) {
                                throw new BadRequestException("Durasi inspection time slot harus satu jam.");
                        }

                        LocalDateTime now = LocalDateTime.now(ZONE);
                        LocalDate today = LocalDate.now(ZONE);
                        LocalDateTime slotStart = LocalDateTime.of(today, start);
                        LocalDateTime slotEnd = LocalDateTime.of(today, end);

                        if (!slotEnd.isAfter(slotStart)) {
                                slotEnd = slotEnd.plusDays(1);
                        }

                        LocalDateTime allowedUntil = slotEnd.plusMinutes(GRACE_PERIOD_MINUTES);

                        if (now.isBefore(slotEnd)) {
                                throw new BadRequestException("Slot inspeksi belum selesai. "
                                                + "Inspeksi hanya dapat diinput setelah slot berakhir.");
                        }

                        if (now.isAfter(allowedUntil)) {
                                throw new BadRequestException("Waktu input inspeksi untuk slot " + timeSlot
                                                + " sudah melewati batas toleransi 30 menit.");
                        }

                } catch (DateTimeParseException e) {
                        throw new BadRequestException(
                                        "Format inspection time slot tidak valid. " + "Gunakan format HH:mm-HH:mm.");
                }
        }

        private void validateDuplicateTimeSlot(Line line, String section, String timeSlot) {
                if (!"SEWING".equals(section) && !"FINISHING".equals(section) && !"WASHING".equals(section)) {
                        return;
                }

                LocalDate today = LocalDate.now(ZONE);
                Instant startOfDay = today
                                .atStartOfDay(ZONE)
                                .toInstant();

                Instant endOfDay = today
                                .plusDays(1)
                                .atStartOfDay(ZONE)
                                .toInstant();

                boolean alreadyExists = inspectionRepository
                                .existsByLine_IdAndInspectionTimeSlotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                                line.getId(),
                                                timeSlot,
                                                startOfDay,
                                                endOfDay);

                if (alreadyExists) {
                        throw new BadRequestException("Inspeksi untuk line " + line.getName() + " pada slot " + timeSlot
                                        + " hari ini sudah pernah diinput. "
                                        + "Silakan gunakan fitur update untuk mengubah data.");
                }
        }

        @Transactional(readOnly = true)
        public InspectionResponse getById(String id) {
                Inspection inspection = findInspection(id);

                User currentUser = getCurrentUser();
                validateInspectionReadAccess(currentUser, inspection);

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

                if (selectedSection != null && !SUPPORTED_SECTIONS.contains(selectedSection)) {
                        throw new BadRequestException(
                                        "Section tidak valid. Pilihan: " + String.join(", ", SUPPORTED_SECTIONS));
                }

                return inspectionRepository.findAll()
                                .stream().filter(inspection -> canReadInspection(currentUser, inspection))
                                .filter(inspection -> {
                                        if (selectedSection == null) {
                                                return true;
                                        }

                                        return inspection.getLine() != null && inspection.getLine().getSection() != null
                                                        && selectedSection.equals(inspection.getLine()
                                                                        .getSection()
                                                                        .name());
                                })
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public Page<InspectionResponse> getAllPaginated(String section, Pageable pageable) {
                // for (int i = 1; i <= 5; i++) {
                // long dbTestStart = System.nanoTime();

                // jdbcTemplate.queryForObject("SELECT 1", Integer.class);

                // long dbTestElapsed = (System.nanoTime() - dbTestStart) / 1_000_000;

                // System.out.println(
                // "[DB TEST] SELECT 1 #" + i + ": " + dbTestElapsed + " ms");
                // }

                long totalStart = System.nanoTime();

                User currentUser = getCurrentUser();

                String role = currentUser.getRole().name();
                String selectedSection = normalizeOptional(section);

                if (selectedSection != null && !SUPPORTED_SECTIONS.contains(selectedSection)) {
                        throw new BadRequestException(
                                        "Section tidak valid. Pilihan: "
                                                        + String.join(", ", SUPPORTED_SECTIONS));
                }
                Pageable safePageable = PageRequest.of(
                                Math.max(pageable.getPageNumber(), 0),
                                Math.min(Math.max(pageable.getPageSize(), 1), 100));

                logHikariStats("Before ID query");

                long idQueryStart = System.nanoTime();

                Page<String> idPage;

                if (SUPER_ADMIN.equals(role)) {

                        if (selectedSection == null) {
                                throw new BadRequestException(
                                                "Super Admin wajib memilih section: "
                                                                + String.join(", ", SUPPORTED_SECTIONS));
                        }

                        Section selected = Section.valueOf(selectedSection);

                        idPage = inspectionRepository.findIdsByLine_Section(
                                        selected,
                                        safePageable);

                } else if (ADMIN.equals(role)) {

                        if (currentUser.getSection() == null) {
                                throw new BadRequestException("Admin belum memiliki section");
                        }

                        Section adminSection = currentUser.getSection();

                        if (selectedSection != null
                                        && !adminSection.name().equals(selectedSection)) {
                                return Page.empty(safePageable);
                        }

                        idPage = inspectionRepository.findIdsByLine_Section(
                                        adminSection,
                                        safePageable);

                } else if (USER.equals(role)) {

                        if (selectedSection == null) {
                                idPage = inspectionRepository.findIdsBySubmittedBy_Id(
                                                currentUser.getId(),
                                                safePageable);
                        } else {
                                Section selected = Section.valueOf(selectedSection);

                                idPage = inspectionRepository.findIdsBySubmittedBy_IdAndLine_Section(
                                                currentUser.getId(),
                                                selected,
                                                safePageable);
                        }

                } else if (VISITOR.equals(role)) {

                        if (selectedSection == null) {
                                Page<Inspection> inspectionPage = inspectionRepository.findAll(safePageable);

                                Page<InspectionResponse> result = inspectionPage.map(this::toResponse);

                                System.out.println(
                                                "[PERF] Visitor findAll + mapping: "
                                                                + ((System.nanoTime() - idQueryStart) / 1_000_000)
                                                                + " ms");

                                System.out.println(
                                                "[PERF] Total request service: "
                                                                + ((System.nanoTime() - totalStart) / 1_000_000)
                                                                + " ms");

                                return result;
                        }

                        Section selected = Section.valueOf(selectedSection);

                        idPage = inspectionRepository.findIdsByLine_Section(
                                        selected,
                                        safePageable);

                } else {
                        throw new UnauthorizedException(
                                        "Role tidak memiliki izin untuk melihat inspection");
                }

                long idQueryEnd = System.nanoTime();

                System.out.println(
                                "[PERF] Fetch inspection IDs + count: "
                                                + ((idQueryEnd - idQueryStart) / 1_000_000)
                                                + " ms");

                if (idPage.isEmpty()) {
                        System.out.println(
                                        "[PERF] Total request service: "
                                                        + ((System.nanoTime() - totalStart) / 1_000_000)
                                                        + " ms");

                        return new PageImpl<>(
                                        List.of(),
                                        safePageable,
                                        idPage.getTotalElements());
                }

                logHikariStats("Before detail query");

                long detailStart = System.nanoTime();

                List<Inspection> inspections = inspectionRepository.findAllWithDetailsByIdIn(
                                idPage.getContent());

                long detailEnd = System.nanoTime();

                logHikariStats("After detail query");

                System.out.println(
                                "[PERF] Fetch inspection details: "
                                                + ((detailEnd - detailStart) / 1_000_000)
                                                + " ms");

                Map<String, Inspection> inspectionMap = new HashMap<>();

                for (Inspection inspection : inspections) {
                        inspectionMap.put(inspection.getId(), inspection);
                }

                long mappingStart = System.nanoTime();

                List<InspectionResponse> responses = idPage.getContent()
                                .stream()
                                .map(inspectionMap::get)
                                .filter(Objects::nonNull)
                                .map(this::toResponse)
                                .toList();

                long mappingEnd = System.nanoTime();

                System.out.println(
                                "[PERF] Inspection mapping: "
                                                + ((mappingEnd - mappingStart) / 1_000_000)
                                                + " ms");

                Page<InspectionResponse> result = new PageImpl<>(
                                responses,
                                safePageable,
                                idPage.getTotalElements());

                System.out.println(
                                "[PERF] Total request service: "
                                                + ((System.nanoTime() - totalStart) / 1_000_000)
                                                + " ms");

                return result;
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

                validateInspectionTimeSlot(section, inspectionTimeSlot);

                validateDuplicateTimeSlot(line, section, inspectionTimeSlot);

                int inspectedQuantity = validateInspectedQuantity(request.getInspectedQuantity());

                int transferQuantity = validateTransferQuantity(
                                request.getTransferQuantity(),
                                inspectedQuantity,
                                section);

                StyleColor styleColor = findOrCreateStyleColor(request);

                applyRequestFields(
                                inspection,
                                request,
                                line,
                                styleColor,
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

                Object principal = authentication.getPrincipal();

                if (principal instanceof User user) {
                        return user;
                }

                return userRepository
                                .findByUsername(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User yang login tidak ditemukan"));
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
                                                "Admin hanya dapat menggunakan line dari section miliknya");
                        }
                        return;
                }

                if (USER.equals(role)) {
                        return;
                }

                throw new UnauthorizedException("Role tidak memiliki izin untuk membuat atau mengubah inspection");
        }

        private Section getCurrentInspectionSection() {
                User currentUser = getCurrentUser();

                String role = currentUser.getRole().name();

                if (SUPER_ADMIN.equals(role) || VISITOR.equals(role)) {
                        return null;
                }

                if (ADMIN.equals(role) || USER.equals(role)) {
                        if (currentUser.getSection() == null) {
                                throw new BadRequestException("User belum memiliki section");
                        }

                        return currentUser.getSection();
                }

                throw new UnauthorizedException("Role tidak memiliki akses pencarian defect");
        }

        private void validateInspectionAccess(User user, Inspection inspection) {
                if (!canAccessInspection(user, inspection)) {
                        throw new AccessDeniedException("Anda tidak memiliki akses ke data inspection ini");
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
                        return user.getSection() != null && user.getSection().equals(line.getSection());
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

                return inspectionRepository.findByIdWithDetails(id.trim())
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
                        throw new BadRequestException("Field 'qtyCheckCartonToday' wajib diisi untuk section PACKING");
                }

                if (qtyCheckCartonToday < 0) {
                        throw new BadRequestException("Field 'qtyCheckCartonToday' tidak boleh negatif");
                }
        }

        private void validateRequest(InspectionRequest request) {

                if (request == null) {
                        throw new BadRequestException("Request body wajib diisi");
                }

                if (request.getBuyerId() == null || request.getBuyerId().isBlank()) {
                        throw new BadRequestException("Field 'buyerId' wajib diisi");
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

                if (request.getStyleCode().trim().length() > 100) {
                        throw new BadRequestException("Field 'styleCode' maksimal 100 karakter");
                }

                if (request.getColorName().trim().length() > 100) {
                        throw new BadRequestException("Field 'colorName' maksimal 100 karakter");
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
                                                "Field 'transferQuantity' wajib diisi untuk section SEWING");
                        }

                        return 0;
                }

                if (transferQuantity < 0) {
                        throw new BadRequestException("Field 'transferQuantity' tidak boleh negatif");
                }

                if (transferQuantity > inspectedQuantity) {
                        throw new BadRequestException(
                                        "Field 'transferQuantity' tidak boleh melebihi inspectedQuantity");
                }

                return transferQuantity;
        }

        private void validateSectionFields(String section, String handfeel, String grade, String process,
                        WashingType washingType, String inspectionTimeSlot) {
                switch (section) {
                        case "SEWING":
                                if (handfeel != null || grade != null || process != null || washingType != null) {
                                        throw new BadRequestException("Section SEWING tidak menggunakan "
                                                        + "handfeel, grade, process, atau washingType");
                                }

                                if (inspectionTimeSlot == null) {
                                        throw new BadRequestException(
                                                        "Field 'inspectionTimeSlot' wajib diisi untuk section SEWING");
                                }

                                break;

                        case "WASHING":
                                if (handfeel == null) {
                                        throw new BadRequestException(
                                                        "Field 'handfeel' wajib diisi untuk section WASHING");
                                }

                                if (grade == null) {
                                        throw new BadRequestException(
                                                        "Field 'grade' wajib diisi untuk section WASHING");
                                }

                                if (washingType == null) {
                                        throw new BadRequestException(
                                                        "Field 'washingType' wajib diisi untuk section WASHING. Pilihan: BULK atau REPAIR");
                                }

                                if (inspectionTimeSlot == null) {
                                        throw new BadRequestException(
                                                        "Field 'inspectionTimeSlot' wajib diisi untuk section WASHING");
                                }

                                if (process != null) {
                                        throw new BadRequestException("Section WASHING tidak menggunakan process");
                                }

                                break;

                        case "FINISHING":
                                if (handfeel != null || grade != null || washingType != null) {
                                        throw new BadRequestException(
                                                        "Section FINISHING tidak menggunakan handfeel, grade, atau washingType");
                                }

                                if (inspectionTimeSlot == null) {
                                        throw new BadRequestException(
                                                        "Field 'inspectionTimeSlot' wajib diisi untuk section FINISHING");
                                }

                                if (process != null
                                                && !process.equals("WASH")
                                                && !process.equals("NON_WASH")) {

                                        throw new BadRequestException(
                                                        "Field 'process' untuk FINISHING hanya boleh WASH atau NON_WASH");
                                }

                                break;

                        case "PACKING":
                                if (handfeel != null || grade != null || process != null || washingType != null) {
                                        throw new BadRequestException("Section PACKING tidak menggunakan "
                                                        + "handfeel, grade, process, atau washingType");
                                }

                                if (inspectionTimeSlot != null) {
                                        throw new BadRequestException(
                                                        "Section PACKING tidak menggunakan inspectionTimeSlot");
                                }

                                break;

                        default:
                                throw new BadRequestException("Section '" + section + "' tidak didukung");
                }
        }

        private void validateInspectionReadAccess(User user, Inspection inspection) {
                if (!canReadInspection(user, inspection)) {
                        throw new AccessDeniedException("Anda tidak memiliki akses untuk melihat data inspection ini");
                }
        }

        private boolean canReadInspection(User user, Inspection inspection) {
                if (user == null || inspection == null) {
                        return false;
                }

                if (VISITOR.equals(user.getRole().name())) {
                        return true;
                }

                return canAccessInspection(user, inspection);
        }

        private void applyRequestFields(
                        Inspection inspection,
                        InspectionRequest request,
                        Line line,
                        StyleColor styleColor,
                        int inspectedQuantity,
                        int transferQuantity,
                        Integer qtyCheckCartonToday,
                        String section,
                        String handfeel,
                        String grade,
                        String process,
                        WashingType washingType) {
                inspection.setStyleColor(styleColor);

                inspection.setLine(line);
                inspection.setInspectQuantity(inspectedQuantity);
                inspection.setTransferQuantity(transferQuantity);

                inspection.setComment(request.getComment());

                inspection.setInspectionTimeSlot("PACKING".equals(section)
                                ? null
                                : normalizeOptional(request.getInspectionTimeSlot()));

                inspection.setHandfeel(handfeel);
                inspection.setGrade(grade);

                inspection.setProcess("FINISHING".equals(section)
                                ? process
                                : null);

                inspection.setWashingType("WASHING".equals(section)
                                ? washingType
                                : null);

                inspection.setQtyCheckCartonToday("PACKING".equals(section)
                                ? qtyCheckCartonToday
                                : null);
        }

        private void logHikariStats(String label) {
                if (!(dataSource instanceof HikariDataSource hikariDataSource)) {
                        return;
                }

                HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();

                if (pool == null) {
                        return;
                }

                System.out.printf(
                                "[HIKARI] %s | active=%d | idle=%d | total=%d | waiting=%d%n",
                                label,
                                pool.getActiveConnections(),
                                pool.getIdleConnections(),
                                pool.getTotalConnections(),
                                pool.getThreadsAwaitingConnection());
        }

        private void replaceDefects(
                        Inspection inspection,
                        List<InspectionDefectRequest> defects,
                        int inspectedQuantity) {

                inspection.getInspectionDefects().clear();

                if (defects == null || defects.isEmpty()) {
                        return;
                }

                int totalDefect = 0;
                Set<String> defectTypeIds = new java.util.LinkedHashSet<>();

                for (int i = 0; i < defects.size(); i++) {

                        InspectionDefectRequest item = defects.get(i);

                        if (item == null) {
                                throw new BadRequestException(
                                                "Item defects pada indeks " + i + " tidak boleh null");
                        }

                        String defectTypeId = item.getDefectTypeId();

                        if (defectTypeId == null || defectTypeId.isBlank()) {
                                throw new BadRequestException(
                                                "Field 'defectTypeId' wajib diisi pada defects indeks " + i);
                        }

                        Integer quantity = item.getQuantity();

                        if (quantity == null) {
                                throw new BadRequestException(
                                                "Field 'quantity' wajib diisi pada defects indeks " + i);
                        }

                        if (quantity < 1) {
                                throw new BadRequestException(
                                                "Quantity defect minimal 1 pada defects indeks " + i);
                        }

                        try {
                                totalDefect = Math.addExact(totalDefect, quantity);
                        } catch (ArithmeticException ex) {
                                throw new BadRequestException(
                                                "Total quantity defect melebihi batas angka yang diperbolehkan");
                        }

                        defectTypeIds.add(defectTypeId.trim());
                }

                List<DefectType> foundTypes = defectTypeRepository.findAllById(defectTypeIds);

                Map<String, DefectType> defectTypeMap = new HashMap<>();

                for (DefectType type : foundTypes) {
                        defectTypeMap.put(type.getId(), type);
                }

                for (String defectTypeId : defectTypeIds) {

                        DefectType defectType = defectTypeMap.get(defectTypeId);

                        if (defectType == null) {
                                throw new ResourceNotFoundException(
                                                "Defect type dengan ID " + defectTypeId + " tidak ditemukan");
                        }

                        if (!defectType.isActive()) {
                                throw new BadRequestException(
                                                "Defect type dengan ID " + defectTypeId + " sudah tidak aktif");
                        }
                }

                if (totalDefect > inspectedQuantity) {
                        throw new BadRequestException(
                                        "Total quantity defect (" + totalDefect
                                                        + ") tidak boleh melebihi inspectedQuantity ("
                                                        + inspectedQuantity + ")");
                }

                for (InspectionDefectRequest item : defects) {

                        DefectType defectType = defectTypeMap.get(item.getDefectTypeId().trim());

                        InspectionDefect defect = new InspectionDefect();

                        defect.setInspection(inspection);
                        defect.setDefectType(defectType);
                        defect.setQuantity(item.getQuantity());

                        inspection.getInspectionDefects().add(defect);
                }
        }

        private String normalizeOptional(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }

                return value.trim().toUpperCase(Locale.ROOT);
        }

        @Transactional(readOnly = true)
        public List<DefectArea> searchDefectAreas(String keyword) {
                Section section = getCurrentInspectionSection();
                String key = normalizeOptional(keyword);

                if (key == null) {
                        return defectTypeRepository.findDistinctActiveDefectAreas(section);
                }

                return defectTypeRepository.findDistinctActiveDefectAreasByKeyword(section, key);
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

                Section section = getCurrentInspectionSection();

                List<DefectType> types;

                if (section == null) {
                        types = defectTypeRepository.findByDefectAreaAndActiveTrueOrderByNameAsc(area);
                } else {
                        types = defectTypeRepository.findBySectionAndDefectAreaAndActiveTrue(section, area);
                }

                String key = normalizeOptional(keyword);

                return types.stream()
                                .filter(type -> key == null
                                                || type.getName().toUpperCase(Locale.ROOT).contains(key))
                                .sorted(Comparator.comparing(
                                                DefectType::getName,
                                                String.CASE_INSENSITIVE_ORDER))
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<DefectSearchResponse> searchDefects(
                        String defectAreaId,
                        String keyword) {

                Section section = getCurrentInspectionSection();
                String areaId = defectAreaId == null || defectAreaId.isBlank()
                                ? null
                                : defectAreaId.trim().toLowerCase(Locale.ROOT);
                String key = normalizeOptional(keyword);

                List<DefectType> types;

                if (section == null) {
                        if (areaId != null) {
                                DefectArea area = defectAreaRepository.findById(areaId)
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
                } else {
                        if (areaId != null) {
                                DefectArea area = defectAreaRepository.findById(areaId)
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Defect area tidak ditemukan"));

                                if (!area.isActive()) {
                                        throw new BadRequestException("Defect area tidak aktif");
                                }

                                types = key == null
                                                ? defectTypeRepository.findBySectionAndDefectAreaAndActiveTrue(section,
                                                                area)
                                                : defectTypeRepository
                                                                .findBySectionAndDefectAreaAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
                                                                                section, area, key);

                        } else {
                                types = key == null
                                                ? defectTypeRepository.findBySectionAndActiveTrueOrderByNameAsc(section)
                                                : defectTypeRepository
                                                                .findBySectionAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
                                                                                section, key);
                        }
                }

                return types.stream()
                                .filter(t -> t.getDefectArea() == null || t.getDefectArea().isActive())
                                .sorted(Comparator
                                                .comparing(
                                                                (DefectType t) -> t.getDefectArea() == null
                                                                                ? ""
                                                                                : t.getDefectArea().getName(),
                                                                String.CASE_INSENSITIVE_ORDER)
                                                .thenComparing(
                                                                DefectType::getName,
                                                                String.CASE_INSENSITIVE_ORDER))
                                .map(DefectSearchResponse::from)
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

        private StyleColor findOrCreateStyleColor(InspectionRequest request) {

                String buyerId = request.getBuyerId().trim();
                String styleCode = request.getStyleCode().trim()
                                .toUpperCase(Locale.ROOT);
                String colorName = request.getColorName().trim()
                                .toUpperCase(Locale.ROOT);

                Buyer buyer = buyerRepository.findById(buyerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Buyer dengan ID " + buyerId + " tidak ditemukan"));

                Style style = styleRepository
                                .findByBuyer_IdAndStyleCodeIgnoreCase(buyer.getId(), styleCode)
                                .orElseGet(() -> {
                                        Style newStyle = new Style();
                                        newStyle.setBuyer(buyer);
                                        newStyle.setStyleCode(styleCode);

                                        return styleRepository.save(newStyle);
                                });

                return styleColorRepository
                                .findByStyle_IdAndColorNameIgnoreCase(style.getId(), colorName)
                                .orElseGet(() -> {
                                        StyleColor newColor = new StyleColor();
                                        newColor.setStyle(style);
                                        newColor.setColorName(colorName);

                                        return styleColorRepository.save(newColor);
                                });
        }

        private InspectionResponse toResponse(Inspection inspection) {
                List<InspectionResponse.DefectItem> defectItems = inspection.getInspectionDefects() == null
                                ? List.of()
                                : inspection.getInspectionDefects().stream()
                                                .map(d -> new InspectionResponse.DefectItem(
                                                                d.getId(),
                                                                d.getDefectType() != null
                                                                                ? d.getDefectType().getId()
                                                                                : null,
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
                                inspection.getStyleColor().getStyle().getBuyer().getName(),
                                inspection.getStyleColor().getStyle().getStyleCode(),
                                inspection.getStyleColor().getColorName(),
                                inspection.getLine().getName(),
                                inspection.getLine().getSection().name(),
                                inspectQuantity,
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
