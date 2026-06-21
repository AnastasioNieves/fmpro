package com.tmpro.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.ArrayList;

public class Team {

    private String id;
    private String name;
    private String coach;
    private String ownerUserId;

    // En Firestore, podríamos optar por no anidar jugadores si la colección "players" tiene un campo "teamId"
    // Sin embargo, para mantener el contrato con el frontend, podemos dejar la lista transitoria
    // o rellenarla en el servicio cuando sea necesario.
    @JsonIgnore
    private List<Player> players = new ArrayList<>();

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

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }
}
