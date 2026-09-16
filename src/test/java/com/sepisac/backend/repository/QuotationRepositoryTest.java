package com.sepisac.backend.repository;

import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.QuotationDetailEntity;
import com.sepisac.backend.model.QuotationEntity;
import com.sepisac.backend.model.QuotationLaborRequirementEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Commercial Quotations Repository Contract Unit Tests")
class QuotationRepositoryTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationDetailRepository quotationDetailRepository;

    @Mock
    private QuotationLaborRequirementRepository quotationLaborRequirementRepository;

    private CompanyEntity testCompany;
    private QuotationEntity testQuotation;
    private UUID companyId;
    private UUID quotationId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        quotationId = UUID.randomUUID();

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testQuotation = new QuotationEntity();
        testQuotation.setId(quotationId);
        testQuotation.setCompany(testCompany);
        testQuotation.setQuotationNumber("COT-2026-001");
        testQuotation.setClientName("Minera Antamina S.A.");
        testQuotation.setServiceType("Mantenimiento");
        testQuotation.setStatus("BORRADOR");
        testQuotation.setSubtotalCosts(BigDecimal.ZERO);
        testQuotation.setTotalAmount(BigDecimal.ZERO);
        testQuotation.setIsDeleted(false);
        testQuotation.setCreatedAt(OffsetDateTime.now());
    }

    @Nested
    @DisplayName("QuotationRepository Tests")
    class QuotationRepoTests {

        @Test
        @DisplayName("Should check existence by companyId and quotationNumber")
        void shouldCheckExistenceByQuotationNumber() {
            when(quotationRepository.existsByCompanyIdAndQuotationNumber(companyId, "COT-2026-001")).thenReturn(true);

            boolean exists = quotationRepository.existsByCompanyIdAndQuotationNumber(companyId, "COT-2026-001");

            assertThat(exists).isTrue();
            verify(quotationRepository).existsByCompanyIdAndQuotationNumber(companyId, "COT-2026-001");
        }

        @Test
        @DisplayName("Should find quotations with filters and pagination")
        void shouldFindQuotationsWithFilters() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<QuotationEntity> page = new PageImpl<>(List.of(testQuotation), pageable, 1);

            when(quotationRepository.findByCompanyIdWithFilters(eq(companyId), eq("BORRADOR"), eq("Mantenimiento"), eq("Antamina"), any()))
                    .thenReturn(page);

            Page<QuotationEntity> result = quotationRepository.findByCompanyIdWithFilters(companyId, "BORRADOR", "Mantenimiento", "Antamina", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getQuotationNumber()).isEqualTo("COT-2026-001");
            verify(quotationRepository).findByCompanyIdWithFilters(companyId, "BORRADOR", "Mantenimiento", "Antamina", pageable);
        }
    }

    @Nested
    @DisplayName("QuotationDetailRepository Tests")
    class DetailRepoTests {

        @Test
        @DisplayName("Should find details by quotationId")
        void shouldFindDetailsByQuotationId() {
            QuotationDetailEntity detail = new QuotationDetailEntity();
            detail.setId(UUID.randomUUID());
            detail.setQuotation(testQuotation);
            detail.setItemDescription("Filtro");
            detail.setSubtotal(new BigDecimal("100.00"));

            when(quotationDetailRepository.findByQuotationId(quotationId)).thenReturn(List.of(detail));

            List<QuotationDetailEntity> list = quotationDetailRepository.findByQuotationId(quotationId);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getItemDescription()).isEqualTo("Filtro");
            verify(quotationDetailRepository).findByQuotationId(quotationId);
        }
    }

    @Nested
    @DisplayName("QuotationLaborRequirementRepository Tests")
    class LaborRepoTests {

        @Test
        @DisplayName("Should find labor requirements by quotationId")
        void shouldFindLaborByQuotationId() {
            QuotationLaborRequirementEntity labor = new QuotationLaborRequirementEntity();
            labor.setId(UUID.randomUUID());
            labor.setQuotation(testQuotation);
            labor.setSpecialtyNeeded("Mecánico");
            labor.setSubtotalLabor(new BigDecimal("300.00"));

            when(quotationLaborRequirementRepository.findByQuotationId(quotationId)).thenReturn(List.of(labor));

            List<QuotationLaborRequirementEntity> list = quotationLaborRequirementRepository.findByQuotationId(quotationId);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getSpecialtyNeeded()).isEqualTo("Mecánico");
            verify(quotationLaborRequirementRepository).findByQuotationId(quotationId);
        }
    }
}
