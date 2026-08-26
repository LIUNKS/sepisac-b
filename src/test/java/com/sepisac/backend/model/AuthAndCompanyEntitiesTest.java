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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("1. Auth & Company Module Entities - JPA Mapping Tests")
class AuthAndCompanyEntitiesTest {

    @Nested
    @DisplayName("CompanyEntity (Table: companies)")
    class CompanyEntityTests {

        @Test
        @DisplayName("Should be mapped to 'companies' table with soft-delete SQL restriction")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(CompanyEntity.class).hasAnnotation(Entity.class);

            Table table = CompanyEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("companies");

            SQLRestriction sqlRestriction = CompanyEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should configure ID as UUID with UUID generation strategy")
        void shouldHaveValidIdMapping() throws Exception {
            Field idField = CompanyEntity.class.getDeclaredField("id");
            assertThat(idField.getType()).isEqualTo(UUID.class);
            assertThat(idField.isAnnotationPresent(Id.class)).isTrue();

            GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
            assertThat(generatedValue).isNotNull();
            assertThat(generatedValue.strategy()).isEqualTo(GenerationType.UUID);
        }

        @Test
        @DisplayName("Should map business columns with correct types, lengths, and constraints")
        void shouldHaveValidColumnMappings() throws Exception {
            Field businessNameField = CompanyEntity.class.getDeclaredField("businessName");
            assertThat(businessNameField.getType()).isEqualTo(String.class);
            Column businessNameCol = businessNameField.getAnnotation(Column.class);
            assertThat(businessNameCol).isNotNull();
            assertThat(businessNameCol.name()).isEqualTo("business_name");
            assertThat(businessNameCol.nullable()).isFalse();
            assertThat(businessNameCol.length()).isEqualTo(150);

            Field rucField = CompanyEntity.class.getDeclaredField("ruc");
            assertThat(rucField.getType()).isEqualTo(String.class);
            Column rucCol = rucField.getAnnotation(Column.class);
            assertThat(rucCol).isNotNull();
            assertThat(rucCol.name()).isEqualTo("ruc");
            assertThat(rucCol.nullable()).isFalse();
            assertThat(rucCol.length()).isEqualTo(20);

            Field subscriptionStatusField = CompanyEntity.class.getDeclaredField("subscriptionStatus");
            assertThat(subscriptionStatusField.getType()).isEqualTo(String.class);
            Column subCol = subscriptionStatusField.getAnnotation(Column.class);
            assertThat(subCol).isNotNull();
            assertThat(subCol.name()).isEqualTo("subscription_status");
            assertThat(subCol.length()).isEqualTo(30);

            Field isDeletedField = CompanyEntity.class.getDeclaredField("isDeleted");
            assertThat(isDeletedField.getType()).isEqualTo(Boolean.class);
            Column isDeletedCol = isDeletedField.getAnnotation(Column.class);
            assertThat(isDeletedCol).isNotNull();
            assertThat(isDeletedCol.name()).isEqualTo("is_deleted");

            Field createdAtField = CompanyEntity.class.getDeclaredField("createdAt");
            assertThat(createdAtField.getType()).isEqualTo(OffsetDateTime.class);
            Column createdAtCol = createdAtField.getAnnotation(Column.class);
            assertThat(createdAtCol).isNotNull();
            assertThat(createdAtCol.name()).isEqualTo("created_at");
            assertThat(createdAtCol.updatable()).isFalse();

            Field updatedAtField = CompanyEntity.class.getDeclaredField("updatedAt");
            assertThat(updatedAtField.getType()).isEqualTo(OffsetDateTime.class);
            Column updatedAtCol = updatedAtField.getAnnotation(Column.class);
            assertThat(updatedAtCol).isNotNull();
            assertThat(updatedAtCol.name()).isEqualTo("updated_at");
        }

        @Test
        @DisplayName("Should execute @PrePersist and @PreUpdate lifecycle hooks")
        void shouldHandleLifecycleHooks() throws Exception {
            CompanyEntity company = new CompanyEntity();

            Method prePersistMethod = Arrays.stream(CompanyEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersistMethod.setAccessible(true);
            prePersistMethod.invoke(company);

            assertThat(company.getCreatedAt()).isNotNull();
            assertThat(company.getUpdatedAt()).isNotNull();
            assertThat(company.getIsDeleted()).isFalse();
            assertThat(company.getSubscriptionStatus()).isEqualTo("ACTIVE");

            Method preUpdateMethod = Arrays.stream(CompanyEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PreUpdate.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PreUpdate method found"));
            preUpdateMethod.setAccessible(true);

            OffsetDateTime initialUpdated = OffsetDateTime.now().minusMinutes(5);
            company.setUpdatedAt(initialUpdated);
            preUpdateMethod.invoke(company);

            assertThat(company.getUpdatedAt()).isAfter(initialUpdated);
        }
    }

    @Nested
    @DisplayName("RoleEntity (Table: roles)")
    class RoleEntityTests {

        @Test
        @DisplayName("Should be mapped to 'roles' table without soft delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(RoleEntity.class).hasAnnotation(Entity.class);

            Table table = RoleEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("roles");

            assertThat(RoleEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should configure ID as Integer with IDENTITY generation strategy")
        void shouldHaveValidIdMapping() throws Exception {
            Field idField = RoleEntity.class.getDeclaredField("id");
            assertThat(idField.getType()).isEqualTo(Integer.class);
            assertThat(idField.isAnnotationPresent(Id.class)).isTrue();

            GeneratedValue gen = idField.getAnnotation(GeneratedValue.class);
            assertThat(gen).isNotNull();
            assertThat(gen.strategy()).isEqualTo(GenerationType.IDENTITY);
        }

        @Test
        @DisplayName("Should map role columns with correct constraints")
        void shouldHaveValidColumnMappings() throws Exception {
            Field nameField = RoleEntity.class.getDeclaredField("name");
            assertThat(nameField.getType()).isEqualTo(String.class);
            Column nameCol = nameField.getAnnotation(Column.class);
            assertThat(nameCol).isNotNull();
            assertThat(nameCol.name()).isEqualTo("name");
            assertThat(nameCol.nullable()).isFalse();
            assertThat(nameCol.length()).isEqualTo(50);
            assertThat(nameCol.unique()).isTrue();

            Field descField = RoleEntity.class.getDeclaredField("description");
            assertThat(descField.getType()).isEqualTo(String.class);
            Column descCol = descField.getAnnotation(Column.class);
            assertThat(descCol).isNotNull();
            assertThat(descCol.name()).isEqualTo("description");
            assertThat(descCol.columnDefinition()).isEqualToIgnoringCase("TEXT");
        }
    }

    @Nested
    @DisplayName("UserEntity (Table: users)")
    class UserEntityTests {

        @Test
        @DisplayName("Should be mapped to 'users' table with unique composite constraint and soft-delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(UserEntity.class).hasAnnotation(Entity.class);

            Table table = UserEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("users");

            boolean foundConstraint = Arrays.stream(table.uniqueConstraints())
                    .anyMatch(uc -> "uq_users_company_username".equals(uc.name())
                            && Arrays.equals(uc.columnNames(), new String[]{"company_id", "username"}));
            assertThat(foundConstraint).as("Should declare uq_users_company_username").isTrue();

            SQLRestriction sqlRestriction = UserEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should configure ID as UUID")
        void shouldHaveValidIdMapping() throws Exception {
            Field idField = UserEntity.class.getDeclaredField("id");
            assertThat(idField.getType()).isEqualTo(UUID.class);
            assertThat(idField.isAnnotationPresent(Id.class)).isTrue();
            GeneratedValue gen = idField.getAnnotation(GeneratedValue.class);
            assertThat(gen).isNotNull();
            assertThat(gen.strategy()).isEqualTo(GenerationType.UUID);
        }

        @Test
        @DisplayName("Should map relations lazily to CompanyEntity and RoleEntity")
        void shouldHaveValidRelations() throws Exception {
            Field companyField = UserEntity.class.getDeclaredField("company");
            assertThat(companyField.getType()).isEqualTo(CompanyEntity.class);
            ManyToOne companyManyToOne = companyField.getAnnotation(ManyToOne.class);
            assertThat(companyManyToOne).isNotNull();
            assertThat(companyManyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn companyJoin = companyField.getAnnotation(JoinColumn.class);
            assertThat(companyJoin).isNotNull();
            assertThat(companyJoin.name()).isEqualTo("company_id");
            assertThat(companyJoin.nullable()).isTrue(); // nullable for superadmin

            Field roleField = UserEntity.class.getDeclaredField("role");
            assertThat(roleField.getType()).isEqualTo(RoleEntity.class);
            ManyToOne roleManyToOne = roleField.getAnnotation(ManyToOne.class);
            assertThat(roleManyToOne).isNotNull();
            assertThat(roleManyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn roleJoin = roleField.getAnnotation(JoinColumn.class);
            assertThat(roleJoin).isNotNull();
            assertThat(roleJoin.name()).isEqualTo("role_id");
        }

        @Test
        @DisplayName("Should map column attributes correctly")
        void shouldHaveValidColumns() throws Exception {
            Field usernameField = UserEntity.class.getDeclaredField("username");
            Column usernameCol = usernameField.getAnnotation(Column.class);
            assertThat(usernameCol.name()).isEqualTo("username");
            assertThat(usernameCol.nullable()).isFalse();
            assertThat(usernameCol.length()).isEqualTo(50);

            Field emailField = UserEntity.class.getDeclaredField("email");
            Column emailCol = emailField.getAnnotation(Column.class);
            assertThat(emailCol.name()).isEqualTo("email");
            assertThat(emailCol.nullable()).isFalse();
            assertThat(emailCol.length()).isEqualTo(100);
            assertThat(emailCol.unique()).isTrue();

            Field passwordHashField = UserEntity.class.getDeclaredField("passwordHash");
            Column pwdCol = passwordHashField.getAnnotation(Column.class);
            assertThat(pwdCol.name()).isEqualTo("password_hash");
            assertThat(pwdCol.nullable()).isFalse();
            assertThat(pwdCol.length()).isEqualTo(255);

            Field fullNameField = UserEntity.class.getDeclaredField("fullName");
            Column fnCol = fullNameField.getAnnotation(Column.class);
            assertThat(fnCol.name()).isEqualTo("full_name");
            assertThat(fnCol.nullable()).isFalse();
            assertThat(fnCol.length()).isEqualTo(150);

            Field isActiveField = UserEntity.class.getDeclaredField("isActive");
            Column isActiveCol = isActiveField.getAnnotation(Column.class);
            assertThat(isActiveCol.name()).isEqualTo("is_active");

            Field isDeletedField = UserEntity.class.getDeclaredField("isDeleted");
            Column isDeletedCol = isDeletedField.getAnnotation(Column.class);
            assertThat(isDeletedCol.name()).isEqualTo("is_deleted");
        }

        @Test
        @DisplayName("Should set default values on lifecycle hooks")
        void shouldHandleLifecycle() throws Exception {
            UserEntity user = new UserEntity();

            Method prePersistMethod = Arrays.stream(UserEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersistMethod.setAccessible(true);
            prePersistMethod.invoke(user);

            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isNotNull();
            assertThat(user.getIsActive()).isTrue();
            assertThat(user.getIsDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("AuditLogEntity (Table: audit_logs)")
    class AuditLogEntityTests {

        @Test
        @DisplayName("Should be mapped to 'audit_logs' table without soft delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(AuditLogEntity.class).hasAnnotation(Entity.class);

            Table table = AuditLogEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("audit_logs");

            assertThat(AuditLogEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should configure ID as UUID")
        void shouldHaveValidIdMapping() throws Exception {
            Field idField = AuditLogEntity.class.getDeclaredField("id");
            assertThat(idField.getType()).isEqualTo(UUID.class);
            assertThat(idField.isAnnotationPresent(Id.class)).isTrue();
            GeneratedValue gen = idField.getAnnotation(GeneratedValue.class);
            assertThat(gen.strategy()).isEqualTo(GenerationType.UUID);
        }

        @Test
        @DisplayName("Should map relations lazily to CompanyEntity and UserEntity")
        void shouldHaveValidRelations() throws Exception {
            Field companyField = AuditLogEntity.class.getDeclaredField("company");
            assertThat(companyField.getType()).isEqualTo(CompanyEntity.class);
            ManyToOne companyManyToOne = companyField.getAnnotation(ManyToOne.class);
            assertThat(companyManyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn companyJoin = companyField.getAnnotation(JoinColumn.class);
            assertThat(companyJoin.name()).isEqualTo("company_id");

            Field userField = AuditLogEntity.class.getDeclaredField("user");
            assertThat(userField.getType()).isEqualTo(UserEntity.class);
            ManyToOne userManyToOne = userField.getAnnotation(ManyToOne.class);
            assertThat(userManyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn userJoin = userField.getAnnotation(JoinColumn.class);
            assertThat(userJoin.name()).isEqualTo("user_id");
        }

        @Test
        @DisplayName("Should map audit fields and lifecycle hook for created_at")
        void shouldMapAuditFieldsAndLifecycle() throws Exception {
            Field actionField = AuditLogEntity.class.getDeclaredField("action");
            Column actionCol = actionField.getAnnotation(Column.class);
            assertThat(actionCol.name()).isEqualTo("action");
            assertThat(actionCol.nullable()).isFalse();
            assertThat(actionCol.length()).isEqualTo(50);

            Field moduleField = AuditLogEntity.class.getDeclaredField("moduleAffected");
            Column moduleCol = moduleField.getAnnotation(Column.class);
            assertThat(moduleCol.name()).isEqualTo("module_affected");
            assertThat(moduleCol.nullable()).isFalse();
            assertThat(moduleCol.length()).isEqualTo(50);

            Field descField = AuditLogEntity.class.getDeclaredField("description");
            Column descCol = descField.getAnnotation(Column.class);
            assertThat(descCol.name()).isEqualTo("description");
            assertThat(descCol.columnDefinition()).isEqualToIgnoringCase("TEXT");

            Field createdAtField = AuditLogEntity.class.getDeclaredField("createdAt");
            Column createdAtCol = createdAtField.getAnnotation(Column.class);
            assertThat(createdAtCol.name()).isEqualTo("created_at");
            assertThat(createdAtCol.updatable()).isFalse();

            AuditLogEntity log = new AuditLogEntity();
            Method prePersistMethod = Arrays.stream(AuditLogEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersistMethod.setAccessible(true);
            prePersistMethod.invoke(log);

            assertThat(log.getCreatedAt()).isNotNull();
        }
    }
}
