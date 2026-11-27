-- =========================================================================
-- V2: Categoría real en inventory_items y alineación de item_type en quotation_details
-- =========================================================================

-- 1. Agregar columna category en inventory_items
ALTER TABLE public.inventory_items
    ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Backfill inteligente para artículos ya registrados
UPDATE public.inventory_items
SET category = CASE
    WHEN LOWER(COALESCE(name, '') || ' ' || COALESCE(description, '') || ' ' || COALESCE(sku, '')) ~ '(rodamiento|faja|reten|retén|sello|pinon|piñón|engranaje|repuesto)' THEN 'REPUESTOS_MECANICOS'
    WHEN LOWER(COALESCE(name, '') || ' ' || COALESCE(description, '') || ' ' || COALESCE(sku, '')) ~ '(aceite|grasa|lubricante|refrigerante|quimico|químico)' THEN 'LUBRICANTES_FLUIDOS'
    WHEN LOWER(COALESCE(name, '') || ' ' || COALESCE(description, '') || ' ' || COALESCE(sku, '')) ~ '(taladro|esmeril|sierra|llave|herramienta|amoladora|prensa|her-)' THEN 'HERRAMIENTAS_EQUIPOS'
    WHEN LOWER(COALESCE(name, '') || ' ' || COALESCE(description, '') || ' ' || COALESCE(sku, '')) ~ '(casco|guante|lente|epp|careta|bota)' THEN 'SEGURIDAD_EPP'
    ELSE 'CONSUMIBLES_SOLDADURA'
END
WHERE category IS NULL OR TRIM(category) = '';

ALTER TABLE public.inventory_items
    ALTER COLUMN category SET DEFAULT 'CONSUMIBLES_SOLDADURA',
    ALTER COLUMN category SET NOT NULL;

ALTER TABLE public.inventory_items
    DROP CONSTRAINT IF EXISTS chk_inventory_items_category;

ALTER TABLE public.inventory_items
    ADD CONSTRAINT chk_inventory_items_category
    CHECK (category IN (
        'CONSUMIBLES_SOLDADURA',
        'REPUESTOS_MECANICOS',
        'LUBRICANTES_FLUIDOS',
        'HERRAMIENTAS_EQUIPOS',
        'SEGURIDAD_EPP',
        'OTROS'
    ));

CREATE INDEX IF NOT EXISTS idx_inventory_items_company_category
    ON public.inventory_items(company_id, category);

-- 2. Alinear constraint CHECK de item_type en quotation_details
DO $$
DECLARE
    v_constraint_name TEXT;
BEGIN
    FOR v_constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'public'
          AND t.relname = 'quotation_details'
          AND c.contype = 'c'
          AND pg_get_constraintdef(c.oid) ILIKE '%item_type%'
    LOOP
        EXECUTE format('ALTER TABLE public.quotation_details DROP CONSTRAINT %I', v_constraint_name);
    END LOOP;
END $$;

-- Normalizar registros legados (ej. 'OTRO' -> 'OTROS' y mayúsculas)
UPDATE public.quotation_details
SET item_type = CASE
    WHEN UPPER(TRIM(item_type)) = 'OTRO' THEN 'OTROS'
    WHEN UPPER(TRIM(item_type)) IN ('MATERIAL', 'REPUESTO', 'EQUIPO', 'HERRAMIENTA', 'TRANSPORTE', 'VIATICOS', 'OTROS') THEN UPPER(TRIM(item_type))
    ELSE 'OTROS'
END;

ALTER TABLE public.quotation_details
    ADD CONSTRAINT chk_quotation_details_item_type
    CHECK (item_type IN (
        'MATERIAL',
        'REPUESTO',
        'EQUIPO',
        'HERRAMIENTA',
        'TRANSPORTE',
        'VIATICOS',
        'OTROS'
    ));
