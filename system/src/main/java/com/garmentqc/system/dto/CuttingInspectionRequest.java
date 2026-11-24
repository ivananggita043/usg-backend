
package com.garmentqc.system.dto;

import java.util.List;

import com.garmentqc.system.enums.CuttingProcessCheck;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CuttingInspectionRequest {

    @NotNull
    private CuttingProcessCheck processCheck;

    @NotBlank
    private String lineId;

    private String comment;

    @NotEmpty
    @Valid
    private List<StyleItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StyleItemRequest {

        private String styleColorId;

        private String lot;

        private String operatorName;

        private Integer quantityInspect;

        private Integer quantityDefect;

        private String defectTypeId;
    }
}
