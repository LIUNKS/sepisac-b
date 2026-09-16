package com.sepisac.backend.repository;

import com.sepisac.backend.model.EmployeeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {

    List<EmployeeEntity> findByCompanyId(UUID companyId);

    @Query("SELECT e FROM EmployeeEntity e WHERE e.company.id = :companyId " +
            "AND (:contractType IS NULL OR e.contractType = :contractType) " +
            "AND (:isAvailable IS NULL OR e.isAvailable = :isAvailable) " +
            "AND (:search IS NULL OR (LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.specialty) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<EmployeeEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("contractType") String contractType,
            @Param("isAvailable") Boolean isAvailable,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT e FROM EmployeeEntity e WHERE (:companyId IS NULL OR e.company.id = :companyId) " +
            "AND (:contractType IS NULL OR e.contractType = :contractType) " +
            "AND (:isAvailable IS NULL OR e.isAvailable = :isAvailable) " +
            "AND (:search IS NULL OR (LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.specialty) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<EmployeeEntity> findAllWithFilters(
            @Param("companyId") UUID companyId,
            @Param("contractType") String contractType,
            @Param("isAvailable") Boolean isAvailable,
            @Param("search") String search,
            Pageable pageable);
}
