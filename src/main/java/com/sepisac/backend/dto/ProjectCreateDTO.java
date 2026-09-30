package com.sepisac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public class ProjectCreateDTO {

    @NotNull(message = "El ID de la empresa es obligatorio")
    private UUID companyId;

    @NotNull(message = "El ID de la cotización es obligatorio")
    private UUID quotationId;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede tener más de 50 caracteres")
    private String code;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede tener más de 150 caracteres")
    private String title;

    private String description;
    
    private String status;

    private LocalDate startDate;
    
    private LocalDate endDate;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 150, message = "El nombre del cliente no puede tener más de 150 caracteres")
    private String clientName;

    // Getters and Setters
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getQuotationId() { return quotationId; }
    public void setQuotationId(UUID quotationId) { this.quotationId = quotationId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
}
