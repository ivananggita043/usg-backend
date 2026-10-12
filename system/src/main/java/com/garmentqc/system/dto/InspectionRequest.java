package com.garmentqc.system.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InspectionRequest {

    @NotBlank
    private String buyerName;

    @NotBlank
    private String styleCode;

    @NotBlank
    private String colorName;

    @NotBlank
    private String lineId;

    @NotNull
    @Min(1)
    private Integer inspectedQuantity;

    @NotNull
    @Min(0)
    private Integer transferQuantity = 0;

    private String comment;

    @Valid
    private List<InspectionDefectRequest> defects = new ArrayList<>();

}
