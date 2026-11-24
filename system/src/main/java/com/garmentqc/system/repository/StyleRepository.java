
package com.garmentqc.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.Style;

public interface StyleRepository extends JpaRepository<Style, String> {

    List<Style> findByBuyer_Id(String buyerId);

    Optional<Style> findByBuyer_IdAndStyleCodeIgnoreCase(String buyerId, String styleCode);

    boolean existsByBuyer_Id(String buyerId);

    boolean existsByBuyer_NameIgnoreCaseAndStyleCodeIgnoreCase(
            String buyerName,
            String styleCode);

    boolean existsByStyleCodeIgnoreCase(String styleCode);
}
