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
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("3. Inventory & Supply Chain Module Entities - JPA Mapping Tests")
class InventoryEntitiesTest {

    @Nested
    @DisplayName("InventoryItemEntity (Table: inventory_items)")
    class InventoryItemEntityTests {

        @Test
        @DisplayName("Should map to 'inventory_items' with composite unique tenant SKU constraint and soft-delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(InventoryItemEntity.class).hasAnnotation(Entity.class);

            Table table = InventoryItemEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("inventory_items");

            boolean foundConstraint = Arrays.stream(table.uniqueConstraints())
                    .anyMatch(uc -> "uq_inventory_items_company_sku".equals(uc.name())
                            && Arrays.equals(uc.columnNames(), new String[]{"company_id", "sku"}));
            assertThat(foundConstraint).as("Should declare uq_inventory_items_company_sku").isTrue();

            SQLRestriction sqlRestriction = InventoryItemEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should map relations and ID")
        void shouldHaveValidIdAndRelations() throws Exception {
            Field idField = InventoryItemEntity.class.getDeclaredField("id");
            assertThat(idField.getType()).isEqualTo(UUID.class);
            GeneratedValue gen = idField.getAnnotation(GeneratedValue.class);
            assertThat(gen.strategy()).isEqualTo(GenerationType.UUID);

            Field companyField = InventoryItemEntity.class.getDeclaredField("company");
            assertThat(companyField.getType()).isEqualTo(CompanyEntity.class);
            ManyToOne manyToOne = companyField.getAnnotation(ManyToOne.class);
            assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn joinCol = companyField.getAnnotation(JoinColumn.class);
            assertThat(joinCol.name()).isEqualTo("company_id");
            assertThat(joinCol.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map SKU, stock, prices, and default alert limits")
        void shouldHaveValidColumns() throws Exception {
            Field skuField = InventoryItemEntity.class.getDeclaredField("sku");
            Column skuCol = skuField.getAnnotation(Column.class);
            assertThat(skuCol.name()).isEqualTo("sku");
            assertThat(skuCol.nullable()).isFalse();
            assertThat(skuCol.length()).isEqualTo(50);

            Field nameField = InventoryItemEntity.class.getDeclaredField("name");
            Column nameCol = nameField.getAnnotation(Column.class);
            assertThat(nameCol.name()).isEqualTo("name");
            assertThat(nameCol.nullable()).isFalse();
            assertThat(nameCol.length()).isEqualTo(150);

            Field descField = InventoryItemEntity.class.getDeclaredField("description");
            Column descCol = descField.getAnnotation(Column.class);
            assertThat(descCol.name()).isEqualTo("description");
            assertThat(descCol.columnDefinition()).isEqualToIgnoringCase("TEXT");

            Field stockField = InventoryItemEntity.class.getDeclaredField("stockQuantity");
            assertThat(stockField.getType()).isEqualTo(Integer.class);
            Column stockCol = stockField.getAnnotation(Column.class);
            assertThat(stockCol.name()).isEqualTo("stock_quantity");
            assertThat(stockCol.nullable()).isFalse();

            Field costField = InventoryItemEntity.class.getDeclaredField("purchaseCost");
            assertThat(costField.getType()).isEqualTo(BigDecimal.class);
            Column costCol = costField.getAnnotation(Column.class);
            assertThat(costCol.name()).isEqualTo("purchase_cost");
            assertThat(costCol.precision()).isEqualTo(10);
            assertThat(costCol.scale()).isEqualTo(2);

            Field priceField = InventoryItemEntity.class.getDeclaredField("salePrice");
            assertThat(priceField.getType()).isEqualTo(BigDecimal.class);
            Column priceCol = priceField.getAnnotation(Column.class);
            assertThat(priceCol.name()).isEqualTo("sale_price");
            assertThat(priceCol.precision()).isEqualTo(10);
            assertThat(priceCol.scale()).isEqualTo(2);

            Field alertField = InventoryItemEntity.class.getDeclaredField("minStockAlert");
            assertThat(alertField.getType()).isEqualTo(Integer.class);
            Column alertCol = alertField.getAnnotation(Column.class);
            assertThat(alertCol.name()).isEqualTo("min_stock_alert");
        }

        @Test
        @DisplayName("Should execute lifecycle hooks for defaults")
        void shouldHandleLifecycle() throws Exception {
            InventoryItemEntity item = new InventoryItemEntity();

            Method prePersist = Arrays.stream(InventoryItemEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(item);

            assertThat(item.getCreatedAt()).isNotNull();
            assertThat(item.getUpdatedAt()).isNotNull();
            assertThat(item.getStockQuantity()).isEqualTo(0);
            assertThat(item.getPurchaseCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getSalePrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getMinStockAlert()).isEqualTo(5);
            assertThat(item.getIsDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("InventoryMovementEntity (Table: inventory_movements)")
    class InventoryMovementEntityTests {

        @Test
        @DisplayName("Should map to 'inventory_movements' without soft delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(InventoryMovementEntity.class).hasAnnotation(Entity.class);

            Table table = InventoryMovementEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("inventory_movements");

            assertThat(InventoryMovementEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should map relationships to Company, InventoryItem, and User lazily")
        void shouldHaveValidRelations() throws Exception {
            Field companyField = InventoryMovementEntity.class.getDeclaredField("company");
            ManyToOne companyManyToOne = companyField.getAnnotation(ManyToOne.class);
            assertThat(companyManyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn companyJoin = companyField.getAnnotation(JoinColumn.class);
            assertThat(companyJoin.name()).isEqualTo("company_id");
            assertThat(companyJoin.nullable()).isFalse();

            Field itemField = InventoryMovementEntity.class.getDeclaredField("inventoryItem");
            ManyToOne itemManyToOne = itemField.getAnnotation(ManyToOne.class);
            assertThat(itemManyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn itemJoin = itemField.getAnnotation(JoinColumn.class);
            assertThat(itemJoin.name()).isEqualTo("inventory_item_id");
            assertThat(itemJoin.nullable()).isFalse();

            Field userField = InventoryMovementEntity.class.getDeclaredField("user");
            ManyToOne userManyToOne = userField.getAnnotation(ManyToOne.class);
            assertThat(userManyToOne.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn userJoin = userField.getAnnotation(JoinColumn.class);
            assertThat(userJoin.name()).isEqualTo("user_id");
        }

        @Test
        @DisplayName("Should map movement columns and created_at lifecycle hook")
        void shouldMapColumnsAndLifecycle() throws Exception {
            Field movementTypeField = InventoryMovementEntity.class.getDeclaredField("movementType");
            Column moveCol = movementTypeField.getAnnotation(Column.class);
            assertThat(moveCol.name()).isEqualTo("movement_type");
            assertThat(moveCol.nullable()).isFalse();
            assertThat(moveCol.length()).isEqualTo(30);

            Field qtyField = InventoryMovementEntity.class.getDeclaredField("quantityChanged");
            assertThat(qtyField.getType()).isEqualTo(Integer.class);
            Column qtyCol = qtyField.getAnnotation(Column.class);
            assertThat(qtyCol.name()).isEqualTo("quantity_changed");
            assertThat(qtyCol.nullable()).isFalse();

            Field reasonField = InventoryMovementEntity.class.getDeclaredField("reason");
            Column reasonCol = reasonField.getAnnotation(Column.class);
            assertThat(reasonCol.name()).isEqualTo("reason");
            assertThat(reasonCol.columnDefinition()).isEqualToIgnoringCase("TEXT");

            InventoryMovementEntity movement = new InventoryMovementEntity();
            Method prePersist = Arrays.stream(InventoryMovementEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(movement);

            assertThat(movement.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("PurchaseOrderEntity (Table: purchase_orders)")
    class PurchaseOrderEntityTests {

        @Test
        @DisplayName("Should map to 'purchase_orders' with unique tenant order number constraint and soft-delete")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(PurchaseOrderEntity.class).hasAnnotation(Entity.class);

            Table table = PurchaseOrderEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("purchase_orders");

            boolean foundConstraint = Arrays.stream(table.uniqueConstraints())
                    .anyMatch(uc -> "uq_purchase_orders_company_number".equals(uc.name())
                            && Arrays.equals(uc.columnNames(), new String[]{"company_id", "order_number"}));
            assertThat(foundConstraint).as("Should declare uq_purchase_orders_company_number").isTrue();

            SQLRestriction sqlRestriction = PurchaseOrderEntity.class.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction).isNotNull();
            assertThat(sqlRestriction.value()).isEqualTo("is_deleted = false");
        }

        @Test
        @DisplayName("Should map relationships to Company and Supplier lazily")
        void shouldHaveValidRelations() throws Exception {
            Field companyField = PurchaseOrderEntity.class.getDeclaredField("company");
            ManyToOne compMany = companyField.getAnnotation(ManyToOne.class);
            assertThat(compMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn compJoin = companyField.getAnnotation(JoinColumn.class);
            assertThat(compJoin.name()).isEqualTo("company_id");
            assertThat(compJoin.nullable()).isFalse();

            Field supplierField = PurchaseOrderEntity.class.getDeclaredField("supplier");
            ManyToOne supMany = supplierField.getAnnotation(ManyToOne.class);
            assertThat(supMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn supJoin = supplierField.getAnnotation(JoinColumn.class);
            assertThat(supJoin.name()).isEqualTo("supplier_id");
            assertThat(supJoin.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map order attributes and currency with default values")
        void shouldMapColumnsAndDefaults() throws Exception {
            Field orderNumField = PurchaseOrderEntity.class.getDeclaredField("orderNumber");
            Column numCol = orderNumField.getAnnotation(Column.class);
            assertThat(numCol.name()).isEqualTo("order_number");
            assertThat(numCol.nullable()).isFalse();
            assertThat(numCol.length()).isEqualTo(50);

            Field currField = PurchaseOrderEntity.class.getDeclaredField("currency");
            Column currCol = currField.getAnnotation(Column.class);
            assertThat(currCol.name()).isEqualTo("currency");
            assertThat(currCol.length()).isEqualTo(10);

            Field rateField = PurchaseOrderEntity.class.getDeclaredField("exchangeRate");
            assertThat(rateField.getType()).isEqualTo(BigDecimal.class);
            Column rateCol = rateField.getAnnotation(Column.class);
            assertThat(rateCol.name()).isEqualTo("exchange_rate");
            assertThat(rateCol.precision()).isEqualTo(10);
            assertThat(rateCol.scale()).isEqualTo(4);

            Field statusField = PurchaseOrderEntity.class.getDeclaredField("status");
            Column statusCol = statusField.getAnnotation(Column.class);
            assertThat(statusCol.name()).isEqualTo("status");
            assertThat(statusCol.length()).isEqualTo(30);

            Field totalField = PurchaseOrderEntity.class.getDeclaredField("totalAmount");
            assertThat(totalField.getType()).isEqualTo(BigDecimal.class);
            Column totalCol = totalField.getAnnotation(Column.class);
            assertThat(totalCol.name()).isEqualTo("total_amount");
            assertThat(totalCol.precision()).isEqualTo(10);
            assertThat(totalCol.scale()).isEqualTo(2);

            PurchaseOrderEntity po = new PurchaseOrderEntity();
            Method prePersist = Arrays.stream(PurchaseOrderEntity.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PrePersist.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No @PrePersist method found"));
            prePersist.setAccessible(true);
            prePersist.invoke(po);

            assertThat(po.getCreatedAt()).isNotNull();
            assertThat(po.getUpdatedAt()).isNotNull();
            assertThat(po.getCurrency()).isEqualTo("PEN");
            assertThat(po.getExchangeRate()).isEqualByComparingTo(new BigDecimal("1.0000"));
            assertThat(po.getStatus()).isEqualTo("PENDIENTE");
            assertThat(po.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(po.getIsDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("PurchaseOrderDetailEntity (Table: purchase_order_details)")
    class PurchaseOrderDetailEntityTests {

        @Test
        @DisplayName("Should map to 'purchase_order_details' without soft delete or timestamps")
        void shouldHaveCorrectClassLevelAnnotations() {
            assertThat(PurchaseOrderDetailEntity.class).hasAnnotation(Entity.class);

            Table table = PurchaseOrderDetailEntity.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("purchase_order_details");

            assertThat(PurchaseOrderDetailEntity.class.getAnnotation(SQLRestriction.class)).isNull();
        }

        @Test
        @DisplayName("Should map lazy relations to PurchaseOrder and InventoryItem")
        void shouldHaveValidRelations() throws Exception {
            Field poField = PurchaseOrderDetailEntity.class.getDeclaredField("purchaseOrder");
            ManyToOne poMany = poField.getAnnotation(ManyToOne.class);
            assertThat(poMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn poJoin = poField.getAnnotation(JoinColumn.class);
            assertThat(poJoin.name()).isEqualTo("purchase_order_id");
            assertThat(poJoin.nullable()).isFalse();

            Field itemField = PurchaseOrderDetailEntity.class.getDeclaredField("inventoryItem");
            ManyToOne itemMany = itemField.getAnnotation(ManyToOne.class);
            assertThat(itemMany.fetch()).isEqualTo(FetchType.LAZY);
            JoinColumn itemJoin = itemField.getAnnotation(JoinColumn.class);
            assertThat(itemJoin.name()).isEqualTo("inventory_item_id");
            assertThat(itemJoin.nullable()).isFalse();
        }

        @Test
        @DisplayName("Should map quantity, unitCost, and subtotal with numeric scales")
        void shouldMapColumns() throws Exception {
            Field qtyField = PurchaseOrderDetailEntity.class.getDeclaredField("quantity");
            assertThat(qtyField.getType()).isEqualTo(Integer.class);
            Column qtyCol = qtyField.getAnnotation(Column.class);
            assertThat(qtyCol.name()).isEqualTo("quantity");
            assertThat(qtyCol.nullable()).isFalse();

            Field costField = PurchaseOrderDetailEntity.class.getDeclaredField("unitCost");
            assertThat(costField.getType()).isEqualTo(BigDecimal.class);
            Column costCol = costField.getAnnotation(Column.class);
            assertThat(costCol.name()).isEqualTo("unit_cost");
            assertThat(costCol.precision()).isEqualTo(10);
            assertThat(costCol.scale()).isEqualTo(2);
            assertThat(costCol.nullable()).isFalse();

            Field subtotalField = PurchaseOrderDetailEntity.class.getDeclaredField("subtotal");
            assertThat(subtotalField.getType()).isEqualTo(BigDecimal.class);
            Column subCol = subtotalField.getAnnotation(Column.class);
            assertThat(subCol.name()).isEqualTo("subtotal");
            assertThat(subCol.precision()).isEqualTo(10);
            assertThat(subCol.scale()).isEqualTo(2);
            assertThat(subCol.nullable()).isFalse();
        }
    }
}
