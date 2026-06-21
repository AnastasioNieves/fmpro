package com.tmpro.model.dto;

import java.util.ArrayList;
import java.util.List;

public class PlayerIdsRequest {

    private List<String> playerIds = new ArrayList<>();

    public List<String> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<String> playerIds) {
        this.playerIds = playerIds;
    }
}
