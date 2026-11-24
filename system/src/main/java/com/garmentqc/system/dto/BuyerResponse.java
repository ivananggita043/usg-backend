package com.garmentqc.system.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BuyerResponse {

    private String id;

    private String name;

    private Instant createdAt;

    private Instant updatedAt;
}