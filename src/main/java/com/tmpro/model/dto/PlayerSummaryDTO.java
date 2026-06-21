package com.tmpro.model.dto;

import com.tmpro.model.Player;

public class PlayerSummaryDTO {

    private String id;
    private String name;
    private String position;
    private String dorsal;

    public PlayerSummaryDTO() {
    }

    public PlayerSummaryDTO(Player player) {
        this.id = player.getId();
        this.name = player.getName();
        this.position = player.getPosition();
        this.dorsal = player.getDorsal();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDorsal() {
        return dorsal;
    }

    public void setDorsal(String dorsal) {
        this.dorsal = dorsal;
    }
}
