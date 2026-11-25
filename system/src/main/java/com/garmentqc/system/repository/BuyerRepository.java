
package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.Buyer;

public interface BuyerRepository extends JpaRepository<Buyer, String> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    List<Buyer> findAllByOrderByNameAsc();

    Page<Buyer> findAllByOrderByNameAsc(Pageable pageable);

    List<Buyer> findTop20ByNameContainingIgnoreCaseOrderByNameAsc(String keyword);
}
