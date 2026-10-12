
package com.garmentqc.system.repository;

import com.garmentqc.system.model.Style;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StyleRepository extends JpaRepository<Style, String> {

    List<Style> findByBuyer_Id(String buyerId);

}