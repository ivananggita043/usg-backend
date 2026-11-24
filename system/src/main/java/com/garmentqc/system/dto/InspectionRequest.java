
package com.garmentqc.system.dto;

import java.util.ArrayList;
import java.util.List;

import com.garmentqc.system.enums.WashingType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InspectionRequest {

    @NotBlank(message = "Buyer wajib dipilih")
    private String buyerId;

    @NotBlank(message = "Style wajib diisi")
    @Size(max = 100, message = "Style maksimal 100 karakter")
    private String styleCode;

    @NotBlank(message = "Color name wajib diisi")
    @Size(max = 100, message = "Color name maksimal 100 karakter")
    private String colorName;

    @NotBlank
    private String lineId;

    @Min(0)
    private Integer qtyCheckCartonToday;

    @NotNull
    @Min(1)
    private Integer inspectedQuantity;

    @Min(0)
    private Integer transferQuantity = 0;

    private String comment;

    private String inspectionTimeSlot;

    private String handfeel;

    private String grade;

    private String process;

    private WashingType washingType;

    @Valid
    private List<InspectionDefectRequest> defects = new ArrayList<>();
}
