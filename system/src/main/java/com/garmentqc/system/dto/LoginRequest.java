package com.garmentqc.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class LoginRequest {

    @NotBlank (message = "Username wajib diisi")
    private String username;

    @NotBlank (message = "Password wajib diisi")
    private String password;

}
