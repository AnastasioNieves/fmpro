package com.tmpro.model;

public class AuthResponse {

    private Long id;
    private String username;
    private Long roleId;
    private String roleName;
    private Long teamId;

    public AuthResponse() {
    }

    public AuthResponse(Long id, String username, Long roleId, String roleName) {
        this(id, username, roleId, roleName, null);
    }

    public AuthResponse(Long id, String username, Long roleId, String roleName, Long teamId) {
        this.id = id;
        this.username = username;
        this.roleId = roleId;
        this.roleName = roleName;
        this.teamId = teamId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }
}
