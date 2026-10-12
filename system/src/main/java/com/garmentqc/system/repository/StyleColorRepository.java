package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.StyleColor;

public interface StyleColorRepository extends JpaRepository<StyleColor, String> {

    List<StyleColor> findByStyle_Id(String styleId);

}
