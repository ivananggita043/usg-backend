package com.garmentqc.system.model;

import java.time.Instant;
import java.time.LocalDate;

import com.garmentqc.system.enums.AttendanceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attendances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_attendance_user_date", columnNames = { "user_id", "attendance_date" })

})
@Getter
@Setter
@NoArgsConstructor
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Column(name = "check_in_latitude")
    private Double checkInLatitude;

    @Column(name = "check_in_longitude")
    private Double checkInLongitude;

    @Column(name = "check_out_latitude")
    private Double checkOutLatitude;

    @Column(name = "check_out_longitude")
    private Double checkOutLongitude;

    @Column(name = "check_in_accuracy")
    private Double checkInAccuracy;

    @Column(name = "check_out_accuracy")
    private Double checkOutAccuracy;

    @Column(name = "break_start_at")
    private Instant breakStartAt;

    @Column(name = "break_end_at")
    private Instant breakEndAt;

    @Column(name = "working_minutes")
    private Integer workingMinutes = 0;

    @Column(name = "break_minutes")
    private Integer breakMinutes = 0;

    @Column(name = "effective_minutes")
    private Integer effectiveMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AttendanceStatus status = AttendanceStatus.INCOMPLETE;

}
