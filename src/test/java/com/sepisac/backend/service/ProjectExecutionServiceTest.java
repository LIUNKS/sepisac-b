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
import java.util.List;
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
    private QuotationLaborRequirementRepository quotationLaborRequirementRepository;

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
        @DisplayName("Debe formatear el título del proyecto cuando serviceType usa un código del catálogo")
        void shouldFormatCatalogServiceTypeInProjectTitle() {
            testQuotation.setServiceType("TORNEADO_EJES");
            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(projectRepository.existsByCompanyIdAndCode(companyId, "PRJ-COT-2026-001")).thenReturn(false);
            when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> {
                ProjectEntity p = invocation.getArgument(0);
                p.setId(projectId);
                return p;
            });

            ProjectResponseDTO result = projectExecutionService.createProjectFromQuotation(quotationId);

            assertThat(result.getTitle()).isEqualTo("Torneado de Ejes - Minera Chinalco Perú");
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
        @DisplayName("Debe actualizar el estado a EN_PROCESO correctamente cuando cubre el presupuesto")
        void shouldUpdateStatusToEnProceso() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(i -> i.getArgument(0));

            ProjectStatusUpdateDTO dto = new ProjectStatusUpdateDTO("EN_PROCESO");
            ProjectResponseDTO result = projectExecutionService.updateProjectStatus(projectId, dto);

            assertThat(result.getStatus()).isEqualTo("EN_PROCESO");
            verify(projectRepository).save(testProject);
        }

        @Test
        @DisplayName("Debe bloquear inicio (PENDIENTE -> EN_PROCESO) si falta personal o maquinaria presupuestada")
        void shouldBlockStartProjectWhenBudgetedEmployeesOrMachineryMissing() {
            QuotationLaborRequirementEntity laborReq = new QuotationLaborRequirementEntity();
            laborReq.setSpecialtyNeeded("Soldador 6G");
            laborReq.setQuantityRequired(3);
            laborReq.setEstimatedHours(40);

            QuotationDetailEntity equipReq = new QuotationDetailEntity();
            equipReq.setItemType("EQUIPO");
            equipReq.setItemDescription("Grúa Pluma 5T");
            equipReq.setQuantity(4);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(quotationLaborRequirementRepository.findByQuotationId(quotationId)).thenReturn(List.of(laborReq));
            when(quotationDetailRepository.findByQuotationId(quotationId)).thenReturn(List.of(equipReq));
            when(projectAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of());
            when(projectMachineryAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of());

            ProjectStatusUpdateDTO dto = new ProjectStatusUpdateDTO("EN_PROCESO");
            assertThatThrownBy(() -> projectExecutionService.updateProjectStatus(projectId, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Personal asignado 0/3 (faltan 3)")
                    .hasMessageContaining("Maquinaria asignada 0/1 (faltan 1)");

            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe permitir iniciar proyecto (PENDIENTE -> EN_PROCESO) cuando todos los recursos presupuestados están asignados")
        void shouldAllowStartProjectWhenAllBudgetedResourcesAssigned() {
            QuotationLaborRequirementEntity laborReq = new QuotationLaborRequirementEntity();
            laborReq.setSpecialtyNeeded("Supervisor");
            laborReq.setQuantityRequired(1);
            laborReq.setEstimatedHours(40);

            QuotationDetailEntity equipReq = new QuotationDetailEntity();
            equipReq.setItemType("EQUIPO");
            equipReq.setItemDescription("Generador Eléctrico 50kW");
            equipReq.setQuantity(3);

            ProjectAssignmentEntity empAssign = new ProjectAssignmentEntity();
            empAssign.setId(UUID.randomUUID());
            empAssign.setEmployee(testEmployee);
            empAssign.setIsActive(true);

            ProjectMachineryAssignmentEntity machAssign = new ProjectMachineryAssignmentEntity();
            machAssign.setId(UUID.randomUUID());
            machAssign.setMachineryEquipment(testMachinery);
            machAssign.setStatus("EN_USO");

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(quotationLaborRequirementRepository.findByQuotationId(quotationId)).thenReturn(List.of(laborReq));
            when(quotationDetailRepository.findByQuotationId(quotationId)).thenReturn(List.of(equipReq));
            when(projectAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of(empAssign));
            when(projectMachineryAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of(machAssign));
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
        @DisplayName("Debe asignar maquinaria disponible y cambiar su estado a EN_USO dentro del cupo")
        void shouldAssignMachinerySuccessfully() {
            QuotationDetailEntity equipDetail = new QuotationDetailEntity();
            equipDetail.setItemType("EQUIPO");
            equipDetail.setItemDescription("Generador Eléctrico 50kW");
            equipDetail.setQuantity(5);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(machineryEquipmentRepository.findById(machineryId)).thenReturn(Optional.of(testMachinery));
            when(quotationDetailRepository.findByQuotationId(quotationId)).thenReturn(List.of(equipDetail));
            when(projectMachineryAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of());
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
        @DisplayName("Debe bloquear asignación de maquinaria si la cotización presupuesta 0 equipos")
        void shouldBlockAssignMachineryWhenQuotationBudgetsZeroEquipment() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(machineryEquipmentRepository.findById(machineryId)).thenReturn(Optional.of(testMachinery));
            when(quotationDetailRepository.findByQuotationId(quotationId)).thenReturn(List.of());

            ProjectMachineryAssignmentCreateDTO dto = new ProjectMachineryAssignmentCreateDTO(machineryId, LocalDate.now(), null);
            assertThatThrownBy(() -> projectExecutionService.assignMachinery(projectId, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("0 equipos");

            verify(projectMachineryAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe bloquear asignación de maquinaria si ya se alcanzó el cupo presupuestado")
        void shouldBlockAssignMachineryWhenBudgetLimitReached() {
            QuotationDetailEntity equipDetail = new QuotationDetailEntity();
            equipDetail.setItemType("EQUIPO");
            equipDetail.setItemDescription("Grúa Pluma");
            equipDetail.setQuantity(3);

            ProjectMachineryAssignmentEntity existing = new ProjectMachineryAssignmentEntity();
            existing.setId(UUID.randomUUID());
            existing.setStatus("EN_USO");

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(machineryEquipmentRepository.findById(machineryId)).thenReturn(Optional.of(testMachinery));
            when(quotationDetailRepository.findByQuotationId(quotationId)).thenReturn(List.of(equipDetail));
            when(projectMachineryAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of(existing));

            ProjectMachineryAssignmentCreateDTO dto = new ProjectMachineryAssignmentCreateDTO(machineryId, LocalDate.now(), null);
            assertThatThrownBy(() -> projectExecutionService.assignMachinery(projectId, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Límite de maquinaria alcanzado");

            verify(projectMachineryAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe permitir asignar maquinaria cuando la asignación previa ya fue DEVUELTO")
        void shouldAllowAssignMachineryWhenPreviousAssignmentIsReturned() {
            QuotationDetailEntity equipDetail = new QuotationDetailEntity();
            equipDetail.setItemType("EQUIPO");
            equipDetail.setItemDescription("Generador Eléctrico 50kW");
            equipDetail.setQuantity(3);

            ProjectMachineryAssignmentEntity returned = new ProjectMachineryAssignmentEntity();
            returned.setId(UUID.randomUUID());
            returned.setStatus("DEVUELTO");

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(machineryEquipmentRepository.findById(machineryId)).thenReturn(Optional.of(testMachinery));
            when(quotationDetailRepository.findByQuotationId(quotationId)).thenReturn(List.of(equipDetail));
            when(projectMachineryAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of(returned));
            when(projectMachineryAssignmentRepository.save(any(ProjectMachineryAssignmentEntity.class))).thenAnswer(i -> {
                ProjectMachineryAssignmentEntity a = i.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            ProjectMachineryAssignmentCreateDTO dto = new ProjectMachineryAssignmentCreateDTO(machineryId, LocalDate.now(), null);
            ProjectMachineryAssignmentResponseDTO result = projectExecutionService.assignMachinery(projectId, dto);

            assertThat(result.getStatus()).isEqualTo("EN_USO");
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
        @DisplayName("Debe asignar empleado a proyecto correctamente dentro del cupo presupuestado")
        void shouldAssignEmployeeSuccessfully() {
            QuotationLaborRequirementEntity laborReq = new QuotationLaborRequirementEntity();
            laborReq.setSpecialtyNeeded("Supervisor");
            laborReq.setQuantityRequired(2);
            laborReq.setEstimatedHours(40);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
            when(projectAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of());
            when(quotationLaborRequirementRepository.findByQuotationId(quotationId)).thenReturn(List.of(laborReq));
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

        @Test
        @DisplayName("Debe bloquear asignación si la cotización presupuesta 0 empleados")
        void shouldBlockAssignEmployeeWhenQuotationBudgetsZeroEmployees() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
            when(projectAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of());
            when(quotationLaborRequirementRepository.findByQuotationId(quotationId)).thenReturn(List.of());

            ProjectAssignmentCreateDTO dto = new ProjectAssignmentCreateDTO(employeeId, "Supervisor", LocalDate.now());
            assertThatThrownBy(() -> projectExecutionService.assignEmployee(projectId, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("0 empleados");

            verify(projectAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe bloquear asignación cuando ya se cubrió el máximo presupuestado de empleados")
        void shouldBlockAssignEmployeeWhenBudgetLimitReached() {
            QuotationLaborRequirementEntity laborReq = new QuotationLaborRequirementEntity();
            laborReq.setSpecialtyNeeded("Soldador");
            laborReq.setQuantityRequired(1);
            laborReq.setEstimatedHours(40);

            EmployeeEntity existingEmp = new EmployeeEntity();
            existingEmp.setId(UUID.randomUUID());
            existingEmp.setFullName("Otro Técnico");

            ProjectAssignmentEntity active = new ProjectAssignmentEntity();
            active.setId(UUID.randomUUID());
            active.setEmployee(existingEmp);
            active.setIsActive(true);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
            when(projectAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of(active));
            when(quotationLaborRequirementRepository.findByQuotationId(quotationId)).thenReturn(List.of(laborReq));

            ProjectAssignmentCreateDTO dto = new ProjectAssignmentCreateDTO(employeeId, "Supervisor", LocalDate.now());
            assertThatThrownBy(() -> projectExecutionService.assignEmployee(projectId, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Límite de personal alcanzado");

            verify(projectAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe bloquear asignación duplicada si el mismo empleado ya está activo en el proyecto")
        void shouldBlockDuplicateActiveEmployeeAssignment() {
            ProjectAssignmentEntity active = new ProjectAssignmentEntity();
            active.setId(UUID.randomUUID());
            active.setEmployee(testEmployee);
            active.setIsActive(true);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
            when(projectAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of(active));

            ProjectAssignmentCreateDTO dto = new ProjectAssignmentCreateDTO(employeeId, "Supervisor", LocalDate.now());
            assertThatThrownBy(() -> projectExecutionService.assignEmployee(projectId, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ya se encuentra asignado activamente");

            verify(projectAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe permitir asignar reemplazo si la asignación previa está inactiva (isActive = false)")
        void shouldAllowAssignEmployeeWhenPreviousAssignmentIsInactive() {
            QuotationLaborRequirementEntity laborReq = new QuotationLaborRequirementEntity();
            laborReq.setSpecialtyNeeded("Supervisor");
            laborReq.setQuantityRequired(1);
            laborReq.setEstimatedHours(40);

            ProjectAssignmentEntity inactive = new ProjectAssignmentEntity();
            inactive.setId(UUID.randomUUID());
            inactive.setEmployee(testEmployee);
            inactive.setIsActive(false);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
            when(projectAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of(inactive));
            when(quotationLaborRequirementRepository.findByQuotationId(quotationId)).thenReturn(List.of(laborReq));
            when(projectAssignmentRepository.save(any(ProjectAssignmentEntity.class))).thenAnswer(i -> {
                ProjectAssignmentEntity a = i.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            ProjectAssignmentCreateDTO dto = new ProjectAssignmentCreateDTO(employeeId, "Supervisor", LocalDate.now());
            ProjectAssignmentResponseDTO result = projectExecutionService.assignEmployee(projectId, dto);

            assertThat(result.getIsActive()).isTrue();
            verify(projectAssignmentRepository).save(any(ProjectAssignmentEntity.class));
        }

        @Test
        @DisplayName("Debe retornar el resumen de requerimientos presupuestados vs asignados")
        void shouldReturnResourceRequirementsSummary() {
            QuotationLaborRequirementEntity labor1 = new QuotationLaborRequirementEntity();
            labor1.setSpecialtyNeeded("Soldador 6G");
            labor1.setQuantityRequired(2);
            labor1.setEstimatedHours(48);

            QuotationLaborRequirementEntity labor2 = new QuotationLaborRequirementEntity();
            labor2.setSpecialtyNeeded("Ayudante");
            labor2.setQuantityRequired(1);
            labor2.setEstimatedHours(48);

            QuotationDetailEntity equip1 = new QuotationDetailEntity();
            equip1.setItemType("EQUIPO");
            equip1.setItemDescription("Soldadora Miller");
            equip1.setQuantity(5);

            QuotationDetailEntity mat1 = new QuotationDetailEntity();
            mat1.setItemType("MATERIAL");
            mat1.setItemDescription("Electrodo 7018");
            mat1.setQuantity(10);

            ProjectAssignmentEntity activeEmp = new ProjectAssignmentEntity();
            activeEmp.setId(UUID.randomUUID());
            activeEmp.setEmployee(testEmployee);
            activeEmp.setIsActive(true);

            ProjectMachineryAssignmentEntity activeMach = new ProjectMachineryAssignmentEntity();
            activeMach.setId(UUID.randomUUID());
            activeMach.setMachineryEquipment(testMachinery);
            activeMach.setStatus("EN_USO");

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(quotationLaborRequirementRepository.findByQuotationId(quotationId)).thenReturn(List.of(labor1, labor2));
            when(quotationDetailRepository.findByQuotationId(quotationId)).thenReturn(List.of(equip1, mat1));
            when(projectAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of(activeEmp));
            when(projectMachineryAssignmentRepository.findByProjectId(projectId)).thenReturn(List.of(activeMach));

            ProjectResourceRequirementsResponseDTO summary =
                    projectExecutionService.getResourceRequirements(projectId, testUserPrincipal);

            assertThat(summary.getProjectId()).isEqualTo(projectId);
            assertThat(summary.getQuotationId()).isEqualTo(quotationId);
            assertThat(summary.getQuotationNumber()).isEqualTo("COT-2026-001");
            assertThat(summary.getBudgetedEmployees()).isEqualTo(3);
            assertThat(summary.getAssignedEmployees()).isEqualTo(1);
            assertThat(summary.getBudgetedMachinery()).isEqualTo(1);
            assertThat(summary.getAssignedMachinery()).isEqualTo(1);
            assertThat(summary.isReadyToStart()).isFalse();
            assertThat(summary.getLaborRequirements()).hasSize(2);
            assertThat(summary.getEquipmentRequirements()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("6. Consultas de Recursos del Proyecto")
    class GetProjectResourcesTests {

        @Test
        @DisplayName("Debe listar consumos de inventario del proyecto")
        void shouldGetInventoryConsumptionsSuccessfully() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

            ProjectInventoryConsumptionEntity consumption = new ProjectInventoryConsumptionEntity();
            consumption.setId(UUID.randomUUID());
            consumption.setProject(testProject);
            consumption.setInventoryItem(testInventoryItem);
            consumption.setQuantityConsumed(10);
            consumption.setConsumptionDate(OffsetDateTime.now());

            when(projectInventoryConsumptionRepository.findByProjectId(projectId))
                    .thenReturn(List.of(consumption));

            List<ProjectInventoryConsumptionResponseDTO> result =
                    projectExecutionService.getInventoryConsumptions(projectId, testUserPrincipal);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getItemSku()).isEqualTo("TUB-AC-001");
            assertThat(result.get(0).getQuantityConsumed()).isEqualTo(10);
        }

        @Test
        @DisplayName("Debe listar maquinarias asignadas al proyecto")
        void shouldGetMachineryAssignmentsSuccessfully() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

            ProjectMachineryAssignmentEntity assignment = new ProjectMachineryAssignmentEntity();
            assignment.setId(UUID.randomUUID());
            assignment.setProject(testProject);
            assignment.setMachineryEquipment(testMachinery);
            assignment.setAssignedDate(LocalDate.now());
            assignment.setStatus("EN_USO");

            when(projectMachineryAssignmentRepository.findByProjectId(projectId))
                    .thenReturn(List.of(assignment));

            List<ProjectMachineryAssignmentResponseDTO> result =
                    projectExecutionService.getMachineryAssignments(projectId, testUserPrincipal);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMachineryCode()).isEqualTo("MAQ-GEN-01");
            assertThat(result.get(0).getStatus()).isEqualTo("EN_USO");
        }

        @Test
        @DisplayName("Debe listar empleados asignados al proyecto")
        void shouldGetEmployeeAssignmentsSuccessfully() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

            ProjectAssignmentEntity assignment = new ProjectAssignmentEntity();
            assignment.setId(UUID.randomUUID());
            assignment.setProject(testProject);
            assignment.setEmployee(testEmployee);
            assignment.setAssignedRole("Técnico");
            assignment.setAssignedDate(LocalDate.now());
            assignment.setIsActive(true);

            when(projectAssignmentRepository.findByProjectId(projectId))
                    .thenReturn(List.of(assignment));

            List<ProjectAssignmentResponseDTO> result =
                    projectExecutionService.getEmployeeAssignments(projectId, testUserPrincipal);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmployeeName()).contains("Carlos Mendoza");
            assertThat(result.get(0).getAssignedRole()).isEqualTo("Técnico");
        }
    }

    @Nested
    @DisplayName("7. Liberación y Desasignación de Recursos")
    class ReleaseResourcesTests {

        @Test
        @DisplayName("Debe liberar maquinaria y restaurar estado a DISPONIBLE")
        void shouldReleaseMachinerySuccessfully() {
            UUID assignmentId = UUID.randomUUID();
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

            ProjectMachineryAssignmentEntity assignment = new ProjectMachineryAssignmentEntity();
            assignment.setId(assignmentId);
            assignment.setProject(testProject);
            assignment.setMachineryEquipment(testMachinery);
            assignment.setStatus("EN_USO");

            when(projectMachineryAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

            projectExecutionService.releaseMachineryAssignment(projectId, assignmentId, testUserPrincipal);

            assertThat(testMachinery.getStatus()).isEqualTo("DISPONIBLE");
            verify(machineryEquipmentRepository).save(testMachinery);
            verify(projectMachineryAssignmentRepository).delete(assignment);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la asignación de maquinaria no existe")
        void shouldThrowWhenMachineryAssignmentNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
            when(projectMachineryAssignmentRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectExecutionService.releaseMachineryAssignment(projectId, unknownId, testUserPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Debe desasignar empleado del proyecto correctamente")
        void shouldRemoveEmployeeAssignmentSuccessfully() {
            UUID assignmentId = UUID.randomUUID();
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

            ProjectAssignmentEntity assignment = new ProjectAssignmentEntity();
            assignment.setId(assignmentId);
            assignment.setProject(testProject);
            assignment.setEmployee(testEmployee);

            when(projectAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

            projectExecutionService.removeEmployeeAssignment(projectId, assignmentId, testUserPrincipal);

            verify(projectAssignmentRepository).delete(assignment);
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleException si la asignación no pertenece al proyecto")
        void shouldThrowWhenAssignmentBelongsToOtherProject() {
            UUID assignmentId = UUID.randomUUID();
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

            ProjectEntity otherProject = new ProjectEntity();
            otherProject.setId(UUID.randomUUID());

            ProjectAssignmentEntity assignment = new ProjectAssignmentEntity();
            assignment.setId(assignmentId);
            assignment.setProject(otherProject);

            when(projectAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> projectExecutionService.removeEmployeeAssignment(projectId, assignmentId, testUserPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("La asignación no corresponde al proyecto indicado");
        }
    }
}
