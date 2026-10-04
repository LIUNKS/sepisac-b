package com.sepisac.backend.service;

import com.sepisac.backend.dto.InventoryMovementFilterDTO;
import com.sepisac.backend.dto.InventoryMovementResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.InventoryItemEntity;
import com.sepisac.backend.model.InventoryMovementEntity;
import com.sepisac.backend.repository.InventoryItemRepository;
import com.sepisac.backend.repository.InventoryMovementRepository;
import com.sepisac.backend.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryMovementService {

    private final InventoryMovementRepository movementRepository;
    private final InventoryItemRepository itemRepository;

    public InventoryMovementService(InventoryMovementRepository movementRepository,
                                  InventoryItemRepository itemRepository) {
        this.movementRepository = movementRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<InventoryMovementResponseDTO> getMovementsPaged(
            UUID companyId,
            UUID inventoryItemId,
            String movementType,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            int page,
            int size,
            String sort,
            UserPrincipal currentUser) {

        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId;

        if (isSuperAdmin) {
            targetCompanyId = companyId;
        } else {
            if (companyId != null) {
                validateTenantAccess(companyId, currentUser);
            }
            targetCompanyId = currentUser != null ? currentUser.getCompanyId() : null;
            if (targetCompanyId == null) {
                throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa.");
            }
        }

        Pageable pageable = createPageable(page, size, sort);

        Page<InventoryMovementEntity> pageResult;
        if (targetCompanyId != null) {
            pageResult = movementRepository.findByCompanyIdWithFilters(
                    targetCompanyId, inventoryItemId, movementType, startDate, endDate, pageable
            );
        } else {
            pageResult = movementRepository.findAllWithFiltersGlobal(
                    companyId, inventoryItemId, movementType, startDate, endDate, pageable
            );
        }

        List<InventoryMovementResponseDTO> content = pageResult.getContent().stream()
                .map(this::mapToDTO)
                .toList();

        return new PageResponseDTO<>(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<InventoryMovementResponseDTO> queryMovements(
            InventoryMovementFilterDTO filter,
            UserPrincipal currentUser) {
        return getMovementsPaged(
                filter.getCompanyId(),
                filter.getInventoryItemId(),
                filter.getMovementType(),
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getPage(),
                filter.getSize(),
                filter.getSort(),
                currentUser
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<InventoryMovementResponseDTO> getMovementsByItemIdPaged(
            UUID itemId,
            int page,
            int size,
            String sort,
            UserPrincipal currentUser) {

        InventoryItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de inventario no encontrado con ID: " + itemId));

        if (item.getCompany() != null) {
            validateTenantAccess(item.getCompany().getId(), currentUser);
        }

        Pageable pageable = createPageable(page, size, sort);
        Page<InventoryMovementEntity> pageResult = movementRepository.findByInventoryItemId(itemId, pageable);

        List<InventoryMovementResponseDTO> content = pageResult.getContent().stream()
                .map(this::mapToDTO)
                .toList();

        return new PageResponseDTO<>(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }

    private void validateTenantAccess(UUID companyId, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        if (!isSuperAdmin) {
            if (currentUser == null || currentUser.getCompanyId() == null || !currentUser.getCompanyId().equals(companyId)) {
                throw new AccessDeniedException("Acceso denegado: No tiene permisos para acceder a recursos de otra empresa.");
            }
        }
    }

    private Pageable createPageable(int page, int size, String sortStr) {
        int pageNum = Math.max(0, page);
        int pageSize = size > 0 ? size : 10;

        Sort.Direction direction = Sort.Direction.DESC;
        String property = "createdAt";

        if (sortStr != null && !sortStr.trim().isEmpty()) {
            String[] parts = sortStr.split(",");
            property = parts[0].trim();
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.ASC;
            }
        }

        return PageRequest.of(pageNum, pageSize, Sort.by(direction, property));
    }

    private InventoryMovementResponseDTO mapToDTO(InventoryMovementEntity entity) {
        return new InventoryMovementResponseDTO(
                entity.getId(),
                entity.getCompany() != null ? entity.getCompany().getId() : null,
                entity.getInventoryItem() != null ? entity.getInventoryItem().getId() : null,
                entity.getInventoryItem() != null ? entity.getInventoryItem().getSku() : null,
                entity.getInventoryItem() != null ? entity.getInventoryItem().getName() : null,
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getUser() != null ? entity.getUser().getFullName() : null,
                entity.getMovementType(),
                entity.getQuantityChanged(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
