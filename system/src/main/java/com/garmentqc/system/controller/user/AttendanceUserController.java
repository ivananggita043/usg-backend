
package com.garmentqc.system.controller.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.AttendanceResponse;
import com.garmentqc.system.dto.CheckInRequest;
import com.garmentqc.system.dto.CheckOutRequest;
import com.garmentqc.system.service.AttendanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class AttendanceUserController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public AttendanceResponse checkIn(
            Authentication authentication,
            @Valid @RequestBody CheckInRequest request) {

        return attendanceService.checkIn(
                authentication.getName(), request);
    }

    @PostMapping("/check-out")
    public AttendanceResponse checkOut(
            Authentication authentication,
            @Valid @RequestBody CheckOutRequest request) {

        return attendanceService.checkOut(
                authentication.getName(), request);
    }

    @GetMapping("/today")
    public AttendanceResponse getToday(
            Authentication authentication) {

        return attendanceService.getToday(
                authentication.getName());
    }
}
