-- =========================================================================
-- V3: Normalizar títulos de proyectos que guardaron códigos internos de catálogo
-- =========================================================================

UPDATE public.projects
SET title = CASE
    WHEN title = 'MANTENIMIENTO_PREVENTIVO' THEN 'Mantenimiento Preventivo'
    WHEN title LIKE 'MANTENIMIENTO_PREVENTIVO - %' THEN 'Mantenimiento Preventivo - ' || SUBSTRING(title FROM LENGTH('MANTENIMIENTO_PREVENTIVO - ') + 1)
    WHEN title = 'MANTENIMIENTO_CORRECTIVO' THEN 'Mantenimiento Correctivo'
    WHEN title LIKE 'MANTENIMIENTO_CORRECTIVO - %' THEN 'Mantenimiento Correctivo - ' || SUBSTRING(title FROM LENGTH('MANTENIMIENTO_CORRECTIVO - ') + 1)
    WHEN title = 'MANTENIMIENTO_CALDERAS' THEN 'Mantenimiento de Calderas'
    WHEN title LIKE 'MANTENIMIENTO_CALDERAS - %' THEN 'Mantenimiento de Calderas - ' || SUBSTRING(title FROM LENGTH('MANTENIMIENTO_CALDERAS - ') + 1)
    WHEN title = 'FABRICACION_MONTAJE' THEN 'Fabricación y Montaje'
    WHEN title LIKE 'FABRICACION_MONTAJE - %' THEN 'Fabricación y Montaje - ' || SUBSTRING(title FROM LENGTH('FABRICACION_MONTAJE - ') + 1)
    WHEN title = 'SOLDADURA_ESTRUCTURAL' THEN 'Soldadura Estructural'
    WHEN title LIKE 'SOLDADURA_ESTRUCTURAL - %' THEN 'Soldadura Estructural - ' || SUBSTRING(title FROM LENGTH('SOLDADURA_ESTRUCTURAL - ') + 1)
    WHEN title = 'TORNEADO_EJES' THEN 'Torneado de Ejes'
    WHEN title LIKE 'TORNEADO_EJES - %' THEN 'Torneado de Ejes - ' || SUBSTRING(title FROM LENGTH('TORNEADO_EJES - ') + 1)
    WHEN title = 'MECANIZADO_GENERAL' THEN 'Mecanizado General'
    WHEN title LIKE 'MECANIZADO_GENERAL - %' THEN 'Mecanizado General - ' || SUBSTRING(title FROM LENGTH('MECANIZADO_GENERAL - ') + 1)
    WHEN title = 'OVERHAUL_REDUCTORES' THEN 'Overhaul de Reductores'
    WHEN title LIKE 'OVERHAUL_REDUCTORES - %' THEN 'Overhaul de Reductores - ' || SUBSTRING(title FROM LENGTH('OVERHAUL_REDUCTORES - ') + 1)
    WHEN title = 'MANTENIMIENTO' THEN 'Mantenimiento General'
    WHEN title LIKE 'MANTENIMIENTO - %' THEN 'Mantenimiento General - ' || SUBSTRING(title FROM LENGTH('MANTENIMIENTO - ') + 1)
    ELSE title
END
WHERE title IN (
    'MANTENIMIENTO_PREVENTIVO',
    'MANTENIMIENTO_CORRECTIVO',
    'MANTENIMIENTO_CALDERAS',
    'FABRICACION_MONTAJE',
    'SOLDADURA_ESTRUCTURAL',
    'TORNEADO_EJES',
    'MECANIZADO_GENERAL',
    'OVERHAUL_REDUCTORES',
    'MANTENIMIENTO'
)
OR title LIKE 'MANTENIMIENTO_PREVENTIVO - %'
OR title LIKE 'MANTENIMIENTO_CORRECTIVO - %'
OR title LIKE 'MANTENIMIENTO_CALDERAS - %'
OR title LIKE 'FABRICACION_MONTAJE - %'
OR title LIKE 'SOLDADURA_ESTRUCTURAL - %'
OR title LIKE 'TORNEADO_EJES - %'
OR title LIKE 'MECANIZADO_GENERAL - %'
OR title LIKE 'OVERHAUL_REDUCTORES - %'
OR title LIKE 'MANTENIMIENTO - %';
