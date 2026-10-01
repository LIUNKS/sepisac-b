package com.sepisac.backend.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PurchaseReceptionResponseDTO {

    private UUID purchaseOrderId;
    private String orderNumber;
    private String status;
    private OffsetDateTime receptionDate;
    private List<ReceptionMovementDTO> movements = new ArrayList<>();

    public PurchaseReceptionResponseDTO() {
    }

    public PurchaseReceptionResponseDTO(UUID purchaseOrderId, String orderNumber, String status,
                                        OffsetDateTime receptionDate, List<ReceptionMovementDTO> movements) {
        this.purchaseOrderId = purchaseOrderId;
        this.orderNumber = orderNumber;
        this.status = status;
        this.receptionDate = receptionDate;
        this.movements = movements != null ? movements : new ArrayList<>();
    }

    public UUID getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(UUID purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getReceptionDate() {
        return receptionDate;
    }

    public void setReceptionDate(OffsetDateTime receptionDate) {
        this.receptionDate = receptionDate;
    }

    public List<ReceptionMovementDTO> getMovements() {
        return movements;
    }

    public void setMovements(List<ReceptionMovementDTO> movements) {
        this.movements = movements != null ? movements : new ArrayList<>();
    }
}
