package com.sepisac.backend.service;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.*;
import com.sepisac.backend.repository.*;
import com.sepisac.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectExecutionService Unit Tests - Sprint 4")
class ProjectExecutionServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationDetailRepository quotationDetailRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private MachineryEquipmentRepository machineryEquipmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ProjectAssignmentRepository projectAssignmentRepository;

    @Mock
    private ProjectMachineryAssignmentRepository projectMachineryAssignmentRepository;

    @Mock
    private ProjectInventoryConsumptionRepository projectInventoryConsumptionRepository;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private ProjectExecutionService projectExecutionService;

    private CompanyEntity testCompany;
    private ProjectEntity testProject;
    private QuotationEntity testQuotation;
    private InventoryItemEntity testInventoryItem;
    private MachineryEquipmentEntity testMachinery;
    private EmployeeEntity testEmployee;
    private UserPrincipal testUserPrincipal;

    private UUID companyId;
    private UUID projectId;
    private UUID quotationId;
    private UUID itemId;
    private UUID machineryId;
    private UUID employeeId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        quotationId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        machineryId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testQuotation = new QuotationEntity();
        testQuotation.setId(quotationId);
        testQuotation.setCompany(testCompany);
        testQuotation.setQuotationNumber("COT-2026-001");
        testQuotation.setClientName("Minera Chinalco Perú");
        testQuotation.setServiceType("Mantenimiento de Plantas");
        testQuotation.setStatus("APROBADA");
        testQuotation.setTotalAmount(new BigDecimal("25000.00"));

        testProject = new ProjectEntity();
        testProject.setId(projectId);
        testProject.setCompany(testCompany);
        testProject.setQuotation(testQuotation);
        testProject.setCode("PRJ-COT-2026-001");
        testProject.setTitle("Mantenimiento de Plantas - Minera Chinalco Perú");
        testProject.setClientName("Minera Chinalco Perú");
        testProject.setStatus("PENDIENTE");

        testInventoryItem = new InventoryItemEntity();
        testInventoryItem.setId(itemId);
        testInventoryItem.setCompany(testCompany);
        testInventoryItem.setSku("TUB-AC-001");
        testInventoryItem.setName("Tubo de Acero 2 pulgadas");
        testInventoryItem.setStockQuantity(20);

        testMachinery = new MachineryEquipmentEntity();
        testMachinery.setId(machineryId);
        testMachinery.setCompany(testCompany);
        testMachinery.setCode("MAQ-GEN-01");
        testMachinery.setName("Generador Eléctrico 50kW");
        testMachinery.setStatus("DISPONIBLE");

        testEmployee = new EmployeeEntity();
        testEmployee.setId(employeeId);
        testEmployee.setCompany(testCompany);
        testEmployee.setFullName("Carlos Mendoza");
        testEmployee.setSpecialty("Supervisor");
        testEmployee.setIsAvailable(true);

        testUserPrincipal = new UserPrincipal(
                userId, "carlos@sepisac.com", "carlos.mendoza", "pass",
                companyId, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPERADMIN")), true
        );
    }

    @Nested
    @DisplayName("1. Crear Proyecto desde Cotización Aprobada")
    class CreateProjectFromQuotationTests {

        @Test
        @DisplayName("Debe crear un proyecto activo exitosamente cuando la cotización está APROBADA")
        void shouldCreateProjectSuccessfullyWhenQuotationApproved() {
            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(projectRepository.existsByCompanyIdAndCode(companyId, "PRJ-COT-2026-001")).thenReturn(false);
            when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> {
                ProjectEntity p = invocation.getArgument(0);
                p.setId(projectId);
                return p;
            });

            ProjectResponseDTO result = projectExecutionService.createProjectFromQuotation(quotationId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(projectId);
            assertThat(result.getCode()).isEqualTo("PRJ-COT-2026-001");
            assertThat(result.getClientName()).isEqualTo("Minera Chinalco Perú");
            assertThat(result.getStatus()).isEqualTo("PENDIENTE");

            verify(projectRepository).save(any(ProjectEntity.class));
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleException si la cotización NO está APROBADA")
        void shouldThrowExceptionWhenQuotationNotApproved() {
            testQuotation.setStatus("BORRADOR");
            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));

            assertThatThrownBy(() -> projectExecutionService.createProjectFromQuotation(quotationId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Solo se pueden crear proyectos a partir de cotizaciones aprobadas");

            verify(projectRepository, never()).save(any(ProjectEntity.class));
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la cotización no existe")
        void shouldThrowExceptionWhenQuotationNotFound() {
            when(quotationRepository.findById(quotationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectExecutionService.createProjectFromQuotation(quotationId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cotización no encontrada");
        }
    }

    @Nested
    @DisplayName("2. Actualizar Estado del Proyecto")
    class UpdateProjectStatusTests {

        @Test
        @DisplayName("Debe actualizar el estado a EN_PROCESO correctamente")
        void shouldUpdateStatusToEnProceso() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(i -> i.getArgument(0));

            ProjectStatusUpdateDTO dto = new ProjectStatusUpdateDTO("EN_PROCESO");
            ProjectResponseDTO result = projectExecutionService.updateProjectStatus(projectId, dto);

            assertThat(result.getStatus()).isEqualTo("EN_PROCESO");
            verify(projectRepository).save(testProject);
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException si el estado no es permitido")
        void shouldThrowWhenInvalidStatus() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

            ProjectStatusUpdateDTO dto = new ProjectStatusUpdateDTO("ESTADO_INVALIDO");
            assertThatThrownBy(() -> projectExecutionService.updateProjectStatus(projectId, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Estado no permitido");
        }
    }

    @Nested
    @DisplayName("3. Asignación de Maquinaria y Disponibilidad de Activos (Test 2)")
    class AssignMachineryTests {

        @Test
        @DisplayName("Debe asignar maquinaria disponible y cambiar su estado a EN_USO")
        void shouldAssignMachinerySuccessfully() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(machineryEquipmentRepository.findById(machineryId)).thenReturn(Optional.of(testMachinery));
            when(projectMachineryAssignmentRepository.save(any(ProjectMachineryAssignmentEntity.class))).thenAnswer(i -> {
                ProjectMachineryAssignmentEntity a = i.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            ProjectMachineryAssignmentCreateDTO dto = new ProjectMachineryAssignmentCreateDTO(machineryId, LocalDate.now(), LocalDate.now().plusDays(7));
            ProjectMachineryAssignmentResponseDTO result = projectExecutionService.assignMachinery(projectId, dto);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("EN_USO");
            assertThat(testMachinery.getStatus()).isEqualTo("EN_USO");

            verify(machineryEquipmentRepository).save(testMachinery);
            verify(projectMachineryAssignmentRepository).save(any(ProjectMachineryAssignmentEntity.class));
        }

        @Test
        @DisplayName("Debe fallar con HTTP 400 (IllegalArgumentException) si la maquinaria está EN_MANTENIMIENTO")
        void shouldThrowWhenMachineryInMaintenance() {
            testMachinery.setStatus("EN_MANTENIMIENTO");
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(machineryEquipmentRepository.findById(machineryId)).thenReturn(Optional.of(testMachinery));

            ProjectMachineryAssignmentCreateDTO dto = new ProjectMachineryAssignmentCreateDTO(machineryId, LocalDate.now(), null);
            assertThatThrownBy(() -> projectExecutionService.assignMachinery(projectId, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no está disponible");

            verify(projectMachineryAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe fallar con HTTP 400 (IllegalArgumentException) si la maquinaria ya está EN_USO")
        void shouldThrowWhenMachineryAlreadyInUse() {
            testMachinery.setStatus("EN_USO");
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(machineryEquipmentRepository.findById(machineryId)).thenReturn(Optional.of(testMachinery));

            ProjectMachineryAssignmentCreateDTO dto = new ProjectMachineryAssignmentCreateDTO(machineryId, LocalDate.now(), null);
            assertThatThrownBy(() -> projectExecutionService.assignMachinery(projectId, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no está disponible");

            verify(projectMachineryAssignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("4. Consumo de Inventario y Movimiento SALIDA (Test 1 & Test 4)")
    class ConsumeInventoryTests {

        @Test
        @DisplayName("Debe descontar stock, registrar consumo y crear movimiento de SALIDA con auditoría")
        void shouldConsumeInventorySuccessfully() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(testInventoryItem));
            when(inventoryItemRepository.save(any(InventoryItemEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(projectInventoryConsumptionRepository.save(any(ProjectInventoryConsumptionEntity.class))).thenAnswer(i -> {
                ProjectInventoryConsumptionEntity c = i.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            ProjectInventoryConsumptionCreateDTO dto = new ProjectInventoryConsumptionCreateDTO(itemId, 5, "Instalación en sitio");
            ProjectInventoryConsumptionResponseDTO result = projectExecutionService.consumeInventory(projectId, dto, testUserPrincipal);

            assertThat(result).isNotNull();
            assertThat(result.getQuantityConsumed()).isEqualTo(5);
            assertThat(result.getRemainingStock()).isEqualTo(15);
            assertThat(testInventoryItem.getStockQuantity()).isEqualTo(15);

            ArgumentCaptor<InventoryMovementEntity> movementCaptor = ArgumentCaptor.forClass(InventoryMovementEntity.class);
            verify(inventoryMovementRepository).save(movementCaptor.capture());

            InventoryMovementEntity movement = movementCaptor.getValue();
            assertThat(movement.getMovementType()).isEqualTo("SALIDA");
            assertThat(movement.getQuantityChanged()).isEqualTo(5);
            assertThat(movement.getReason()).contains("Consumo en Proyecto");
            assertThat(movement.getInventoryItem()).isEqualTo(testInventoryItem);
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleException evitando stock negativo cuando cantidad supera stock")
        void shouldThrowExceptionWhenStockInsufficient() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(testInventoryItem));

            // Stock actual = 20, solicitamos 25
            ProjectInventoryConsumptionCreateDTO dto = new ProjectInventoryConsumptionCreateDTO(itemId, 25, "Consumo excesivo");

            assertThatThrownBy(() -> projectExecutionService.consumeInventory(projectId, dto, testUserPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Stock insuficiente");

            assertThat(testInventoryItem.getStockQuantity()).isEqualTo(20);
            verify(projectInventoryConsumptionRepository, never()).save(any());
            verify(inventoryMovementRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("5. Asignación de Empleados")
    class AssignEmployeeTests {

        @Test
        @DisplayName("Debe asignar empleado a proyecto correctamente")
        void shouldAssignEmployeeSuccessfully() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
            when(projectAssignmentRepository.save(any(ProjectAssignmentEntity.class))).thenAnswer(i -> {
                ProjectAssignmentEntity a = i.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            ProjectAssignmentCreateDTO dto = new ProjectAssignmentCreateDTO(employeeId, "Supervisor de Obra", LocalDate.now());
            ProjectAssignmentResponseDTO result = projectExecutionService.assignEmployee(projectId, dto);

            assertThat(result).isNotNull();
            assertThat(result.getEmployeeName()).contains("Carlos Mendoza");
            assertThat(result.getAssignedRole()).isEqualTo("Supervisor de Obra");
            assertThat(result.getIsActive()).isTrue();

            verify(projectAssignmentRepository).save(any(ProjectAssignmentEntity.class));
        }
    }
}
