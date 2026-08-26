package com.sepisac.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("2. HR & Supplier Module Entities - JPA Mapping Tests")
class HrAndSupplierEntitiesTest {

    @Nested
    @DisplayName("EmployeeEntity (Table: employees)")
    class EmployeeEntityTests {

        @Test
        @DisplayName("Should be mapped to 'employees' table with soft-delete SQL restriction")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(EmployeeEntity.class).hasAnnotation(Entity.class);

            Table table = EmployeeEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("employees");

            SQLRestriction sqlRestriction = EmployeeEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should configure ID as UUID")
        void shouldHaveValidIdMapping() throws Exception {
            Field idField = EmployeeEntity.class.getDeclaredField("id");
            assertThat(idField.getType()).isEqualTo(UUID.class);
            assertThat(idField.isAnnotationPresent(Id.class)).isTrue();

            GeneratedValue gen = idField.getAnnotation(GeneratedValue.class);
            assertThat(gen).isNotNull();
            assertThat(gen.strategy()).isEqualTo(GenerationType.UUID);
        }

        @Test
        @DisplayName("Should map relations lazily to CompanyEntity and optional UserEntity")
        void shouldHaveValidRelations() throws Exception {
            Field companyField = EmployeeEntity.class.getDeclaredField("company");
            assertThat(companyField.getType()).isEqualTo(CompanyEntity.class);
            ManyToOne companyManyToOne = companyField.getAnnotation(ManyToOne.class);
            assertThat(companyManyToOne).isNotNull();
            assertThat(companyManyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn companyJoin = companyField.getAnnotation(JoinColumn.class);
            assertThat(companyJoin).isNotNull();
            assertThat(companyJoin.name()).isEqualTo("company_id");
            assertThat(companyJoin.nullable()).isFalse();

            Field userField = EmployeeEntity.class.getDeclaredField("user");
            assertThat(userField.getType()).isEqualTo(UserEntity.class);
            ManyToOne userManyToOne = userField.getAnnotation(ManyToOne.class);
            assertThat(userManyToOne).isNotNull();
            assertThat(userManyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn userJoin = userField.getAnnotation(JoinColumn.class);
            assertThat(userJoin).isNotNull();
            assertThat(userJoin.name()).isEqualTo("user_id");
            assertThat(userJoin.nullable()).isTrue();
        }

        @Test
        @DisplayName("Should map employee fields, salaries, and salary scales correctly")
        void shouldHaveValidColumns() throws Exception {
            Field fullNameField = EmployeeEntity.class.getDeclaredField("fullName");
            Column fnCol = fullNameField.getAnnotation(Column.class);
            assertThat(fnCol.name()).isEqualTo("full_name");
            assertThat(fnCol.nullable()).isFalse();
            assertThat(fnCol.length()).isEqualTo(150);

            Field specialtyField = EmployeeEntity.class.getDeclaredField("specialty");
            Column specCol = specialtyField.getAnnotation(Column.class);
            assertThat(specCol.name()).isEqualTo("specialty");
            assertThat(specCol.nullable()).isFalse();
            assertThat(specCol.length()).isEqualTo(100);

            Field contractTypeField = EmployeeEntity.class.getDeclaredField("contractType");
            Column ctCol = contractTypeField.getAnnotation(Column.class);
            assertThat(ctCol.name()).isEqualTo("contract_type");
            assertThat(ctCol.nullable()).isFalse();
            assertThat(ctCol.length()).isEqualTo(50);

            Field baseSalaryField = EmployeeEntity.class.getDeclaredField("baseSalary");
            assertThat(baseSalaryField.getType()).isEqualTo(BigDecimal.class);
            Column salaryCol = baseSalaryField.getAnnotation(Column.class);
            assertThat(salaryCol.name()).isEqualTo("base_salary");
            assertThat(salaryCol.precision()).isEqualTo(10);
            assertThat(salaryCol.scale()).isEqualTo(2);
            assertThat(salaryCol.nullable()).isFalse();

            Field hourlyCostField = EmployeeEntity.class.getDeclaredField("currentHourlyCost");
            assertThat(hourlyCostField.getType()).isEqualTo(BigDecimal.class);
            Column hourlyCol = hourlyCostField.getAnnotation(Column.class);
            assertThat(hourlyCol.name()).isEqualTo("current_hourly_cost");
            assertThat(hourlyCol.precision()).isEqualTo(10);
            assertThat(hourlyCol.scale()).isEqualTo(2);
            assertThat(hourlyCol.nullable()).isFalse();

            Field isAvailableField = EmployeeEntity.class.getDeclaredField("isAvailable");
            Column availCol = isAvailableField.getAnnotation(Column.class);
            assertThat(availCol.name()).isEqualTo("is_available");

            Field isDeletedField = EmployeeEntity.class.getDeclaredField("isDeleted");
            Column delCol = isDeletedField.getAnnotation(Column.class);
            assertThat(delCol.name()).isEqualTo("is_deleted");
        }

        @Test
        @DisplayName("Should execute lifecycle hooks for defaults and timestamps")
        void shouldHandleLifecycle() throws Exception {
            EmployeeEntity emp = new EmployeeEntity();

            Method prePersistMethod = Arrays.stream(EmployeeEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersistMethod.setAccessible(true);
            prePersistMethod.invoke(emp);

            assertThat(emp.getCreatedAt()).isNotNull();
            assertThat(emp.getUpdatedAt()).isNotNull();
            assertThat(emp.getIsAvailable()).isTrue();
            assertThat(emp.getIsDeleted()).isFalse();

            Method preUpdateMethod = Arrays.stream(EmployeeEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PreUpdate.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PreUpdate method found"));
            preUpdateMethod.setAccessible(true);

            OffsetDateTime before = OffsetDateTime.now().minusSeconds(10);
            emp.setUpdatedAt(before);
            preUpdateMethod.invoke(emp);
            assertThat(emp.getUpdatedAt()).isAfter(before);
        }
    }

    @Nested
    @DisplayName("SupplierEntity (Table: suppliers)")
    class SupplierEntityTests {

        @Test
        @DisplayName("Should map to 'suppliers' with tenant-unique RUC constraint and soft-delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(SupplierEntity.class).hasAnnotation(Entity.class);

            Table table = SupplierEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("suppliers");

            boolean foundConstraint = Arrays.stream(table.uniqueConstraints())
                    .anyMatch(uc -> "uq_suppliers_company_ruc".equals(uc.name())
                            && Arrays.equals(uc.columnNames(), new String[]{"company_id", "ruc"}));
            assertThat(foundConstraint).as("Should declare uq_suppliers_company_ruc").isTrue();

            SQLRestriction sqlRestriction = SupplierEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should configure ID and lazy CompanyEntity relation")
        void shouldHaveValidIdAndRelation() throws Exception {
            Field idField = SupplierEntity.class.getDeclaredField("id");
            assertThat(idField.getType()).isEqualTo(UUID.class);
            assertThat(idField.isAnnotationPresent(Id.class)).isTrue();
            GeneratedValue gen = idField.getAnnotation(GeneratedValue.class);
            assertThat(gen.strategy()).isEqualTo(GenerationType.UUID);

            Field companyField = SupplierEntity.class.getDeclaredField("company");
            assertThat(companyField.getType()).isEqualTo(CompanyEntity.class);
            ManyToOne manyToOne = companyField.getAnnotation(ManyToOne.class);
            assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn joinCol = companyField.getAnnotation(JoinColumn.class);
            assertThat(joinCol.name()).isEqualTo("company_id");
            assertThat(joinCol.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map supplier attributes and handle lifecycle")
        void shouldMapAttributesAndLifecycle() throws Exception {
            Field bnField = SupplierEntity.class.getDeclaredField("businessName");
            Column bnCol = bnField.getAnnotation(Column.class);
            assertThat(bnCol.name()).isEqualTo("business_name");
            assertThat(bnCol.nullable()).isFalse();
            assertThat(bnCol.length()).isEqualTo(150);

            Field rucField = SupplierEntity.class.getDeclaredField("ruc");
            Column rucCol = rucField.getAnnotation(Column.class);
            assertThat(rucCol.name()).isEqualTo("ruc");
            assertThat(rucCol.nullable()).isFalse();
            assertThat(rucCol.length()).isEqualTo(20);

            Field phoneField = SupplierEntity.class.getDeclaredField("contactPhone");
            Column phoneCol = phoneField.getAnnotation(Column.class);
            assertThat(phoneCol.name()).isEqualTo("contact_phone");
            assertThat(phoneCol.length()).isEqualTo(30);

            Field emailField = SupplierEntity.class.getDeclaredField("email");
            Column emailCol = emailField.getAnnotation(Column.class);
            assertThat(emailCol.name()).isEqualTo("email");
            assertThat(emailCol.length()).isEqualTo(100);

            SupplierEntity supplier = new SupplierEntity();
            Method prePersist = Arrays.stream(SupplierEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(supplier);

            assertThat(supplier.getCreatedAt()).isNotNull();
            assertThat(supplier.getUpdatedAt()).isNotNull();
            assertThat(supplier.getIsDeleted()).isFalse();
        }
    }
}
