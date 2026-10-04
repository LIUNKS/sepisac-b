package com.sepisac.backend.dto;

import java.util.UUID;

public class InvoiceFilterDTO {

    private UUID companyId;
    private String status;
    private String search;
    private int page = 0;
    private int size = 10;
    private String sort = "createdAt,desc";

    public InvoiceFilterDTO() {
    }

    public InvoiceFilterDTO(UUID companyId, String status, String search, int page, int size, String sort) {
        this.companyId = companyId;
        this.status = status;
        this.search = search;
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
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
