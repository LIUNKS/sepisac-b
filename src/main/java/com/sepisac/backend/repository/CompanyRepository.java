package com.sepisac.backend.repository;

import com.sepisac.backend.model.CompanyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID> {

    Optional<CompanyEntity> findByRuc(String ruc);

    boolean existsByRuc(String ruc);

    @Query("SELECT c FROM CompanyEntity c WHERE " +
           "(:subscriptionStatus IS NULL OR c.subscriptionStatus = :subscriptionStatus) " +
           "AND (:search IS NULL OR LOWER(c.businessName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(c.ruc) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CompanyEntity> findAllWithFilters(
            @Param("subscriptionStatus") String subscriptionStatus,
            @Param("search") String search,
            Pageable pageable
    );
}
