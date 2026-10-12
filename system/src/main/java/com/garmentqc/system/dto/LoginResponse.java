package com.garmentqc.system.dto;

import java.time.Instant;
import java.util.List;

import com.garmentqc.system.enums.Role;
import com.garmentqc.system.enums.Section;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String token;

    private String id;

    private String nama;

    private String username;

    private Role role;

    private Section section;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    private List<String> lineIds;
}