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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotationService Unit Tests")
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationDetailRepository detailRepository;

    @Mock
    private QuotationLaborRequirementRepository laborRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private QuotationService quotationService;

    private UserPrincipal adminEmpresaPrincipal;
    private UserPrincipal superAdminPrincipal;
    private CompanyEntity testCompany;
    private QuotationEntity testQuotation;
    private UUID companyId;
    private UUID quotationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        quotationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        adminEmpresaPrincipal = new UserPrincipal(
                userId, "admin@empresa.com", "admin_empresa", "hashedpwd", companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
        );

        superAdminPrincipal = new UserPrincipal(
                UUID.randomUUID(), "super@sepisac.com", "superadmin", "hashedpwd", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPERADMIN")), true
        );

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testQuotation = new QuotationEntity();
        testQuotation.setId(quotationId);
        testQuotation.setCompany(testCompany);
        testQuotation.setQuotationNumber("COT-2026-001");
        testQuotation.setClientName("Minera Antamina S.A.");
        testQuotation.setServiceType("Mantenimiento de Maquinaria Pesada");
        testQuotation.setCurrency("PEN");
        testQuotation.setExchangeRate(new BigDecimal("1.0000"));
        testQuotation.setSubtotalCosts(BigDecimal.ZERO);
        testQuotation.setProfitMarginPercentage(new BigDecimal("20.00"));
        testQuotation.setTotalAmount(BigDecimal.ZERO);
        testQuotation.setStatus("BORRADOR");
        testQuotation.setIsDeleted(false);
        testQuotation.setCreatedAt(OffsetDateTime.now());
    }

    @Nested
    @DisplayName("createQuotation")
    class CreateQuotationTests {

        @Test
        @DisplayName("Should create basic quotation successfully with defaults")
        void shouldCreateBasicQuotationSuccessfully() {
            QuotationCreateDTO request = new QuotationCreateDTO(
                    companyId, "COT-2026-001", "Minera Antamina S.A.", "Mantenimiento",
                    null, null, null, null, null
            );

            when(quotationRepository.existsByCompanyIdAndQuotationNumber(companyId, "COT-2026-001")).thenReturn(false);
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
            when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> {
                QuotationEntity q = invocation.getArgument(0);
                q.setId(quotationId);
                q.setCreatedAt(OffsetDateTime.now());
                return q;
            });

            QuotationFullDetailResponseDTO response = quotationService.createQuotation(request, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(quotationId);
            assertThat(response.getQuotationNumber()).isEqualTo("COT-2026-001");
            assertThat(response.getCurrency()).isEqualTo("PEN");
            assertThat(response.getStatus()).isEqualTo("BORRADOR");
            assertThat(response.getProfitMarginPercentage()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(response.getSubtotalCosts()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);

            verify(quotationRepository, atLeastOnce()).save(any(QuotationEntity.class));
            verify(auditLogService).log(eq(companyId), eq(userId), eq("CREATE"), eq("COMMERCIAL_QUOTATIONS"), any());
        }

        @Test
        @DisplayName("Should create composite quotation with items and labor and exact mathematical calculations")
        void shouldCreateCompositeQuotationWithCalculations() {
            List<QuotationDetailCreateDTO> details = List.of(
                    new QuotationDetailCreateDTO("Filtro de Aceite Hidráulico", "MATERIAL", 4, new BigDecimal("150.00")),
                    new QuotationDetailCreateDTO("Camión Grúa 10TN (Día)", "EQUIPO", 2, new BigDecimal("500.00"))
            );

            List<QuotationLaborCreateDTO> labor = List.of(
                    new QuotationLaborCreateDTO("Técnico Mecánico Senior", 2, 8, new BigDecimal("45.00"))
            );

            // Subtotal items = (4 * 150) + (2 * 500) = 600 + 1000 = 1600.00
            // Subtotal labor = 2 * 8 * 45 = 720.00
            // Subtotal costs = 1600 + 720 = 2320.00
            // Margin 25% = 2320 * 0.25 = 580.00
            // Total amount = 2320 + 580 = 2900.00

            QuotationCreateDTO request = new QuotationCreateDTO(
                    companyId, "COT-2026-002", "Minera Las Bambas", "Mantenimiento Preventivo",
                    "USD", new BigDecimal("3.7500"), new BigDecimal("25.00"), details, labor
            );

            when(quotationRepository.existsByCompanyIdAndQuotationNumber(companyId, "COT-2026-002")).thenReturn(false);
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
            when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> {
                QuotationEntity q = invocation.getArgument(0);
                q.setId(quotationId);
                q.setCreatedAt(OffsetDateTime.now());
                return q;
            });
            when(detailRepository.save(any(QuotationDetailEntity.class))).thenAnswer(invocation -> {
                QuotationDetailEntity d = invocation.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(laborRepository.save(any(QuotationLaborRequirementEntity.class))).thenAnswer(invocation -> {
                QuotationLaborRequirementEntity l = invocation.getArgument(0);
                l.setId(UUID.randomUUID());
                return l;
            });

            QuotationFullDetailResponseDTO response = quotationService.createQuotation(request, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getSubtotalCosts()).isEqualByComparingTo(new BigDecimal("2320.00"));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("2900.00"));
            assertThat(response.getDetails()).hasSize(2);
            assertThat(response.getLaborRequirements()).hasSize(1);

            verify(detailRepository, times(2)).save(any(QuotationDetailEntity.class));
            verify(laborRepository, times(1)).save(any(QuotationLaborRequirementEntity.class));
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when quotationNumber exists in company")
        void shouldThrowWhenQuotationNumberExists() {
            QuotationCreateDTO request = new QuotationCreateDTO(
                    companyId, "COT-2026-001", "Cliente X", "Servicio", null, null, null, null, null
            );

            when(quotationRepository.existsByCompanyIdAndQuotationNumber(companyId, "COT-2026-001")).thenReturn(true);

            assertThatThrownBy(() -> quotationService.createQuotation(request, adminEmpresaPrincipal))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("COT-2026-001");

            verify(quotationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateQuotation")
    class UpdateQuotationTests {

        @Test
        @DisplayName("Should update quotation header in BORRADOR and recompute total if margin changes")
        void shouldUpdateQuotationInBorrador() {
            QuotationDetailEntity detail = new QuotationDetailEntity();
            detail.setId(UUID.randomUUID());
            detail.setSubtotal(new BigDecimal("1000.00"));

            testQuotation.setSubtotalCosts(new BigDecimal("1000.00"));
            testQuotation.setProfitMarginPercentage(new BigDecimal("20.00"));
            testQuotation.setTotalAmount(new BigDecimal("1200.00"));

            QuotationUpdateDTO updateDTO = new QuotationUpdateDTO(
                    "Cliente Renombrado", "Servicio Actualizado", "PEN", new BigDecimal("1.0000"), new BigDecimal("30.00")
            );

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(detailRepository.findByQuotationId(quotationId)).thenReturn(List.of(detail));
            when(laborRepository.findByQuotationId(quotationId)).thenReturn(Collections.emptyList());
            when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            QuotationResponseDTO response = quotationService.updateQuotation(quotationId, updateDTO, adminEmpresaPrincipal);

            assertThat(response.getClientName()).isEqualTo("Cliente Renombrado");
            assertThat(response.getProfitMarginPercentage()).isEqualByComparingTo(new BigDecimal("30.00"));
            // 1000 + (1000 * 0.30) = 1300.00
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1300.00"));

            verify(quotationRepository).save(testQuotation);
            verify(auditLogService).log(eq(companyId), eq(userId), eq("UPDATE"), eq("COMMERCIAL_QUOTATIONS"), any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when modifying quotation in ENVIADA or APROBADA")
        void shouldThrowWhenModifyingNonBorradorQuotation() {
            testQuotation.setStatus("ENVIADA");
            QuotationUpdateDTO updateDTO = new QuotationUpdateDTO(
                    "Cliente Modificado", "Servicio", "PEN", new BigDecimal("1.0000"), new BigDecimal("20.00")
            );

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));

            assertThatThrownBy(() -> quotationService.updateQuotation(quotationId, updateDTO, adminEmpresaPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("BORRADOR");

            verify(quotationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateQuotationStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should transition from BORRADOR to ENVIADA")
        void shouldTransitionBorradorToEnviada() {
            testQuotation.setStatus("BORRADOR");
            QuotationStatusUpdateDTO statusDTO = new QuotationStatusUpdateDTO("ENVIADA");

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            QuotationResponseDTO response = quotationService.updateQuotationStatus(quotationId, statusDTO, adminEmpresaPrincipal);

            assertThat(response.getStatus()).isEqualTo("ENVIADA");
            verify(auditLogService).log(eq(companyId), eq(userId), eq("STATUS_UPDATE"), eq("COMMERCIAL_QUOTATIONS"), any());
        }

        @Test
        @DisplayName("Should transition from ENVIADA to APROBADA")
        void shouldTransitionEnviadaToAprobada() {
            testQuotation.setStatus("ENVIADA");
            QuotationStatusUpdateDTO statusDTO = new QuotationStatusUpdateDTO("APROBADA");

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            QuotationResponseDTO response = quotationService.updateQuotationStatus(quotationId, statusDTO, adminEmpresaPrincipal);

            assertThat(response.getStatus()).isEqualTo("APROBADA");
        }

        @Test
        @DisplayName("Should transition from ENVIADA to RECHAZADA")
        void shouldTransitionEnviadaToRechazada() {
            testQuotation.setStatus("ENVIADA");
            QuotationStatusUpdateDTO statusDTO = new QuotationStatusUpdateDTO("RECHAZADA");

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(quotationRepository.save(any(QuotationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            QuotationResponseDTO response = quotationService.updateQuotationStatus(quotationId, statusDTO, adminEmpresaPrincipal);

            assertThat(response.getStatus()).isEqualTo("RECHAZADA");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException on invalid transition from BORRADOR directly to APROBADA")
        void shouldThrowOnInvalidDirectTransition() {
            testQuotation.setStatus("BORRADOR");
            QuotationStatusUpdateDTO statusDTO = new QuotationStatusUpdateDTO("APROBADA");

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));

            assertThatThrownBy(() -> quotationService.updateQuotationStatus(quotationId, statusDTO, adminEmpresaPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("BORRADOR -> APROBADA");

            verify(quotationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException on transition from terminal state APROBADA")
        void shouldThrowOnTerminalStateTransition() {
            testQuotation.setStatus("APROBADA");
            QuotationStatusUpdateDTO statusDTO = new QuotationStatusUpdateDTO("BORRADOR");

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));

            assertThatThrownBy(() -> quotationService.updateQuotationStatus(quotationId, statusDTO, adminEmpresaPrincipal))
                    .isInstanceOf(BusinessRuleException.class);

            verify(quotationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteQuotation")
    class DeleteQuotationTests {

        @Test
        @DisplayName("Should soft delete quotation in BORRADOR")
        void shouldSoftDeleteQuotation() {
            testQuotation.setStatus("BORRADOR");

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(quotationRepository.save(any(QuotationEntity.class))).thenReturn(testQuotation);

            quotationService.deleteQuotation(quotationId, adminEmpresaPrincipal);

            assertThat(testQuotation.getIsDeleted()).isTrue();
            verify(quotationRepository).save(testQuotation);
            verify(auditLogService).log(eq(companyId), eq(userId), eq("DELETE"), eq("COMMERCIAL_QUOTATIONS"), any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when attempting to delete APROBADA quotation")
        void shouldThrowWhenDeletingAprobada() {
            testQuotation.setStatus("APROBADA");

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));

            assertThatThrownBy(() -> quotationService.deleteQuotation(quotationId, adminEmpresaPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("APROBADA");

            verify(quotationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Detail Items and Labor Requirements Operations")
    class DetailAndLaborTests {

        @Test
        @DisplayName("Should add detail item and recalculate quotation totals")
        void shouldAddDetailItemAndRecalculate() {
            testQuotation.setStatus("BORRADOR");
            testQuotation.setProfitMarginPercentage(new BigDecimal("20.00"));

            QuotationDetailCreateDTO detailDTO = new QuotationDetailCreateDTO(
                    "Sensor de Presión", "REPUESTO", 2, new BigDecimal("100.00")
            );

            QuotationDetailEntity savedDetail = new QuotationDetailEntity();
            savedDetail.setId(UUID.randomUUID());
            savedDetail.setQuotation(testQuotation);
            savedDetail.setItemDescription("Sensor de Presión");
            savedDetail.setItemType("REPUESTO");
            savedDetail.setQuantity(2);
            savedDetail.setUnitPrice(new BigDecimal("100.00"));
            savedDetail.setSubtotal(new BigDecimal("200.00"));

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(detailRepository.save(any(QuotationDetailEntity.class))).thenReturn(savedDetail);
            when(detailRepository.findByQuotationId(quotationId)).thenReturn(List.of(savedDetail));
            when(laborRepository.findByQuotationId(quotationId)).thenReturn(Collections.emptyList());

            QuotationDetailResponseDTO response = quotationService.addDetail(quotationId, detailDTO, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
            // Total costs = 200.00, Margin = 20% -> Total amount = 240.00
            assertThat(testQuotation.getSubtotalCosts()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(testQuotation.getTotalAmount()).isEqualByComparingTo(new BigDecimal("240.00"));

            verify(quotationRepository).save(testQuotation);
        }

        @Test
        @DisplayName("Should add labor requirement and recalculate quotation totals")
        void shouldAddLaborAndRecalculate() {
            testQuotation.setStatus("BORRADOR");
            testQuotation.setProfitMarginPercentage(new BigDecimal("10.00"));

            QuotationLaborCreateDTO laborDTO = new QuotationLaborCreateDTO(
                    "Supervisor Eléctrico", 1, 10, new BigDecimal("50.00")
            );

            QuotationLaborRequirementEntity savedLabor = new QuotationLaborRequirementEntity();
            savedLabor.setId(UUID.randomUUID());
            savedLabor.setQuotation(testQuotation);
            savedLabor.setSpecialtyNeeded("Supervisor Eléctrico");
            savedLabor.setQuantityRequired(1);
            savedLabor.setEstimatedHours(10);
            savedLabor.setLockedHourlyCost(new BigDecimal("50.00"));
            savedLabor.setSubtotalLabor(new BigDecimal("500.00"));

            when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(testQuotation));
            when(laborRepository.save(any(QuotationLaborRequirementEntity.class))).thenReturn(savedLabor);
            when(detailRepository.findByQuotationId(quotationId)).thenReturn(Collections.emptyList());
            when(laborRepository.findByQuotationId(quotationId)).thenReturn(List.of(savedLabor));

            QuotationLaborResponseDTO response = quotationService.addLaborRequirement(quotationId, laborDTO, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getSubtotalLabor()).isEqualByComparingTo(new BigDecimal("500.00"));
            // Total costs = 500.00, Margin = 10% -> Total amount = 550.00
            assertThat(testQuotation.getSubtotalCosts()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(testQuotation.getTotalAmount()).isEqualByComparingTo(new BigDecimal("550.00"));

            verify(quotationRepository).save(testQuotation);
        }
    }
}
