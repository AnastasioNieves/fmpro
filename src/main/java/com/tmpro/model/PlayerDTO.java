package com.tmpro.model;



public class PlayerDTO {

    private String id;

    private String name;  // Clave primaria

    private String position;
    private String dorsal;




    private String team_id;

    private String photoUrl;

    // Constructor sin argumentos
    public PlayerDTO() {}

    // Constructor que acepta un objeto Player
    public PlayerDTO(Player player) {
        this.id = player.getId();
        this.name = player.getName();
        this.position = player.getPosition();
        this.dorsal = player.getDorsal();
        this.photoUrl = player.getPhotoUrl();
        if (player.getTeamId() != null) {
            this.team_id = player.getTeamId();
        }
    }

    // Getters y Setters


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

    public String getTeam_id() {
        return team_id;
    }

    public void setTeam_id(String team_id) {
        this.team_id = team_id;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
