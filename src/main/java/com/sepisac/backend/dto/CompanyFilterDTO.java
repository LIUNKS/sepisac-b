package com.sepisac.backend.dto;

public class CompanyFilterDTO {

    private String search;
    private String subscriptionStatus;
    private int page = 0;
    private int size = 10;
    private String sort = "createdAt,desc";

    public CompanyFilterDTO() {
    }

    public CompanyFilterDTO(String search, String subscriptionStatus, int page, int size, String sort) {
        this.search = search;
        this.subscriptionStatus = subscriptionStatus;
        this.page = page;
        this.size = size > 0 ? size : 10;
        this.sort = (sort != null && !sort.trim().isEmpty()) ? sort : "createdAt,desc";
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
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
