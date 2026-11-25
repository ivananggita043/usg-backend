
package com.garmentqc.system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.garmentqc.system.dto.DailyProductivityResponse;
import com.garmentqc.system.dto.HourlyProductivityResponse;
import com.garmentqc.system.dto.ProductivitySummaryResponse;
import com.garmentqc.system.dto.SectionProductivitySummaryResponse;
import com.garmentqc.system.enums.Section;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.model.Attendance;
import com.garmentqc.system.model.Inspection;
import com.garmentqc.system.model.SectionProductivityTarget;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.AttendanceRepository;
import com.garmentqc.system.repository.InspectionRepository;
import com.garmentqc.system.repository.SectionProductivityTargetRepository;
import com.garmentqc.system.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductivityService {

        private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");

        private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        private final UserRepository userRepository;
        private final AttendanceRepository attendanceRepository;
        private final InspectionRepository inspectionRepository;
        private final SectionProductivityTargetRepository targetRepository;

        public DailyProductivityResponse getDailyProductivity(String username, LocalDate date) {
                User user = getUser(username);

                if (user.getSection() == null) {
                        throw new BadRequestException("User belum memiliki section.");
                }

                Attendance attendance = attendanceRepository
                                .findByUser_IdAndAttendanceDate(user.getId(), date)
                                .orElseThrow(() -> new ResourceNotFoundException("Data absensi tidak ditemukan."));
                validateAttendance(attendance);

                if (attendance.getCheckOutAt() == null) {
                        throw new BadRequestException("Produktivitas harian tersedia setelah check-out.");
                }

                int targetPerHour = getTargetPerHour(user);

                long attendanceMinutes = ChronoUnit.MINUTES.between(
                                attendance.getCheckInAt(),
                                attendance.getCheckOutAt());

                int breakMinutes = attendance.getBreakMinutes() != null
                                ? attendance.getBreakMinutes()
                                : 0;

                int effectiveMinutes = attendance.getEffectiveMinutes() != null
                                ? attendance.getEffectiveMinutes()
                                : 0;

                int effectiveHours = effectiveMinutes / 60;
                int targetEffectiveMinutes = effectiveHours * 60;
                double dailyTarget = (double) targetPerHour * effectiveHours;

                List<Inspection> inspections = getDailyInspections(user, date);

                int totalInspected = inspections.stream()
                                .filter(i -> i.getCreatedAt() != null)
                                .filter(i -> !i.getCreatedAt().isBefore(attendance.getCheckInAt()))
                                .filter(i -> !i.getCreatedAt().isAfter(attendance.getCheckOutAt()))
                                .mapToInt(i -> i.getInspectQuantity() != null
                                                ? i.getInspectQuantity()
                                                : 0)
                                .sum();

                double dailyRate = calculateRate(totalInspected, dailyTarget);

                int remaining = Math.max(
                                0,
                                (int) Math.ceil(dailyTarget - totalInspected));

                ProductivitySummaryResponse summary = new ProductivitySummaryResponse(
                                user.getUsername(),
                                user.getNama(),
                                user.getSection().name(),
                                date.toString(),
                                totalInspected,
                                effectiveMinutes,
                                round(dailyTarget),
                                round(dailyRate),
                                remaining);

                List<HourlyProductivityResponse> hourly = calculateDailyHourlyProductivity(
                                attendance,
                                inspections,
                                targetPerHour,
                                targetEffectiveMinutes);

                return new DailyProductivityResponse(summary, hourly);
        }

        public ProductivitySummaryResponse getLiveProductivitySummary(String username) {
                return getLiveProductivityResult(username).summary();
        }

        public List<HourlyProductivityResponse> getLiveHourlyProductivity(String username) {
                return getLiveProductivityResult(username).hourly();
        }

        public DailyProductivityResponse getLiveProductivity(String username) {
                LiveProductivityResult result = getLiveProductivityResult(username);

                return new DailyProductivityResponse(
                                result.summary(),
                                result.hourly());
        }

        public DailyProductivityResponse getDailyProductivityForAdmin(String viewerUsername, String targetUserId,
                        LocalDate date) {
                User targetUser = validateProductivityAccess(
                                viewerUsername,
                                targetUserId);

                return getDailyProductivity(
                                targetUser.getUsername(),
                                date);
        }

        public ProductivitySummaryResponse getLiveProductivitySummaryForAdmin(String viewerUsername,
                        String targetUserId) {
                User targetUser = validateProductivityAccess(
                                viewerUsername,
                                targetUserId);

                return getLiveProductivitySummary(
                                targetUser.getUsername());
        }

        public List<HourlyProductivityResponse> getLiveHourlyProductivityForAdmin(String viewerUsername,
                        String targetUserId) {
                User targetUser = validateProductivityAccess(
                                viewerUsername,
                                targetUserId);

                return getLiveHourlyProductivity(
                                targetUser.getUsername());
        }

        public List<ProductivitySummaryResponse> getSectionDailyProductivity(
                        String viewerUsername,
                        String sectionName,
                        LocalDate date) {

                Section section = validateProductivitySection(sectionName);

                validateSectionAccess(viewerUsername, section);

                if (date == null) {
                        throw new BadRequestException("Tanggal wajib diisi.");
                }

                List<User> users = userRepository.findBySection(section);

                List<ProductivitySummaryResponse> result = new ArrayList<>();

                users.stream()
                                .filter(user -> user.getRole() != null
                                                && "USER".equals(user.getRole().name()))
                                .sorted(Comparator.comparing(
                                                User::getNama,
                                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                                .forEach(user -> {
                                        Attendance attendance = attendanceRepository
                                                        .findByUser_IdAndAttendanceDate(user.getId(), date)
                                                        .orElse(null);

                                        if (attendance == null
                                                        || attendance.getCheckInAt() == null
                                                        || attendance.getCheckOutAt() == null
                                                        || !attendance.getCheckOutAt()
                                                                        .isAfter(attendance.getCheckInAt())) {
                                                return;
                                        }

                                        result.add(getDailyProductivity(user.getUsername(), date)
                                                        .summary());
                                });

                return result;
        }

        public List<ProductivitySummaryResponse> getSectionLiveProductivity(
                        String viewerUsername,
                        String sectionName) {

                Section section = validateProductivitySection(sectionName);

                validateSectionAccess(viewerUsername, section);

                LocalDate today = LocalDate.now(ZONE);
                Instant now = Instant.now();

                List<User> users = userRepository.findBySection(section);

                List<ProductivitySummaryResponse> result = new ArrayList<>();

                users.stream()
                                .filter(user -> user.getRole() != null
                                                && "USER".equals(user.getRole().name()))
                                .sorted(Comparator.comparing(
                                                User::getNama,
                                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                                .forEach(user -> {
                                        Attendance attendance = attendanceRepository
                                                        .findByUser_IdAndAttendanceDate(user.getId(), today)
                                                        .orElse(null);

                                        if (attendance == null
                                                        || attendance.getCheckInAt() == null
                                                        || attendance.getCheckOutAt() != null
                                                        || !now.isAfter(attendance.getCheckInAt())) {
                                                return;
                                        }

                                        result.add(getLiveProductivitySummary(user.getUsername()));
                                });

                return result;
        }

        public SectionProductivitySummaryResponse getSectionDailySummary(
                        String viewerUsername,
                        String sectionName,
                        LocalDate date) {

                Section section = validateProductivitySection(sectionName);
                validateSectionAccess(viewerUsername, section);

                if (date == null) {
                        throw new BadRequestException("Tanggal wajib diisi.");
                }

                List<User> users = userRepository.findBySection(section);

                int completedOperators = 0;
                int totalInspected = 0;
                double totalTarget = 0;

                for (User user : users) {

                        if (user.getRole() == null
                                        || !"USER".equals(user.getRole().name())) {
                                continue;
                        }

                        Attendance attendance = attendanceRepository
                                        .findByUser_IdAndAttendanceDate(user.getId(), date)
                                        .orElse(null);

                        if (attendance == null
                                        || attendance.getCheckInAt() == null
                                        || attendance.getCheckOutAt() == null
                                        || !attendance.getCheckOutAt()
                                                        .isAfter(attendance.getCheckInAt())) {
                                continue;
                        }

                        ProductivitySummaryResponse summary = getDailyProductivity(user.getUsername(), date).summary();

                        totalInspected += summary.inspectedQuantity() != null
                                        ? summary.inspectedQuantity()
                                        : 0;

                        totalTarget += summary.targetQuantity() != null
                                        ? summary.targetQuantity()
                                        : 0;

                        completedOperators++;
                }

                double productivityRate = calculateRate(
                                totalInspected, totalTarget);

                int remainingQuantity = Math.max(
                                0,
                                (int) Math.ceil(totalTarget - totalInspected));

                return new SectionProductivitySummaryResponse(
                                section.name(),
                                date.toString(),
                                completedOperators,
                                0,
                                completedOperators,
                                totalInspected,
                                (int) Math.round(totalTarget),
                                round(productivityRate),
                                remainingQuantity);
        }

        public SectionProductivitySummaryResponse getSectionLiveSummary(
                        String viewerUsername,
                        String sectionName) {

                Section section = validateProductivitySection(sectionName);
                validateSectionAccess(viewerUsername, section);

                LocalDate today = LocalDate.now(ZONE);
                Instant now = Instant.now();

                List<User> users = userRepository.findBySection(section);

                int activeOperators = 0;
                int completedOperators = 0;
                int totalInspected = 0;
                int totalTarget = 0;

                for (User user : users) {

                        if (user.getRole() == null
                                        || !"USER".equals(user.getRole().name())) {
                                continue;
                        }

                        Attendance attendance = attendanceRepository
                                        .findByUser_IdAndAttendanceDate(user.getId(), today)
                                        .orElse(null);

                        if (attendance == null
                                        || attendance.getCheckInAt() == null
                                        || !attendance.getCheckInAt().isBefore(now)) {
                                continue;
                        }

                        ProductivitySummaryResponse summary;

                        if (attendance.getCheckOutAt() != null) {

                                if (!attendance.getCheckOutAt()
                                                .isAfter(attendance.getCheckInAt())) {
                                        continue;
                                }

                                summary = getDailyProductivity(
                                                user.getUsername(), today).summary();

                                completedOperators++;

                        } else {

                                summary = getLiveProductivitySummary(
                                                user.getUsername());

                                activeOperators++;
                        }

                        totalInspected += summary.inspectedQuantity();
                        totalTarget += summary.targetQuantity();
                }

                int totalOperators = activeOperators + completedOperators;

                double productivityRate = calculateRate(
                                totalInspected,
                                totalTarget);

                int remainingQuantity = Math.max(
                                0,
                                totalTarget - totalInspected);

                return new SectionProductivitySummaryResponse(
                                section.name(),
                                today.toString(),
                                totalOperators,
                                activeOperators,
                                completedOperators,
                                totalInspected,
                                totalTarget,
                                round(productivityRate),
                                remainingQuantity);
        }

        private Section validateProductivitySection(String sectionName) {
                if (sectionName == null || sectionName.isBlank()) {
                        throw new BadRequestException("Section wajib diisi.");
                }

                Section section;

                try {
                        section = Section.valueOf(
                                        sectionName.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                        throw new BadRequestException("Section tidak valid.");
                }

                if (section != Section.SEWING
                                && section != Section.FINISHING
                                && section != Section.WASHING) {
                        throw new BadRequestException(
                                        "Produktivitas hanya tersedia untuk section SEWING, FINISHING, dan WASHING.");
                }

                return section;
        }

        private void validateSectionAccess(String username, Section requestedSection) {
                User currentUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new BadRequestException("User tidak ditemukan."));

                String role = currentUser.getRole().name();

                if ("SUPER_ADMIN".equals(role)) {
                        return;
                }

                if ("ADMIN".equals(role)) {
                        if (currentUser.getSection() != requestedSection) {
                                throw new AccessDeniedException(
                                                "Kamu hanya dapat melihat produktivitas section sendiri.");
                        }
                        return;
                }

                throw new AccessDeniedException(
                                "Kamu tidak memiliki akses.");
        }

        private User validateProductivityAccess(String viewerUsername, String targetUserId) {
                User viewer = getUser(viewerUsername);

                if (viewer.getRole() == null || (!"ADMIN".equals(viewer.getRole().name())
                                && !"SUPER_ADMIN".equals(viewer.getRole().name()))) {
                        throw new AccessDeniedException("Anda tidak memiliki akses melihat produktivitas user lain.");
                }

                User targetUser = userRepository.findById(targetUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User QC tidak ditemukan."));

                if (targetUser.getRole() == null || !"USER".equals(targetUser.getRole().name())) {
                        throw new BadRequestException("Produktivitas hanya tersedia untuk user QC.");
                }

                if (targetUser.getSection() == null) {
                        throw new BadRequestException("User QC belum memiliki section.");
                }

                if ("SUPER_ADMIN".equals(viewer.getRole().name())) {
                        return targetUser;
                }

                if ("ADMIN".equals(viewer.getRole().name())) {

                        if (viewer.getSection() == null || viewer.getSection() != targetUser.getSection()) {
                                throw new AccessDeniedException(
                                                "Anda hanya dapat melihat produktivitas QC dari section Anda.");
                        }

                        return targetUser;
                }
                throw new AccessDeniedException("Anda tidak memiliki akses melihat produktivitas user lain.");
        }

        private LiveProductivityResult getLiveProductivityResult(String username) {
                User user = getUser(username);

                if (user.getSection() == null) {
                        throw new BadRequestException("User belum memiliki section.");
                }

                LocalDate today = LocalDate.now(ZONE);
                Instant now = Instant.now();

                Attendance attendance = attendanceRepository
                                .findByUser_IdAndAttendanceDate(user.getId(), today)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Data absensi hari ini tidak ditemukan."));

                validateAttendance(attendance);

                if (attendance.getCheckOutAt() != null) {
                        throw new BadRequestException(
                                        "Karyawan sudah check-out. Silakan gunakan produktivitas harian.");
                }

                if (!now.isAfter(attendance.getCheckInAt())) {
                        throw new BadRequestException("Waktu check-in belum dimulai.");
                }

                int targetPerHour = getTargetPerHour(user);

                Instant completedUntil = now
                                .atZone(ZONE)
                                .truncatedTo(ChronoUnit.HOURS)
                                .toInstant();

                List<Inspection> inspections = getDailyInspections(user, today);

                LiveCalculation calculation = calculateLiveHourlyProductivity(
                                attendance,
                                inspections,
                                targetPerHour,
                                completedUntil,
                                now);

                double accumulatedTarget = calculation.totalTarget();

                double accumulatedRate = calculateRate(
                                calculation.totalInspected(),
                                accumulatedTarget);

                int remaining = Math.max(
                                0,
                                (int) Math.ceil(
                                                accumulatedTarget - calculation.totalInspected()));

                ProductivitySummaryResponse summary = new ProductivitySummaryResponse(
                                user.getUsername(),
                                user.getNama(),
                                user.getSection().name(),
                                today.toString(),
                                calculation.totalInspected(),
                                calculation.effectiveMinutes(),
                                round(accumulatedTarget),
                                round(accumulatedRate),
                                remaining);

                return new LiveProductivityResult(
                                summary,
                                calculation.hourly());
        }

        private LiveCalculation calculateLiveHourlyProductivity(Attendance attendance, List<Inspection> inspections,
                        int targetPerHour, Instant completedUntil, Instant now) {
                Map<Instant, Integer> inspectedByHour = groupInspectionsByHour(attendance, inspections, now);

                List<HourlyProductivityResponse> result = new ArrayList<>();

                Instant checkIn = attendance.getCheckInAt();

                long elapsedMinutes = ChronoUnit.MINUTES.between(
                                checkIn,
                                now);

                int estimatedBreakMinutes = calculateBreakMinutes(elapsedMinutes);

                int effectiveMinutes = (int) Math.max(
                                0,
                                elapsedMinutes - estimatedBreakMinutes);

                int remainingTargetHours = effectiveMinutes / 60;

                Instant currentHour = checkIn
                                .atZone(ZONE)
                                .truncatedTo(ChronoUnit.HOURS)
                                .toInstant();

                int totalInspected = 0;
                double totalTarget = 0;

                while (!currentHour.plus(1, ChronoUnit.HOURS)
                                .isAfter(completedUntil)) {

                        Instant nextHour = currentHour.plus(
                                        1,
                                        ChronoUnit.HOURS);

                        Instant effectiveStart = currentHour.isAfter(checkIn)
                                        ? currentHour
                                        : checkIn;

                        long workingMinutes = ChronoUnit.MINUTES.between(
                                        effectiveStart,
                                        nextHour);

                        int inspected = inspectedByHour.getOrDefault(
                                        currentHour,
                                        0);

                        double hourlyTarget = 0;

                        if (workingMinutes == 60
                                        && remainingTargetHours > 0) {

                                hourlyTarget = targetPerHour;
                                remainingTargetHours--;
                        }

                        if (workingMinutes > 0) {

                                double hourlyRate = calculateRate(
                                                inspected,
                                                hourlyTarget);

                                String timeSlot = formatTimeSlot(
                                                currentHour,
                                                nextHour);

                                result.add(new HourlyProductivityResponse(
                                                timeSlot,
                                                inspected,
                                                round(hourlyTarget),
                                                round(hourlyRate)));

                                totalInspected += inspected;
                                totalTarget += hourlyTarget;
                        }

                        currentHour = nextHour;
                }

                return new LiveCalculation(
                                result,
                                totalInspected,
                                effectiveMinutes,
                                totalTarget);
        }

        private List<HourlyProductivityResponse> calculateDailyHourlyProductivity(Attendance attendance,
                        List<Inspection> inspections, int targetPerHour, int targetEffectiveMinutes) {
                Map<Instant, Integer> inspectedByHour = groupInspectionsByHour(attendance, inspections);

                List<HourlyProductivityResponse> result = new ArrayList<>();

                Instant checkIn = attendance.getCheckInAt();
                Instant checkOut = attendance.getCheckOutAt();

                Instant currentHour = checkIn
                                .atZone(ZONE)
                                .truncatedTo(ChronoUnit.HOURS)
                                .toInstant();

                int remainingTargetHours = targetEffectiveMinutes / 60;

                while (currentHour.isBefore(checkOut)) {

                        Instant nextHour = currentHour.plus(1, ChronoUnit.HOURS);

                        Instant overlapStart = currentHour.isAfter(checkIn)
                                        ? currentHour
                                        : checkIn;

                        Instant overlapEnd = nextHour.isBefore(checkOut)
                                        ? nextHour
                                        : checkOut;

                        long workingMinutes = Math.max(
                                        0,
                                        ChronoUnit.MINUTES.between(
                                                        overlapStart,
                                                        overlapEnd));

                        int inspected = inspectedByHour.getOrDefault(
                                        currentHour,
                                        0);

                        boolean isFullHour = workingMinutes == 60;

                        double hourlyTarget = 0;

                        if (isFullHour && remainingTargetHours > 0) {
                                hourlyTarget = targetPerHour;
                                remainingTargetHours--;
                        }

                        if (workingMinutes > 0) {

                                double rate = calculateRate(
                                                inspected,
                                                hourlyTarget);

                                String timeSlot = formatTimeSlot(
                                                currentHour,
                                                nextHour);

                                result.add(new HourlyProductivityResponse(
                                                timeSlot,
                                                inspected,
                                                round(hourlyTarget),
                                                round(rate)));
                        }

                        currentHour = nextHour;
                }

                return result;
        }

        private int calculateBreakMinutes(long workingMinutes) {
                if (workingMinutes >= 600) {
                        return 60;
                }

                // if (workingMinutes >= 510) {
                // return 30;
                // }

                return 0;
        }

        private Map<Instant, Integer> groupInspectionsByHour(Attendance attendance, List<Inspection> inspections,
                        Instant now) {
                Map<Instant, Integer> inspectedByHour = new TreeMap<>();

                for (Inspection inspection : inspections) {

                        Instant createdAt = inspection.getCreatedAt();

                        if (createdAt == null || createdAt.isBefore(attendance.getCheckInAt())
                                        || createdAt.isAfter(now)) {
                                continue;
                        }

                        if (attendance.getCheckOutAt() != null && createdAt.isAfter(attendance.getCheckOutAt())) {
                                continue;
                        }

                        InspectionSlot slot = parseInspectionSlot(inspection);

                        if (slot == null || !slot.end().isAfter(attendance.getCheckInAt())) {
                                continue;
                        }

                        if (attendance.getCheckOutAt() != null && slot.start().isAfter(attendance.getCheckOutAt())) {
                                continue;
                        }

                        int quantity = inspection.getInspectQuantity() != null
                                        ? inspection.getInspectQuantity()
                                        : 0;

                        inspectedByHour.merge(
                                        slot.start(),
                                        quantity,
                                        Integer::sum);
                }

                return inspectedByHour;
        }

        private Map<Instant, Integer> groupInspectionsByHour(Attendance attendance, List<Inspection> inspections) {
                Instant cutoff = attendance.getCheckOutAt() != null
                                ? attendance.getCheckOutAt()
                                : Instant.now();

                return groupInspectionsByHour(
                                attendance,
                                inspections,
                                cutoff);
        }

        private InspectionSlot parseInspectionSlot(Inspection inspection) {
                String timeSlot = inspection.getInspectionTimeSlot();

                if (timeSlot == null || timeSlot.isBlank() || inspection.getCreatedAt() == null) {
                        return null;
                }

                try {
                        String[] parts = timeSlot.split("\\s*-\\s*");

                        if (parts.length != 2) {
                                return null;
                        }

                        LocalDate inspectionDate = inspection.getCreatedAt()
                                        .atZone(ZONE)
                                        .toLocalDate();

                        LocalTime startTime = LocalTime.parse(parts[0].trim());
                        LocalTime endTime = LocalTime.parse(parts[1].trim());

                        LocalDateTime startDateTime = LocalDateTime.of(inspectionDate, startTime);

                        LocalDateTime endDateTime = LocalDateTime.of(inspectionDate, endTime);

                        if (!endDateTime.isAfter(startDateTime)) {
                                endDateTime = endDateTime.plusDays(1);
                        }

                        Instant start = startDateTime
                                        .atZone(ZONE)
                                        .toInstant();

                        Instant end = endDateTime
                                        .atZone(ZONE)
                                        .toInstant();

                        return new InspectionSlot(start, end);

                } catch (Exception e) {
                        return null;
                }
        }

        private String formatTimeSlot(Instant start, Instant end) {
                return start.atZone(ZONE).format(HOUR_FORMATTER) + "-" + end.atZone(ZONE).format(HOUR_FORMATTER);
        }

        private User getUser(String username) {
                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan."));
        }

        private int getTargetPerHour(User user) {

                SectionProductivityTarget targetConfig = targetRepository.findBySectionAndActiveTrue(user.getSection())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Target produktivitas section belum dikonfigurasi."));

                return targetConfig.getTargetPerHour();
        }

        private List<Inspection> getDailyInspections(User user, LocalDate date) {
                Instant startOfDay = date
                                .atStartOfDay(ZONE)
                                .toInstant();

                Instant endOfDay = date
                                .plusDays(1)
                                .atStartOfDay(ZONE)
                                .toInstant();

                return inspectionRepository.findBySubmittedBy_IdAndCreatedAtBetween(
                                user.getId(),
                                startOfDay,
                                endOfDay);
        }

        private void validateAttendance(Attendance attendance) {
                if (attendance.getCheckInAt() == null) {
                        throw new BadRequestException("Data check-in tidak ditemukan.");
                }

                if (attendance.getCheckOutAt() != null
                                && !attendance.getCheckOutAt().isAfter(attendance.getCheckInAt())) {

                        throw new BadRequestException("Waktu check-out tidak valid.");
                }
        }

        private long calculateOverlapMinutes(Instant start, Instant end, Instant breakStart, Instant breakEnd) {
                if (breakStart == null || breakEnd == null) {
                        return 0;
                }

                Instant overlapStart = start.isAfter(breakStart)
                                ? start
                                : breakStart;

                Instant overlapEnd = end.isBefore(breakEnd)
                                ? end
                                : breakEnd;

                if (!overlapEnd.isAfter(overlapStart)) {
                        return 0;
                }

                return ChronoUnit.MINUTES.between(
                                overlapStart,
                                overlapEnd);
        }

        private double calculateRate(double inspected, double target) {
                if (target <= 0) {
                        return 0;
                }
                return inspected / target * 100.0;
        }

        private double round(double value) {
                return Math.round(value * 100.0) / 100.0;
        }

        private record InspectionSlot(
                        Instant start,
                        Instant end) {

        }

        private record LiveCalculation(
                        List<HourlyProductivityResponse> hourly,
                        int totalInspected,
                        int effectiveMinutes,
                        double totalTarget) {
        }

        private record LiveProductivityResult(
                        ProductivitySummaryResponse summary,
                        List<HourlyProductivityResponse> hourly) {
        }
}
