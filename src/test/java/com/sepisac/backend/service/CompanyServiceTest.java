package com.sepisac.backend.service;

import com.sepisac.backend.dto.CompanyCreateDTO;
import com.sepisac.backend.dto.CompanyResponseDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.repository.CompanyRepository;
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

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService Unit Tests")
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CompanyService companyService;

    private UserPrincipal superAdminPrincipal;
    private UserPrincipal adminEmpresaPrincipal;
    private CompanyEntity testCompany;
    private UUID companyId;
    private UUID superAdminId;
    private UUID adminEmpresaId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        superAdminId = UUID.randomUUID();
        adminEmpresaId = UUID.randomUUID();

        superAdminPrincipal = new UserPrincipal(
                superAdminId,
                "superadmin@sepisac.com",
                "superadmin",
                "hashedpwd", "Test User", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPERADMIN")),
                true
        );

        adminEmpresaPrincipal = new UserPrincipal(
                adminEmpresaId,
                "admin@empresa.com",
                "admin_empresa",
                "hashedpwd", "Test User", companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")),
                true
        );

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");
        testCompany.setRuc("20123456789");
        testCompany.setSubscriptionStatus("ACTIVE");
        testCompany.setIsDeleted(false);
        testCompany.setCreatedAt(OffsetDateTime.now());
    }

    @Nested
    @DisplayName("createCompany")
    class CreateCompanyTests {

        @Test
        @DisplayName("Should create company successfully and record audit log when input is valid")
        void shouldCreateCompanySuccessfully() {
            CompanyCreateDTO request = new CompanyCreateDTO("SEPI S.A.C.", "20123456789");

            when(companyRepository.existsByRuc("20123456789")).thenReturn(false);
            when(companyRepository.save(any(CompanyEntity.class))).thenAnswer(invocation -> {
                CompanyEntity c = invocation.getArgument(0);
                c.setId(companyId);
                c.setCreatedAt(OffsetDateTime.now());
                return c;
            });

            CompanyResponseDTO response = companyService.createCompany(request, superAdminPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(companyId);
            assertThat(response.getBusinessName()).isEqualTo("SEPI S.A.C.");
            assertThat(response.getRuc()).isEqualTo("20123456789");
            assertThat(response.getSubscriptionStatus()).isEqualTo("ACTIVE");

            verify(companyRepository).existsByRuc("20123456789");
            verify(companyRepository).save(any(CompanyEntity.class));
            verify(auditLogService).log(
                    eq(companyId),
                    eq(superAdminId),
                    eq("CREATE_COMPANY"),
                    eq("COMPANIES"),
                    any()
            );
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when RUC already exists")
        void shouldThrowDuplicateResourceExceptionWhenRucExists() {
            CompanyCreateDTO request = new CompanyCreateDTO("SEPI S.A.C.", "20123456789");

            when(companyRepository.existsByRuc("20123456789")).thenReturn(true);

            assertThatThrownBy(() -> companyService.createCompany(request, superAdminPrincipal))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("20123456789");

            verify(companyRepository).existsByRuc("20123456789");
            verify(companyRepository, never()).save(any());
            verify(auditLogService, never()).log(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getCompanyById")
    class GetCompanyByIdTests {

        @Test
        @DisplayName("Should return company for SUPERADMIN even if companyId does not match user company")
        void shouldReturnCompanyForSuperAdmin() {
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));

            CompanyResponseDTO response = companyService.getCompanyById(companyId, superAdminPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(companyId);
            assertThat(response.getBusinessName()).isEqualTo("SEPI S.A.C.");
            verify(companyRepository).findById(companyId);
        }

        @Test
        @DisplayName("Should return company for ADMIN_EMPRESA when requesting their own company")
        void shouldReturnCompanyForAdminEmpresaOwnCompany() {
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));

            CompanyResponseDTO response = companyService.getCompanyById(companyId, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(companyId);
            verify(companyRepository).findById(companyId);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when ADMIN_EMPRESA requests a different company")
        void shouldThrowAccessDeniedWhenAdminEmpresaRequestsDifferentCompany() {
            UUID otherCompanyId = UUID.randomUUID();
            CompanyEntity otherCompany = new CompanyEntity();
            otherCompany.setId(otherCompanyId);
            otherCompany.setBusinessName("Otra Empresa");

            when(companyRepository.findById(otherCompanyId)).thenReturn(Optional.of(otherCompany));

            assertThatThrownBy(() -> companyService.getCompanyById(otherCompanyId, adminEmpresaPrincipal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Acceso denegado");

            verify(companyRepository).findById(otherCompanyId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when company does not exist")
        void shouldThrowResourceNotFoundExceptionWhenCompanyNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(companyRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> companyService.getCompanyById(nonExistentId, superAdminPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(nonExistentId.toString());

            verify(companyRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("getAllCompanies")
    class GetAllCompaniesTests {

        @Test
        @DisplayName("Should return list of all active companies")
        void shouldReturnAllCompanies() {
            when(companyRepository.findAll()).thenReturn(List.of(testCompany));

            List<CompanyResponseDTO> result = companyService.getAllCompanies();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(companyId);
            assertThat(result.get(0).getBusinessName()).isEqualTo("SEPI S.A.C.");
            verify(companyRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getCompaniesPaged & queryCompanies")
    class PagedCompaniesTests {

        @Test
        @DisplayName("Should return paged companies matching filters")
        void shouldReturnPagedCompanies() {
            org.springframework.data.domain.Page<CompanyEntity> page =
                    new org.springframework.data.domain.PageImpl<>(List.of(testCompany), org.springframework.data.domain.PageRequest.of(0, 10), 1);

            when(companyRepository.findAllWithFilters(eq("ACTIVE"), eq("SEPI"), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(page);

            com.sepisac.backend.dto.PageResponseDTO<CompanyResponseDTO> result =
                    companyService.getCompaniesPaged(0, 10, "SEPI", "ACTIVE", "createdAt,desc");

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPageNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return query results for SUPERADMIN using CompanyFilterDTO")
        void shouldQueryCompaniesForSuperAdmin() {
            org.springframework.data.domain.Page<CompanyEntity> page =
                    new org.springframework.data.domain.PageImpl<>(List.of(testCompany), org.springframework.data.domain.PageRequest.of(0, 10), 1);

            when(companyRepository.findAllWithFilters(any(), any(), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(page);

            com.sepisac.backend.dto.CompanyFilterDTO filter = new com.sepisac.backend.dto.CompanyFilterDTO("SEPI", "ACTIVE", 0, 10, "createdAt,desc");
            com.sepisac.backend.dto.PageResponseDTO<CompanyResponseDTO> result =
                    companyService.queryCompanies(filter, superAdminPrincipal);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when non-SUPERADMIN queries companies")
        void shouldThrowAccessDeniedWhenNonSuperAdminQueriesCompanies() {
            com.sepisac.backend.dto.CompanyFilterDTO filter = new com.sepisac.backend.dto.CompanyFilterDTO("SEPI", "ACTIVE", 0, 10, "createdAt,desc");

            assertThatThrownBy(() -> companyService.queryCompanies(filter, adminEmpresaPrincipal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("SUPERADMIN");
        }
    }
}


