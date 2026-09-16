package com.sepisac.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class SupplierCreateDTO {

    private UUID companyId;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150, message = "La razón social no puede exceder 150 caracteres")
    private String businessName;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe contener exactamente 11 dígitos numéricos")
    private String ruc;

    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
    private String contactPhone;

    @Email(message = "Formato de correo electrónico inválido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String email;

    public SupplierCreateDTO() {
    }

    public SupplierCreateDTO(UUID companyId, String businessName, String ruc, String contactPhone, String email) {
        this.companyId = companyId;
        this.businessName = businessName;
        this.ruc = ruc;
        this.contactPhone = contactPhone;
        this.email = email;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
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

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
