package com.sepisac.backend.controller;

import com.sepisac.backend.dto.DashboardResponseDTO;
import com.sepisac.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<DashboardResponseDTO> getDashboardData(@PathVariable UUID companyId) {
        DashboardResponseDTO response = dashboardService.getDashboardData(companyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{companyId}/seed")
    public ResponseEntity<Void> seedDashboardData(@PathVariable UUID companyId) {
        dashboardService.seedMockData(companyId);
        return ResponseEntity.ok().build();
    }
}
