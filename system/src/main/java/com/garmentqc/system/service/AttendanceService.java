
package com.garmentqc.system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.AttendanceResponse;
import com.garmentqc.system.dto.CheckInRequest;
import com.garmentqc.system.dto.CheckOutRequest;
import com.garmentqc.system.enums.AttendanceStatus;
import com.garmentqc.system.enums.Role;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.Attendance;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.AttendanceRepository;
import com.garmentqc.system.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

        private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");

        private final AttendanceRepository attendanceRepository;
        private final UserRepository userRepository;

        @Value("${app.attendance.latitude}")
        private double officeLatitude;

        @Value("${app.attendance.longitude}")
        private double officeLongitude;

        @Value("${app.attendance.radius-meter:100}")
        private double radiusMeter;

        @Value("${app.attendance.max-gps-accuracy-meter:50}")
        private double maxGpsAccuracy;

        private User getUser(String username) {
                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        private User validateAttendanceUser(String username) {
                User user = getUser(username);

                if (!user.isActive()) {
                        throw new UnauthorizedException("User tidak aktif");
                }

                if (user.getRole() != Role.USER) {
                        throw new UnauthorizedException("Hanya user QC yang dapat melakukan absensi");
                }

                return user;
        }

        private void validateLocation(double latitude, double longitude, double accuracy) {
                if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || !Double.isFinite(accuracy)) {
                        throw new BadRequestException("Koordinat GPS tidak valid");
                }

                if (latitude < -90 || latitude > 90) {
                        throw new BadRequestException("Latitude tidak valid");
                }

                if (longitude < -180 || longitude > 180) {
                        throw new BadRequestException("Longitude tidak valid");
                }

                if (accuracy <= 0 || accuracy > maxGpsAccuracy) {
                        throw new BadRequestException("Akurasi GPS tidak memenuhi persyaratan");
                }

                double distance = calculateDistance(
                                latitude,
                                longitude,
                                officeLatitude,
                                officeLongitude);

                if (distance > radiusMeter) {
                        throw new BadRequestException("Lokasi di luar radius absensi " + (int) radiusMeter + " meter");
                }
        }

        private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
                final double EARTH_RADIUS = 6371000;

                double dLat = Math.toRadians(lat2 - lat1);
                double dLon = Math.toRadians(lon2 - lon1);

                double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                                + Math.cos(Math.toRadians(lat1))
                                                * Math.cos(Math.toRadians(lat2))
                                                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

                double c = 2 * Math.atan2(
                                Math.sqrt(a),
                                Math.sqrt(1 - a));

                return EARTH_RADIUS * c;
        }

        private AttendanceResponse toResponse(Attendance attendance) {
                return AttendanceResponse.builder()
                                .id(attendance.getId())
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
                                .build();
        }

        @Transactional
        public AttendanceResponse checkIn(String username, CheckInRequest request) {
                User user = validateAttendanceUser(username);

                LocalDate today = LocalDate.now(ZONE);

                validateLocation(
                                request.getLatitude(),
                                request.getLongitude(),
                                request.getAccuracy());

                if (attendanceRepository.existsByUser_IdAndAttendanceDate(
                                user.getId(), today)) {

                        throw new BadRequestException("Anda sudah melakukan absensi hari ini");
                }

                Instant now = Instant.now();

                Attendance attendance = new Attendance();

                attendance.setUser(user);
                attendance.setAttendanceDate(today);
                attendance.setCheckInAt(now);

                attendance.setCheckInLatitude(request.getLatitude());
                attendance.setCheckInLongitude(request.getLongitude());
                attendance.setCheckInAccuracy(request.getAccuracy());

                attendance.setStatus(AttendanceStatus.INCOMPLETE);

                Attendance saved = attendanceRepository.save(attendance);

                return toResponse(saved);
        }

        @Transactional
        public AttendanceResponse checkOut(String username, CheckOutRequest request) {
                User user = validateAttendanceUser(username);

                LocalDate today = LocalDate.now(ZONE);

                Attendance attendance = attendanceRepository
                                .findByUser_IdAndAttendanceDate(
                                                user.getId(),
                                                today)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Anda belum melakukan check-in hari ini."));

                if (attendance.getCheckOutAt() != null) {
                        throw new BadRequestException("Anda sudah melakukan check-out hari ini.");
                }

                validateLocation(
                                request.getLatitude(),
                                request.getLongitude(),
                                request.getAccuracy());

                Instant now = Instant.now();
                Instant checkIn = attendance.getCheckInAt();

                if (checkIn == null || !now.isAfter(checkIn)) {
                        throw new BadRequestException("Waktu check-out tidak valid.");
                }

                long workingMinutes = ChronoUnit.MINUTES.between(
                                checkIn,
                                now);

                int breakMinutes;

                if (workingMinutes <= 570) {
                        breakMinutes = 0;
                } else if (workingMinutes <= 600) {
                        breakMinutes = 30;
                } else {
                        breakMinutes = 60;
                }

                int effectiveMinutes = Math.toIntExact(
                                workingMinutes - breakMinutes);

                attendance.setCheckOutAt(now);

                attendance.setCheckOutLatitude(request.getLatitude());
                attendance.setCheckOutLongitude(request.getLongitude());
                attendance.setCheckOutAccuracy(request.getAccuracy());

                attendance.setWorkingMinutes(Math.toIntExact(workingMinutes));
                attendance.setBreakMinutes(breakMinutes);
                attendance.setEffectiveMinutes(effectiveMinutes);
                attendance.setStatus(AttendanceStatus.PRESENT);

                Attendance saved = attendanceRepository.save(attendance);

                return toResponse(saved);
        }

        @Transactional(readOnly = true)
        public AttendanceResponse getToday(String username) {
                User user = validateAttendanceUser(username);

                LocalDate today = LocalDate.now(ZONE);

                Attendance attendance = attendanceRepository
                                .findByUser_IdAndAttendanceDate(
                                                user.getId(),
                                                today)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Data absensi hari ini belum tersedia."));

                return toResponse(attendance);
        }
}
