package com.sepisac.backend.service;

import com.sepisac.backend.dto.InventoryItemCreateDTO;
import com.sepisac.backend.dto.InventoryItemResponseDTO;
import com.sepisac.backend.dto.InventoryItemUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.InventoryItemEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.InventoryItemRepository;
import com.sepisac.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryItemService Unit Tests")
class InventoryItemServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InventoryItemService inventoryItemService;

    private UserPrincipal adminEmpresaPrincipal;
    private CompanyEntity testCompany;
    private InventoryItemEntity testItem;
    private UUID companyId;
    private UUID itemId;
    private UUID adminEmpresaId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        adminEmpresaId = UUID.randomUUID();

        adminEmpresaPrincipal = new UserPrincipal(
                adminEmpresaId, "admin@empresa.com", "admin_empresa", "hashedpwd", companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
        );

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testItem = new InventoryItemEntity();
        testItem.setId(itemId);
        testItem.setCompany(testCompany);
        testItem.setSku("TUB-AC-2PULG");
        testItem.setName("Tubo de Acero ASTM A53");
        testItem.setStockQuantity(50);
        testItem.setPurchaseCost(new BigDecimal("120.50"));
        testItem.setSalePrice(new BigDecimal("185.00"));
        testItem.setMinStockAlert(10);
        testItem.setIsDeleted(false);
        testItem.setCreatedAt(OffsetDateTime.now());
    }

    @Nested
    @DisplayName("createItem")
    class CreateItemTests {

        @Test
        @DisplayName("Should create item successfully and calculate low stock flag")
        void shouldCreateItemSuccessfully() {
            InventoryItemCreateDTO request = new InventoryItemCreateDTO(
                    companyId, "TUB-AC-2PULG", "Tubo de Acero ASTM A53", "Desc",
                    50, new BigDecimal("120.50"), new BigDecimal("185.00"), 10
            );

            when(inventoryItemRepository.existsByCompanyIdAndSku(companyId, "TUB-AC-2PULG")).thenReturn(false);
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
            when(inventoryItemRepository.save(any(InventoryItemEntity.class))).thenAnswer(invocation -> {
                InventoryItemEntity item = invocation.getArgument(0);
                item.setId(itemId);
                item.setCreatedAt(OffsetDateTime.now());
                return item;
            });

            InventoryItemResponseDTO response = inventoryItemService.createItem(request, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(itemId);
            assertThat(response.getSku()).isEqualTo("TUB-AC-2PULG");
            assertThat(response.getIsLowStock()).isFalse();

            verify(inventoryItemRepository).save(any(InventoryItemEntity.class));
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("CREATE"), eq("INVENTORY"), any());
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when SKU already exists in same company")
        void shouldThrowWhenSkuExistsInCompany() {
            InventoryItemCreateDTO request = new InventoryItemCreateDTO(
                    companyId, "TUB-AC-2PULG", "Tubo de Acero", "Desc",
                    50, new BigDecimal("120.50"), new BigDecimal("185.00"), 10
            );

            when(inventoryItemRepository.existsByCompanyIdAndSku(companyId, "TUB-AC-2PULG")).thenReturn(true);

            assertThatThrownBy(() -> inventoryItemService.createItem(request, adminEmpresaPrincipal))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("TUB-AC-2PULG");

            verify(inventoryItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateItem and deleteItem")
    class UpdateAndDeleteTests {

        @Test
        @DisplayName("Should update inventory item details successfully")
        void shouldUpdateItem() {
            InventoryItemUpdateDTO updateDTO = new InventoryItemUpdateDTO(
                    "TUB-AC-2PULG", "Tubo de Acero Modificado", "Nueva desc",
                    new BigDecimal("130.00"), new BigDecimal("195.00"), 15
            );

            when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            when(inventoryItemRepository.save(any(InventoryItemEntity.class))).thenReturn(testItem);

            InventoryItemResponseDTO response = inventoryItemService.updateItem(itemId, updateDTO, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            verify(inventoryItemRepository).save(testItem);
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("UPDATE"), eq("INVENTORY"), any());
        }

        @Test
        @DisplayName("Should soft delete inventory item successfully")
        void shouldSoftDeleteItem() {
            when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            when(inventoryItemRepository.save(any(InventoryItemEntity.class))).thenReturn(testItem);

            inventoryItemService.deleteItem(itemId, adminEmpresaPrincipal);

            assertThat(testItem.getIsDeleted()).isTrue();
            verify(inventoryItemRepository).save(testItem);
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("DELETE"), eq("INVENTORY"), any());
        }
    }
}
