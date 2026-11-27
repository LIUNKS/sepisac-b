package com.sepisac.backend.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectResourceRequirementsResponseDTO {

    private UUID projectId;
    private UUID quotationId;
    private String quotationNumber;
    private int budgetedEmployees;
    private int assignedEmployees;
    private int budgetedMachinery;
    private int assignedMachinery;
    private boolean readyToStart;
    private List<LaborRequirementSummary> laborRequirements = new ArrayList<>();
    private List<EquipmentRequirementSummary> equipmentRequirements = new ArrayList<>();

    public ProjectResourceRequirementsResponseDTO() {
    }

    public ProjectResourceRequirementsResponseDTO(
            UUID projectId,
            UUID quotationId,
            String quotationNumber,
            int budgetedEmployees,
            int assignedEmployees,
            int budgetedMachinery,
            int assignedMachinery,
            boolean readyToStart,
            List<LaborRequirementSummary> laborRequirements,
            List<EquipmentRequirementSummary> equipmentRequirements) {
        this.projectId = projectId;
        this.quotationId = quotationId;
        this.quotationNumber = quotationNumber;
        this.budgetedEmployees = budgetedEmployees;
        this.assignedEmployees = assignedEmployees;
        this.budgetedMachinery = budgetedMachinery;
        this.assignedMachinery = assignedMachinery;
        this.readyToStart = readyToStart;
        this.laborRequirements = laborRequirements != null ? laborRequirements : new ArrayList<>();
        this.equipmentRequirements = equipmentRequirements != null ? equipmentRequirements : new ArrayList<>();
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getQuotationId() {
        return quotationId;
    }

    public void setQuotationId(UUID quotationId) {
        this.quotationId = quotationId;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
    }

    public int getBudgetedEmployees() {
        return budgetedEmployees;
    }

    public void setBudgetedEmployees(int budgetedEmployees) {
        this.budgetedEmployees = budgetedEmployees;
    }

    public int getAssignedEmployees() {
        return assignedEmployees;
    }

    public void setAssignedEmployees(int assignedEmployees) {
        this.assignedEmployees = assignedEmployees;
    }

    public int getBudgetedMachinery() {
        return budgetedMachinery;
    }

    public void setBudgetedMachinery(int budgetedMachinery) {
        this.budgetedMachinery = budgetedMachinery;
    }

    public int getAssignedMachinery() {
        return assignedMachinery;
    }

    public void setAssignedMachinery(int assignedMachinery) {
        this.assignedMachinery = assignedMachinery;
    }

    public boolean isReadyToStart() {
        return readyToStart;
    }

    public void setReadyToStart(boolean readyToStart) {
        this.readyToStart = readyToStart;
    }

    public List<LaborRequirementSummary> getLaborRequirements() {
        return laborRequirements;
    }

    public void setLaborRequirements(List<LaborRequirementSummary> laborRequirements) {
        this.laborRequirements = laborRequirements != null ? laborRequirements : new ArrayList<>();
    }

    public List<EquipmentRequirementSummary> getEquipmentRequirements() {
        return equipmentRequirements;
    }

    public void setEquipmentRequirements(List<EquipmentRequirementSummary> equipmentRequirements) {
        this.equipmentRequirements = equipmentRequirements != null ? equipmentRequirements : new ArrayList<>();
    }

    public static class LaborRequirementSummary {
        private String specialtyNeeded;
        private Integer quantityRequired;
        private Integer estimatedHours;

        public LaborRequirementSummary() {
        }

        public LaborRequirementSummary(String specialtyNeeded, Integer quantityRequired, Integer estimatedHours) {
            this.specialtyNeeded = specialtyNeeded;
            this.quantityRequired = quantityRequired;
            this.estimatedHours = estimatedHours;
        }

        public String getSpecialtyNeeded() {
            return specialtyNeeded;
        }

        public void setSpecialtyNeeded(String specialtyNeeded) {
            this.specialtyNeeded = specialtyNeeded;
        }

        public Integer getQuantityRequired() {
            return quantityRequired;
        }

        public void setQuantityRequired(Integer quantityRequired) {
            this.quantityRequired = quantityRequired;
        }

        public Integer getEstimatedHours() {
            return estimatedHours;
        }

        public void setEstimatedHours(Integer estimatedHours) {
            this.estimatedHours = estimatedHours;
        }
    }

    public static class EquipmentRequirementSummary {
        private String itemDescription;
        private Integer quantity;

        public EquipmentRequirementSummary() {
        }

        public EquipmentRequirementSummary(String itemDescription, Integer quantity) {
            this.itemDescription = itemDescription;
            this.quantity = quantity;
        }

        public String getItemDescription() {
            return itemDescription;
        }

        public void setItemDescription(String itemDescription) {
            this.itemDescription = itemDescription;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
