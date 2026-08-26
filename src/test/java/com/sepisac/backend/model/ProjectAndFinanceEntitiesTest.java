package com.sepisac.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("5. Project & Finance Module Entities - JPA Mapping Tests")
class ProjectAndFinanceEntitiesTest {

    @Nested
    @DisplayName("ProjectEntity (Table: projects)")
    class ProjectEntityTests {

        @Test
        @DisplayName("Should map to 'projects' with unique tenant code constraint and soft-delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(ProjectEntity.class).hasAnnotation(Entity.class);

            Table table = ProjectEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("projects");

            boolean foundConstraint = Arrays.stream(table.uniqueConstraints())
                    .anyMatch(uc -> "uq_projects_company_code".equals(uc.name())
                            && Arrays.equals(uc.columnNames(), new String[]{"company_id", "code"}));
            assertThat(foundConstraint).as("Should declare uq_projects_company_code").isTrue();

            SQLRestriction sqlRestriction = ProjectEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should map relations lazily to CompanyEntity and QuotationEntity")
        void shouldHaveValidRelations() throws Exception {
            Field compField = ProjectEntity.class.getDeclaredField("company");
            ManyToOne compMany = compField.getAnnotation(ManyToOne.class);
            assertThat(compMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn compJoin = compField.getAnnotation(JoinColumn.class);
            assertThat(compJoin.name()).isEqualTo("company_id");
            assertThat(compJoin.nullable()).isFalse();

            Field qField = ProjectEntity.class.getDeclaredField("quotation");
            ManyToOne qMany = qField.getAnnotation(ManyToOne.class);
            assertThat(qMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn qJoin = qField.getAnnotation(JoinColumn.class);
            assertThat(qJoin.name()).isEqualTo("quotation_id");
            assertThat(qJoin.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map project details, dates, and lifecycle defaults")
        void shouldMapColumnsAndLifecycle() throws Exception {
            Field codeField = ProjectEntity.class.getDeclaredField("code");
            Column codeCol = codeField.getAnnotation(Column.class);
            assertThat(codeCol.name()).isEqualTo("code");
            assertThat(codeCol.nullable()).isFalse();
            assertThat(codeCol.length()).isEqualTo(50);

            Field titleField = ProjectEntity.class.getDeclaredField("title");
            Column titleCol = titleField.getAnnotation(Column.class);
            assertThat(titleCol.name()).isEqualTo("title");
            assertThat(titleCol.nullable()).isFalse();
            assertThat(titleCol.length()).isEqualTo(150);

            Field descField = ProjectEntity.class.getDeclaredField("description");
            Column descCol = descField.getAnnotation(Column.class);
            assertThat(descCol.name()).isEqualTo("description");
            assertThat(descCol.columnDefinition()).isEqualToIgnoringCase("TEXT");

            Field statusField = ProjectEntity.class.getDeclaredField("status");
            Column statusCol = statusField.getAnnotation(Column.class);
            assertThat(statusCol.name()).isEqualTo("status");
            assertThat(statusCol.length()).isEqualTo(30);

            Field startField = ProjectEntity.class.getDeclaredField("startDate");
            assertThat(startField.getType()).isEqualTo(LocalDate.class);
            Column startCol = startField.getAnnotation(Column.class);
            assertThat(startCol.name()).isEqualTo("start_date");

            Field endField = ProjectEntity.class.getDeclaredField("endDate");
            assertThat(endField.getType()).isEqualTo(LocalDate.class);
            Column endCol = endField.getAnnotation(Column.class);
            assertThat(endCol.name()).isEqualTo("end_date");

            Field clientField = ProjectEntity.class.getDeclaredField("clientName");
            Column clientCol = clientField.getAnnotation(Column.class);
            assertThat(clientCol.name()).isEqualTo("client_name");
            assertThat(clientCol.nullable()).isFalse();
            assertThat(clientCol.length()).isEqualTo(150);

            ProjectEntity proj = new ProjectEntity();
            Method prePersist = Arrays.stream(ProjectEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(proj);

            assertThat(proj.getCreatedAt()).isNotNull();
            assertThat(proj.getUpdatedAt()).isNotNull();
            assertThat(proj.getStatus()).isEqualTo("PENDIENTE");
            assertThat(proj.getIsDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("ProjectAssignmentEntity (Table: project_assignments)")
    class ProjectAssignmentEntityTests {

        @Test
        @DisplayName("Should map to 'project_assignments' without soft delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(ProjectAssignmentEntity.class).hasAnnotation(Entity.class);

            Table table = ProjectAssignmentEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("project_assignments");

            assertThat(ProjectAssignmentEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should map lazy relations to Project and Employee")
        void shouldHaveValidRelations() throws Exception {
            Field projField = ProjectAssignmentEntity.class.getDeclaredField("project");
            ManyToOne projMany = projField.getAnnotation(ManyToOne.class);
            assertThat(projMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn projJoin = projField.getAnnotation(JoinColumn.class);
            assertThat(projJoin.name()).isEqualTo("project_id");
            assertThat(projJoin.nullable()).isFalse();

            Field empField = ProjectAssignmentEntity.class.getDeclaredField("employee");
            ManyToOne empMany = empField.getAnnotation(ManyToOne.class);
            assertThat(empMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn empJoin = empField.getAnnotation(JoinColumn.class);
            assertThat(empJoin.name()).isEqualTo("employee_id");
            assertThat(empJoin.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map assignment columns and lifecycle defaults")
        void shouldMapColumns() throws Exception {
            Field roleField = ProjectAssignmentEntity.class.getDeclaredField("assignedRole");
            Column roleCol = roleField.getAnnotation(Column.class);
            assertThat(roleCol.name()).isEqualTo("assigned_role");
            assertThat(roleCol.length()).isEqualTo(100);

            Field dateField = ProjectAssignmentEntity.class.getDeclaredField("assignedDate");
            assertThat(dateField.getType()).isEqualTo(LocalDate.class);
            Column dateCol = dateField.getAnnotation(Column.class);
            assertThat(dateCol.name()).isEqualTo("assigned_date");

            Field activeField = ProjectAssignmentEntity.class.getDeclaredField("isActive");
            assertThat(activeField.getType()).isEqualTo(Boolean.class);
            Column activeCol = activeField.getAnnotation(Column.class);
            assertThat(activeCol.name()).isEqualTo("is_active");

            ProjectAssignmentEntity assignment = new ProjectAssignmentEntity();
            Method prePersist = Arrays.stream(ProjectAssignmentEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(assignment);

            assertThat(assignment.getAssignedDate()).isNotNull();
            assertThat(assignment.getIsActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("ProjectMachineryAssignmentEntity (Table: project_machinery_assignments)")
    class ProjectMachineryAssignmentEntityTests {

        @Test
        @DisplayName("Should map to 'project_machinery_assignments' without soft delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(ProjectMachineryAssignmentEntity.class).hasAnnotation(Entity.class);

            Table table = ProjectMachineryAssignmentEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("project_machinery_assignments");

            assertThat(ProjectMachineryAssignmentEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should map lazy relations to Project and MachineryEquipment")
        void shouldHaveValidRelations() throws Exception {
            Field projField = ProjectMachineryAssignmentEntity.class.getDeclaredField("project");
            ManyToOne projMany = projField.getAnnotation(ManyToOne.class);
            assertThat(projMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn projJoin = projField.getAnnotation(JoinColumn.class);
            assertThat(projJoin.name()).isEqualTo("project_id");
            assertThat(projJoin.nullable()).isFalse();

            Field mField = ProjectMachineryAssignmentEntity.class.getDeclaredField("machineryEquipment");
            ManyToOne mMany = mField.getAnnotation(ManyToOne.class);
            assertThat(mMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn mJoin = mField.getAnnotation(JoinColumn.class);
            assertThat(mJoin.name()).isEqualTo("machinery_equipment_id");
            assertThat(mJoin.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map assignment dates and status")
        void shouldMapColumns() throws Exception {
            Field assignDateField = ProjectMachineryAssignmentEntity.class.getDeclaredField("assignedDate");
            assertThat(assignDateField.getType()).isEqualTo(LocalDate.class);
            Column assignDateCol = assignDateField.getAnnotation(Column.class);
            assertThat(assignDateCol.name()).isEqualTo("assigned_date");

            Field retDateField = ProjectMachineryAssignmentEntity.class.getDeclaredField("returnDate");
            assertThat(retDateField.getType()).isEqualTo(LocalDate.class);
            Column retDateCol = retDateField.getAnnotation(Column.class);
            assertThat(retDateCol.name()).isEqualTo("return_date");

            Field statusField = ProjectMachineryAssignmentEntity.class.getDeclaredField("status");
            Column statusCol = statusField.getAnnotation(Column.class);
            assertThat(statusCol.name()).isEqualTo("status");
            assertThat(statusCol.length()).isEqualTo(30);

            ProjectMachineryAssignmentEntity assignment = new ProjectMachineryAssignmentEntity();
            Method prePersist = Arrays.stream(ProjectMachineryAssignmentEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(assignment);

            assertThat(assignment.getAssignedDate()).isNotNull();
            assertThat(assignment.getStatus()).isEqualTo("EN_USO");
        }
    }

    @Nested
    @DisplayName("ProjectInventoryConsumptionEntity (Table: project_inventory_consumptions)")
    class ProjectInventoryConsumptionEntityTests {

        @Test
        @DisplayName("Should map to 'project_inventory_consumptions' without soft delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(ProjectInventoryConsumptionEntity.class).hasAnnotation(Entity.class);

            Table table = ProjectInventoryConsumptionEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("project_inventory_consumptions");

            assertThat(ProjectInventoryConsumptionEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should map lazy relations to Project and InventoryItem")
        void shouldHaveValidRelations() throws Exception {
            Field projField = ProjectInventoryConsumptionEntity.class.getDeclaredField("project");
            ManyToOne projMany = projField.getAnnotation(ManyToOne.class);
            assertThat(projMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn projJoin = projField.getAnnotation(JoinColumn.class);
            assertThat(projJoin.name()).isEqualTo("project_id");
            assertThat(projJoin.nullable()).isFalse();

            Field itemField = ProjectInventoryConsumptionEntity.class.getDeclaredField("inventoryItem");
            ManyToOne itemMany = itemField.getAnnotation(ManyToOne.class);
            assertThat(itemMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn itemJoin = itemField.getAnnotation(JoinColumn.class);
            assertThat(itemJoin.name()).isEqualTo("inventory_item_id");
            assertThat(itemJoin.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map consumption quantity and timestamp")
        void shouldMapColumns() throws Exception {
            Field qtyField = ProjectInventoryConsumptionEntity.class.getDeclaredField("quantityConsumed");
            assertThat(qtyField.getType()).isEqualTo(Integer.class);
            Column qtyCol = qtyField.getAnnotation(Column.class);
            assertThat(qtyCol.name()).isEqualTo("quantity_consumed");
            assertThat(qtyCol.nullable()).isFalse();

            Field dateField = ProjectInventoryConsumptionEntity.class.getDeclaredField("consumptionDate");
            assertThat(dateField.getType()).isEqualTo(OffsetDateTime.class);
            Column dateCol = dateField.getAnnotation(Column.class);
            assertThat(dateCol.name()).isEqualTo("consumption_date");

            ProjectInventoryConsumptionEntity consumption = new ProjectInventoryConsumptionEntity();
            Method prePersist = Arrays.stream(ProjectInventoryConsumptionEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(consumption);

            assertThat(consumption.getConsumptionDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("InvoiceEntity (Table: invoices)")
    class InvoiceEntityTests {

        @Test
        @DisplayName("Should map to 'invoices' with unique tenant invoice number constraint and soft-delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(InvoiceEntity.class).hasAnnotation(Entity.class);

            Table table = InvoiceEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("invoices");

            boolean foundConstraint = Arrays.stream(table.uniqueConstraints())
                    .anyMatch(uc -> "uq_invoices_company_number".equals(uc.name())
                            && Arrays.equals(uc.columnNames(), new String[]{"company_id", "invoice_number"}));
            assertThat(foundConstraint).as("Should declare uq_invoices_company_number").isTrue();

            SQLRestriction sqlRestriction = InvoiceEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should map lazy relations to Company, Quotation, and optional Project")
        void shouldHaveValidRelations() throws Exception {
            Field compField = InvoiceEntity.class.getDeclaredField("company");
            ManyToOne compMany = compField.getAnnotation(ManyToOne.class);
            assertThat(compMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn compJoin = compField.getAnnotation(JoinColumn.class);
            assertThat(compJoin.name()).isEqualTo("company_id");
            assertThat(compJoin.nullable()).isFalse();

            Field qField = InvoiceEntity.class.getDeclaredField("quotation");
            ManyToOne qMany = qField.getAnnotation(ManyToOne.class);
            assertThat(qMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn qJoin = qField.getAnnotation(JoinColumn.class);
            assertThat(qJoin.name()).isEqualTo("quotation_id");
            assertThat(qJoin.nullable()).isFalse();

            Field projField = InvoiceEntity.class.getDeclaredField("project");
            ManyToOne projMany = projField.getAnnotation(ManyToOne.class);
            assertThat(projMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn projJoin = projField.getAnnotation(JoinColumn.class);
            assertThat(projJoin.name()).isEqualTo("project_id");
            assertThat(projJoin.nullable()).isTrue(); // optional project milestone billing
        }

        @Test
        @DisplayName("Should map invoice columns, currency, and lifecycle defaults")
        void shouldMapColumnsAndLifecycle() throws Exception {
            Field numField = InvoiceEntity.class.getDeclaredField("invoiceNumber");
            Column numCol = numField.getAnnotation(Column.class);
            assertThat(numCol.name()).isEqualTo("invoice_number");
            assertThat(numCol.nullable()).isFalse();
            assertThat(numCol.length()).isEqualTo(50);

            Field currField = InvoiceEntity.class.getDeclaredField("currency");
            Column currCol = currField.getAnnotation(Column.class);
            assertThat(currCol.name()).isEqualTo("currency");
            assertThat(currCol.length()).isEqualTo(10);

            Field totalField = InvoiceEntity.class.getDeclaredField("totalAmount");
            assertThat(totalField.getType()).isEqualTo(BigDecimal.class);
            Column totalCol = totalField.getAnnotation(Column.class);
            assertThat(totalCol.name()).isEqualTo("total_amount");
            assertThat(totalCol.precision()).isEqualTo(10);
            assertThat(totalCol.scale()).isEqualTo(2);
            assertThat(totalCol.nullable()).isFalse();

            Field statusField = InvoiceEntity.class.getDeclaredField("paymentStatus");
            Column statusCol = statusField.getAnnotation(Column.class);
            assertThat(statusCol.name()).isEqualTo("payment_status");
            assertThat(statusCol.length()).isEqualTo(30);

            Field issueField = InvoiceEntity.class.getDeclaredField("issueDate");
            assertThat(issueField.getType()).isEqualTo(LocalDate.class);
            Column issueCol = issueField.getAnnotation(Column.class);
            assertThat(issueCol.name()).isEqualTo("issue_date");
            assertThat(issueCol.nullable()).isFalse();

            Field dueField = InvoiceEntity.class.getDeclaredField("dueDate");
            assertThat(dueField.getType()).isEqualTo(LocalDate.class);
            Column dueCol = dueField.getAnnotation(Column.class);
            assertThat(dueCol.name()).isEqualTo("due_date");

            InvoiceEntity inv = new InvoiceEntity();
            Method prePersist = Arrays.stream(InvoiceEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(inv);

            assertThat(inv.getCreatedAt()).isNotNull();
            assertThat(inv.getUpdatedAt()).isNotNull();
            assertThat(inv.getCurrency()).isEqualTo("PEN");
            assertThat(inv.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(inv.getPaymentStatus()).isEqualTo("PENDIENTE");
            assertThat(inv.getIsDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("InvoicePaymentEntity (Table: invoice_payments)")
    class InvoicePaymentEntityTests {

        @Test
        @DisplayName("Should map to 'invoice_payments' without soft delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(InvoicePaymentEntity.class).hasAnnotation(Entity.class);

            Table table = InvoicePaymentEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("invoice_payments");

            assertThat(InvoicePaymentEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should map lazy relation to InvoiceEntity")
        void shouldHaveValidRelation() throws Exception {
            Field invField = InvoicePaymentEntity.class.getDeclaredField("invoice");
            ManyToOne invMany = invField.getAnnotation(ManyToOne.class);
            assertThat(invMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn invJoin = invField.getAnnotation(JoinColumn.class);
            assertThat(invJoin.name()).isEqualTo("invoice_id");
            assertThat(invJoin.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map payment amount, payment method, reference code and payment date")
        void shouldMapColumns() throws Exception {
            Field amountField = InvoicePaymentEntity.class.getDeclaredField("amountPaid");
            assertThat(amountField.getType()).isEqualTo(BigDecimal.class);
            Column amountCol = amountField.getAnnotation(Column.class);
            assertThat(amountCol.name()).isEqualTo("amount_paid");
            assertThat(amountCol.precision()).isEqualTo(10);
            assertThat(amountCol.scale()).isEqualTo(2);
            assertThat(amountCol.nullable()).isFalse();

            Field methodField = InvoicePaymentEntity.class.getDeclaredField("paymentMethod");
            Column methodCol = methodField.getAnnotation(Column.class);
            assertThat(methodCol.name()).isEqualTo("payment_method");
            assertThat(methodCol.nullable()).isFalse();
            assertThat(methodCol.length()).isEqualTo(50);

            Field refField = InvoicePaymentEntity.class.getDeclaredField("referenceCode");
            Column refCol = refField.getAnnotation(Column.class);
            assertThat(refCol.name()).isEqualTo("reference_code");
            assertThat(refCol.length()).isEqualTo(100);

            Field dateField = InvoicePaymentEntity.class.getDeclaredField("paymentDate");
            assertThat(dateField.getType()).isEqualTo(OffsetDateTime.class);
            Column dateCol = dateField.getAnnotation(Column.class);
            assertThat(dateCol.name()).isEqualTo("payment_date");

            InvoicePaymentEntity payment = new InvoicePaymentEntity();
            Method prePersist = Arrays.stream(InvoicePaymentEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(payment);

            assertThat(payment.getPaymentDate()).isNotNull();
        }
    }
}
