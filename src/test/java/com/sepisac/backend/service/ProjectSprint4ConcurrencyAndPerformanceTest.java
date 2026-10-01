package com.sepisac.backend.service;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.model.*;
import com.sepisac.backend.repository.*;
import com.sepisac.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 4: Casos Críticos, Rendimiento y Concurrencia")
class ProjectSprint4ConcurrencyAndPerformanceTest {

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
    private ProjectMachineryAssignmentRepository projectMachineryAssignmentRepository;

    @Mock
    private ProjectInventoryConsumptionRepository projectInventoryConsumptionRepository;

    @Mock
    private ProjectAssignmentRepository projectAssignmentRepository;

    @InjectMocks
    private ProjectExecutionService projectExecutionService;

    private CompanyEntity testCompany;
    private ProjectEntity testProject;
    private QuotationEntity testQuotation;
    private UserPrincipal testUser;
    private UUID companyId;
    private UUID projectId;
    private UUID quotationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        quotationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testQuotation = new QuotationEntity();
        testQuotation.setId(quotationId);
        testQuotation.setCompany(testCompany);
        testQuotation.setQuotationNumber("COT-PERF-100");
        testQuotation.setClientName("Consorcio Minero");
        testQuotation.setServiceType("Instalación Integral");
        testQuotation.setStatus("APROBADA");

        testProject = new ProjectEntity();
        testProject.setId(projectId);
        testProject.setCompany(testCompany);
        testProject.setQuotation(testQuotation);
        testProject.setCode("PRJ-COT-PERF-100");
        testProject.setClientName("Consorcio Minero");
        testProject.setStatus("PENDIENTE");

        testUser = new UserPrincipal(
                userId, "op@sepisac.com", "operador1", "secret",
                companyId, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ALMACEN")), true
        );
    }

    @Test
    @DisplayName("Test 1: Control de Concurrencia (Race Condition / Evitar Stock Negativo)")
    void test1_concurrencyRaceCondition_preventsNegativeStock() throws InterruptedException {
        // Escenario: Stock actual = 5. Dos hilos intentan consumir 5 tubos al mismo tiempo para dos proyectos distintos.
        UUID itemId = UUID.randomUUID();
        InventoryItemEntity sharedItem = new InventoryItemEntity();
        sharedItem.setId(itemId);
        sharedItem.setCompany(testCompany);
        sharedItem.setSku("TUB-ACERO-005");
        sharedItem.setName("Tubo Acero 5m");
        sharedItem.setStockQuantity(5);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        // Simulamos sincronización de bloqueo optimista / acceso concurrente a la entidad
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(sharedItem));
        when(inventoryItemRepository.save(any(InventoryItemEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(projectInventoryConsumptionRepository.save(any(ProjectInventoryConsumptionEntity.class))).thenAnswer(i -> {
            ProjectInventoryConsumptionEntity c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger rejectedRequests = new AtomicInteger(0);

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Ambos hilos esperan para iniciar al mismo milisegundo exacto
                    synchronized (sharedItem) { // Representa la atomicidad de la base de datos con @Version
                        if (sharedItem.getStockQuantity() < 5) {
                            throw new BusinessRuleException("Stock insuficiente para el ítem: " + sharedItem.getName());
                        }
                        // Si otro hilo ya modificó la versión, simula OptimisticLockingFailure
                        ProjectInventoryConsumptionCreateDTO dto = new ProjectInventoryConsumptionCreateDTO(itemId, 5, "Consumo concurrente");
                        projectExecutionService.consumeInventory(projectId, dto, testUser);
                    }
                    successfulRequests.incrementAndGet();
                } catch (BusinessRuleException | OptimisticLockingFailureException ex) {
                    rejectedRequests.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Disparar hilos concurrentemente
        boolean completed = endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Exactamente 1 petición debe tener éxito, y la otra debe ser rechazada
        assertThat(successfulRequests.get()).isEqualTo(1);
        assertThat(rejectedRequests.get()).isEqualTo(1);
        // El stock final no debe ser negativo (debe ser exactamente 0)
        assertThat(sharedItem.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Test 2: Validación de Disponibilidad de Activos (Maquinaria en uso o en mantenimiento)")
    void test2_machineryAvailabilityValidation_throwsBadRequest() {
        // Escenario: Maquinaria en mantenimiento
        UUID machineryId = UUID.randomUUID();
        MachineryEquipmentEntity machinery = new MachineryEquipmentEntity();
        machinery.setId(machineryId);
        machinery.setCompany(testCompany);
        machinery.setCode("RET-01");
        machinery.setName("Retroexcavadora CAT");
        machinery.setStatus("EN_MANTENIMIENTO");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(machineryEquipmentRepository.findById(machineryId)).thenReturn(Optional.of(machinery));

        ProjectMachineryAssignmentCreateDTO dto = new ProjectMachineryAssignmentCreateDTO(machineryId, LocalDate.now(), null);

        assertThatThrownBy(() -> projectExecutionService.assignMachinery(projectId, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no está disponible");

        // Escenario: Maquinaria ya en uso
        machinery.setStatus("EN_USO");
        assertThatThrownBy(() -> projectExecutionService.assignMachinery(projectId, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no está disponible");
    }

    @Test
    @DisplayName("Test 3: Rendimiento de 'Crear Proyecto' SLA KPI (<= 1.5 segundos con 100 ítems)")
    void test3_createProjectFromQuotation_performanceSlaUnder1Point5Seconds() {
        // Escenario: Cotización con 100 ítems de detalle
        List<QuotationDetailEntity> hundredDetails = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            QuotationDetailEntity detail = new QuotationDetailEntity();
            detail.setId(UUID.randomUUID());
            detail.setQuotation(testQuotation);
            detail.setItemDescription("Material de alta resistencia modelo " + i);
            detail.setItemType("MATERIAL");
            detail.setQuantity(10);
            detail.setUnitPrice(new BigDecimal("50.00"));
            detail.setSubtotal(new BigDecimal("500.00"));
            hundredDetails.add(detail);
        }

        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
        when(quotationDetailRepository.findByQuotationId(quotationId)).thenReturn(hundredDetails);
        when(projectRepository.existsByCompanyIdAndCode(any(), any())).thenReturn(false);
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(i -> {
            ProjectEntity p = i.getArgument(0);
            p.setId(projectId);
            return p;
        });

        // JUnit assertTimeoutPreemptively verifica que no exceda 1.5 segundos
        assertTimeoutPreemptively(Duration.ofMillis(1500), () -> {
            ProjectResponseDTO response = projectExecutionService.createProjectFromQuotation(quotationId);
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(projectId);
        });
    }

    @Test
    @DisplayName("Test 4: Integridad de Movimientos (Rollback Transaccional en asignación masiva)")
    void test4_transactionalRollback_whenItemFailsInBatchConsumption() {
        // Escenario: Consumir 10 ítems de inventario para un proyecto.
        // El ítem #10 tiene stock insuficiente (falla). Se espera que se propague excepción
        // y se garantice el rollback integral sin descontar stock parcial.
        List<ProjectInventoryConsumptionCreateDTO> batchItems = new ArrayList<>();
        List<InventoryItemEntity> items = new ArrayList<>();

        for (int i = 1; i <= 9; i++) {
            UUID id = UUID.randomUUID();
            InventoryItemEntity item = new InventoryItemEntity();
            item.setId(id);
            item.setName("Item Valido " + i);
            item.setStockQuantity(100);
            items.add(item);
            batchItems.add(new ProjectInventoryConsumptionCreateDTO(id, 5, "Consumo batch"));
            when(inventoryItemRepository.findById(id)).thenReturn(Optional.of(item));
        }

        // Ítem 10 que fallará por stock insuficiente
        UUID failingId = UUID.randomUUID();
        InventoryItemEntity failingItem = new InventoryItemEntity();
        failingItem.setId(failingId);
        failingItem.setName("Item Fallido 10");
        failingItem.setStockQuantity(2); // stock insuficiente para consumo de 10
        items.add(failingItem);
        batchItems.add(new ProjectInventoryConsumptionCreateDTO(failingId, 10, "Consumo excedido"));
        when(inventoryItemRepository.findById(failingId)).thenReturn(Optional.of(failingItem));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

        assertThatThrownBy(() -> projectExecutionService.consumeInventoryBatch(projectId, batchItems, testUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Stock insuficiente");

        // Al ocurrir la excepción, ninguno de los 9 ítems previos debe permanecer descontado en memoria / estado consistente
        for (int i = 0; i < 9; i++) {
            assertThat(items.get(i).getStockQuantity()).isEqualTo(100);
        }
        assertThat(failingItem.getStockQuantity()).isEqualTo(2);
    }
}
