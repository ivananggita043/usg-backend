package com.garmentqc.system.controller.superadmin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.CreateUserRequest;
import com.garmentqc.system.dto.CreateUserResponse;
import com.garmentqc.system.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/super-admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserManagementSuperAdminController {

    private final UserService userService;

    private void validateSuperAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            throw new AccessDeniedException("Akses hanya untuk Super Admin.");
        }
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(Authentication authentication,
            @Valid @RequestBody CreateUserRequest request) {
        validateSuperAdmin(authentication);
        CreateUserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CreateUserResponse>> getAllUsers(Authentication authentication) {
        validateSuperAdmin(authentication);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreateUserResponse> getUserById(Authentication authentication, @PathVariable String id) {
        validateSuperAdmin(authentication);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreateUserResponse> updateUser(Authentication authentication, @PathVariable String id,
            @Valid @RequestBody CreateUserRequest request) {
        validateSuperAdmin(authentication);
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(Authentication authentication, @PathVariable String id) {
        validateSuperAdmin(authentication);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CreateUserResponse> updateUserStatus(Authentication authentication, @PathVariable String id,
            @RequestParam boolean active) {
        validateSuperAdmin(authentication);
        return ResponseEntity.ok(userService.updateUserStatus(id, active));
    }

    @GetMapping("/me")
    public ResponseEntity<CreateUserResponse> getCurrentUserProfile(Authentication authentication) {
        validateSuperAdmin(authentication);
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }
}