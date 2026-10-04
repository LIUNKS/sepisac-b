package com.sepisac.backend.repository;

import com.sepisac.backend.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<UserEntity> findByCompanyId(UUID companyId);

    boolean existsByRoleId(Integer roleId);

    boolean existsByCompanyIdAndUsername(UUID companyId, String username);

    @Query("SELECT u FROM UserEntity u WHERE u.company.id = :companyId " +
           "AND (:isActive IS NULL OR u.isActive = :isActive) " +
           "AND (:roleId IS NULL OR u.role.id = :roleId) " +
           "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UserEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("isActive") Boolean isActive,
            @Param("roleId") Integer roleId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT u FROM UserEntity u WHERE " +
           "(:companyId IS NULL OR u.company.id = :companyId) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive) " +
           "AND (:roleId IS NULL OR u.role.id = :roleId) " +
           "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UserEntity> findAllWithFilters(
            @Param("companyId") UUID companyId,
            @Param("isActive") Boolean isActive,
            @Param("roleId") Integer roleId,
            @Param("search") String search,
            Pageable pageable
    );
}
