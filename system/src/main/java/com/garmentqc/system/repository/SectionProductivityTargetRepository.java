package com.garmentqc.system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.enums.Section;
import com.garmentqc.system.model.SectionProductivityTarget;

public interface SectionProductivityTargetRepository extends JpaRepository<SectionProductivityTarget, String> {

    Optional<SectionProductivityTarget> findBySectionAndActiveTrue(Section section);

}
