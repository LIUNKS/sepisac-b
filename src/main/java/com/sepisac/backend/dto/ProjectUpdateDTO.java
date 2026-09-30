package com.sepisac.backend.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ProjectUpdateDTO {

    @Size(max = 150, message = "El título no puede tener más de 150 caracteres")
    private String title;

    private String description;
    
    private String status;

    private LocalDate startDate;
    
    private LocalDate endDate;

    @Size(max = 150, message = "El nombre del cliente no puede tener más de 150 caracteres")
    private String clientName;

    // Getters and Setters
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
