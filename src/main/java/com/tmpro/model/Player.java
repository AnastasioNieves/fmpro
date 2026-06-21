package com.tmpro.model;

public class Player {

    private String id;
    private String name;
    private String position;
    private String dorsal;
    private String photoUrl;

    // En lugar de anidar Team (OneToMany), almacenamos el ID del equipo.
    // El frontend o DTO puede requerir el objeto Team, pero a nivel modelo es mejor el ID.
    // Para simplificar la migración, dejaremos que Team se pueda inyectar temporalmente si se usa DTO, 
    // pero el modelo persistido usará teamId.
    private String teamId;

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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }
}
