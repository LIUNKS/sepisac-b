package com.sepisac.backend.repository;

import com.sepisac.backend.model.PurchaseOrderDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseOrderDetailRepository extends JpaRepository<PurchaseOrderDetailEntity, UUID> {

    List<PurchaseOrderDetailEntity> findByPurchaseOrderId(UUID purchaseOrderId);

    void deleteByPurchaseOrderId(UUID purchaseOrderId);
}
