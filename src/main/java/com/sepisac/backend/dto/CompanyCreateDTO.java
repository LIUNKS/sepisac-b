package com.sepisac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CompanyCreateDTO {

    @NotBlank(message = "La razón social es obligatoria")
    private String businessName;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe contener exactamente 11 dígitos numéricos")
    private String ruc;

    public CompanyCreateDTO() {
    }

    public CompanyCreateDTO(String businessName, String ruc) {
        this.businessName = businessName;
        this.ruc = ruc;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }
}
