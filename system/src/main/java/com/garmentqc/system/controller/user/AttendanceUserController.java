
package com.garmentqc.system.controller.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.AttendanceResponse;
import com.garmentqc.system.dto.CheckInRequest;
import com.garmentqc.system.dto.CheckOutRequest;
import com.garmentqc.system.model.User;
import com.garmentqc.system.service.AttendanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class AttendanceUserController {

        private final AttendanceService attendanceService;

        private User getCurrentUser(Authentication authentication) {
                return (User) authentication.getPrincipal();
        }

        @PostMapping("/check-in")
        public AttendanceResponse checkIn(
                        Authentication authentication,
                        @Valid @RequestBody CheckInRequest request) {
                return attendanceService.checkIn(getCurrentUser(authentication), request);
        }

        @PostMapping("/check-out")
        public AttendanceResponse checkOut(
                        Authentication authentication,
                        @Valid @RequestBody CheckOutRequest request) {
                return attendanceService.checkOut(getCurrentUser(authentication), request);
        }

        @GetMapping("/today")
        public AttendanceResponse getToday(Authentication authentication) {
                return attendanceService.getToday(getCurrentUser(authentication));
        }

        @GetMapping("/all")
        public List<AttendanceResponse> getAll(Authentication authentication) {
                return attendanceService.getAll(getCurrentUser(authentication));
        }

        @GetMapping("/paginated")
        public Page<AttendanceResponse> getAllPaginated(
                        Authentication authentication,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "attendanceDate") String sort,
                        @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

                Pageable pageable = PageRequest.of(
                                page,
                                Math.min(Math.max(size, 1), 100));

                return attendanceService.getAllPaginated(
                                getCurrentUser(authentication), pageable);
        }

        @GetMapping("/{id}")
        public AttendanceResponse getById(
                        Authentication authentication,
                        @PathVariable String id) {
                return attendanceService.getById(getCurrentUser(authentication), id);
        }
}
