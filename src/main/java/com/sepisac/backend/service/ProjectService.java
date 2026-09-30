package com.sepisac.backend.service;

import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.dto.ProjectCreateDTO;
import com.sepisac.backend.dto.ProjectResponseDTO;
import com.sepisac.backend.dto.ProjectUpdateDTO;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.ProjectEntity;
import com.sepisac.backend.model.QuotationEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.ProjectRepository;
import com.sepisac.backend.repository.QuotationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final QuotationRepository quotationRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, CompanyRepository companyRepository, QuotationRepository quotationRepository) {
        this.projectRepository = projectRepository;
        this.companyRepository = companyRepository;
        this.quotationRepository = quotationRepository;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ProjectResponseDTO> getProjects(UUID companyId, String status, String search, int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<ProjectEntity> projectPage = projectRepository.findByCompanyIdWithFilters(companyId, status, search, pageable);

        List<ProjectResponseDTO> content = projectPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        PageResponseDTO<ProjectResponseDTO> response = new PageResponseDTO<>();
        response.setContent(content);
        response.setTotalElements(projectPage.getTotalElements());
        response.setTotalPages(projectPage.getTotalPages());
        response.setPageNumber(projectPage.getNumber());
        response.setPageSize(projectPage.getSize());
        response.setLast(projectPage.isLast());

        return response;
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectById(UUID id) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + id));
        return mapToDTO(project);
    }

    @Transactional
    public ProjectResponseDTO createProject(ProjectCreateDTO createDTO) {
        if (projectRepository.existsByCompanyIdAndCode(createDTO.getCompanyId(), createDTO.getCode())) {
            throw new IllegalArgumentException("Ya existe un proyecto con ese código para la empresa");
        }

        CompanyEntity company = companyRepository.findById(createDTO.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + createDTO.getCompanyId()));
                
        QuotationEntity quotation = quotationRepository.findById(createDTO.getQuotationId())
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + createDTO.getQuotationId()));

        ProjectEntity project = new ProjectEntity();
        project.setCompany(company);
        project.setQuotation(quotation);
        project.setCode(createDTO.getCode());
        project.setTitle(createDTO.getTitle());
        project.setDescription(createDTO.getDescription());
        project.setStatus(createDTO.getStatus() != null ? createDTO.getStatus() : "EN PROGRESO");
        project.setStartDate(createDTO.getStartDate());
        project.setEndDate(createDTO.getEndDate());
        project.setClientName(createDTO.getClientName());

        ProjectEntity savedProject = projectRepository.save(project);
        return mapToDTO(savedProject);
    }

    @Transactional
    public ProjectResponseDTO updateProject(UUID id, ProjectUpdateDTO updateDTO) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + id));

        if (updateDTO.getTitle() != null) project.setTitle(updateDTO.getTitle());
        if (updateDTO.getDescription() != null) project.setDescription(updateDTO.getDescription());
        if (updateDTO.getStatus() != null) project.setStatus(updateDTO.getStatus());
        if (updateDTO.getStartDate() != null) project.setStartDate(updateDTO.getStartDate());
        if (updateDTO.getEndDate() != null) project.setEndDate(updateDTO.getEndDate());
        if (updateDTO.getClientName() != null) project.setClientName(updateDTO.getClientName());

        ProjectEntity updatedProject = projectRepository.save(project);
        return mapToDTO(updatedProject);
    }

    @Transactional
    public void deleteProject(UUID id) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + id));
        project.setIsDeleted(true);
        projectRepository.save(project);
    }

    private ProjectResponseDTO mapToDTO(ProjectEntity project) {
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
