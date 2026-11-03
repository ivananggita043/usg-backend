package com.garmentqc.system.dto;

import java.util.List;

public record DailyProductivityResponse(

        ProductivitySummaryResponse summary,

        List<HourlyProductivityResponse> hourlyProductivities) {

}
