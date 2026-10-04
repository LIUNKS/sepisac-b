package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Datos para actualización de empresa")
public class CompanyUpdateDTO {

    @NotBlank(message = "La razón social es obligatoria")
    @Schema(description = "Razón social de la empresa", example = "SEPI S.A.C.")
    private String businessName;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe contener exactamente 11 dígitos numéricos")
    @Schema(description = "RUC de la empresa (11 dígitos)", example = "20123456789")
    private String ruc;

    @Schema(description = "Estado de suscripción", example = "ACTIVE")
    private String subscriptionStatus;

    public CompanyUpdateDTO() {
    }

    public CompanyUpdateDTO(String businessName, String ruc) {
        this.businessName = businessName;
        this.ruc = ruc;
    }

    public CompanyUpdateDTO(String businessName, String ruc, String subscriptionStatus) {
        this.businessName = businessName;
        this.ruc = ruc;
        this.subscriptionStatus = subscriptionStatus;
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

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }
}
