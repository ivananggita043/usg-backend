package com.garmentqc.system.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.LineRequest;
import com.garmentqc.system.enums.Section;
import com.garmentqc.system.exception.BadRequestException;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.LineRepository;
import com.garmentqc.system.repository.UserLineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LineService {

    private final LineRepository lineRepository;
    private final UserLineRepository userLineRepository;

    public Line createLine(LineRequest request) {
        validateDuplicate(request.getName(), request.getSection(), null);
        Line line = new Line();
        line.setName(request.getName());
        line.setSection(request.getSection());
        line.setActive(true);

        return lineRepository.save(line);
    }

    public List<Line> getAllLines() {
        return lineRepository.findAll();
    }

    public Line getLineById(String id) {
        return findLine(id);
    }

    public Line updateLine(String id, LineRequest request) {
        Line line = findLine(id);

        validateDuplicate(request.getName(), request.getSection(), id);
        line.setName(request.getName());
        line.setSection(request.getSection());

        return lineRepository.save(line);
    }

    public Line updateLineStatus(String id, boolean active) {
        Line line = findLine(id);
        line.setActive(active);

        return lineRepository.save(line);
    }

    @Transactional
    public void deleteLine(String id) {
        Line line = findLine(id);

        if (userLineRepository.existsByLine(line)) {
            throw new BadRequestException(
                    "Line masih digunakan oleh user QC. " + "Nonaktifkan line jika sudah tidak digunakan.");
        }

        lineRepository.delete(line);
    }

    private Line findLine(String id) {
        return lineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Line dengan ID " + id + " tidak ditemukan"));
    }

    private void validateAdminSection(User currentUser) {
        if (currentUser.getSection() == null) {
            throw new BadRequestException(
                    "Admin belum memiliki section");
        }
    }

    private Line findLineForAdmin(String id, User currentUser) {
        validateAdminSection(currentUser);
        Line line = findLine(id);

        if (line.getSection() != currentUser.getSection()) {
            throw new UnauthorizedException("Admin hanya dapat mengakses line pada section sendiri");
        }

        return line;
    }

    @Transactional
    public Line createLineForAdmin(LineRequest request, User currentUser) {
        validateAdminSection(currentUser);
        var section = currentUser.getSection();
        validateDuplicate(request.getName(), section, null);

        Line line = new Line();
        line.setName(request.getName());
        line.setSection(section);
        line.setActive(true);

        return lineRepository.save(line);
    }

    @Transactional(readOnly = true)
    public List<Line> getLinesForAdmin(User currentUser) {
        validateAdminSection(currentUser);
        return lineRepository.findAll().stream()
                .filter(line -> line.getSection() == currentUser.getSection())
                .toList();
    }

    @Transactional(readOnly = true)
    public Line getLineForAdmin(String id, User currentUser) {
        return findLineForAdmin(id, currentUser);
    }

    @Transactional
    public Line updateLineForAdmin(String id, LineRequest request, User currentUser) {
        Line line = findLineForAdmin(id, currentUser);
        var section = currentUser.getSection();
        validateDuplicate(request.getName(), section, id);

        line.setName(request.getName());
        line.setSection(section);

        return lineRepository.save(line);
    }

    @Transactional
    public Line updateLineStatusForAdmin(
            String id, boolean active, User currentUser) {

        Line line = findLineForAdmin(id, currentUser);

        line.setActive(active);

        return lineRepository.save(line);
    }

    private void validateDuplicate(String name, Section section, String currentId) {
        boolean exists;
        if (currentId == null) {
            exists = lineRepository.existsByNameAndSection(
                    name, section);
        } else {
            exists = lineRepository.existsByNameAndSectionAndIdNot(
                    name, section, currentId);
        }

        if (exists) {
            throw new BadRequestException("Nama line sudah digunakan pada section tersebut");
        }

    }
}
