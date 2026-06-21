package com.tmpro.model;

public class AuthResponse {

    private String id;
    private String username;
    private String roleId;
    private String roleName;
    private String teamId;

    public AuthResponse() {
    }

    public AuthResponse(String id, String username, String roleId, String roleName) {
        this(id, username, roleId, roleName, null);
    }

    public AuthResponse(String id, String username, String roleId, String roleName, String teamId) {
        this.id = id;
        this.username = username;
        this.roleId = roleId;
        this.roleName = roleName;
        this.teamId = teamId;
    }

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

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }
}
