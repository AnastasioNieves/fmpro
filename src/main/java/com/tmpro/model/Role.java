package com.tmpro.model;

public class Role {

    private String id;
    private String name; // Puede ser "Admin", "User" o "Trainer"

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
}
