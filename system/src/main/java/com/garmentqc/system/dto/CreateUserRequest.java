package com.garmentqc.system.dto;

import java.util.List;

import com.garmentqc.system.enums.Role;
import com.garmentqc.system.enums.Section;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    
    @NotBlank(message = "Nama wajib diisi")
    private String nama;

    @NotBlank(message = "Username wajib diisi")
    private String username;

    @NotBlank @Size(max = 30, message = "NIK maksimal 30 karakter")
    private String nik;

    @NotNull(message = "Role wajib dipilih")
    private Role role;
    
    private Section section;

    @NotBlank(message = "Password wajib diisi")
    private String password;

    private List<String> lineIds;

}
