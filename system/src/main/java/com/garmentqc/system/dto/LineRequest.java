package com.garmentqc.system.dto;

import com.garmentqc.system.enums.Section;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class LineRequest {

    @NotBlank (message = "Nama Line wajib diisi")
    private String name;

    @NotNull (message = "Section wajib dipilih")
    private Section section;
    
}
