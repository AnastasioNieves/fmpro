package com.tmpro.model.dto;

import java.util.ArrayList;
import java.util.List;

public class PlayerIdsRequest {

    private List<Long> playerIds = new ArrayList<>();

    public List<Long> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<Long> playerIds) {
        this.playerIds = playerIds;
    }
}
