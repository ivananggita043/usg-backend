package com.garmentqc.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LineResponse {
    private String id;
    
    private String name;
}