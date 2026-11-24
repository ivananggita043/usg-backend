
package com.garmentqc.system.controller.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
        public AttendanceResponse checkIn(Authentication authentication, @Valid @RequestBody CheckInRequest request) {
                return attendanceService.checkIn(authentication.getName(), request);
        }

        @PostMapping("/check-out")
        public AttendanceResponse checkOut(Authentication authentication, @Valid @RequestBody CheckOutRequest request) {
                return attendanceService.checkOut(authentication.getName(), request);
        }

        @GetMapping("/today")
        public AttendanceResponse getToday(Authentication authentication) {
                return attendanceService.getToday(authentication.getName());
        }

        @GetMapping("/all")
        public List<AttendanceResponse> getAll(Authentication authentication) {
                return attendanceService.getAll(authentication.getName());
        }

        @GetMapping("/paginated")
        public Page<AttendanceResponse> getAllPaginated(Authentication authentication,
                        @PageableDefault(page = 0, size = 10, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable) {
                return attendanceService.getAllPaginated(
                                authentication.getName(),
                                pageable);
        }

        @GetMapping("/{id}")
        public AttendanceResponse getById(Authentication authentication, @PathVariable String id) {
                return attendanceService.getById(
                                authentication.getName(),
                                id);
        }
}
