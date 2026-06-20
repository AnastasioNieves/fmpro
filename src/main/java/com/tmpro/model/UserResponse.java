package com.tmpro.model;

public class UserResponse {
    private Long id;
    private String username;
    private Long roleId;
    private String roleName;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, Long roleId, String roleName) {
        this.id = id;
        this.username = username;
        this.roleId = roleId;
        this.roleName = roleName;
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
}
