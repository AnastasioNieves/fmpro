package com.tmpro.model;

public class TeamDTO {

    private String id;
    private String name;
    private String coach;

    public TeamDTO() {
    }

    public TeamDTO(String id, String name, String coach) {
        this.id = id;
        this.name = name;
        this.coach = coach;
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

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }
}
