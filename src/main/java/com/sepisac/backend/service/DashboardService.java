package com.sepisac.backend.service;

import com.sepisac.backend.dto.DashboardResponseDTO;
import com.sepisac.backend.dto.ProjectResponseDTO;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.ProjectEntity;
import com.sepisac.backend.model.QuotationEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.ProjectRepository;
import com.sepisac.backend.repository.QuotationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final QuotationRepository quotationRepository;
    private final CompanyRepository companyRepository;

    @Autowired
    public DashboardService(ProjectRepository projectRepository, QuotationRepository quotationRepository, CompanyRepository companyRepository) {
        this.projectRepository = projectRepository;
        this.quotationRepository = quotationRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponseDTO getDashboardData(UUID companyId) {
        DashboardResponseDTO dto = new DashboardResponseDTO();

        // Obtener proyectos y cotizaciones
        List<ProjectEntity> projects = projectRepository.findByCompanyId(companyId);
        List<QuotationEntity> quotations = quotationRepository.findByCompanyId(companyId);

        // 1. Ingresos y Egresos del mes actual (simulado con cotizaciones aprobadas)
        int currentMonth = OffsetDateTime.now().getMonthValue();
        int currentYear = OffsetDateTime.now().getYear();

        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        BigDecimal ingresosMesAnterior = BigDecimal.ZERO;
        BigDecimal egresosMesAnterior = BigDecimal.ZERO;
        
        int cotizacionesAprobadasMesActual = 0;
        int cotizacionesAprobadasMesAnterior = 0;

        for (QuotationEntity q : quotations) {
            if ("APROBADO".equalsIgnoreCase(q.getStatus())) {
                if (q.getCreatedAt().getMonthValue() == currentMonth && q.getCreatedAt().getYear() == currentYear) {
                    ingresos = ingresos.add(q.getTotalAmount());
                    egresos = egresos.add(q.getSubtotalCosts());
                    cotizacionesAprobadasMesActual++;
                } else if (q.getCreatedAt().getMonthValue() == (currentMonth == 1 ? 12 : currentMonth - 1)) {
                    ingresosMesAnterior = ingresosMesAnterior.add(q.getTotalAmount());
                    egresosMesAnterior = egresosMesAnterior.add(q.getSubtotalCosts());
                    cotizacionesAprobadasMesAnterior++;
                }
            }
        }

        dto.setIngresosMensuales(ingresos);
        dto.setEgresosMensuales(egresos);
        dto.setCotizacionesAprobadas(cotizacionesAprobadasMesActual);
        
        if (ingresosMesAnterior.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal trend = ingresos.subtract(ingresosMesAnterior).divide(ingresosMesAnterior, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            dto.setIngresosTrend(trend);
        } else {
            dto.setIngresosTrend(ingresos.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100") : BigDecimal.ZERO);
        }

        if (egresosMesAnterior.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal trend = egresos.subtract(egresosMesAnterior).divide(egresosMesAnterior, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            dto.setEgresosTrend(trend);
        } else {
            dto.setEgresosTrend(egresos.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100") : BigDecimal.ZERO);
        }

        dto.setCotizacionesTrend(cotizacionesAprobadasMesActual - cotizacionesAprobadasMesAnterior);

        // 2. Proyectos Activos
        int activos = 0;
        int terminanPronto = 0;
        LocalDate inSevenDays = LocalDate.now().plusDays(7);

        int countProgreso = 0;
        int countCompletados = 0;
        int countPendientes = 0;

        for (ProjectEntity p : projects) {
            if ("EN PROGRESO".equalsIgnoreCase(p.getStatus())) {
                activos++;
                countProgreso++;
                if (p.getEndDate() != null && !p.getEndDate().isAfter(inSevenDays)) {
                    terminanPronto++;
                }
            } else if ("COMPLETADO".equalsIgnoreCase(p.getStatus())) {
                countCompletados++;
            } else {
                countPendientes++;
            }
        }
        
        dto.setProyectosActivos(activos);
        dto.setProyectosTerminanPronto(terminanPronto);

        // 3. Estado Proyectos (Pie)
        List<DashboardResponseDTO.PieData> pieData = new ArrayList<>();
        pieData.add(new DashboardResponseDTO.PieData("En progreso", countProgreso, "#0062ff"));
        pieData.add(new DashboardResponseDTO.PieData("Completados", countCompletados, "#16a34a"));
        pieData.add(new DashboardResponseDTO.PieData("Pendientes", countPendientes, "#d97706"));
        dto.setEstadoProyectos(pieData);

        // 4. Ingresos vs Egresos (Bar Chart) - Últimos 8 meses
        List<DashboardResponseDTO.ChartData> chartDataList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM", new Locale("es", "ES"));
        
        for (int i = 7; i >= 0; i--) {
            OffsetDateTime targetMonth = OffsetDateTime.now().minusMonths(i);
            BigDecimal mesIngresos = BigDecimal.ZERO;
            BigDecimal mesEgresos = BigDecimal.ZERO;

            for (QuotationEntity q : quotations) {
                if ("APROBADO".equalsIgnoreCase(q.getStatus()) && q.getCreatedAt().getMonthValue() == targetMonth.getMonthValue() && q.getCreatedAt().getYear() == targetMonth.getYear()) {
                    mesIngresos = mesIngresos.add(q.getTotalAmount());
                    mesEgresos = mesEgresos.add(q.getSubtotalCosts());
                }
            }
            // Capitalize first letter of month
            String monthName = targetMonth.format(formatter);
            monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
            chartDataList.add(new DashboardResponseDTO.ChartData(monthName, mesIngresos.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP), mesEgresos.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP)));
        }
        dto.setIngresosVsEgresos(chartDataList);

        // 5. Proyectos recientes
        List<ProjectResponseDTO> recientes = projects.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        dto.setProyectosRecientes(recientes);

        return dto;
    }

    @Transactional
    public void seedMockData(UUID companyId) {
        CompanyEntity company = companyRepository.findById(companyId).orElseThrow();
        
        // Generar 15 cotizaciones históricas
        for (int i = 0; i < 15; i++) {
            QuotationEntity q = new QuotationEntity();
            q.setCompany(company);
            q.setQuotationNumber("COT-MOCK-" + (1000 + i));
            q.setClientName(i % 2 == 0 ? "Minera Cerro Verde" : "Petroperú");
            q.setServiceType("Mantenimiento");
            
            // Random past months
            OffsetDateTime mockDate = OffsetDateTime.now().minusDays((long)(Math.random() * 200));
            q.setCreatedAt(mockDate);
            q.setUpdatedAt(mockDate);
            
            BigDecimal baseCost = new BigDecimal(20000 + (Math.random() * 50000));
            q.setSubtotalCosts(baseCost);
            q.setTotalAmount(baseCost.multiply(new BigDecimal("1.4"))); // 40% margin
            q.setStatus(i % 5 == 0 ? "RECHAZADO" : "APROBADO"); // Most approved
            
            quotationRepository.save(q);

            // Create projects for approved quotations
            if ("APROBADO".equals(q.getStatus())) {
                ProjectEntity p = new ProjectEntity();
                p.setCompany(company);
                p.setQuotation(q);
                p.setCode("PRJ-" + (1000 + i));
                p.setTitle("Proyecto " + q.getServiceType() + " " + q.getClientName());
                p.setClientName(q.getClientName());
                p.setCreatedAt(mockDate);
                
                int r = (int)(Math.random() * 10);
                if (r < 3) p.setStatus("COMPLETADO");
                else if (r < 8) p.setStatus("EN PROGRESO");
                else p.setStatus("PENDIENTE");

                p.setStartDate(mockDate.toLocalDate().plusDays(5));
                p.setEndDate(mockDate.toLocalDate().plusDays(60));
                
                projectRepository.save(p);
            }
        }
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
