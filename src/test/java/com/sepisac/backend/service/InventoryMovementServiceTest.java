package com.sepisac.backend.service;

import com.sepisac.backend.dto.InventoryMovementFilterDTO;
import com.sepisac.backend.dto.InventoryMovementResponseDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.InventoryItemEntity;
import com.sepisac.backend.model.InventoryMovementEntity;
import com.sepisac.backend.model.UserEntity;
import com.sepisac.backend.repository.InventoryItemRepository;
import com.sepisac.backend.repository.InventoryMovementRepository;
import com.sepisac.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryMovementService Unit Tests")
class InventoryMovementServiceTest {

    @Mock
    private InventoryMovementRepository movementRepository;

    @Mock
    private InventoryItemRepository itemRepository;

    @InjectMocks
    private InventoryMovementService movementService;

    private UUID companyId;
    private UUID itemId;
    private UserPrincipal adminPrincipal;
    private UserPrincipal otherCompanyPrincipal;
    private UserPrincipal superAdminPrincipal;
    private InventoryItemEntity mockItem;
    private InventoryMovementEntity mockMovement;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        adminPrincipal = new UserPrincipal(
                UUID.randomUUID(), "admin@sepisac.com", "admin", "hash", "Admin User", companyId,
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
        );
        otherCompanyPrincipal = new UserPrincipal(
                UUID.randomUUID(), "other@sepisac.com", "other", "hash", "Other User", UUID.randomUUID(),
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
        );
        superAdminPrincipal = new UserPrincipal(
                UUID.randomUUID(), "super@sepisac.com", "super", "hash", "Super User", null,
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPERADMIN")), true
        );

        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);

        mockItem = new InventoryItemEntity();
        mockItem.setId(itemId);
        mockItem.setSku("SKU-MAT-01");
        mockItem.setName("Tubo PVC 1/2 pulgada");
        mockItem.setCompany(company);

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setFullName("Juan Perez Almacenero");

        mockMovement = new InventoryMovementEntity();
        mockMovement.setId(UUID.randomUUID());
        mockMovement.setCompany(company);
        mockMovement.setInventoryItem(mockItem);
        mockMovement.setUser(user);
        mockMovement.setMovementType("ENTRADA");
        mockMovement.setQuantityChanged(50);
        mockMovement.setReason("Recepción de OC OC-2026-001");
        mockMovement.prePersist();
    }

    @Nested
    @DisplayName("getMovementsPaged")
    class GetMovementsPagedTests {

        @Test
        @DisplayName("Should return paged movements successfully for company")
        void shouldReturnPagedMovementsSuccessfully() {
            Page<InventoryMovementEntity> pageMock = new PageImpl<>(List.of(mockMovement));
            when(movementRepository.findByCompanyIdWithFilters(eq(companyId), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(pageMock);

            PageResponseDTO<InventoryMovementResponseDTO> result = movementService.getMovementsPaged(
                    companyId, null, null, null, null, 0, 10, "createdAt,desc", adminPrincipal
            );

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            InventoryMovementResponseDTO dto = result.getContent().get(0);
            assertThat(dto.getMovementType()).isEqualTo("ENTRADA");
            assertThat(dto.getQuantityChanged()).isEqualTo(50);
            assertThat(dto.getInventoryItemSku()).isEqualTo("SKU-MAT-01");
            assertThat(dto.getInventoryItemName()).isEqualTo("Tubo PVC 1/2 pulgada");
            assertThat(dto.getUserFullName()).isEqualTo("Juan Perez Almacenero");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when non-superadmin accesses other company")
        void shouldThrowAccessDeniedWhenAccessingOtherCompany() {
            assertThatThrownBy(() -> movementService.getMovementsPaged(
                    companyId, null, null, null, null, 0, 10, "createdAt,desc", otherCompanyPrincipal
            )).isInstanceOf(AccessDeniedException.class)
              .hasMessageContaining("No tiene permisos para acceder a recursos de otra empresa");
        }

        @Test
        @DisplayName("SUPERADMIN should query globally when companyId is null")
        void superAdminShouldQueryGlobally() {
            Page<InventoryMovementEntity> pageMock = new PageImpl<>(List.of(mockMovement));
            when(movementRepository.findAllWithFiltersGlobal(any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(pageMock);

            PageResponseDTO<InventoryMovementResponseDTO> result = movementService.getMovementsPaged(
                    null, null, null, null, null, 0, 10, "createdAt,desc", superAdminPrincipal
            );

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getMovementsByItemIdPaged")
    class GetMovementsByItemIdPagedTests {

        @Test
        @DisplayName("Should return movements for specific item successfully")
        void shouldReturnMovementsForItemSuccessfully() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(mockItem));
            Page<InventoryMovementEntity> pageMock = new PageImpl<>(List.of(mockMovement));
            when(movementRepository.findByInventoryItemId(eq(itemId), any(Pageable.class)))
                    .thenReturn(pageMock);

            PageResponseDTO<InventoryMovementResponseDTO> result = movementService.getMovementsByItemIdPaged(
                    itemId, 0, 10, "createdAt,desc", adminPrincipal
            );

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getInventoryItemId()).isEqualTo(itemId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when item does not exist")
        void shouldThrowNotFoundWhenItemDoesNotExist() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> movementService.getMovementsByItemIdPaged(
                    itemId, 0, 10, "createdAt,desc", adminPrincipal
            )).isInstanceOf(ResourceNotFoundException.class)
              .hasMessageContaining("Ítem de inventario no encontrado");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when item belongs to another company")
        void shouldThrowAccessDeniedWhenItemBelongsToOtherCompany() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(mockItem));

            assertThatThrownBy(() -> movementService.getMovementsByItemIdPaged(
                    itemId, 0, 10, "createdAt,desc", otherCompanyPrincipal
            )).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("queryMovements")
    class QueryMovementsTests {

        @Test
        @DisplayName("Should query movements with filter DTO")
        void shouldQueryWithFilterDto() {
            InventoryMovementFilterDTO filter = new InventoryMovementFilterDTO();
            filter.setCompanyId(companyId);
            filter.setMovementType("ENTRADA");

            Page<InventoryMovementEntity> pageMock = new PageImpl<>(List.of(mockMovement));
            when(movementRepository.findByCompanyIdWithFilters(eq(companyId), any(), eq("ENTRADA"), any(), any(), any(Pageable.class)))
                    .thenReturn(pageMock);

            PageResponseDTO<InventoryMovementResponseDTO> result = movementService.queryMovements(filter, adminPrincipal);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
