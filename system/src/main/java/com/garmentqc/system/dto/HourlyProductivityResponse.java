package com.garmentqc.system.dto;

public record HourlyProductivityResponse(

                String timeSlot,

                int inspectedQuantity,

                double target,

                double achievementRate) {
}