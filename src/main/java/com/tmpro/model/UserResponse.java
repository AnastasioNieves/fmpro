package com.tmpro.model;

public class UserResponse {
    private String id;
    private String username;
    private String roleId;
    private String roleName;

    public UserResponse() {
    }

    public UserResponse(String id, String username, String roleId, String roleName) {
        this.id = id;
        this.username = username;
        this.roleId = roleId;
        this.roleName = roleName;
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
}
