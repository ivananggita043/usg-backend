package com.garmentqc.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.enums.Section;
import com.garmentqc.system.model.User;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);

    List<User> findBySection(Section section);

}
