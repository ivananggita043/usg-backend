package com.garmentqc.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.StyleColor;

public interface StyleColorRepository extends JpaRepository<StyleColor, String> {

    List<StyleColor> findByStyle_Id(String styleId);

    Optional<StyleColor> findByStyle_IdAndColorNameIgnoreCase(String styleId, String colorName);
}