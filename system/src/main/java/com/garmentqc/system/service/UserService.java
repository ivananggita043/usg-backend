
package com.garmentqc.system.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.garmentqc.system.dto.CreateUserRequest;
import com.garmentqc.system.dto.CreateUserResponse;
import com.garmentqc.system.enums.Role;
import com.garmentqc.system.enums.Section;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.model.UserLine;
import com.garmentqc.system.repository.LineRepository;
import com.garmentqc.system.repository.UserLineRepository;
import com.garmentqc.system.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

        private final UserRepository userRepository;
        private final UserLineRepository userLineRepository;
        private final LineRepository lineRepository;
        private final PasswordEncoder passwordEncoder;

        public List<CreateUserResponse> getAllUsers() {

                User currentUser = getCurrentUser();

                List<User> users;

                if (currentUser.getRole() == Role.SUPER_ADMIN) {
                        users = userRepository.findAll();

                } else if (currentUser.getRole() == Role.ADMIN) {

                        users = userRepository.findBySection(
                                        currentUser.getSection());

                } else {
                        throw new BadRequestException(
                                        "Anda tidak memiliki akses untuk melihat daftar user");
                }

                return users.stream()
                                .map(user -> {
                                        List<Line> lines = userLineRepository
                                                        .findByUser(user)
                                                        .stream()
                                                        .map(UserLine::getLine)
                                                        .toList();

                                        return toResponse(user, lines);
                                })
                                .toList();
        }

        public CreateUserResponse getUserById(String id) {

                User currentUser = getCurrentUser();

                User user = userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User tidak ditemukan"));

                validateUserAccess(currentUser, user);

                List<Line> lines = userLineRepository
                                .findByUser(user)
                                .stream()
                                .map(UserLine::getLine)
                                .toList();

                return toResponse(user, lines);
        }

        @Transactional
        public CreateUserResponse registerUser(CreateUserRequest request) {

                if (userRepository.existsByUsername(request.getUsername())) {
                        throw new BadRequestException("Username sudah digunakan");
                }

                User user = new User();
                user.setNama(request.getNama());
                user.setUsername(request.getUsername());
                user.setNik(request.getNik());
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user.setActive(true);

                Role role = request.getRole();

                if (role == null) {
                        throw new BadRequestException("Role wajib dipilih");
                }

                user.setRole(role);

                List<Line> lines = getLinesFromRequest(request.getLineIds());

                if (role == Role.SUPER_ADMIN) {

                        user.setSection(null);
                        lines = List.of();

                } else if (role == Role.ADMIN) {

                        if (request.getSection() == null) {
                                throw new BadRequestException(
                                                "Section wajib diisi untuk ADMIN");
                        }

                        user.setSection(request.getSection());
                        lines = List.of();

                } else if (role == Role.USER) {

                        if (lines.isEmpty()) {
                                throw new BadRequestException(
                                                "Pilih minimal satu line untuk akun QC");
                        }

                        validateActiveLines(lines);
                        validateSameSection(lines);

                        Section section = lines.get(0).getSection();

                        if (request.getSection() != null
                                        && request.getSection() != section) {
                                throw new BadRequestException(
                                                "Section tidak sesuai dengan section line");
                        }

                        user.setSection(section);
                }

                User savedUser = userRepository.save(user);
                saveUserLines(savedUser, lines);

                return toResponse(savedUser, lines);
        }

        @Transactional
        public CreateUserResponse updateUser(
                        String id,
                        CreateUserRequest request) {

                User currentUser = getCurrentUser();

                User user = userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User tidak ditemukan"));

                validateUserAccess(currentUser, user);

                if (request.getRole() == null) {
                        throw new BadRequestException("Role wajib dipilih");
                }

                if (currentUser.getRole() == Role.ADMIN) {

                        if (request.getRole() == Role.SUPER_ADMIN) {
                                throw new BadRequestException(
                                                "ADMIN tidak dapat mengubah user menjadi SUPER ADMIN");
                        }

                        if (request.getSection() != null
                                        && request.getSection() != currentUser.getSection()) {
                                throw new BadRequestException(
                                                "ADMIN hanya dapat mengelola user pada section-nya");
                        }
                }

                if (!user.getUsername().equals(request.getUsername())
                                && userRepository.existsByUsername(request.getUsername())) {
                        throw new BadRequestException("Username sudah digunakan");
                }

                user.setNama(request.getNama());
                user.setUsername(request.getUsername());
                user.setNik(request.getNik());
                user.setRole(request.getRole());

                if (request.getPassword() != null
                                && !request.getPassword().isBlank()) {
                        user.setPassword(
                                        passwordEncoder.encode(request.getPassword()));
                }

                List<Line> lines = getLinesFromRequest(request.getLineIds());

                if (request.getRole() == Role.SUPER_ADMIN) {

                        user.setSection(null);
                        lines = List.of();

                } else if (request.getRole() == Role.ADMIN) {

                        Section section = currentUser.getRole() == Role.ADMIN
                                        ? currentUser.getSection()
                                        : request.getSection();

                        if (section == null) {
                                throw new BadRequestException(
                                                "Section wajib diisi untuk ADMIN");
                        }

                        user.setSection(section);
                        lines = List.of();

                } else if (request.getRole() == Role.USER) {

                        if (lines.isEmpty()) {
                                throw new BadRequestException(
                                                "User QC harus memiliki minimal satu line");
                        }

                        validateActiveLines(lines);
                        validateSameSection(lines);
                        validateAdminLines(currentUser, lines);

                        Section lineSection = lines.get(0).getSection();

                        if (request.getSection() != null
                                        && request.getSection() != lineSection) {
                                throw new BadRequestException(
                                                "Section tidak sesuai dengan section line");
                        }

                        user.setSection(lineSection);

                } else {
                        throw new BadRequestException("Role tidak valid");
                }

                User savedUser = userRepository.save(user);

                userLineRepository.deleteByUser(savedUser);
                saveUserLines(savedUser, lines);

                return toResponse(savedUser, lines);
        }

        @Transactional
        public CreateUserResponse updateUserStatus(
                        String id,
                        boolean active) {

                User currentUser = getCurrentUser();

                User user = userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User tidak ditemukan"));

                validateUserAccess(currentUser, user);

                List<Line> lines = userLineRepository
                                .findByUser(user)
                                .stream()
                                .map(UserLine::getLine)
                                .toList();

                user.setActive(active);

                User savedUser = userRepository.save(user);

                return toResponse(savedUser, lines);
        }

        @Transactional
        public void deleteUser(String id) {

                User currentUser = getCurrentUser();

                User user = userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User tidak ditemukan"));

                validateUserAccess(currentUser, user);

                if (currentUser.getRole() == Role.ADMIN
                                && user.getRole() == Role.SUPER_ADMIN) {
                        throw new BadRequestException(
                                        "ADMIN tidak dapat menghapus SUPER ADMIN");
                }

                userLineRepository.deleteByUser(user);
                userRepository.delete(user);
        }

        private User getCurrentUser() {

                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                        throw new BadRequestException(
                                        "User belum terautentikasi. Silakan login kembali.");
                }

                String username;

                Object principal = authentication.getPrincipal();

                if (principal instanceof User user) {
                        username = user.getUsername();
                } else if (principal instanceof UserDetails userDetails) {
                        username = userDetails.getUsername();
                } else {
                        username = authentication.getName();
                }

                if (username == null || username.isBlank()
                                || "anonymousUser".equals(username)) {
                        throw new BadRequestException(
                                        "User belum terautentikasi. Silakan login kembali.");
                }

                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Data user yang login tidak ditemukan"));
        }

        private void validateUserAccess(
                        User currentUser,
                        User targetUser) {

                if (currentUser.getRole() == Role.SUPER_ADMIN) {
                        return;
                }

                if (currentUser.getRole() == Role.ADMIN) {

                        if (currentUser.getSection() == null) {
                                throw new BadRequestException(
                                                "Section ADMIN belum ditentukan");
                        }

                        if (targetUser.getSection() != currentUser.getSection()) {
                                throw new BadRequestException(
                                                "Anda hanya dapat mengelola user pada section Anda");
                        }

                        if (targetUser.getRole() == Role.SUPER_ADMIN) {
                                throw new BadRequestException(
                                                "ADMIN tidak memiliki akses ke SUPER ADMIN");
                        }

                        return;
                }

                throw new BadRequestException(
                                "Anda tidak memiliki akses");
        }

        private List<Line> getLinesFromRequest(List<String> lineIds) {

                if (lineIds == null || lineIds.isEmpty()) {
                        return List.of();
                }

                List<Line> lines = lineRepository.findAllById(lineIds);

                if (lines.size() != lineIds.size()) {
                        throw new ResourceNotFoundException(
                                        "Line yang dipilih tidak ditemukan");
                }

                return lines;
        }

        public CreateUserResponse getCurrentUserProfile() {

                User currentUser = getCurrentUser();

                List<Line> lines = userLineRepository
                                .findByUser(currentUser)
                                .stream()
                                .map(UserLine::getLine)
                                .toList();

                return toResponse(currentUser, lines);
        }

        private void validateActiveLines(List<Line> lines) {

                boolean hasInactiveLine = lines.stream()
                                .anyMatch(line -> !line.isActive());

                if (hasInactiveLine) {
                        throw new BadRequestException(
                                        "Line yang dipilih sudah tidak aktif");
                }
        }

        private void validateSameSection(List<Line> lines) {

                if (lines.isEmpty()) {
                        return;
                }

                Section section = lines.get(0).getSection();

                boolean differentSection = lines.stream()
                                .anyMatch(line -> line.getSection() != section);

                if (differentSection) {
                        throw new BadRequestException(
                                        "Semua line harus berasal dari section yang sama");
                }
        }

        private void validateAdminLines(
                        User currentUser,
                        List<Line> lines) {

                if (currentUser.getRole() == Role.SUPER_ADMIN) {
                        return;
                }

                if (currentUser.getRole() == Role.ADMIN) {

                        if (currentUser.getSection() == null) {
                                throw new BadRequestException(
                                                "Section ADMIN belum ditentukan");
                        }

                        boolean differentSection = lines.stream()
                                        .anyMatch(line -> line.getSection() != currentUser.getSection());

                        if (differentSection) {
                                throw new BadRequestException(
                                                "Anda hanya dapat menggunakan line dari section Anda");
                        }
                }
        }

        private void saveUserLines(User user, List<Line> lines) {

                for (Line line : lines) {

                        UserLine userLine = new UserLine();
                        userLine.setUser(user);
                        userLine.setLine(line);

                        userLineRepository.save(userLine);
                }
        }

        private CreateUserResponse toResponse(
                        User user,
                        List<Line> lines) {

                List<String> lineIds = lines.stream()
                                .map(Line::getId)
                                .toList();

                return CreateUserResponse.builder()
                                .id(user.getId())
                                .nama(user.getNama())
                                .username(user.getUsername())
                                .nik(user.getNik())
                                .role(user.getRole())
                                .section(user.getSection())
                                .active(user.isActive())
                                .lineIds(lineIds)
                                .createdAt(user.getCreatedAt())
                                .updatedAt(user.getUpdatedAt())
                                .build();
        }
}
