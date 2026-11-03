package com.garmentqc.system.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, String> {

    Optional<Attendance> findByUser_IdAndAttendanceDate(String userId, LocalDate attendanceDate);

    boolean existsByUser_IdAndAttendanceDate(String userId, LocalDate attendanceDate);

}
