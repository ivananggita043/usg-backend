
package com.garmentqc.system.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.CreateUserRequest;
import com.garmentqc.system.dto.CreateUserResponse;
import com.garmentqc.system.enums.Role;
import com.garmentqc.system.enums.Section;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.model.UserLine;
import com.garmentqc.system.repository.LineRepository;
import com.garmentqc.system.repository.UserLineRepository;
import com.garmentqc.system.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

        private final UserRepository userRepository;
        private final UserLineRepository userLineRepository;
        private final LineRepository lineRepository;
        private final PasswordEncoder passwordEncoder;

        @Transactional(readOnly = true)
        public List<CreateUserResponse> getAllUsers() {
                User currentUser = getCurrentUser();
                List<User> users;
                if (currentUser.getRole() == Role.SUPER_ADMIN) {
                        users = userRepository.findAll();
                } else if (currentUser.getRole() == Role.ADMIN) {
                        validateAdminSection(currentUser);
                        users = userRepository.findBySection(currentUser.getSection());
                } else {
                        throw new BadRequestException("Anda tidak memiliki akses untuk melihat daftar user");
                }
                return users.stream()
                                .map(user -> toResponse(user, getUserLines(user)))
                                .toList();
        }

        @Transactional(readOnly = true)
        public CreateUserResponse getUserById(String id) {
                validateId(id);
                User currentUser = getCurrentUser();
                User targetUser = findUser(id);
                validateUserAccess(currentUser, targetUser);
                return toResponse(targetUser, getUserLines(targetUser));
        }

        @Transactional
        public CreateUserResponse registerUser(CreateUserRequest request) {
                validateCreateRequest(request);
                User currentUser = getCurrentUser();
                validateManager(currentUser);

                String username = request.getUsername().trim();
                String nama = request.getNama().trim();
                String nik = request.getNik().trim();
                if (userRepository.existsByUsername(username)) {
                        throw new BadRequestException("Username sudah digunakan");
                }

                Role role = request.getRole();
                if (currentUser.getRole() == Role.ADMIN && role == Role.SUPER_ADMIN) {
                        throw new BadRequestException("ADMIN tidak dapat membuat SUPER ADMIN");
                }

                User user = new User();
                user.setNama(nama);
                user.setUsername(username);
                user.setNik(nik);
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user.setActive(true);
                user.setRole(role);

                List<Line> lines = List.of();
                if (role == Role.SUPER_ADMIN) {
                        user.setSection(null);

                } else if (role == Role.ADMIN) {
                        Section section;
                        if (currentUser.getRole() == Role.ADMIN) {
                                validateAdminSection(currentUser);
                                section = currentUser.getSection();
                        } else {
                                section = request.getSection();
                                if (section == null) {
                                        throw new BadRequestException("Section wajib diisi untuk ADMIN");
                                }
                        }
                        user.setSection(section);
                } else if (role == Role.USER) {
                        lines = getLinesFromRequest(request.getLineIds());
                        if (lines.isEmpty()) {
                                throw new BadRequestException("Pilih minimal satu line untuk akun QC");
                        }

                        validateActiveLines(lines);
                        validateSameSection(lines);
                        validateAdminLines(currentUser, lines);
                        Section lineSection = lines.get(0).getSection();

                        if (request.getSection() != null && request.getSection() != lineSection) {
                                throw new BadRequestException("Section tidak sesuai dengan section line");
                        }
                        user.setSection(lineSection);
                } else {
                        throw new BadRequestException("Role tidak valid");
                }
                User savedUser = userRepository.save(user);
                saveUserLines(savedUser, lines);
                return toResponse(savedUser, lines);
        }

        @Transactional
        public CreateUserResponse updateUser(String id, CreateUserRequest request) {
                validateId(id);
                validateUpdateRequest(request);

                User currentUser = getCurrentUser();
                validateManager(currentUser);

                User user = findUser(id);
                validateUserAccess(currentUser, user);

                Role role = request.getRole();
                if (currentUser.getRole() == Role.ADMIN) {
                        if (role == Role.SUPER_ADMIN) {
                                throw new BadRequestException("ADMIN tidak dapat mengubah user menjadi SUPER ADMIN");
                        }
                        if (request.getSection() != null && request.getSection() != currentUser.getSection()) {
                                throw new BadRequestException("ADMIN hanya dapat mengelola user pada section-nya");
                        }
                }

                if (currentUser.getId().equals(user.getId())
                                && currentUser.getRole() == Role.SUPER_ADMIN
                                && role != Role.SUPER_ADMIN) {
                        throw new BadRequestException("Super Admin tidak dapat mengubah role akunnya sendiri");
                }

                String username = request.getUsername().trim();
                if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
                        throw new BadRequestException("Username sudah digunakan");
                }
                user.setNama(request.getNama().trim());
                user.setUsername(username);
                user.setNik(request.getNik().trim());
                user.setRole(role);

                if (request.getPassword() != null && !request.getPassword().isBlank()) {
                        user.setPassword(passwordEncoder.encode(request.getPassword()));
                }

                List<Line> lines = List.of();
                if (role == Role.SUPER_ADMIN) {
                        user.setSection(null);

                } else if (role == Role.ADMIN) {
                        Section section;
                        if (currentUser.getRole() == Role.ADMIN) {
                                validateAdminSection(currentUser);
                                section = currentUser.getSection();
                        } else {
                                section = request.getSection();
                                if (section == null) {
                                        throw new BadRequestException("Section wajib diisi untuk ADMIN");
                                }
                        }
                        user.setSection(section);
                } else if (role == Role.USER) {
                        lines = getLinesFromRequest(request.getLineIds());
                        if (lines.isEmpty()) {
                                throw new BadRequestException("User QC harus memiliki minimal satu line");
                        }
                        validateActiveLines(lines);
                        validateSameSection(lines);
                        validateAdminLines(currentUser, lines);
                        Section lineSection = lines.get(0).getSection();
                        if (request.getSection() != null && request.getSection() != lineSection) {
                                throw new BadRequestException("Section tidak sesuai dengan section line");
                        }
                        user.setSection(lineSection);
                } else {
                        throw new BadRequestException("Role tidak valid");
                }
                User savedUser = userRepository.save(user);
                userLineRepository.deleteByUser(savedUser);
                userLineRepository.flush();
                saveUserLines(savedUser, lines);
                return toResponse(savedUser, lines);
        }

        @Transactional
        public CreateUserResponse updateUserStatus(String id, boolean active) {
                validateId(id);

                User currentUser = getCurrentUser();
                validateManager(currentUser);

                User user = findUser(id);
                validateUserAccess(currentUser, user);

                if (currentUser.getId().equals(user.getId())) {
                        throw new BadRequestException("Anda tidak dapat mengubah status akun sendiri");
                }
                user.setActive(active);
                User savedUser = userRepository.save(user);
                return toResponse(savedUser, getUserLines(savedUser));
        }

        @Transactional
        public void deleteUser(String id) {
                validateId(id);

                User currentUser = getCurrentUser();
                validateManager(currentUser);

                User user = findUser(id);
                validateUserAccess(currentUser, user);
                if (currentUser.getId().equals(user.getId())) {
                        throw new BadRequestException("Anda tidak dapat menghapus akun sendiri");
                }
                userLineRepository.deleteByUser(user);
                userRepository.delete(user);
        }

        @Transactional(readOnly = true)
        public CreateUserResponse getCurrentUserProfile() {
                User currentUser = getCurrentUser();
                return toResponse(currentUser, getUserLines(currentUser));
        }

        private void validateCreateRequest(CreateUserRequest request) {
                if (request == null) {
                        throw new BadRequestException("Request body wajib diisi");
                }
                requireText(request.getNama(), "Nama wajib diisi");
                requireText(request.getUsername(), "Username wajib diisi");
                requireText(request.getNik(), "NIK wajib diisi");
                requireText(request.getPassword(), "Password wajib diisi");
                if (request.getRole() == null) {
                        throw new BadRequestException("Role wajib dipilih");
                }
        }

        private void validateUpdateRequest(CreateUserRequest request) {
                if (request == null) {
                        throw new BadRequestException("Request body wajib diisi");
                }
                requireText(request.getNama(), "Nama wajib diisi");
                requireText(request.getUsername(), "Username wajib diisi");
                requireText(request.getNik(), "NIK wajib diisi");
                if (request.getRole() == null) {
                        throw new BadRequestException("Role wajib dipilih");
                }

        }

        private void requireText(String value, String message) {
                if (value == null || value.isBlank()) {
                        throw new BadRequestException(message);
                }
        }

        private void validateId(String id) {
                requireText(id, "ID user wajib diisi");
        }

        private User getCurrentUser() {
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();
                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication instanceof AnonymousAuthenticationToken) {
                        throw new UnauthorizedException("User belum terautentikasi. Silakan login kembali.");
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

                if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
                        throw new UnauthorizedException("User belum terautentikasi. Silakan login kembali.");
                }

                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Data user yang login tidak ditemukan"));
        }

        private void validateUserAccess(User currentUser, User targetUser) {
                if (currentUser.getRole() == Role.SUPER_ADMIN) {
                        return;
                }
                if (currentUser.getRole() != Role.ADMIN) {
                        throw new BadRequestException("Anda tidak memiliki akses");
                }
                validateAdminSection(currentUser);
                if (targetUser.getRole() == Role.SUPER_ADMIN) {
                        throw new BadRequestException("ADMIN tidak memiliki akses ke SUPER ADMIN");
                }

                if (currentUser.getId().equals(targetUser.getId())) {
                        throw new BadRequestException("ADMIN tidak dapat mengelola akun sendiri melalui fitur ini");
                }

                if (targetUser.getSection() != currentUser.getSection()) {
                        throw new BadRequestException("Anda hanya dapat mengelola user pada section Anda");
                }
        }

        private void validateManager(User currentUser) {
                if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPER_ADMIN) {
                        throw new BadRequestException("Anda tidak memiliki akses untuk mengelola user");
                }
                if (currentUser.getRole() == Role.ADMIN) {
                        validateAdminSection(currentUser);
                }
        }

        private void validateAdminSection(User currentUser) {
                if (currentUser.getSection() == null) {
                        throw new BadRequestException("Section ADMIN belum ditentukan");
                }
        }

        private List<Line> getLinesFromRequest(List<String> lineIds) {
                if (lineIds == null || lineIds.isEmpty()) {
                        return List.of();
                }
                if (lineIds.stream().anyMatch(id -> id == null || id.isBlank())) {
                        throw new BadRequestException("ID line tidak boleh kosong");
                }

                Set<String> uniqueIds = new HashSet<>(lineIds);
                if (uniqueIds.size() != lineIds.size()) {
                        throw new BadRequestException("Line yang dipilih tidak boleh duplikat");
                }

                List<Line> lines = lineRepository.findAllById(lineIds);
                if (lines.size() != uniqueIds.size()) {
                        throw new ResourceNotFoundException("Line yang dipilih tidak ditemukan");
                }
                return lines;
        }

        private List<Line> getUserLines(User user) {
                return userLineRepository.findByUser(user)
                                .stream()
                                .map(UserLine::getLine)
                                .toList();
        }

        private void validateActiveLines(List<Line> lines) {
                boolean hasInactiveLine = lines.stream().anyMatch(line -> !line.isActive());
                if (hasInactiveLine) {
                        throw new BadRequestException("Line yang dipilih sudah tidak aktif");
                }
        }

        private void validateSameSection(List<Line> lines) {
                if (lines.isEmpty()) {
                        return;
                }
                Section section = lines.get(0).getSection();
                boolean differentSection = lines.stream().anyMatch(line -> line.getSection() != section);
                if (differentSection) {
                        throw new BadRequestException("Semua line harus berasal dari section yang sama");
                }
        }

        private void validateAdminLines(User currentUser, List<Line> lines) {
                if (currentUser.getRole() == Role.SUPER_ADMIN) {
                        return;
                }
                if (currentUser.getRole() == Role.ADMIN) {
                        validateAdminSection(currentUser);
                        boolean differentSection = lines.stream()
                                        .anyMatch(line -> line.getSection() != currentUser.getSection());
                        if (differentSection) {
                                throw new BadRequestException("Anda hanya dapat menggunakan line dari section Anda");
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

        private User findUser(String id) {
                return userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));
        }

        private CreateUserResponse toResponse(User user, List<Line> lines) {
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
