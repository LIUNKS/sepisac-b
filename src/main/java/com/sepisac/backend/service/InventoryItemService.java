package com.sepisac.backend.service;

import com.sepisac.backend.dto.InventoryItemCreateDTO;
import com.sepisac.backend.dto.InventoryItemFilterDTO;
import com.sepisac.backend.dto.InventoryItemResponseDTO;
import com.sepisac.backend.dto.InventoryItemUpdateDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.InventoryItemEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.InventoryItemRepository;
import com.sepisac.backend.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InventoryItemService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "sku", "name", "stockQuantity", "purchaseCost", "salePrice", "minStockAlert", "createdAt", "updatedAt"
    );

    private final InventoryItemRepository inventoryItemRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public InventoryItemService(InventoryItemRepository inventoryItemRepository,
                                CompanyRepository companyRepository,
                                AuditLogService auditLogService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public InventoryItemResponseDTO createItem(InventoryItemCreateDTO dto, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? dto.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : dto.getCompanyId());

        if (targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa del producto.");
        }

        if (inventoryItemRepository.existsByCompanyIdAndSku(targetCompanyId, dto.getSku())) {
            throw new DuplicateResourceException("El SKU '" + dto.getSku() + "' ya está registrado en esta empresa.");
        }

        CompanyEntity company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + targetCompanyId));

        InventoryItemEntity item = new InventoryItemEntity();
        item.setCompany(company);
        item.setSku(dto.getSku());
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setStockQuantity(dto.getStockQuantity());
        item.setPurchaseCost(dto.getPurchaseCost() != null ? dto.getPurchaseCost() : BigDecimal.ZERO);
        item.setSalePrice(dto.getSalePrice() != null ? dto.getSalePrice() : BigDecimal.ZERO);
        item.setMinStockAlert(dto.getMinStockAlert() != null ? dto.getMinStockAlert() : 5);
        item.setIsDeleted(false);

        InventoryItemEntity saved = inventoryItemRepository.save(item);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                targetCompanyId,
                actorId,
                "CREATE",
                "INVENTORY",
                "Ítem de inventario creado: [SKU: " + saved.getSku() + "] " + saved.getName()
        );

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponseDTO> getItemsByCompany(UUID companyId, UserPrincipal currentUser) {
        validateTenantAccess(companyId, currentUser);
        return inventoryItemRepository.findByCompanyId(companyId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<InventoryItemResponseDTO> getItemsPaged(
            UUID companyId,
            int page,
            int size,
            String search,
            Boolean lowStock,
            String sort,
            UserPrincipal currentUser) {

        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? companyId : (currentUser != null ? currentUser.getCompanyId() : companyId);

        if (!isSuperAdmin && targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa.");
        }

        if (targetCompanyId != null) {
            validateTenantAccess(targetCompanyId, currentUser);
        }

        Pageable pageable = createPageable(page, size, sort);
        String searchParam = (search != null && !search.trim().isEmpty()) ? "%" + search.trim().toLowerCase() + "%" : null;

        Page<InventoryItemEntity> pageResult;
        if (targetCompanyId != null) {
            pageResult = inventoryItemRepository.findByCompanyIdWithFilters(targetCompanyId, lowStock, searchParam, pageable);
        } else {
            pageResult = inventoryItemRepository.findAllWithFiltersGlobal(lowStock, searchParam, pageable);
        }

        List<InventoryItemResponseDTO> content = pageResult.getContent().stream()
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
    public PageResponseDTO<InventoryItemResponseDTO> queryItems(InventoryItemFilterDTO filter, UserPrincipal currentUser) {
        return getItemsPaged(
                filter.getCompanyId(),
                filter.getPage(),
                filter.getSize(),
                filter.getSearch(),
                filter.getLowStock(),
                filter.getSort(),
                currentUser
        );
    }

    @Transactional(readOnly = true)
    public InventoryItemResponseDTO getItemById(UUID id, UserPrincipal currentUser) {
        InventoryItemEntity item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de inventario no encontrado con id: " + id));

        validateTenantAccess(item.getCompany().getId(), currentUser);
        return mapToDTO(item);
    }

    @Transactional
    public InventoryItemResponseDTO updateItem(UUID id, InventoryItemUpdateDTO dto, UserPrincipal currentUser) {
        InventoryItemEntity item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de inventario no encontrado con id: " + id));

        validateTenantAccess(item.getCompany().getId(), currentUser);

        if (!dto.getSku().equals(item.getSku())) {
            if (inventoryItemRepository.existsByCompanyIdAndSku(item.getCompany().getId(), dto.getSku())) {
                throw new DuplicateResourceException("El SKU '" + dto.getSku() + "' ya está registrado en esta empresa.");
            }
            item.setSku(dto.getSku());
        }

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        if (dto.getPurchaseCost() != null) {
            item.setPurchaseCost(dto.getPurchaseCost());
        }
        if (dto.getSalePrice() != null) {
            item.setSalePrice(dto.getSalePrice());
        }
        if (dto.getMinStockAlert() != null) {
            item.setMinStockAlert(dto.getMinStockAlert());
        }

        InventoryItemEntity updated = inventoryItemRepository.save(item);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                item.getCompany().getId(),
                actorId,
                "UPDATE",
                "INVENTORY",
                "Ítem de inventario actualizado: [SKU: " + updated.getSku() + "]"
        );

        return mapToDTO(updated);
    }

    @Transactional
    public void deleteItem(UUID id, UserPrincipal currentUser) {
        InventoryItemEntity item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de inventario no encontrado con id: " + id));

        validateTenantAccess(item.getCompany().getId(), currentUser);

        item.setIsDeleted(true);
        inventoryItemRepository.save(item);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                item.getCompany().getId(),
                actorId,
                "DELETE",
                "INVENTORY",
                "Ítem de inventario eliminado lógicamente (id: " + id + ")"
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
            String candidateProperty = parts[0].trim();
            if (ALLOWED_SORT_PROPERTIES.contains(candidateProperty)) {
                property = candidateProperty;
            }
            if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }
        }

        return PageRequest.of(pageNum, pageSize, Sort.by(direction, property));
    }

    private InventoryItemResponseDTO mapToDTO(InventoryItemEntity entity) {
        UUID companyId = entity.getCompany() != null ? entity.getCompany().getId() : null;
        String companyName = entity.getCompany() != null ? entity.getCompany().getBusinessName() : null;
        boolean lowStock = entity.getStockQuantity() != null && entity.getMinStockAlert() != null
                && entity.getStockQuantity() <= entity.getMinStockAlert();

        return new InventoryItemResponseDTO(
                entity.getId(),
                companyId,
                companyName,
                entity.getSku(),
                entity.getName(),
                entity.getDescription(),
                entity.getStockQuantity(),
                entity.getPurchaseCost(),
                entity.getSalePrice(),
                entity.getMinStockAlert(),
                lowStock,
                entity.getCreatedAt()
        );
    }

    @Transactional
    public void seedInventoryData(UUID companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Compañía no encontrada"));
            
        if (inventoryItemRepository.count() > 0) return;

        Object[][] mocks = {
            {"HER-042", "Taladro Percutor Bosch", "Almacén A - Estante 2", 1, 3, 450.00},
            {"HER-089", "Esmeril Angular 7\"", "Almacén A - Estante 4", 0, 2, 320.00},
            {"EPP-005", "Casco de Seguridad EPP", "Almacén B - Casilleros", 2, 10, 45.50},
            {"CON-112", "Cable Eléctrico 12 AWG THW", "Almacén C - Bobinas", 450, 100, 2.50},
            {"HER-015", "Sierra Circular Makita", "Almacén A - Estante 3", 5, 2, 580.00},
            {"EPP-012", "Lentes de Seguridad 3M", "Almacén B - Casilleros", 15, 20, 12.00},
            {"CON-045", "Cinta Aislante 3M", "Almacén C - Estante 1", 50, 20, 3.50},
            {"HER-101", "Amoladora DeWalt", "Almacén A - Estante 1", 3, 2, 410.00},
            {"EPP-033", "Guantes de Cuero", "Almacén B - Cajas", 5, 15, 25.00},
            {"CON-201", "Aceite Lubricante 1L", "Almacén C - Líquidos", 8, 10, 35.00}
        };

        for (Object[] mock : mocks) {
            InventoryItemEntity item = new InventoryItemEntity();
            item.setCompany(company);
            item.setSku((String) mock[0]);
            item.setName((String) mock[1]);
            item.setDescription((String) mock[2]); 
            item.setStockQuantity((Integer) mock[3]);
            item.setMinStockAlert((Integer) mock[4]);
            item.setPurchaseCost(new BigDecimal(mock[5].toString()));
            item.setSalePrice(new BigDecimal(mock[5].toString()).multiply(new BigDecimal("1.3")));
            inventoryItemRepository.save(item);
        }
    }
}
