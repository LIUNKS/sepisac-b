package com.sepisac.backend.dto;

import java.util.UUID;

public class UserFilterDTO {

    private UUID companyId;
    private String search;
    private Boolean isActive;
    private Integer roleId;
    private int page = 0;
    private int size = 10;
    private String sort = "createdAt,desc";

    public UserFilterDTO() {
    }

    public UserFilterDTO(UUID companyId, String search, Boolean isActive, Integer roleId, int page, int size, String sort) {
        this.companyId = companyId;
        this.search = search;
        this.isActive = isActive;
        this.roleId = roleId;
        this.page = page;
        this.size = size > 0 ? size : 10;
        this.sort = (sort != null && !sort.trim().isEmpty()) ? sort : "createdAt,desc";
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size > 0 ? size : 10;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = (sort != null && !sort.trim().isEmpty()) ? sort : "createdAt,desc";
    }
}
