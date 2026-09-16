package com.sepisac.backend.repository;

import com.sepisac.backend.model.MachineryEquipmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MachineryEquipmentRepository extends JpaRepository<MachineryEquipmentEntity, UUID> {

    boolean existsByCompanyIdAndCode(UUID companyId, String code);

    List<MachineryEquipmentEntity> findByCompanyId(UUID companyId);

    @Query("SELECT m FROM MachineryEquipmentEntity m WHERE m.company.id = :companyId " +
            "AND (:status IS NULL OR m.status = :status) " +
            "AND (:search IS NULL OR (LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<MachineryEquipmentEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT m FROM MachineryEquipmentEntity m WHERE (:companyId IS NULL OR m.company.id = :companyId) " +
            "AND (:status IS NULL OR m.status = :status) " +
            "AND (:search IS NULL OR (LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<MachineryEquipmentEntity> findAllWithFilters(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
