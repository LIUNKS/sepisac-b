package com.sepisac.backend.service;

import com.sepisac.backend.dto.MachineryCreateDTO;
import com.sepisac.backend.dto.MachineryResponseDTO;
import com.sepisac.backend.dto.MachineryStatusUpdateDTO;
import com.sepisac.backend.dto.MachineryUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.MachineryEquipmentEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.MachineryEquipmentRepository;
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

import java.time.LocalDate;
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
@DisplayName("MachineryEquipmentService Unit Tests")
class MachineryEquipmentServiceTest {

    @Mock
    private MachineryEquipmentRepository machineryRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private MachineryEquipmentService machineryService;

    private UserPrincipal adminEmpresaPrincipal;
    private CompanyEntity testCompany;
    private MachineryEquipmentEntity testMachinery;
    private UUID companyId;
    private UUID machineryId;
    private UUID adminEmpresaId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        machineryId = UUID.randomUUID();
        adminEmpresaId = UUID.randomUUID();

        adminEmpresaPrincipal = new UserPrincipal(
                adminEmpresaId, "admin@empresa.com", "admin_empresa", "hashedpwd", companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
        );

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testMachinery = new MachineryEquipmentEntity();
        testMachinery.setId(machineryId);
        testMachinery.setCompany(testCompany);
        testMachinery.setCode("GEN-CAT-150KW");
        testMachinery.setName("Grupo Electrógeno Caterpillar 150 kW");
        testMachinery.setStatus("DISPONIBLE");
        testMachinery.setLastMaintenanceDate(LocalDate.of(2026, 8, 1));
        testMachinery.setNextMaintenanceDate(LocalDate.of(2026, 11, 1));
        testMachinery.setIsDeleted(false);
        testMachinery.setCreatedAt(OffsetDateTime.now());
    }

    @Nested
    @DisplayName("createMachinery")
    class CreateMachineryTests {

        @Test
        @DisplayName("Should create machinery successfully with default status DISPONIBLE")
        void shouldCreateMachinerySuccessfully() {
            MachineryCreateDTO request = new MachineryCreateDTO(
                    companyId, "GEN-CAT-150KW", "Grupo Electrógeno Caterpillar 150 kW", null,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1)
            );

            when(machineryRepository.existsByCompanyIdAndCode(companyId, "GEN-CAT-150KW")).thenReturn(false);
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
            when(machineryRepository.save(any(MachineryEquipmentEntity.class))).thenAnswer(invocation -> {
                MachineryEquipmentEntity m = invocation.getArgument(0);
                m.setId(machineryId);
                m.setCreatedAt(OffsetDateTime.now());
                return m;
            });

            MachineryResponseDTO response = machineryService.createMachinery(request, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(machineryId);
            assertThat(response.getCode()).isEqualTo("GEN-CAT-150KW");
            assertThat(response.getStatus()).isEqualTo("DISPONIBLE");

            verify(machineryRepository).save(any(MachineryEquipmentEntity.class));
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("CREATE"), eq("MACHINERY"), any());
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when machinery code already exists")
        void shouldThrowWhenCodeExistsInCompany() {
            MachineryCreateDTO request = new MachineryCreateDTO(
                    companyId, "GEN-CAT-150KW", "Grupo Electrógeno", "DISPONIBLE", null, null
            );

            when(machineryRepository.existsByCompanyIdAndCode(companyId, "GEN-CAT-150KW")).thenReturn(true);

            assertThatThrownBy(() -> machineryService.createMachinery(request, adminEmpresaPrincipal))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("GEN-CAT-150KW");

            verify(machineryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateStatus and deleteMachinery")
    class StatusAndUpdateTests {

        @Test
        @DisplayName("Should update status to EN_USO successfully")
        void shouldUpdateStatus() {
            MachineryStatusUpdateDTO statusDTO = new MachineryStatusUpdateDTO("EN_USO");

            when(machineryRepository.findById(machineryId)).thenReturn(Optional.of(testMachinery));
            when(machineryRepository.save(any(MachineryEquipmentEntity.class))).thenReturn(testMachinery);

            MachineryResponseDTO response = machineryService.updateStatus(machineryId, statusDTO, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            verify(machineryRepository).save(testMachinery);
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("UPDATE"), eq("MACHINERY"), any());
        }

        @Test
        @DisplayName("Should soft delete machinery successfully")
        void shouldSoftDeleteMachinery() {
            when(machineryRepository.findById(machineryId)).thenReturn(Optional.of(testMachinery));
            when(machineryRepository.save(any(MachineryEquipmentEntity.class))).thenReturn(testMachinery);

            machineryService.deleteMachinery(machineryId, adminEmpresaPrincipal);

            assertThat(testMachinery.getIsDeleted()).isTrue();
            verify(machineryRepository).save(testMachinery);
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("DELETE"), eq("MACHINERY"), any());
        }
    }
}
