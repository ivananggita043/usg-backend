package com.garmentqc.system.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.garmentqc.system.dto.CreateUserRequest;
import com.garmentqc.system.dto.CreateUserResponse;
import com.garmentqc.system.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementAdminController {

    private final UserService userService;

    private void validateAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Akses hanya untuk Admin.");
        }
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(Authentication authentication,
            @Valid @RequestBody CreateUserRequest request) {
        validateAdmin(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CreateUserResponse>> getAllUsers(Authentication authentication) {
        validateAdmin(authentication);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreateUserResponse> getUserById(Authentication authentication, @PathVariable String id) {
        validateAdmin(authentication);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreateUserResponse> updateUser(Authentication authentication, @PathVariable String id,
            @Valid @RequestBody CreateUserRequest request) {
        validateAdmin(authentication);
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CreateUserResponse> updateUserStatus(Authentication authentication, @PathVariable String id,
            @RequestParam boolean active) {
        validateAdmin(authentication);
        return ResponseEntity.ok(userService.updateUserStatus(id, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(Authentication authentication, @PathVariable String id) {
        validateAdmin(authentication);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CreateUserResponse> getCurrentUserProfile(Authentication authentication) {
        validateAdmin(authentication);
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }
}