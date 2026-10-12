package com.garmentqc.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garmentqc.system.model.Line;
import com.garmentqc.system.model.User;
import com.garmentqc.system.model.UserLine;

public interface UserLineRepository extends JpaRepository<UserLine, String> {

    List<UserLine> findByUserId(Long userId);

    List<UserLine> findByUser(User user);

    void deleteByUserId(Long userId);

    void deleteByUser(User user);

    boolean existsByLine(Line line);

}
