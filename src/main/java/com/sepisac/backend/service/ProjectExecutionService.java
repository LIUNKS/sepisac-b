package com.sepisac.backend.service;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.*;
import com.sepisac.backend.repository.*;
import com.sepisac.backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectExecutionService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDIENTE", "EN_PROCESO", "COMPLETADO", "CANCELADO");

    private final ProjectRepository projectRepository;
    private final QuotationRepository quotationRepository;
    private final QuotationDetailRepository quotationDetailRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final MachineryEquipmentRepository machineryEquipmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectAssignmentRepository projectAssignmentRepository;
    private final ProjectMachineryAssignmentRepository projectMachineryAssignmentRepository;
    private final ProjectInventoryConsumptionRepository projectInventoryConsumptionRepository;
    private final UserRepository userRepository;

    @Autowired
    public ProjectExecutionService(
            ProjectRepository projectRepository,
            QuotationRepository quotationRepository,
            QuotationDetailRepository quotationDetailRepository,
            InventoryItemRepository inventoryItemRepository,
            InventoryMovementRepository inventoryMovementRepository,
            MachineryEquipmentRepository machineryEquipmentRepository,
            EmployeeRepository employeeRepository,
            ProjectAssignmentRepository projectAssignmentRepository,
            ProjectMachineryAssignmentRepository projectMachineryAssignmentRepository,
            ProjectInventoryConsumptionRepository projectInventoryConsumptionRepository,
            @Autowired(required = false) UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.quotationRepository = quotationRepository;
        this.quotationDetailRepository = quotationDetailRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.machineryEquipmentRepository = machineryEquipmentRepository;
        this.employeeRepository = employeeRepository;
        this.projectAssignmentRepository = projectAssignmentRepository;
        this.projectMachineryAssignmentRepository = projectMachineryAssignmentRepository;
        this.projectInventoryConsumptionRepository = projectInventoryConsumptionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectResponseDTO createProjectFromQuotation(UUID quotationId) {
        QuotationEntity quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + quotationId));

        if (!"APROBADA".equalsIgnoreCase(quotation.getStatus())) {
            throw new BusinessRuleException("Solo se pueden crear proyectos a partir de cotizaciones aprobadas. Estado actual: " + quotation.getStatus());
        }

        // Inspeccionar requerimientos/detalles de la cotización para preparar entorno
        List<QuotationDetailEntity> quotationDetails = quotationDetailRepository.findByQuotationId(quotationId);

        String code = "PRJ-" + quotation.getQuotationNumber();
        if (projectRepository.existsByCompanyIdAndCode(quotation.getCompany().getId(), code)) {
            code = code + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        }

        ProjectEntity project = new ProjectEntity();
        project.setCompany(quotation.getCompany());
        project.setQuotation(quotation);
        project.setCode(code);
        project.setTitle(quotation.getServiceType() + " - " + quotation.getClientName());
        project.setDescription("Proyecto generado automáticamente a partir de la cotización " + quotation.getQuotationNumber() + 
                (quotationDetails != null ? " con " + quotationDetails.size() + " ítems requeridos." : "."));
        project.setStatus("PENDIENTE");
        project.setClientName(quotation.getClientName());
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusMonths(1));

        ProjectEntity savedProject = projectRepository.save(project);
        return mapToProjectDTO(savedProject);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectResponseDTO updateProjectStatus(UUID id, ProjectStatusUpdateDTO updateDTO) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + id));

        String targetStatus = updateDTO.getStatus() != null ? updateDTO.getStatus().trim().toUpperCase() : "";
        if (!ALLOWED_STATUSES.contains(targetStatus)) {
            throw new IllegalArgumentException("Estado no permitido: " + updateDTO.getStatus() + ". Permitidos: " + ALLOWED_STATUSES);
        }

        project.setStatus(targetStatus);
        ProjectEntity updated = projectRepository.save(project);
        return mapToProjectDTO(updated);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectMachineryAssignmentResponseDTO assignMachinery(UUID projectId, ProjectMachineryAssignmentCreateDTO dto) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));

        MachineryEquipmentEntity machinery = machineryEquipmentRepository.findById(dto.getMachineryEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Maquinaria no encontrada con ID: " + dto.getMachineryEquipmentId()));

        if ("EN_MANTENIMIENTO".equalsIgnoreCase(machinery.getStatus()) || "EN_USO".equalsIgnoreCase(machinery.getStatus())) {
            throw new IllegalArgumentException("La maquinaria '" + machinery.getName() + "' no está disponible para asignación. Estado actual: " + machinery.getStatus());
        }

        machinery.setStatus("EN_USO");
        machineryEquipmentRepository.save(machinery);

        ProjectMachineryAssignmentEntity assignment = new ProjectMachineryAssignmentEntity();
        assignment.setProject(project);
        assignment.setMachineryEquipment(machinery);
        assignment.setAssignedDate(dto.getAssignedDate() != null ? dto.getAssignedDate() : LocalDate.now());
        assignment.setReturnDate(dto.getReturnDate());
        assignment.setStatus("EN_USO");

        ProjectMachineryAssignmentEntity saved = projectMachineryAssignmentRepository.save(assignment);

        return new ProjectMachineryAssignmentResponseDTO(
                saved.getId(),
                project.getId(),
                machinery.getId(),
                machinery.getCode(),
                machinery.getName(),
                saved.getAssignedDate(),
                saved.getReturnDate(),
                saved.getStatus()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectInventoryConsumptionResponseDTO consumeInventory(UUID projectId, ProjectInventoryConsumptionCreateDTO dto, UserPrincipal currentUser) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));

        InventoryItemEntity item = inventoryItemRepository.findById(dto.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de inventario no encontrado con ID: " + dto.getInventoryItemId()));

        if (item.getStockQuantity() < dto.getQuantity()) {
            throw new BusinessRuleException("Stock insuficiente para el ítem: " + item.getName() + 
                    ". Stock actual: " + item.getStockQuantity() + ", requerido: " + dto.getQuantity());
        }

        // Descontar stock dinámicamente
        item.setStockQuantity(item.getStockQuantity() - dto.getQuantity());
        InventoryItemEntity updatedItem = inventoryItemRepository.save(item);

        // Registrar movimiento de auditoría tipo SALIDA
        InventoryMovementEntity movement = new InventoryMovementEntity();
        movement.setCompany(project.getCompany());
        movement.setInventoryItem(updatedItem);
        if (currentUser != null && userRepository != null) {
            userRepository.findById(currentUser.getId()).ifPresent(movement::setUser);
        }
        movement.setMovementType("SALIDA");
        movement.setQuantityChanged(dto.getQuantity());
        String reasonText = "Consumo en Proyecto " + project.getCode();
        if (dto.getReason() != null && !dto.getReason().trim().isEmpty()) {
            reasonText = reasonText + ": " + dto.getReason().trim();
        }
        movement.setReason(reasonText);
        inventoryMovementRepository.save(movement);

        // Registrar consumo en obra
        ProjectInventoryConsumptionEntity consumption = new ProjectInventoryConsumptionEntity();
        consumption.setProject(project);
        consumption.setInventoryItem(updatedItem);
        consumption.setQuantityConsumed(dto.getQuantity());
        consumption.setConsumptionDate(OffsetDateTime.now());

        ProjectInventoryConsumptionEntity saved = projectInventoryConsumptionRepository.save(consumption);

        return new ProjectInventoryConsumptionResponseDTO(
                saved.getId(),
                project.getId(),
                updatedItem.getId(),
                updatedItem.getSku(),
                updatedItem.getName(),
                saved.getQuantityConsumed(),
                updatedItem.getStockQuantity(),
                saved.getConsumptionDate()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ProjectInventoryConsumptionResponseDTO> consumeInventoryBatch(
            UUID projectId, List<ProjectInventoryConsumptionCreateDTO> items, UserPrincipal currentUser) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));

        // Validación previa atómica de stock para garantizar consistencia antes de descontar
        for (ProjectInventoryConsumptionCreateDTO dto : items) {
            InventoryItemEntity item = inventoryItemRepository.findById(dto.getInventoryItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado con ID: " + dto.getInventoryItemId()));
            if (item.getStockQuantity() < dto.getQuantity()) {
                throw new BusinessRuleException("Stock insuficiente para el ítem: " + item.getName() + 
                        ". Stock actual: " + item.getStockQuantity() + ", requerido: " + dto.getQuantity());
            }
        }

        List<ProjectInventoryConsumptionResponseDTO> responses = new ArrayList<>();
        for (ProjectInventoryConsumptionCreateDTO dto : items) {
            responses.add(consumeInventory(projectId, dto, currentUser));
        }
        return responses;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectAssignmentResponseDTO assignEmployee(UUID projectId, ProjectAssignmentCreateDTO dto) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));

        EmployeeEntity employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + dto.getEmployeeId()));

        ProjectAssignmentEntity assignment = new ProjectAssignmentEntity();
        assignment.setProject(project);
        assignment.setEmployee(employee);
        assignment.setAssignedRole(dto.getAssignedRole());
        assignment.setAssignedDate(dto.getAssignedDate() != null ? dto.getAssignedDate() : LocalDate.now());
        assignment.setIsActive(true);

        ProjectAssignmentEntity saved = projectAssignmentRepository.save(assignment);

        return new ProjectAssignmentResponseDTO(
                saved.getId(),
                project.getId(),
                employee.getId(),
                employee.getFullName(),
                employee.getSpecialty(),
                saved.getAssignedRole(),
                saved.getAssignedDate(),
                saved.getIsActive()
        );
    }

    private ProjectResponseDTO mapToProjectDTO(ProjectEntity project) {
        ProjectResponseDTO dto = new ProjectResponseDTO();
        dto.setId(project.getId());
        dto.setCompanyId(project.getCompany().getId());
        dto.setQuotationId(project.getQuotation().getId());
        dto.setCode(project.getCode());
        dto.setTitle(project.getTitle());
        dto.setDescription(project.getDescription());
        dto.setStatus(project.getStatus());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        dto.setClientName(project.getClientName());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
        return dto;
    }
}
