package com.sepisac.backend.service;

import com.sepisac.backend.dto.*;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.QuotationDetailEntity;
import com.sepisac.backend.model.QuotationEntity;
import com.sepisac.backend.model.QuotationLaborRequirementEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.QuotationDetailRepository;
import com.sepisac.backend.repository.QuotationLaborRequirementRepository;
import com.sepisac.backend.repository.QuotationRepository;
import com.sepisac.backend.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class QuotationService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "quotationNumber", "clientName", "serviceType", "currency", "totalAmount", "status", "createdAt", "updatedAt"
    );

    private final QuotationRepository quotationRepository;
    private final QuotationDetailRepository detailRepository;
    private final QuotationLaborRequirementRepository laborRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public QuotationService(QuotationRepository quotationRepository,
                            QuotationDetailRepository detailRepository,
                            QuotationLaborRequirementRepository laborRepository,
                            CompanyRepository companyRepository,
                            AuditLogService auditLogService) {
        this.quotationRepository = quotationRepository;
        this.detailRepository = detailRepository;
        this.laborRepository = laborRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public QuotationFullDetailResponseDTO createQuotation(QuotationCreateDTO dto, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? dto.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : dto.getCompanyId());

        if (targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa de la cotización.");
        }

        if (quotationRepository.existsByCompanyIdAndQuotationNumber(targetCompanyId, dto.getQuotationNumber())) {
            throw new DuplicateResourceException("El número de cotización '" + dto.getQuotationNumber() + "' ya existe en esta empresa.");
        }

        CompanyEntity company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + targetCompanyId));

        QuotationEntity quotation = new QuotationEntity();
        quotation.setCompany(company);
        quotation.setQuotationNumber(dto.getQuotationNumber());
        quotation.setClientName(dto.getClientName());
        quotation.setServiceType(dto.getServiceType());
        quotation.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "PEN");
        quotation.setExchangeRate(dto.getExchangeRate() != null ? dto.getExchangeRate() : new BigDecimal("1.0000"));
        quotation.setProfitMarginPercentage(dto.getProfitMarginPercentage() != null ? dto.getProfitMarginPercentage() : new BigDecimal("20.00"));
        quotation.setStatus("BORRADOR");
        quotation.setIsDeleted(false);
        quotation.setSubtotalCosts(BigDecimal.ZERO);
        quotation.setTotalAmount(BigDecimal.ZERO);

        QuotationEntity savedQuotation = quotationRepository.save(quotation);

        List<QuotationDetailEntity> savedDetails = new ArrayList<>();
        BigDecimal itemsCost = BigDecimal.ZERO;
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            for (QuotationDetailCreateDTO detailDto : dto.getDetails()) {
                BigDecimal subtotal = detailDto.getUnitPrice()
                        .multiply(BigDecimal.valueOf(detailDto.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP);

                QuotationDetailEntity detail = new QuotationDetailEntity();
                detail.setQuotation(savedQuotation);
                detail.setItemDescription(detailDto.getItemDescription());
                detail.setItemType(detailDto.getItemType());
                detail.setQuantity(detailDto.getQuantity());
                detail.setUnitPrice(detailDto.getUnitPrice());
                detail.setSubtotal(subtotal);

                savedDetails.add(detailRepository.save(detail));
                itemsCost = itemsCost.add(subtotal);
            }
        }

        List<QuotationLaborRequirementEntity> savedLabor = new ArrayList<>();
        BigDecimal laborCost = BigDecimal.ZERO;
        if (dto.getLaborRequirements() != null && !dto.getLaborRequirements().isEmpty()) {
            for (QuotationLaborCreateDTO laborDto : dto.getLaborRequirements()) {
                BigDecimal subtotalLabor = laborDto.getLockedHourlyCost()
                        .multiply(BigDecimal.valueOf(laborDto.getQuantityRequired()))
                        .multiply(BigDecimal.valueOf(laborDto.getEstimatedHours()))
                        .setScale(2, RoundingMode.HALF_UP);

                QuotationLaborRequirementEntity labor = new QuotationLaborRequirementEntity();
                labor.setQuotation(savedQuotation);
                labor.setSpecialtyNeeded(laborDto.getSpecialtyNeeded());
                labor.setQuantityRequired(laborDto.getQuantityRequired());
                labor.setEstimatedHours(laborDto.getEstimatedHours());
                labor.setLockedHourlyCost(laborDto.getLockedHourlyCost());
                labor.setSubtotalLabor(subtotalLabor);

                savedLabor.add(laborRepository.save(labor));
                laborCost = laborCost.add(subtotalLabor);
            }
        }

        BigDecimal subtotalCosts = itemsCost.add(laborCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal marginMultiplier = savedQuotation.getProfitMarginPercentage().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal profitAmount = subtotalCosts.multiply(marginMultiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotalCosts.add(profitAmount).setScale(2, RoundingMode.HALF_UP);

        savedQuotation.setSubtotalCosts(subtotalCosts);
        savedQuotation.setTotalAmount(totalAmount);
        savedQuotation = quotationRepository.save(savedQuotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                targetCompanyId,
                actorId,
                "CREATE",
                "COMMERCIAL_QUOTATIONS",
                "Cotización registrada: [" + savedQuotation.getQuotationNumber() + "] para " + savedQuotation.getClientName()
        );

        return mapToFullDTO(savedQuotation, savedDetails, savedLabor);
    }

    @Transactional(readOnly = true)
    public QuotationFullDetailResponseDTO getQuotationById(UUID id, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + id));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        List<QuotationDetailEntity> details = detailRepository.findByQuotationId(id);
        List<QuotationLaborRequirementEntity> labor = laborRepository.findByQuotationId(id);

        return mapToFullDTO(quotation, details, labor);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<QuotationResponseDTO> queryQuotations(QuotationFilterDTO filter, UserPrincipal currentUser) {
        boolean isSuperAdmin = currentUser != null && "ROLE_SUPERADMIN".equals(currentUser.getRole());
        UUID targetCompanyId = isSuperAdmin ? filter.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : filter.getCompanyId());

        if (!isSuperAdmin && targetCompanyId == null) {
            throw new AccessDeniedException("Acceso denegado: Debe especificar la empresa.");
        }

        if (targetCompanyId != null) {
            validateTenantAccess(targetCompanyId, currentUser);
        }

        Pageable pageable = createPageable(filter.getPage(), filter.getSize(), filter.getSort());
        String searchParam = (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) ? filter.getSearch().trim() : null;
        String statusParam = (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) ? filter.getStatus().trim().toUpperCase() : null;
        String serviceTypeParam = (filter.getServiceType() != null && !filter.getServiceType().trim().isEmpty()) ? filter.getServiceType().trim() : null;

        Page<QuotationEntity> pageResult;
        if (targetCompanyId != null) {
            pageResult = quotationRepository.findByCompanyIdWithFilters(targetCompanyId, statusParam, serviceTypeParam, searchParam, pageable);
        } else {
            pageResult = quotationRepository.findAllWithFiltersGlobal(statusParam, serviceTypeParam, searchParam, pageable);
        }

        List<QuotationResponseDTO> content = pageResult.getContent().stream()
                .map(q -> {
                    int detailsCount = detailRepository.countByQuotationId(q.getId());
                    int laborCount = laborRepository.countByQuotationId(q.getId());
                    return mapToSummaryDTO(q, detailsCount, laborCount);
                })
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

    @Transactional
    public QuotationResponseDTO updateQuotation(UUID id, QuotationUpdateDTO dto, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + id));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        if (!"BORRADOR".equals(quotation.getStatus())) {
            throw new BusinessRuleException("Solo se pueden modificar cotizaciones en estado BORRADOR.");
        }

        quotation.setClientName(dto.getClientName());
        quotation.setServiceType(dto.getServiceType());
        if (dto.getCurrency() != null) {
            quotation.setCurrency(dto.getCurrency());
        }
        if (dto.getExchangeRate() != null) {
            quotation.setExchangeRate(dto.getExchangeRate());
        }
        if (dto.getProfitMarginPercentage() != null) {
            quotation.setProfitMarginPercentage(dto.getProfitMarginPercentage());
        }

        // Recalculate totals
        recalculateTotals(quotation);
        QuotationEntity updated = quotationRepository.save(quotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                quotation.getCompany().getId(),
                actorId,
                "UPDATE",
                "COMMERCIAL_QUOTATIONS",
                "Cabecera de cotización actualizada: [" + updated.getQuotationNumber() + "]"
        );

        int detailsCount = detailRepository.countByQuotationId(id);
        int laborCount = laborRepository.countByQuotationId(id);
        return mapToSummaryDTO(updated, detailsCount, laborCount);
    }

    @Transactional
    public QuotationResponseDTO updateQuotationStatus(UUID id, QuotationStatusUpdateDTO dto, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + id));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        String currentStatus = quotation.getStatus();
        String targetStatus = dto.getStatus().trim().toUpperCase();

        validateStatusTransition(currentStatus, targetStatus);

        quotation.setStatus(targetStatus);
        QuotationEntity updated = quotationRepository.save(quotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                quotation.getCompany().getId(),
                actorId,
                "STATUS_UPDATE",
                "COMMERCIAL_QUOTATIONS",
                "Estado de cotización [" + updated.getQuotationNumber() + "] cambiado de " + currentStatus + " a " + targetStatus
        );

        int detailsCount = detailRepository.countByQuotationId(id);
        int laborCount = laborRepository.countByQuotationId(id);
        return mapToSummaryDTO(updated, detailsCount, laborCount);
    }

    @Transactional
    public void deleteQuotation(UUID id, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + id));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        if ("APROBADA".equals(quotation.getStatus())) {
            throw new BusinessRuleException("No se puede eliminar una cotización en estado APROBADA.");
        }

        quotation.setIsDeleted(true);
        quotationRepository.save(quotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                quotation.getCompany().getId(),
                actorId,
                "DELETE",
                "COMMERCIAL_QUOTATIONS",
                "Cotización eliminada lógicamente (id: " + id + ", número: " + quotation.getQuotationNumber() + ")"
        );
    }

    // Detail items management
    @Transactional
    public QuotationDetailResponseDTO addDetail(UUID quotationId, QuotationDetailCreateDTO dto, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + quotationId));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        if (!"BORRADOR".equals(quotation.getStatus())) {
            throw new BusinessRuleException("Solo se pueden agregar ítems a cotizaciones en estado BORRADOR.");
        }

        BigDecimal subtotal = dto.getUnitPrice()
                .multiply(BigDecimal.valueOf(dto.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        QuotationDetailEntity detail = new QuotationDetailEntity();
        detail.setQuotation(quotation);
        detail.setItemDescription(dto.getItemDescription());
        detail.setItemType(dto.getItemType());
        detail.setQuantity(dto.getQuantity());
        detail.setUnitPrice(dto.getUnitPrice());
        detail.setSubtotal(subtotal);

        QuotationDetailEntity saved = detailRepository.save(detail);
        recalculateTotals(quotation);
        quotationRepository.save(quotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                quotation.getCompany().getId(),
                actorId,
                "ADD_DETAIL",
                "COMMERCIAL_QUOTATIONS",
                "Ítem agregado a cotización [" + quotation.getQuotationNumber() + "]: " + saved.getItemDescription()
        );

        return mapToDetailDTO(saved);
    }

    @Transactional
    public QuotationDetailResponseDTO updateDetail(UUID quotationId, UUID detailId, QuotationDetailUpdateDTO dto, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + quotationId));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        if (!"BORRADOR".equals(quotation.getStatus())) {
            throw new BusinessRuleException("Solo se pueden modificar ítems de cotizaciones en estado BORRADOR.");
        }

        QuotationDetailEntity detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Detalle de cotización no encontrado con id: " + detailId));

        if (!detail.getQuotation().getId().equals(quotationId)) {
            throw new BusinessRuleException("El ítem especificado no pertenece a la cotización indicada.");
        }

        BigDecimal subtotal = dto.getUnitPrice()
                .multiply(BigDecimal.valueOf(dto.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        detail.setItemDescription(dto.getItemDescription());
        detail.setItemType(dto.getItemType());
        detail.setQuantity(dto.getQuantity());
        detail.setUnitPrice(dto.getUnitPrice());
        detail.setSubtotal(subtotal);

        QuotationDetailEntity updated = detailRepository.save(detail);
        recalculateTotals(quotation);
        quotationRepository.save(quotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                quotation.getCompany().getId(),
                actorId,
                "UPDATE_DETAIL",
                "COMMERCIAL_QUOTATIONS",
                "Ítem actualizado en cotización [" + quotation.getQuotationNumber() + "]: " + updated.getItemDescription()
        );

        return mapToDetailDTO(updated);
    }

    @Transactional
    public void deleteDetail(UUID quotationId, UUID detailId, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + quotationId));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        if (!"BORRADOR".equals(quotation.getStatus())) {
            throw new BusinessRuleException("Solo se pueden eliminar ítems de cotizaciones en estado BORRADOR.");
        }

        QuotationDetailEntity detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Detalle de cotización no encontrado con id: " + detailId));

        if (!detail.getQuotation().getId().equals(quotationId)) {
            throw new BusinessRuleException("El ítem especificado no pertenece a la cotización indicada.");
        }

        detailRepository.delete(detail);
        recalculateTotals(quotation);
        quotationRepository.save(quotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                quotation.getCompany().getId(),
                actorId,
                "DELETE_DETAIL",
                "COMMERCIAL_QUOTATIONS",
                "Ítem eliminado de cotización [" + quotation.getQuotationNumber() + "]"
        );
    }

    // Labor Requirements management
    @Transactional
    public QuotationLaborResponseDTO addLaborRequirement(UUID quotationId, QuotationLaborCreateDTO dto, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + quotationId));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        if (!"BORRADOR".equals(quotation.getStatus())) {
            throw new BusinessRuleException("Solo se pueden agregar requerimientos de mano de obra en estado BORRADOR.");
        }

        BigDecimal subtotalLabor = dto.getLockedHourlyCost()
                .multiply(BigDecimal.valueOf(dto.getQuantityRequired()))
                .multiply(BigDecimal.valueOf(dto.getEstimatedHours()))
                .setScale(2, RoundingMode.HALF_UP);

        QuotationLaborRequirementEntity labor = new QuotationLaborRequirementEntity();
        labor.setQuotation(quotation);
        labor.setSpecialtyNeeded(dto.getSpecialtyNeeded());
        labor.setQuantityRequired(dto.getQuantityRequired());
        labor.setEstimatedHours(dto.getEstimatedHours());
        labor.setLockedHourlyCost(dto.getLockedHourlyCost());
        labor.setSubtotalLabor(subtotalLabor);

        QuotationLaborRequirementEntity saved = laborRepository.save(labor);
        recalculateTotals(quotation);
        quotationRepository.save(quotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                quotation.getCompany().getId(),
                actorId,
                "ADD_LABOR",
                "COMMERCIAL_QUOTATIONS",
                "Mano de obra agregada a cotización [" + quotation.getQuotationNumber() + "]: " + saved.getSpecialtyNeeded()
        );

        return mapToLaborDTO(saved);
    }

    @Transactional
    public QuotationLaborResponseDTO updateLaborRequirement(UUID quotationId, UUID laborId, QuotationLaborUpdateDTO dto, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + quotationId));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        if (!"BORRADOR".equals(quotation.getStatus())) {
            throw new BusinessRuleException("Solo se pueden modificar requerimientos de mano de obra en estado BORRADOR.");
        }

        QuotationLaborRequirementEntity labor = laborRepository.findById(laborId)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento de mano de obra no encontrado con id: " + laborId));

        if (!labor.getQuotation().getId().equals(quotationId)) {
            throw new BusinessRuleException("El requerimiento de mano de obra no pertenece a la cotización indicada.");
        }

        BigDecimal subtotalLabor = dto.getLockedHourlyCost()
                .multiply(BigDecimal.valueOf(dto.getQuantityRequired()))
                .multiply(BigDecimal.valueOf(dto.getEstimatedHours()))
                .setScale(2, RoundingMode.HALF_UP);

        labor.setSpecialtyNeeded(dto.getSpecialtyNeeded());
        labor.setQuantityRequired(dto.getQuantityRequired());
        labor.setEstimatedHours(dto.getEstimatedHours());
        labor.setLockedHourlyCost(dto.getLockedHourlyCost());
        labor.setSubtotalLabor(subtotalLabor);

        QuotationLaborRequirementEntity updated = laborRepository.save(labor);
        recalculateTotals(quotation);
        quotationRepository.save(quotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                quotation.getCompany().getId(),
                actorId,
                "UPDATE_LABOR",
                "COMMERCIAL_QUOTATIONS",
                "Mano de obra actualizada en cotización [" + quotation.getQuotationNumber() + "]: " + updated.getSpecialtyNeeded()
        );

        return mapToLaborDTO(updated);
    }

    @Transactional
    public void deleteLaborRequirement(UUID quotationId, UUID laborId, UserPrincipal currentUser) {
        QuotationEntity quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con id: " + quotationId));

        validateTenantAccess(quotation.getCompany().getId(), currentUser);

        if (!"BORRADOR".equals(quotation.getStatus())) {
            throw new BusinessRuleException("Solo se pueden eliminar requerimientos de mano de obra en estado BORRADOR.");
        }

        QuotationLaborRequirementEntity labor = laborRepository.findById(laborId)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento de mano de obra no encontrado con id: " + laborId));

        if (!labor.getQuotation().getId().equals(quotationId)) {
            throw new BusinessRuleException("El requerimiento de mano de obra no pertenece a la cotización indicada.");
        }

        laborRepository.delete(labor);
        recalculateTotals(quotation);
        quotationRepository.save(quotation);

        UUID actorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(
                quotation.getCompany().getId(),
                actorId,
                "DELETE_LABOR",
                "COMMERCIAL_QUOTATIONS",
                "Mano de obra eliminada de cotización [" + quotation.getQuotationNumber() + "]"
        );
    }

    private void recalculateTotals(QuotationEntity quotation) {
        List<QuotationDetailEntity> details = detailRepository.findByQuotationId(quotation.getId());
        List<QuotationLaborRequirementEntity> labor = laborRepository.findByQuotationId(quotation.getId());

        BigDecimal itemsCost = details.stream()
                .map(QuotationDetailEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal laborCost = labor.stream()
                .map(QuotationLaborRequirementEntity::getSubtotalLabor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal subtotalCosts = itemsCost.add(laborCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal marginMultiplier = quotation.getProfitMarginPercentage() != null
                ? quotation.getProfitMarginPercentage().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal profitAmount = subtotalCosts.multiply(marginMultiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotalCosts.add(profitAmount).setScale(2, RoundingMode.HALF_UP);

        quotation.setSubtotalCosts(subtotalCosts);
        quotation.setTotalAmount(totalAmount);
    }

    private void validateStatusTransition(String currentStatus, String targetStatus) {
        if ("APROBADA".equals(currentStatus) || "RECHAZADA".equals(currentStatus)) {
            throw new BusinessRuleException("No se puede cambiar el estado de una cotización en estado terminal: " + currentStatus);
        }

        if ("BORRADOR".equals(currentStatus)) {
            if (!"ENVIADA".equals(targetStatus)) {
                throw new BusinessRuleException("Transición de estado no permitida: " + currentStatus + " -> " + targetStatus + ". Solo puede pasar a ENVIADA.");
            }
        } else if ("ENVIADA".equals(currentStatus)) {
            if (!"APROBADA".equals(targetStatus) && !"RECHAZADA".equals(targetStatus)) {
                throw new BusinessRuleException("Transición de estado no permitida: " + currentStatus + " -> " + targetStatus + ". Solo puede pasar a APROBADA o RECHAZADA.");
            }
        }
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

    private QuotationResponseDTO mapToSummaryDTO(QuotationEntity q, int detailsCount, int laborCount) {
        UUID companyId = q.getCompany() != null ? q.getCompany().getId() : null;
        String companyName = q.getCompany() != null ? q.getCompany().getBusinessName() : null;

        return new QuotationResponseDTO(
                q.getId(),
                companyId,
                companyName,
                q.getQuotationNumber(),
                q.getClientName(),
                q.getServiceType(),
                q.getCurrency(),
                q.getExchangeRate(),
                q.getSubtotalCosts(),
                q.getProfitMarginPercentage(),
                q.getTotalAmount(),
                q.getStatus(),
                detailsCount,
                laborCount,
                q.getCreatedAt()
        );
    }

    private QuotationFullDetailResponseDTO mapToFullDTO(QuotationEntity q, List<QuotationDetailEntity> details, List<QuotationLaborRequirementEntity> labor) {
        UUID companyId = q.getCompany() != null ? q.getCompany().getId() : null;
        String companyName = q.getCompany() != null ? q.getCompany().getBusinessName() : null;

        List<QuotationDetailResponseDTO> detailDTOs = details != null
                ? details.stream().map(this::mapToDetailDTO).toList()
                : new ArrayList<>();

        List<QuotationLaborResponseDTO> laborDTOs = labor != null
                ? labor.stream().map(this::mapToLaborDTO).toList()
                : new ArrayList<>();

        return new QuotationFullDetailResponseDTO(
                q.getId(),
                companyId,
                companyName,
                q.getQuotationNumber(),
                q.getClientName(),
                q.getServiceType(),
                q.getCurrency(),
                q.getExchangeRate(),
                q.getSubtotalCosts(),
                q.getProfitMarginPercentage(),
                q.getTotalAmount(),
                q.getStatus(),
                q.getCreatedAt(),
                detailDTOs,
                laborDTOs
        );
    }

    private QuotationDetailResponseDTO mapToDetailDTO(QuotationDetailEntity d) {
        return new QuotationDetailResponseDTO(
                d.getId(),
                d.getQuotation() != null ? d.getQuotation().getId() : null,
                d.getItemDescription(),
                d.getItemType(),
                d.getQuantity(),
                d.getUnitPrice(),
                d.getSubtotal()
        );
    }

    private QuotationLaborResponseDTO mapToLaborDTO(QuotationLaborRequirementEntity l) {
        return new QuotationLaborResponseDTO(
                l.getId(),
                l.getQuotation() != null ? l.getQuotation().getId() : null,
                l.getSpecialtyNeeded(),
                l.getQuantityRequired(),
                l.getEstimatedHours(),
                l.getLockedHourlyCost(),
                l.getSubtotalLabor()
        );
    }
}
