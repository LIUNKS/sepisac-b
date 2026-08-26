package com.sepisac.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("4. Machinery & Commercial Module Entities - JPA Mapping Tests")
class MachineryAndCommercialEntitiesTest {

    @Nested
    @DisplayName("MachineryEquipmentEntity (Table: machinery_equipment)")
    class MachineryEquipmentEntityTests {

        @Test
        @DisplayName("Should map to 'machinery_equipment' with unique tenant code constraint and soft-delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(MachineryEquipmentEntity.class).hasAnnotation(Entity.class);

            Table table = MachineryEquipmentEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("machinery_equipment");

            boolean foundConstraint = Arrays.stream(table.uniqueConstraints())
                    .anyMatch(uc -> "uq_machinery_company_code".equals(uc.name())
                            && Arrays.equals(uc.columnNames(), new String[]{"company_id", "code"}));
            assertThat(foundConstraint).as("Should declare uq_machinery_company_code").isTrue();

            SQLRestriction sqlRestriction = MachineryEquipmentEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should map ID and lazy Company relation")
        void shouldHaveValidIdAndRelation() throws Exception {
            Field idField = MachineryEquipmentEntity.class.getDeclaredField("id");
            assertThat(idField.getType()).isEqualTo(UUID.class);
            GeneratedValue gen = idField.getAnnotation(GeneratedValue.class);
            assertThat(gen.strategy()).isEqualTo(GenerationType.UUID);

            Field compField = MachineryEquipmentEntity.class.getDeclaredField("company");
            ManyToOne manyToOne = compField.getAnnotation(ManyToOne.class);
            assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn joinCol = compField.getAnnotation(JoinColumn.class);
            assertThat(joinCol.name()).isEqualTo("company_id");
            assertThat(joinCol.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map machinery fields, maintenance dates, and status")
        void shouldMapColumns() throws Exception {
            Field codeField = MachineryEquipmentEntity.class.getDeclaredField("code");
            Column codeCol = codeField.getAnnotation(Column.class);
            assertThat(codeCol.name()).isEqualTo("code");
            assertThat(codeCol.nullable()).isFalse();
            assertThat(codeCol.length()).isEqualTo(50);

            Field nameField = MachineryEquipmentEntity.class.getDeclaredField("name");
            Column nameCol = nameField.getAnnotation(Column.class);
            assertThat(nameCol.name()).isEqualTo("name");
            assertThat(nameCol.nullable()).isFalse();
            assertThat(nameCol.length()).isEqualTo(150);

            Field statusField = MachineryEquipmentEntity.class.getDeclaredField("status");
            Column statusCol = statusField.getAnnotation(Column.class);
            assertThat(statusCol.name()).isEqualTo("status");
            assertThat(statusCol.length()).isEqualTo(30);

            Field lastMaintField = MachineryEquipmentEntity.class.getDeclaredField("lastMaintenanceDate");
            assertThat(lastMaintField.getType()).isEqualTo(LocalDate.class);
            Column lastMaintCol = lastMaintField.getAnnotation(Column.class);
            assertThat(lastMaintCol.name()).isEqualTo("last_maintenance_date");

            Field nextMaintField = MachineryEquipmentEntity.class.getDeclaredField("nextMaintenanceDate");
            assertThat(nextMaintField.getType()).isEqualTo(LocalDate.class);
            Column nextMaintCol = nextMaintField.getAnnotation(Column.class);
            assertThat(nextMaintCol.name()).isEqualTo("next_maintenance_date");
        }

        @Test
        @DisplayName("Should execute lifecycle hooks for defaults")
        void shouldHandleLifecycle() throws Exception {
            MachineryEquipmentEntity machinery = new MachineryEquipmentEntity();

            Method prePersist = Arrays.stream(MachineryEquipmentEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(machinery);

            assertThat(machinery.getCreatedAt()).isNotNull();
            assertThat(machinery.getUpdatedAt()).isNotNull();
            assertThat(machinery.getStatus()).isEqualTo("DISPONIBLE");
            assertThat(machinery.getIsDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("QuotationEntity (Table: quotations)")
    class QuotationEntityTests {

        @Test
        @DisplayName("Should map to 'quotations' with unique tenant quotation number constraint and soft-delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(QuotationEntity.class).hasAnnotation(Entity.class);

            Table table = QuotationEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("quotations");

            boolean foundConstraint = Arrays.stream(table.uniqueConstraints())
                    .anyMatch(uc -> "uq_quotations_company_number".equals(uc.name())
                            && Arrays.equals(uc.columnNames(), new String[]{"company_id", "quotation_number"}));
            assertThat(foundConstraint).as("Should declare uq_quotations_company_number").isTrue();

            SQLRestriction sqlRestriction = QuotationEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should map relations and ID")
        void shouldHaveValidIdAndRelations() throws Exception {
            Field idField = QuotationEntity.class.getDeclaredField("id");
            assertThat(idField.getType()).isEqualTo(UUID.class);
            GeneratedValue gen = idField.getAnnotation(GeneratedValue.class);
            assertThat(gen.strategy()).isEqualTo(GenerationType.UUID);

            Field compField = QuotationEntity.class.getDeclaredField("company");
            ManyToOne manyToOne = compField.getAnnotation(ManyToOne.class);
            assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn joinCol = compField.getAnnotation(JoinColumn.class);
            assertThat(joinCol.name()).isEqualTo("company_id");
            assertThat(joinCol.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map commercial columns, costs, margins, and statuses")
        void shouldMapColumns() throws Exception {
            Field qNumField = QuotationEntity.class.getDeclaredField("quotationNumber");
            Column qNumCol = qNumField.getAnnotation(Column.class);
            assertThat(qNumCol.name()).isEqualTo("quotation_number");
            assertThat(qNumCol.nullable()).isFalse();
            assertThat(qNumCol.length()).isEqualTo(50);

            Field clientField = QuotationEntity.class.getDeclaredField("clientName");
            Column clientCol = clientField.getAnnotation(Column.class);
            assertThat(clientCol.name()).isEqualTo("client_name");
            assertThat(clientCol.nullable()).isFalse();
            assertThat(clientCol.length()).isEqualTo(150);

            Field svcField = QuotationEntity.class.getDeclaredField("serviceType");
            Column svcCol = svcField.getAnnotation(Column.class);
            assertThat(svcCol.name()).isEqualTo("service_type");
            assertThat(svcCol.nullable()).isFalse();
            assertThat(svcCol.length()).isEqualTo(50);

            Field costField = QuotationEntity.class.getDeclaredField("subtotalCosts");
            assertThat(costField.getType()).isEqualTo(BigDecimal.class);
            Column costCol = costField.getAnnotation(Column.class);
            assertThat(costCol.name()).isEqualTo("subtotal_costs");
            assertThat(costCol.precision()).isEqualTo(10);
            assertThat(costCol.scale()).isEqualTo(2);

            Field marginField = QuotationEntity.class.getDeclaredField("profitMarginPercentage");
            assertThat(marginField.getType()).isEqualTo(BigDecimal.class);
            Column marginCol = marginField.getAnnotation(Column.class);
            assertThat(marginCol.name()).isEqualTo("profit_margin_percentage");
            assertThat(marginCol.precision()).isEqualTo(5);
            assertThat(marginCol.scale()).isEqualTo(2);

            Field totalField = QuotationEntity.class.getDeclaredField("totalAmount");
            assertThat(totalField.getType()).isEqualTo(BigDecimal.class);
            Column totalCol = totalField.getAnnotation(Column.class);
            assertThat(totalCol.name()).isEqualTo("total_amount");
            assertThat(totalCol.precision()).isEqualTo(10);
            assertThat(totalCol.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should initialize default financial fields on lifecycle hook")
        void shouldHandleLifecycle() throws Exception {
            QuotationEntity q = new QuotationEntity();

            Method prePersist = Arrays.stream(QuotationEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(q);

            assertThat(q.getCreatedAt()).isNotNull();
            assertThat(q.getUpdatedAt()).isNotNull();
            assertThat(q.getCurrency()).isEqualTo("PEN");
            assertThat(q.getExchangeRate()).isEqualByComparingTo(new BigDecimal("1.0000"));
            assertThat(q.getSubtotalCosts()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(q.getProfitMarginPercentage()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(q.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(q.getStatus()).isEqualTo("BORRADOR");
            assertThat(q.getIsDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("QuotationDetailEntity (Table: quotation_details)")
    class QuotationDetailEntityTests {

        @Test
        @DisplayName("Should map to 'quotation_details' without soft delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(QuotationDetailEntity.class).hasAnnotation(Entity.class);

            Table table = QuotationDetailEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("quotation_details");

            assertThat(QuotationDetailEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should map lazy Quotation relation and line item columns")
        void shouldMapRelationAndColumns() throws Exception {
            Field qField = QuotationDetailEntity.class.getDeclaredField("quotation");
            ManyToOne manyToOne = qField.getAnnotation(ManyToOne.class);
            assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn joinCol = qField.getAnnotation(JoinColumn.class);
            assertThat(joinCol.name()).isEqualTo("quotation_id");
            assertThat(joinCol.nullable()).isFalse();

            Field descField = QuotationDetailEntity.class.getDeclaredField("itemDescription");
            Column descCol = descField.getAnnotation(Column.class);
            assertThat(descCol.name()).isEqualTo("item_description");
            assertThat(descCol.nullable()).isFalse();
            assertThat(descCol.length()).isEqualTo(255);

            Field typeField = QuotationDetailEntity.class.getDeclaredField("itemType");
            Column typeCol = typeField.getAnnotation(Column.class);
            assertThat(typeCol.name()).isEqualTo("item_type");
            assertThat(typeCol.nullable()).isFalse();
            assertThat(typeCol.length()).isEqualTo(30);

            Field qtyField = QuotationDetailEntity.class.getDeclaredField("quantity");
            assertThat(qtyField.getType()).isEqualTo(Integer.class);
            Column qtyCol = qtyField.getAnnotation(Column.class);
            assertThat(qtyCol.name()).isEqualTo("quantity");
            assertThat(qtyCol.nullable()).isFalse();

            Field priceField = QuotationDetailEntity.class.getDeclaredField("unitPrice");
            assertThat(priceField.getType()).isEqualTo(BigDecimal.class);
            Column priceCol = priceField.getAnnotation(Column.class);
            assertThat(priceCol.name()).isEqualTo("unit_price");
            assertThat(priceCol.precision()).isEqualTo(10);
            assertThat(priceCol.scale()).isEqualTo(2);

            Field subField = QuotationDetailEntity.class.getDeclaredField("subtotal");
            assertThat(subField.getType()).isEqualTo(BigDecimal.class);
            Column subCol = subField.getAnnotation(Column.class);
            assertThat(subCol.name()).isEqualTo("subtotal");
            assertThat(subCol.precision()).isEqualTo(10);
            assertThat(subCol.scale()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("QuotationLaborRequirementEntity (Table: quotation_labor_requirements)")
    class QuotationLaborRequirementEntityTests {

        @Test
        @DisplayName("Should map to 'quotation_labor_requirements' without soft delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(QuotationLaborRequirementEntity.class).hasAnnotation(Entity.class);

            Table table = QuotationLaborRequirementEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("quotation_labor_requirements");

            assertThat(QuotationLaborRequirementEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should map lazy Quotation relation and labor breakdown columns")
        void shouldMapRelationAndColumns() throws Exception {
            Field qField = QuotationLaborRequirementEntity.class.getDeclaredField("quotation");
            ManyToOne manyToOne = qField.getAnnotation(ManyToOne.class);
            assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn joinCol = qField.getAnnotation(JoinColumn.class);
            assertThat(joinCol.name()).isEqualTo("quotation_id");
            assertThat(joinCol.nullable()).isFalse();

            Field specField = QuotationLaborRequirementEntity.class.getDeclaredField("specialtyNeeded");
            Column specCol = specField.getAnnotation(Column.class);
            assertThat(specCol.name()).isEqualTo("specialty_needed");
            assertThat(specCol.nullable()).isFalse();
            assertThat(specCol.length()).isEqualTo(100);

            Field reqField = QuotationLaborRequirementEntity.class.getDeclaredField("quantityRequired");
            assertThat(reqField.getType()).isEqualTo(Integer.class);
            Column reqCol = reqField.getAnnotation(Column.class);
            assertThat(reqCol.name()).isEqualTo("quantity_required");
            assertThat(reqCol.nullable()).isFalse();

            Field hrsField = QuotationLaborRequirementEntity.class.getDeclaredField("estimatedHours");
            assertThat(hrsField.getType()).isEqualTo(Integer.class);
            Column hrsCol = hrsField.getAnnotation(Column.class);
            assertThat(hrsCol.name()).isEqualTo("estimated_hours");
            assertThat(hrsCol.nullable()).isFalse();

            Field costField = QuotationLaborRequirementEntity.class.getDeclaredField("lockedHourlyCost");
            assertThat(costField.getType()).isEqualTo(BigDecimal.class);
            Column costCol = costField.getAnnotation(Column.class);
            assertThat(costCol.name()).isEqualTo("locked_hourly_cost");
            assertThat(costCol.precision()).isEqualTo(10);
            assertThat(costCol.scale()).isEqualTo(2);

            Field subField = QuotationLaborRequirementEntity.class.getDeclaredField("subtotalLabor");
            assertThat(subField.getType()).isEqualTo(BigDecimal.class);
            Column subCol = subField.getAnnotation(Column.class);
            assertThat(subCol.name()).isEqualTo("subtotal_labor");
            assertThat(subCol.precision()).isEqualTo(10);
            assertThat(subCol.scale()).isEqualTo(2);
        }
    }
}
