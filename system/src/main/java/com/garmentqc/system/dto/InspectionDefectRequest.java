package com.garmentqc.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InspectionDefectRequest {

    @NotBlank
    private String defectTypeId;

    @NotNull
    @Min(1)
    private Integer quantity = 1;

}
