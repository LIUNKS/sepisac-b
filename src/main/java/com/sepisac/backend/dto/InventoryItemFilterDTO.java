package com.sepisac.backend.dto;

import java.util.UUID;

public class InventoryItemFilterDTO {

    private UUID companyId;
    private String search;
    private Boolean lowStock;
    private int page = 0;
    private int size = 10;
    private String sort = "createdAt,desc";

    public InventoryItemFilterDTO() {
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

    public Boolean getLowStock() {
        return lowStock;
    }

    public void setLowStock(Boolean lowStock) {
        this.lowStock = lowStock;
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
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
