package com.sepisac.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class RevenueKpiResponseDTO {

    private String groupBy;
    private List<RevenueSeriesDTO> series = new ArrayList<>();
    private List<CurrencyTotalDTO> totalsByCurrency = new ArrayList<>();

    public RevenueKpiResponseDTO() {
    }

    public RevenueKpiResponseDTO(String groupBy, List<RevenueSeriesDTO> series, List<CurrencyTotalDTO> totalsByCurrency) {
        this.groupBy = groupBy;
        this.series = series != null ? series : new ArrayList<>();
        this.totalsByCurrency = totalsByCurrency != null ? totalsByCurrency : new ArrayList<>();
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public List<RevenueSeriesDTO> getSeries() {
        return series;
    }

    public void setSeries(List<RevenueSeriesDTO> series) {
        this.series = series;
    }

    public List<CurrencyTotalDTO> getTotalsByCurrency() {
        return totalsByCurrency;
    }

    public void setTotalsByCurrency(List<CurrencyTotalDTO> totalsByCurrency) {
        this.totalsByCurrency = totalsByCurrency;
    }
}
