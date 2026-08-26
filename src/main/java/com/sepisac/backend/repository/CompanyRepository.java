package com.sepisac.backend.repository;

import com.sepisac.backend.model.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID> {
    Optional<CompanyEntity> findByRuc(String ruc);
}
