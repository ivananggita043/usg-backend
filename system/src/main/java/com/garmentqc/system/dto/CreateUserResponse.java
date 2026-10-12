package com.garmentqc.system.dto;

import java.time.Instant;
import java.util.List;

import com.garmentqc.system.enums.Role;
import com.garmentqc.system.enums.Section;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@Builder 
public class CreateUserResponse {

    private String id;

    private String nama;

    private String username;

    private String nik;
    
    private Role role;

    private Section section;

    private List<String> lineIds;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;
    
}
