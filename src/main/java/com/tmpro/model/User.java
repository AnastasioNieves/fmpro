package com.tmpro.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

public class User {

    private String id;
    private String username;
    
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;

    private String roleId;

    /** Equipo seguido (rol USER, solo lectura). */
    private String teamId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JsonProperty("roleId")
    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getRole() {
        return roleId;
    }

    public void setRole(String roleId) {
        this.roleId = roleId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }
}
