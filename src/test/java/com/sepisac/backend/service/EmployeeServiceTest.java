package com.sepisac.backend.service;

import com.sepisac.backend.dto.EmployeeCreateDTO;
import com.sepisac.backend.dto.EmployeeFilterDTO;
import com.sepisac.backend.dto.EmployeeResponseDTO;
import com.sepisac.backend.dto.EmployeeUpdateDTO;
import com.sepisac.backend.dto.PageResponseDTO;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.EmployeeEntity;
import com.sepisac.backend.model.UserEntity;
import com.sepisac.backend.repository.CompanyRepository;
import com.sepisac.backend.repository.EmployeeRepository;
import com.sepisac.backend.repository.UserRepository;
import com.sepisac.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
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
@DisplayName("EmployeeService Unit Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmployeeService employeeService;

    private UserPrincipal superAdminPrincipal;
    private UserPrincipal adminEmpresaPrincipal;
    private CompanyEntity testCompany;
    private EmployeeEntity testEmployee;
    private UUID companyId;
    private UUID employeeId;
    private UUID superAdminId;
    private UUID adminEmpresaId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        superAdminId = UUID.randomUUID();
        adminEmpresaId = UUID.randomUUID();

        superAdminPrincipal = new UserPrincipal(
                superAdminId, "superadmin@sepisac.com", "superadmin", "hashedpwd", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPERADMIN")), true
        );

        adminEmpresaPrincipal = new UserPrincipal(
                adminEmpresaId, "admin@empresa.com", "admin_empresa", "hashedpwd", companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
        );

        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");

        testEmployee = new EmployeeEntity();
        testEmployee.setId(employeeId);
        testEmployee.setCompany(testCompany);
        testEmployee.setFullName("Carlos Mendoza");
        testEmployee.setSpecialty("Soldador");
        testEmployee.setContractType("PLANILLA");
        testEmployee.setBaseSalary(new BigDecimal("2800.00"));
        testEmployee.setCurrentHourlyCost(new BigDecimal("35.00"));
        testEmployee.setIsAvailable(true);
        testEmployee.setIsDeleted(false);
        testEmployee.setCreatedAt(OffsetDateTime.now());
    }

    @Nested
    @DisplayName("createEmployee")
    class CreateEmployeeTests {

        @Test
        @DisplayName("Should create employee successfully and record audit log")
        void shouldCreateEmployeeSuccessfully() {
            EmployeeCreateDTO request = new EmployeeCreateDTO(
                    companyId, null, "Carlos Mendoza", "Soldador", "PLANILLA",
                    new BigDecimal("2800.00"), new BigDecimal("35.00")
            );

            when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
            when(employeeRepository.save(any(EmployeeEntity.class))).thenAnswer(invocation -> {
                EmployeeEntity e = invocation.getArgument(0);
                e.setId(employeeId);
                e.setCreatedAt(OffsetDateTime.now());
                return e;
            });

            EmployeeResponseDTO response = employeeService.createEmployee(request, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(employeeId);
            assertThat(response.getFullName()).isEqualTo("Carlos Mendoza");
            assertThat(response.getCurrentHourlyCost()).isEqualByComparingTo("35.00");
            assertThat(response.getIsAvailable()).isTrue();

            verify(companyRepository).findById(companyId);
            verify(employeeRepository).save(any(EmployeeEntity.class));
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("CREATE"), eq("HR_EMPLOYEES"), any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when company does not exist")
        void shouldThrowWhenCompanyNotFound() {
            EmployeeCreateDTO request = new EmployeeCreateDTO(
                    companyId, null, "Carlos Mendoza", "Soldador", "PLANILLA",
                    new BigDecimal("2800.00"), new BigDecimal("35.00")
            );

            when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> employeeService.createEmployee(request, superAdminPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(companyId.toString());

            verify(employeeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getEmployeeById")
    class GetEmployeeByIdTests {

        @Test
        @DisplayName("Should return employee for user in the same company")
        void shouldReturnEmployeeForSameCompany() {
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));

            EmployeeResponseDTO response = employeeService.getEmployeeById(employeeId, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(employeeId);
            assertThat(response.getFullName()).isEqualTo("Carlos Mendoza");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException for user from different company")
        void shouldThrowWhenAccessingDifferentCompany() {
            UUID otherCompanyId = UUID.randomUUID();
            UserPrincipal otherCompanyUser = new UserPrincipal(
                    UUID.randomUUID(), "other@empresa.com", "other", "pwd", otherCompanyId,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")), true
            );

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));

            assertThatThrownBy(() -> employeeService.getEmployeeById(employeeId, otherCompanyUser))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("toggleAvailability and updateEmployee")
    class UpdateAndToggleTests {

        @Test
        @DisplayName("Should toggle employee availability successfully")
        void shouldToggleAvailability() {
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
            when(employeeRepository.save(any(EmployeeEntity.class))).thenReturn(testEmployee);

            EmployeeResponseDTO response = employeeService.toggleAvailability(employeeId, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            verify(employeeRepository).save(any(EmployeeEntity.class));
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("UPDATE"), eq("HR_EMPLOYEES"), any());
        }

        @Test
        @DisplayName("Should update employee details successfully")
        void shouldUpdateEmployee() {
            EmployeeUpdateDTO updateDTO = new EmployeeUpdateDTO(
                    null, "Carlos Alberto Mendoza", "Supervisor", "PLANILLA",
                    new BigDecimal("3500.00"), new BigDecimal("45.00")
            );

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
            when(employeeRepository.save(any(EmployeeEntity.class))).thenReturn(testEmployee);

            EmployeeResponseDTO response = employeeService.updateEmployee(employeeId, updateDTO, adminEmpresaPrincipal);

            assertThat(response).isNotNull();
            verify(employeeRepository).save(testEmployee);
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("UPDATE"), eq("HR_EMPLOYEES"), any());
        }
    }

    @Nested
    @DisplayName("deleteEmployee (Soft Delete)")
    class DeleteEmployeeTests {

        @Test
        @DisplayName("Should perform soft delete on employee")
        void shouldSoftDeleteEmployee() {
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
            when(employeeRepository.save(any(EmployeeEntity.class))).thenReturn(testEmployee);

            employeeService.deleteEmployee(employeeId, adminEmpresaPrincipal);

            assertThat(testEmployee.getIsDeleted()).isTrue();
            verify(employeeRepository).save(testEmployee);
            verify(auditLogService).log(eq(companyId), eq(adminEmpresaId), eq("DELETE"), eq("HR_EMPLOYEES"), any());
        }
    }
}
