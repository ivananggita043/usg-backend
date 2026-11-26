package com.garmentqc.system.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DebugController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/debug/db-ping")
    public String dbPing() {
        long start = System.currentTimeMillis();

        jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        long duration = System.currentTimeMillis() - start;

        return "Database response: " + duration + " ms";
    }
}