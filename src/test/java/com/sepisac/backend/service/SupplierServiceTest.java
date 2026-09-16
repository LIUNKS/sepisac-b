package com.sepisac.backend.service;

import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.dto.SupplierCreateDTO;
import com.sepisac.backend.dto.SupplierFilterDTO;
import com.sepisac.backend.dto.SupplierResponseDTO;
import com.sepisac.backend.dto.SupplierUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.SupplierEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.SupplierRepository;
import com.sepisac.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
@DisplayName("SupplierService Unit Tests")
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SupplierService supplierService;

    private UserPrincipal adminEmpresaPrincipal;
    private CompanyEntity testCompany;
    private SupplierEntity testSupplier;
    private UUID companyId;
    private UUID supplierId;
    private UUID adminEmpresaId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
        adminEmpresaId = UUID.randomUUID();

        adminEmpresaPrincipal = new UserPrincipal(
                adminEmpresaId, "admin@empresa.com", "admin_empresa", "hashedpwd", companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
        );

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testSupplier = new SupplierEntity();
        testSupplier.setId(supplierId);
        testSupplier.setCompany(testCompany);
        testSupplier.setBusinessName("ACEROS AREQUIPA S.A.");
        testSupplier.setRuc("20100010724");
        testSupplier.setContactPhone("987654321");
        testSupplier.setEmail("ventas@aceros.com");
        testSupplier.setIsDeleted(false);
        testSupplier.setCreatedAt(OffsetDateTime.now());
    }

    @Nested
    @DisplayName("createSupplier")
    class CreateSupplierTests {

        @Test
        @DisplayName("Should create supplier successfully and record audit log")
        void shouldCreateSupplierSuccessfully() {
            SupplierCreateDTO request = new SupplierCreateDTO(
                    companyId, "ACEROS AREQUIPA S.A.", "20100010724", "987654321", "ventas@aceros.com"
            );

            when(supplierRepository.existsByCompanyIdAndRuc(companyId, "20100010724")).thenReturn(false);
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
            when(supplierRepository.save(any(SupplierEntity.class))).thenAnswer(invocation -> {
                SupplierEntity s = invocation.getArgument(0);
                s.setId(supplierId);
                s.setCreatedAt(OffsetDateTime.now());
                return s;
            });

            SupplierResponseDTO response = supplierService.createSupplier(request, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(supplierId);
            assertThat(response.getBusinessName()).isEqualTo("ACEROS AREQUIPA S.A.");
            assertThat(response.getRuc()).isEqualTo("20100010724");

            verify(supplierRepository).save(any(SupplierEntity.class));
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("CREATE"), eq("SUPPLIERS"), any());
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when RUC already exists in same company")
        void shouldThrowWhenRucExistsInCompany() {
            SupplierCreateDTO request = new SupplierCreateDTO(
                    companyId, "ACEROS AREQUIPA S.A.", "20100010724", "987654321", "ventas@aceros.com"
            );

            when(supplierRepository.existsByCompanyIdAndRuc(companyId, "20100010724")).thenReturn(true);

            assertThatThrownBy(() -> supplierService.createSupplier(request, adminEmpresaPrincipal))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("20100010724");

            verify(supplierRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateSupplier and deleteSupplier")
    class UpdateAndDeleteTests {

        @Test
        @DisplayName("Should update supplier successfully")
        void shouldUpdateSupplier() {
            SupplierUpdateDTO updateDTO = new SupplierUpdateDTO(
                    "CORPORACION ACEROS S.A.", "20100010724", "01-555-1234", "contacto@aceros.com"
            );

            when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(testSupplier));
            when(supplierRepository.save(any(SupplierEntity.class))).thenReturn(testSupplier);

            SupplierResponseDTO response = supplierService.updateSupplier(supplierId, updateDTO, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            verify(supplierRepository).save(testSupplier);
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("UPDATE"), eq("SUPPLIERS"), any());
        }

        @Test
        @DisplayName("Should soft delete supplier successfully")
        void shouldSoftDeleteSupplier() {
            when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(testSupplier));
            when(supplierRepository.save(any(SupplierEntity.class))).thenReturn(testSupplier);

            supplierService.deleteSupplier(supplierId, adminEmpresaPrincipal);

            assertThat(testSupplier.getIsDeleted()).isTrue();
            verify(supplierRepository).save(testSupplier);
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("DELETE"), eq("SUPPLIERS"), any());
        }
    }
}
