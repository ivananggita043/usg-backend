
package com.garmentqc.system.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.garmentqc.system.enums.AttendanceStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendanceResponse {

    private String id;

    private LocalDate attendanceDate;

    private Instant checkInAt;

    private Instant checkOutAt;

    private Double checkInLatitude;

    private Double checkInLongitude;

    private Double checkInAccuracy;

    private Double checkOutLatitude;

    private Double checkOutLongitude;

    private Double checkOutAccuracy;

    private Instant breakStartAt;

    private Instant breakEndAt;

    private Integer workingMinutes;

    private Integer breakMinutes;

    private Integer effectiveMinutes;

    private AttendanceStatus status;
}
