package com.sepisac.backend.service;

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
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ProjectSeedService {

    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final QuotationRepository quotationRepository;

    @Autowired
    public ProjectSeedService(ProjectRepository projectRepository, CompanyRepository companyRepository, QuotationRepository quotationRepository) {
        this.projectRepository = projectRepository;
        this.companyRepository = companyRepository;
        this.quotationRepository = quotationRepository;
    }

    @Transactional
    public void seedProjectsData(UUID companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Compañía no encontrada"));

        for (int i = 1; i <= 5; i++) {
            QuotationEntity q = new QuotationEntity();
            q.setCompany(company);
            q.setQuotationNumber("COT-2026-00" + i);
            q.setClientName("Cliente Mock " + i);
            q.setServiceType("Mantenimiento");
            q.setCurrency("PEN");
            q.setTotalAmount(new BigDecimal("1500.00").multiply(new BigDecimal(i)));
            q.setStatus("APROBADA");
            q = quotationRepository.save(q);

            ProjectEntity p = new ProjectEntity();
            p.setCompany(company);
            p.setQuotation(q);
            p.setCode("PROJ-2026-00" + i);
            p.setTitle("Proyecto de Mantenimiento " + i);
            p.setDescription("Mantenimiento general para el Cliente Mock " + i);
            p.setClientName(q.getClientName());
            p.setStatus(i % 2 == 0 ? "EN PROGRESO" : (i == 3 ? "COMPLETADO" : "PENDIENTE"));
            p.setStartDate(LocalDate.now().minusDays(10 * i));
            p.setEndDate(LocalDate.now().plusDays(20 * i));
            projectRepository.save(p);
        }
    }
}
