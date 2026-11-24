package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.enums.AccessoryCategory;
import com.garmentqc.system.model.Accessory;

public interface AccessoryRepository extends JpaRepository<Accessory, String> {

    List<Accessory> findByActiveTrueOrderByCategoryAscSortOrderAscNameAsc();

    List<Accessory> findByCategoryAndActiveTrueOrderBySortOrderAscNameAsc(AccessoryCategory category);

    boolean existsByCategoryAndNameIgnoreCase(AccessoryCategory category, String name);

    List<Accessory> findByActiveTrueAndNameContainingIgnoreCaseOrderByCategoryAscSortOrderAscNameAsc(String name);
}