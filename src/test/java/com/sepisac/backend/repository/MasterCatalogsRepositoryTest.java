package com.sepisac.backend.repository;

import com.sepisac.backend.model.CompanyEntity;
import com.sepisac.backend.model.EmployeeEntity;
import com.sepisac.backend.model.InventoryItemEntity;
import com.sepisac.backend.model.MachineryEquipmentEntity;
import com.sepisac.backend.model.SupplierEntity;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Master Catalogs Repository Contract Unit Tests")
class MasterCatalogsRepositoryTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private MachineryEquipmentRepository machineryEquipmentRepository;

    private CompanyEntity testCompany;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        testCompany = new CompanyEntity();
        testCompany.setId(companyId);
        testCompany.setBusinessName("SEPI S.A.C.");
        testCompany.setRuc("20123456789");
    }

    @Nested
    @DisplayName("EmployeeRepository Tests")
    class EmployeeRepoTests {

        @Test
        @DisplayName("Should find employees by company and filter parameters")
        void shouldFindEmployeesByCompany() {
            EmployeeEntity emp = new EmployeeEntity();
            emp.setId(UUID.randomUUID());
            emp.setCompany(testCompany);
            emp.setFullName("Carlos Mendoza");
            emp.setSpecialty("Soldador");
            emp.setContractType("PLANILLA");

            when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));

            List<EmployeeEntity> list = employeeRepository.findByCompanyId(companyId);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getFullName()).isEqualTo("Carlos Mendoza");
            verify(employeeRepository).findByCompanyId(companyId);
        }
    }

    @Nested
    @DisplayName("SupplierRepository Tests")
    class SupplierRepoTests {

        @Test
        @DisplayName("Should check existence by company and RUC")
        void shouldCheckExistenceByRuc() {
            when(supplierRepository.existsByCompanyIdAndRuc(companyId, "20100010724")).thenReturn(true);

            boolean exists = supplierRepository.existsByCompanyIdAndRuc(companyId, "20100010724");

            assertThat(exists).isTrue();
            verify(supplierRepository).existsByCompanyIdAndRuc(companyId, "20100010724");
        }
    }

    @Nested
    @DisplayName("InventoryItemRepository Tests")
    class InventoryRepoTests {

        @Test
        @DisplayName("Should check existence by company and SKU")
        void shouldCheckExistenceBySku() {
            when(inventoryItemRepository.existsByCompanyIdAndSku(companyId, "TUB-AC-2PULG")).thenReturn(true);

            boolean exists = inventoryItemRepository.existsByCompanyIdAndSku(companyId, "TUB-AC-2PULG");

            assertThat(exists).isTrue();
            verify(inventoryItemRepository).existsByCompanyIdAndSku(companyId, "TUB-AC-2PULG");
        }
    }

    @Nested
    @DisplayName("MachineryEquipmentRepository Tests")
    class MachineryRepoTests {

        @Test
        @DisplayName("Should check existence by company and Code")
        void shouldCheckExistenceByCode() {
            when(machineryEquipmentRepository.existsByCompanyIdAndCode(companyId, "GEN-CAT-150KW")).thenReturn(true);

            boolean exists = machineryEquipmentRepository.existsByCompanyIdAndCode(companyId, "GEN-CAT-150KW");

            assertThat(exists).isTrue();
            verify(machineryEquipmentRepository).existsByCompanyIdAndCode(companyId, "GEN-CAT-150KW");
        }
    }
}
