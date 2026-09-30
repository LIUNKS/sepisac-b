package com.sepisac.backend.controller;

import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.dto.ProjectCreateDTO;
import com.sepisac.backend.dto.ProjectResponseDTO;
import com.sepisac.backend.dto.ProjectUpdateDTO;
import com.sepisac.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectController {

    private final ProjectService projectService;
    private final com.sepisac.backend.service.ProjectSeedService projectSeedService;

    @Autowired
    public ProjectController(ProjectService projectService, com.sepisac.backend.service.ProjectSeedService projectSeedService) {
        this.projectService = projectService;
        this.projectSeedService = projectSeedService;
    }

    @PostMapping("/seed/{companyId}")
    public ResponseEntity<Void> seedProjects(@PathVariable UUID companyId) {
        projectSeedService.seedProjectsData(companyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ProjectResponseDTO>> getProjects(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction) {
        
        PageResponseDTO<ProjectResponseDTO> response = projectService.getProjects(
                companyId, status, search, page, size, sort, direction);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable UUID id) {
        ProjectResponseDTO response = projectService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@Valid @RequestBody ProjectCreateDTO createDTO) {
        ProjectResponseDTO response = projectService.createProject(createDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUpdateDTO updateDTO) {
        ProjectResponseDTO response = projectService.updateProject(id, updateDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
