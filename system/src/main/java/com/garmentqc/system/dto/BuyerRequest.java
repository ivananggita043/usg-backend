package com.garmentqc.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuyerRequest {

    @NotBlank(message = "Buyer name wajib diisi")
    @Size(max = 100, message = "Buyer name maksimal 100 karakter")
    private String name;
}