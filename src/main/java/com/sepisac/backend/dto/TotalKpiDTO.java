package com.sepisac.backend.dto;

import java.math.BigDecimal;

public class TotalKpiDTO {

    private BigDecimal averageDays;
    private int sampleSize;

    public TotalKpiDTO() {
    }

    public TotalKpiDTO(BigDecimal averageDays, int sampleSize) {
        this.averageDays = averageDays;
        this.sampleSize = sampleSize;
    }

    public BigDecimal getAverageDays() {
        return averageDays;
    }

    public void setAverageDays(BigDecimal averageDays) {
        this.averageDays = averageDays;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }
}
