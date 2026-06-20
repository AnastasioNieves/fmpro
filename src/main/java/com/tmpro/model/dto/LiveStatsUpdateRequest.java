package com.tmpro.model.dto;

import java.util.ArrayList;
import java.util.List;

public class LiveStatsUpdateRequest {

    private List<LiveStatDTO> stats = new ArrayList<>();

    public List<LiveStatDTO> getStats() {
        return stats;
    }

    public void setStats(List<LiveStatDTO> stats) {
        this.stats = stats;
    }
}
