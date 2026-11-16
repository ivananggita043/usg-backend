
package com.garmentqc.system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.AttendanceCorrectionRequest;
import com.garmentqc.system.dto.AttendanceResponse;
import com.garmentqc.system.dto.CreateAttendanceStatusRequest;
import com.garmentqc.system.dto.LineResponse;
import com.garmentqc.system.enums.AttendanceStatus;
import com.garmentqc.system.enums.Role;
import com.garmentqc.system.enums.Section;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.Attendance;
import com.garmentqc.system.model.AttendanceCorrection;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.AttendanceCorrectionRepository;
import com.garmentqc.system.repository.AttendanceRepository;
import com.garmentqc.system.repository.OperatorLineAssignmentRepository;
import com.garmentqc.system.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceManagementService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");

    private final AttendanceRepository attendanceRepository;
    private final AttendanceCorrectionRepository correctionRepository;
    private final OperatorLineAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAllAttendancesPaginated(
            String username,
            Pageable pageable) {

        User actor = getAuthorizedUser(username);

        Section section = actor.getRole() == Role.SUPER_ADMIN
                ? null
                : actor.getSection();

        return attendanceRepository
                .findQcAttendances(Role.USER, section, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAttendancesPaginated(String username, LocalDate date, Pageable pageable) {
        User actor = getAuthorizedUser(username);

        LocalDate attendanceDate = date != null
                ? date
                : LocalDate.now(ZONE);

        Section section = actor.getRole() == Role.SUPER_ADMIN
                ? null
                : actor.getSection();

        return attendanceRepository
                .findQcAttendancesByDate(
                        Role.USER,
                        section,
                        attendanceDate,
                        pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAttendancesByUserIdPaginated(String username, String userId, Pageable pageable) {
        User actor = getAuthorizedUser(username);

        User qc = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User QC tidak ditemukan"));

        validateQcAccess(qc, actor);

        return attendanceRepository
                .findAttendancesByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAttendancesBySectionPaginated(String username, Section section, Pageable pageable) {
        User actor = getAuthorizedUser(username);

        validateRequestedSection(actor, section);

        return attendanceRepository
                .findQcAttendancesBySection(
                        Role.USER,
                        section,
                        pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAttendancesBySectionAndLinePaginated(
            String username,
            Section section,
            String lineId,
            Pageable pageable) {

        User actor = getAuthorizedUser(username);

        validateRequestedSection(actor, section);

        if (lineId == null || lineId.isBlank()) {
            throw new BadRequestException("Line ID wajib diisi");
        }

        return attendanceRepository
                .findQcAttendancesBySectionAndLine(
                        Role.USER,
                        section,
                        lineId,
                        pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAllAttendances(String username) {

        User actor = getAuthorizedUser(username);

        return attendanceRepository.findAll()
                .stream()
                .filter(attendance -> {
                    User qc = attendance.getUser();

                    if (qc == null || qc.getRole() != Role.USER) {
                        return false;
                    }

                    return actor.getRole() == Role.SUPER_ADMIN
                            || Objects.equals(qc.getSection(), actor.getSection());
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendances(
            String username,
            LocalDate date) {

        User actor = getAuthorizedUser(username);

        LocalDate attendanceDate = date != null
                ? date
                : LocalDate.now(ZONE);

        return attendanceRepository.findByAttendanceDate(attendanceDate)
                .stream()
                .filter(attendance -> {
                    User qc = attendance.getUser();

                    if (qc == null || qc.getRole() != Role.USER) {
                        return false;
                    }

                    return actor.getRole() == Role.SUPER_ADMIN
                            || Objects.equals(qc.getSection(), actor.getSection());
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(
            String username,
            String attendanceId) {

        User actor = getAuthorizedUser(username);

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Data attendance tidak ditemukan"));

        validateQcAccess(attendance.getUser(), actor);

        return toResponse(attendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendancesByUserId(
            String username,
            String userId) {

        User actor = getAuthorizedUser(username);

        User qc = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User QC tidak ditemukan"));

        validateQcAccess(qc, actor);

        return attendanceRepository.findByUser_Id(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendancesBySection(
            String username,
            Section section) {

        User actor = getAuthorizedUser(username);

        validateRequestedSection(actor, section);

        return attendanceRepository.findAll()
                .stream()
                .filter(attendance -> {
                    User qc = attendance.getUser();

                    return qc != null
                            && qc.getRole() == Role.USER
                            && qc.getSection() == section;
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendancesBySectionAndLine(
            String username,
            Section section,
            String lineId) {

        User actor = getAuthorizedUser(username);

        validateRequestedSection(actor, section);

        if (lineId == null || lineId.isBlank()) {
            throw new BadRequestException("Line ID wajib diisi");
        }

        Map<String, Attendance> attendanceMap = new LinkedHashMap<>();

        assignmentRepository.findByLine_Id(lineId)
                .forEach(assignment -> {

                    Attendance attendance = assignment.getAttendance();

                    if (attendance == null) {
                        return;
                    }

                    User qc = attendance.getUser();

                    if (qc == null || qc.getRole() != Role.USER) {
                        return;
                    }

                    if (!Objects.equals(qc.getSection(), section)) {
                        return;
                    }

                    attendanceMap.putIfAbsent(
                            attendance.getId(),
                            attendance);
                });

        return attendanceMap.values()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceResponse createAttendanceStatus(
            String username,
            CreateAttendanceStatusRequest request) {

        if (request == null) {
            throw new BadRequestException(
                    "Request attendance tidak boleh kosong");
        }

        String reason = validateReason(request.getReason());

        User actor = getAuthorizedUser(username);

        if (request.getStatus() != AttendanceStatus.ABSENT
                && request.getStatus() != AttendanceStatus.PERMISSION) {

            throw new BadRequestException(
                    "Status yang dapat dibuat tanpa check-in hanya ABSENT atau PERMISSION");
        }

        LocalDate attendanceDate = request.getAttendanceDate();

        if (attendanceDate == null) {
            throw new BadRequestException(
                    "Tanggal attendance wajib diisi");
        }

        if (attendanceDate.isAfter(LocalDate.now(ZONE))) {
            throw new BadRequestException(
                    "Tidak dapat membuat attendance untuk tanggal yang akan datang");
        }

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BadRequestException(
                    "User ID QC wajib diisi");
        }

        User qc = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User QC tidak ditemukan"));

        validateQcAccess(qc, actor);

        if (attendanceRepository.existsByUser_IdAndAttendanceDate(
                qc.getId(),
                attendanceDate)) {

            throw new BadRequestException(
                    "Data attendance QC pada tanggal tersebut sudah tersedia");
        }

        Instant now = Instant.now();

        Attendance attendance = new Attendance();

        attendance.setUser(qc);
        attendance.setAttendanceDate(attendanceDate);
        attendance.setStatus(request.getStatus());

        attendance.setWorkingMinutes(0);
        attendance.setBreakMinutes(0);
        attendance.setEffectiveMinutes(0);

        Attendance saved = attendanceRepository.save(attendance);

        saveCorrection(
                saved,
                null,
                request.getStatus(),
                null,
                null,
                null,
                null,
                reason,
                actor,
                now);

        return toResponse(saved);
    }

    @Transactional
    public AttendanceResponse correctAttendance(
            String username,
            String attendanceId,
            AttendanceCorrectionRequest request) {

        if (request == null) {
            throw new BadRequestException(
                    "Request koreksi attendance tidak boleh kosong");
        }

        String reason = validateReason(request.getReason());

        User actor = getAuthorizedUser(username);

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Data attendance tidak ditemukan"));

        validateQcAccess(attendance.getUser(), actor);

        AttendanceStatus requestedStatus = request.getStatus();

        if (requestedStatus == null) {
            throw new BadRequestException(
                    "Status attendance wajib diisi");
        }

        Instant now = Instant.now();

        AttendanceStatus previousStatus = attendance.getStatus();

        Instant previousCheckInAt = attendance.getCheckInAt();
        Instant previousCheckOutAt = attendance.getCheckOutAt();

        Instant newCheckInAt = request.getCheckInAt() != null
                ? request.getCheckInAt()
                : previousCheckInAt;

        if (requestedStatus == AttendanceStatus.INCOMPLETE) {

            if (previousStatus != AttendanceStatus.INCOMPLETE) {
                throw new BadRequestException(
                        "Koreksi check-in saja hanya dapat dilakukan pada attendance INCOMPLETE");
            }

            if (request.getCheckInAt() == null) {
                throw new BadRequestException(
                        "Waktu check-in baru wajib diisi");
            }

            validateCorrectedCheckIn(
                    newCheckInAt,
                    attendance.getAttendanceDate(),
                    now);

            attendance.setCheckInAt(newCheckInAt);

            updateFirstAssignmentStart(
                    attendance,
                    previousCheckInAt,
                    newCheckInAt);
        }

        else if (requestedStatus == AttendanceStatus.PRESENT) {

            if (newCheckInAt == null) {
                throw new BadRequestException(
                        "Waktu check-in wajib tersedia untuk status PRESENT");
            }

            if (request.getCheckOutAt() == null) {
                throw new BadRequestException(
                        "Waktu check-out wajib diisi untuk status PRESENT");
            }

            validateCorrectedCheckIn(
                    newCheckInAt,
                    attendance.getAttendanceDate(),
                    now);

            if (!request.getCheckOutAt().isAfter(newCheckInAt)) {
                throw new BadRequestException(
                        "Waktu check-out harus setelah waktu check-in");
            }

            if (request.getCheckOutAt().isAfter(now)) {
                throw new BadRequestException(
                        "Waktu check-out tidak boleh melebihi waktu sekarang");
            }

            attendance.setCheckInAt(newCheckInAt);
            attendance.setCheckOutAt(request.getCheckOutAt());

            recalculateAttendance(attendance);

            attendance.setCheckOutLatitude(null);
            attendance.setCheckOutLongitude(null);
            attendance.setCheckOutAccuracy(null);

            updateFirstAssignmentStart(
                    attendance,
                    previousCheckInAt,
                    newCheckInAt);

            closeActiveAssignment(
                    attendance,
                    request.getCheckOutAt());
        }

        else if (requestedStatus == AttendanceStatus.ABSENT
                || requestedStatus == AttendanceStatus.PERMISSION) {

            attendance.setCheckOutAt(null);

            attendance.setCheckOutLatitude(null);
            attendance.setCheckOutLongitude(null);
            attendance.setCheckOutAccuracy(null);

            attendance.setWorkingMinutes(0);
            attendance.setBreakMinutes(0);
            attendance.setEffectiveMinutes(0);

            closeActiveAssignment(attendance, now);
        }

        else {
            throw new BadRequestException(
                    "Status koreksi tidak valid");
        }

        attendance.setStatus(requestedStatus);

        Attendance saved = attendanceRepository.save(attendance);

        saveCorrection(
                saved,
                previousStatus,
                requestedStatus,
                previousCheckInAt,
                saved.getCheckInAt(),
                previousCheckOutAt,
                saved.getCheckOutAt(),
                reason,
                actor,
                now);

        return toResponse(saved);
    }

    private User getAuthorizedUser(String username) {

        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User tidak ditemukan"));

        if (!actor.isActive()) {
            throw new UnauthorizedException("User tidak aktif");
        }

        if (actor.getRole() != Role.ADMIN
                && actor.getRole() != Role.SUPER_ADMIN) {

            throw new UnauthorizedException(
                    "Tidak memiliki akses untuk mengelola attendance");
        }

        if (actor.getRole() == Role.ADMIN
                && actor.getSection() == null) {

            throw new BadRequestException(
                    "Admin belum memiliki section");
        }

        return actor;
    }

    private void validateQcAccess(User qc, User actor) {

        if (qc == null || qc.getRole() != Role.USER) {
            throw new BadRequestException(
                    "Attendance hanya dapat dikelola untuk user QC");
        }

        if (actor.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (!Objects.equals(qc.getSection(), actor.getSection())) {
            throw new UnauthorizedException(
                    "Anda hanya dapat mengelola attendance QC di section sendiri");
        }
    }

    private void validateRequestedSection(User actor, Section section) {

        if (section == null) {
            throw new BadRequestException(
                    "Section wajib dipilih");
        }

        if (actor.getRole() == Role.ADMIN
                && !Objects.equals(actor.getSection(), section)) {

            throw new UnauthorizedException(
                    "Anda hanya dapat melihat attendance section sendiri");
        }
    }

    private String validateReason(String reason) {

        if (reason == null || reason.isBlank()) {
            throw new BadRequestException(
                    "Alasan koreksi wajib diisi");
        }

        return reason.trim();
    }

    private void validateCorrectedCheckIn(
            Instant checkInAt,
            LocalDate attendanceDate,
            Instant now) {

        if (checkInAt == null) {
            throw new BadRequestException(
                    "Waktu check-in wajib diisi");
        }

        if (checkInAt.isAfter(now)) {
            throw new BadRequestException(
                    "Waktu check-in tidak boleh melebihi waktu sekarang");
        }

        LocalDate correctedDate = checkInAt
                .atZone(ZONE)
                .toLocalDate();

        if (!correctedDate.equals(attendanceDate)) {
            throw new BadRequestException(
                    "Tanggal check-in harus sesuai dengan tanggal attendance");
        }
    }

    private int calculateBreakMinutes(long workingMinutes) {

        if (workingMinutes < 570) {
            return 0;
        }

        if (workingMinutes < 600) {
            return 30;
        }

        return 60;
    }

    private void recalculateAttendance(Attendance attendance) {

        Instant checkInAt = attendance.getCheckInAt();
        Instant checkOutAt = attendance.getCheckOutAt();

        if (checkInAt == null || checkOutAt == null) {
            throw new BadRequestException(
                    "Waktu check-in dan check-out wajib tersedia");
        }

        if (!checkOutAt.isAfter(checkInAt)) {
            throw new BadRequestException(
                    "Waktu check-out harus setelah waktu check-in");
        }

        long workingMinutes = ChronoUnit.MINUTES.between(
                checkInAt,
                checkOutAt);

        int breakMinutes = calculateBreakMinutes(workingMinutes);

        int effectiveMinutes = Math.toIntExact(
                workingMinutes - breakMinutes);

        attendance.setWorkingMinutes(
                Math.toIntExact(workingMinutes));

        attendance.setBreakMinutes(breakMinutes);
        attendance.setEffectiveMinutes(effectiveMinutes);
    }

    private void updateFirstAssignmentStart(
            Attendance attendance,
            Instant previousCheckInAt,
            Instant newCheckInAt) {

        if (previousCheckInAt == null || newCheckInAt == null) {
            return;
        }

        assignmentRepository
                .findFirstByAttendance_IdOrderByStartAtAsc(
                        attendance.getId())
                .ifPresent(assignment -> {

                    if (Objects.equals(
                            assignment.getStartAt(),
                            previousCheckInAt)) {

                        assignment.setStartAt(newCheckInAt);

                        assignmentRepository.save(assignment);
                    }
                });
    }

    private void closeActiveAssignment(
            Attendance attendance,
            Instant endAt) {

        assignmentRepository
                .findByAttendance_IdAndEndAtIsNull(attendance.getId())
                .ifPresent(assignment -> {

                    Instant finalEndAt = endAt;

                    if (finalEndAt.isBefore(assignment.getStartAt())) {
                        finalEndAt = assignment.getStartAt();
                    }

                    assignment.setEndAt(finalEndAt);

                    assignmentRepository.save(assignment);
                });
    }

    private void saveCorrection(
            Attendance attendance,
            AttendanceStatus previousStatus,
            AttendanceStatus newStatus,
            Instant previousCheckInAt,
            Instant newCheckInAt,
            Instant previousCheckOutAt,
            Instant newCheckOutAt,
            String reason,
            User correctedBy,
            Instant now) {

        AttendanceCorrection correction = new AttendanceCorrection();

        correction.setAttendance(attendance);

        correction.setPreviousStatus(previousStatus);
        correction.setNewStatus(newStatus);

        correction.setPreviousCheckInAt(previousCheckInAt);
        correction.setNewCheckInAt(newCheckInAt);

        correction.setPreviousCheckOutAt(previousCheckOutAt);
        correction.setNewCheckOutAt(newCheckOutAt);

        correction.setReason(validateReason(reason));
        correction.setCorrectedBy(correctedBy);
        correction.setCorrectedAt(now);

        correctionRepository.save(correction);
    }

    private List<LineResponse> getAttendanceLines(Attendance attendance) {

        return assignmentRepository
                .findByAttendance_Id(attendance.getId())
                .stream()
                .filter(assignment -> assignment.getLine() != null)
                .collect(Collectors.toMap(
                        assignment -> assignment.getLine().getId(),
                        assignment -> LineResponse.builder()
                                .id(assignment.getLine().getId())
                                .name(assignment.getLine().getName())
                                .build(),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private AttendanceResponse toResponse(Attendance attendance) {

        User qc = attendance.getUser();

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .userId(qc != null ? qc.getId() : null)
                .username(qc != null ? qc.getUsername() : null)
                .section(qc != null && qc.getSection() != null
                        ? qc.getSection().toString()
                        : null)
                .attendanceDate(attendance.getAttendanceDate())
                .checkInAt(attendance.getCheckInAt())
                .checkOutAt(attendance.getCheckOutAt())
                .checkInLatitude(attendance.getCheckInLatitude())
                .checkInLongitude(attendance.getCheckInLongitude())
                .checkInAccuracy(attendance.getCheckInAccuracy())
                .checkOutLatitude(attendance.getCheckOutLatitude())
                .checkOutLongitude(attendance.getCheckOutLongitude())
                .checkOutAccuracy(attendance.getCheckOutAccuracy())
                .workingMinutes(attendance.getWorkingMinutes())
                .breakMinutes(attendance.getBreakMinutes())
                .effectiveMinutes(attendance.getEffectiveMinutes())
                .status(attendance.getStatus())
                .lines(getAttendanceLines(attendance))
                .build();
    }
}
