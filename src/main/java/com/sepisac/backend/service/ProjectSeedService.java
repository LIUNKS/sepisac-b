package com.sepisac.backend.service;

import com.sepisac.backend.model.*;
import com.sepisac.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class ProjectSeedService {

    private static final Logger log = LoggerFactory.getLogger(ProjectSeedService.class);

    private final CompanyRepository companyRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final MachineryEquipmentRepository machineryEquipmentRepository;
    private final EmployeeRepository employeeRepository;
    private final QuotationRepository quotationRepository;
    private final ProjectRepository projectRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    public ProjectSeedService(
            CompanyRepository companyRepository,
            SupplierRepository supplierRepository,
            InventoryItemRepository inventoryItemRepository,
            MachineryEquipmentRepository machineryEquipmentRepository,
            EmployeeRepository employeeRepository,
            QuotationRepository quotationRepository,
            ProjectRepository projectRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            InvoiceRepository invoiceRepository,
            InvoicePaymentRepository invoicePaymentRepository) {
        this.companyRepository = companyRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.machineryEquipmentRepository = machineryEquipmentRepository;
        this.employeeRepository = employeeRepository;
        this.quotationRepository = quotationRepository;
        this.projectRepository = projectRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
    }

    @Transactional
    public void seedAllCompaniesData() {
        List<CompanyEntity> companies = companyRepository.findAll();
        for (CompanyEntity company : companies) {
            seedProjectsData(company.getId());
        }
    }

    @Transactional
    public void seedProjectsData(UUID companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Compañía no encontrada con ID: " + companyId));

        LocalDate today = LocalDate.now();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 1. PROVEEDORES (Suppliers)
        List<SupplierEntity> suppliers = seedSuppliers(company, now);

        // 2. INVENTARIO (Inventory Items)
        List<InventoryItemEntity> inventory = seedInventory(company, suppliers, now);

        // 3. MAQUINARIA Y EQUIPOS (Machinery)
        List<MachineryEquipmentEntity> machinery = seedMachinery(company, today, now);

        // 4. PERSONAL OPERATIVO (Employees)
        List<EmployeeEntity> employees = seedEmployees(company, now);

        // 5. COTIZACIONES (Quotations)
        List<QuotationEntity> quotations = seedQuotations(company, today, now);

        // 6. PROYECTOS (Projects)
        List<ProjectEntity> projects = seedProjects(company, quotations, today, now);

        // 7. FACTURACIÓN Y PAGOS (Invoices & Payments)
        seedInvoicesAndPayments(company, quotations, projects, today, now);

        // 8. ÓRDENES DE COMPRA (Purchase Orders - Egresos)
        seedPurchaseOrders(company, suppliers, today, now);

        log.info("Semillado masivo completado exitosamente para la empresa: {}", company.getBusinessName());
    }

    private List<SupplierEntity> seedSuppliers(CompanyEntity company, OffsetDateTime now) {
        List<SupplierEntity> list = new ArrayList<>();
        String[][] data = {
                {"Ferreyros S.A.", "20100028699", "(01) 626-4000", "contacto@ferreyros.com.pe"},
                {"Corporación Aceros Arequipa S.A.", "20370146994", "(01) 517-1800", "ventas@acerosarequipa.com"},
                {"ABB S.A.", "20100035806", "(01) 415-5100", "contacto.peru@pe.abb.com"},
                {"Siemens S.A.C.", "20100049424", "(01) 215-0030", "atencion.siemens@siemens.com"},
                {"Sodimac Perú S.A.", "20389230724", "(01) 615-6002", "corporativo@sodimac.com.pe"},
                {"3M Perú S.A.", "20100119228", "(01) 224-2728", "atencionalcliente@3m.com"},
                {"Wurth del Perú S.A.C.", "20381665659", "(01) 710-5000", "pedidos@wurth.com.pe"},
                {"Hilti Perú S.A.", "20502390987", "(01) 618-7000", "servicioalcliente@hilti.com"},
                {"Maestro Perú S.A.", "20216613627", "(01) 615-5000", "ventas.empresas@maestro.com.pe"},
                {"Promart Homecenter S.A.", "20536557858", "(01) 619-4000", "corporativo@promart.pe"}
        };

        for (String[] row : data) {
            String name = row[0];
            String ruc = row[1];
            String phone = row[2];
            String email = row[3];

            if (!supplierRepository.existsByCompanyIdAndRuc(company.getId(), ruc)) {
                SupplierEntity s = new SupplierEntity();
                s.setCompany(company);
                s.setBusinessName(name);
                s.setRuc(ruc);
                s.setContactPhone(phone);
                s.setEmail(email);
                s.setIsDeleted(false);
                s.setCreatedAt(now);
                s.setUpdatedAt(now);
                list.add(supplierRepository.save(s));
            }
        }
        return supplierRepository.findByCompanyId(company.getId());
    }

    private List<InventoryItemEntity> seedInventory(CompanyEntity company, List<SupplierEntity> suppliers, OffsetDateTime now) {
        UUID s1 = !suppliers.isEmpty() ? suppliers.get(0).getId() : null;
        UUID s2 = suppliers.size() > 1 ? suppliers.get(1).getId() : s1;
        UUID s3 = suppliers.size() > 2 ? suppliers.get(2).getId() : s1;

        Object[][] items = {
                {"INV-TUB-001", "Tubería de Acero Carbono ASTM A53 Gr. B 4\" SCH 40", "Tubería estructural de acero al carbono", 85, 180.00, 260.00, 20, s2},
                {"INV-TUB-002", "Tubería HDPE PE100 PN10 110mm x 12m", "Tubería de polietileno de alta densidad para conducción de fluidos", 40, 140.00, 210.00, 15, s2},
                {"INV-VAL-001", "Válvula Mariposa Wafer 4\" Tipo 150 ANSI", "Cuerpo de hierro dúctil, disco de acero inoxidable y sello EPDM", 18, 320.00, 480.00, 5, s1},
                {"INV-VAL-002", "Válvula Check Doble Clapeta 6\" Clase 150", "Válvula de retención tipo wafer doble disco", 12, 550.00, 820.00, 4, s1},
                {"INV-VAL-003", "Válvula Compuerta OS&Y Brindada 3\" ASTM A216", "Válvula de compuerta con vástago ascendente", 3, 780.00, 1150.00, 5, s1}, // Crítico
                {"INV-ELE-001", "Cable de Fuerza Vulcanizado 3x10 AWG N2XOH x 100m", "Cable libre de halógenos para alimentación industrial", 25, 420.00, 610.00, 8, s3},
                {"INV-ELE-002", "Interruptor Termomagnético 3x63A 25kA Schneider", "Protección contra sobrecargas y cortocircuitos trifásicos", 30, 195.00, 290.00, 10, s3},
                {"INV-ELE-003", "Tablero Eléctrico IP65 Adosable 600x400x200mm", "Gabinete metálico con pintura electrostática y chapa de seguridad", 8, 680.00, 990.00, 3, s3},
                {"INV-SOL-001", "Electrodo Soldadura Cellocord AP 6011 1/8\" (Caja 20kg)", "Electrodo celulósico de penetración profunda para cañerías", 65, 85.00, 130.00, 15, s2},
                {"INV-SOL-002", "Alambre Sólido MIG ER70S-6 0.035\" (Rollo 15kg)", "Alambre cobrizado para soldadura semiautomática", 2, 110.00, 165.00, 10, s2}, // Crítico
                {"INV-EPP-001", "Casco de Seguridad Dieléctrico Clase E MSA V-Gard", "Protección craneana dieléctrica hasta 20,000 V", 120, 35.00, 55.00, 30, s1},
                {"INV-EPP-002", "Arnés de Seguridad 4 Anillos Anticaídas 3M DBI-SALA", "Arnés de cuerpo entero con faja lumbar y soporte ergonómico", 35, 240.00, 360.00, 10, s1},
                {"INV-EPP-003", "Respirador Media Cara Serie 6200 con Filtros 2097", "Protección respiratoria contra partículas y vapores orgánicos", 50, 75.00, 115.00, 15, s1},
                {"INV-EPP-004", "Zapatos de Seguridad Puntera Composite Bellota T42", "Calzado dieléctrico con suela antideslizante PU bidensidad", 45, 160.00, 230.00, 12, s1},
                {"INV-PER-001", "Perno Estructural Grado A325 3/4\" x 2-1/2\" con Tuerca", "Perno de alta resistencia para uniones estructurales", 450, 8.50, 13.50, 100, s2},
                {"INV-PER-002", "Varilla Roscada B7 5/8\" x 3m con Arandelas F436", "Varilla de aleación de acero para anclajes industriales", 80, 42.00, 68.00, 20, s2},
                {"INV-INS-001", "Manómetro Digital de Presión 0-100 PSI Wika", "Instrumento de medición de alta precisión con certificado de calibración", 14, 380.00, 560.00, 4, s3},
                {"INV-INS-002", "Sensor de Flujo Electromagnético 2\" Rosemount", "Medidor de caudal volumétrico para plantas de procesos", 4, 2400.00, 3500.00, 2, s3},
                {"INV-LUB-001", "Grasa Sintética Altas Cargas Mobilith SHC 220", "Lubricante de complejo de litio para rodamientos pesados", 30, 65.00, 98.00, 10, s1},
                {"INV-LUB-002", "Aceite Hidráulico Tellus S2 MX 68 (Cilindro 55 Gal)", "Fluido hidráulico de alto rendimiento antidesgaste", 12, 820.00, 1200.00, 4, s1}
        };

        for (Object[] row : items) {
            String sku = (String) row[0];
            String name = (String) row[1];
            String desc = (String) row[2];
            int qty = (int) row[3];
            BigDecimal cost = BigDecimal.valueOf((double) row[4]);
            BigDecimal price = BigDecimal.valueOf((double) row[5]);
            int alert = (int) row[6];
            UUID supplierId = (UUID) row[7];

            if (!inventoryItemRepository.existsByCompanyIdAndSku(company.getId(), sku)) {
                InventoryItemEntity i = new InventoryItemEntity();
                i.setCompany(company);
                i.setSku(sku);
                i.setName(name);
                i.setDescription(desc);
                i.setStockQuantity(qty);
                i.setPurchaseCost(cost);
                i.setSalePrice(price);
                i.setMinStockAlert(alert);
                i.setSupplierId(supplierId);
                i.setIsDeleted(false);
                i.setCreatedAt(now);
                i.setUpdatedAt(now);
                inventoryItemRepository.save(i);
            }
        }
        return inventoryItemRepository.findByCompanyId(company.getId());
    }

    private List<MachineryEquipmentEntity> seedMachinery(CompanyEntity company, LocalDate today, OffsetDateTime now) {
        String[][] list = {
                {"MAQ-EXC-001", "Retroexcavadora Caterpillar 420F2", "EN_USO", "-45", "+45"},
                {"MAQ-GRU-001", "Grúa Móvil Liebherr LTM 1050 50T", "EN_USO", "-30", "+60"},
                {"MAQ-GEN-001", "Grupo Electrógeno Cummins 150 kVA", "DISPONIBLE", "-15", "+75"},
                {"MAQ-GEN-002", "Generador Diésel Silencioso Pramac 60 kVA", "DISPONIBLE", "-60", "+30"},
                {"MAQ-COM-001", "Compresor de Aire Portátil Atlas Copco XAS 185", "EN_USO", "-20", "+70"},
                {"MAQ-SOL-001", "Máquina de Soldar Miller Big Blue 500 Pro", "EN_USO", "-10", "+80"},
                {"MAQ-SOL-002", "Soldadora Inverter Lincoln Electric V350-Pro", "DISPONIBLE", "-40", "+50"},
                {"MAQ-CIS-001", "Camión Cisterna Mercedes-Benz 5,000 Galones", "EN_MANTENIMIENTO", "-5", "+10"},
                {"MAQ-MAN-001", "Plataforma Elevadora Tijera JLG 2630ES", "DISPONIBLE", "-25", "+65"},
                {"MAQ-MAN-002", "Plataforma Articulada Diésel Genie Z-45/25J", "EN_USO", "-35", "+55"},
                {"MAQ-TOR-001", "Torre de Iluminación Portátil Generac MLT6", "DISPONIBLE", "-15", "+75"},
                {"MAQ-ROD-001", "Rodillo Compactador Dynapac CC1200", "DISPONIBLE", "-50", "+40"}
        };

        for (String[] row : list) {
            String code = row[0];
            String name = row[1];
            String status = row[2];
            int lastDays = Integer.parseInt(row[3]);
            int nextDays = Integer.parseInt(row[4]);

            if (!machineryEquipmentRepository.existsByCompanyIdAndCode(company.getId(), code)) {
                MachineryEquipmentEntity m = new MachineryEquipmentEntity();
                m.setCompany(company);
                m.setCode(code);
                m.setName(name);
                m.setStatus(status);
                m.setLastMaintenanceDate(today.plusDays(lastDays));
                m.setNextMaintenanceDate(today.plusDays(nextDays));
                m.setIsDeleted(false);
                m.setCreatedAt(now);
                m.setUpdatedAt(now);
                machineryEquipmentRepository.save(m);
            }
        }
        return machineryEquipmentRepository.findByCompanyId(company.getId());
    }

    private List<EmployeeEntity> seedEmployees(CompanyEntity company, OffsetDateTime now) {
        Object[][] list = {
                {"Carlos Alberto Mendoza Valdivia", "Ingeniero Residente de Obra", "PLANILLA", 7500.00, 42.00, false},
                {"Valeria Sofía Huamán Rivas", "Supervisora SSOMA y Medio Ambiente", "PLANILLA", 6200.00, 35.00, true},
                {"Roberto Carlos Santillán Prado", "Soldador Homologado 6G ASME IX", "PLANILLA", 4800.00, 27.00, false},
                {"Miguel Ángel Quispe Mamani", "Técnico Electricista Industrial", "PLANILLA", 4500.00, 25.50, true},
                {"José Antonio Cárdenas Flores", "Operador de Grúa Telescópica Certificado", "PLANILLA", 5200.00, 29.00, false},
                {"Luis Alberto Morales Chávez", "Mecánico de Mantenimiento de Planta", "PLANILLA", 4300.00, 24.00, true},
                {"Jorge Luis Peña Alarcón", "Montajista de Estructuras Metálicas", "LOCACION", 3800.00, 22.00, false},
                {"Fernando Tapia Castro", "Topógrafo con Estación Total", "LOCACION", 4000.00, 23.50, true},
                {"David Ramos Gutiérrez", "Operador de Maquinaria Pesada", "LOCACION", 4900.00, 28.00, false},
                {"Patricia Elena Espinoza Ruiz", "Inspectora QA/QC en Soldadura", "PLANILLA", 5800.00, 33.00, true},
                {"Raúl Enrique Gálvez Soto", "Instrumentista Industrial y Calibración", "LOCACION", 4600.00, 26.00, true},
                {"Manuel Alejandro Torres Vidal", "Asistente de Logística y Operaciones", "PLANILLA", 2800.00, 16.00, false}
        };

        List<EmployeeEntity> current = employeeRepository.findByCompanyId(company.getId());
        Set<String> existingNames = new HashSet<>();
        for (EmployeeEntity e : current) {
            existingNames.add(e.getFullName());
        }

        for (Object[] row : list) {
            String name = (String) row[0];
            String specialty = (String) row[1];
            String contractType = (String) row[2];
            BigDecimal salary = BigDecimal.valueOf((double) row[3]);
            BigDecimal hourlyCost = BigDecimal.valueOf((double) row[4]);
            boolean isAvailable = (boolean) row[5];

            if (!existingNames.contains(name)) {
                EmployeeEntity e = new EmployeeEntity();
                e.setCompany(company);
                e.setFullName(name);
                e.setSpecialty(specialty);
                e.setContractType(contractType);
                e.setBaseSalary(salary);
                e.setCurrentHourlyCost(hourlyCost);
                e.setIsAvailable(isAvailable);
                e.setIsDeleted(false);
                e.setCreatedAt(now);
                e.setUpdatedAt(now);
                employeeRepository.save(e);
            }
        }
        return employeeRepository.findByCompanyId(company.getId());
    }

    private List<QuotationEntity> seedQuotations(CompanyEntity company, LocalDate today, OffsetDateTime now) {
        Object[][] list = {
                {"COT-2026-001", "Minera Chinalco Perú S.A.", "Montaje de Celdas de Flotación y Piping", 145000.00, "APROBADA", 90},
                {"COT-2026-002", "Compañía Minera Antamina S.A.", "Mantenimiento Mayor Planta de Chancado", 88500.00, "APROBADA", 75},
                {"COT-2026-003", "Sociedad Minera Cerro Verde S.A.A.", "Instalación de Tubería HDPE de Relaves 12\"", 120000.00, "APROBADA", 60},
                {"COT-2026-004", "Nexa Resources Perú S.A.A.", "Reparación Estructural y Soldadura de Tolvas", 54000.00, "APROBADA", 50},
                {"COT-2026-005", "Corporación Aceros Arequipa S.A.", "Montaje de Puente Grúa 20T Nave Laminación", 96000.00, "APROBADA", 40},
                {"COT-2026-006", "Refinería La Pampilla (Repsol)", "Piping y Válvulas en Batería de Tanques", 175000.00, "APROBADA", 30},
                {"COT-2026-007", "Consorcio Constructor Metro Línea 2", "Sistema de Bombeo y Drenaje Estación 18", 65000.00, "APROBADA", 20},
                {"COT-2026-008", "Luz del Sur S.A.A.", "Tendido de Cable de Media Tensión Subterráneo", 78000.00, "APROBADA", 15},
                {"COT-2026-009", "Petroperú Planta Talara", "Mantenimiento de Turbobombas de Alimentación", 115000.00, "APROBADA", 10},
                {"COT-2026-010", "Enel Generación Perú S.A.A.", "Overhaul Mecánico de Turbina Moyopampa", 130000.00, "APROBADA", 5},
                {"COT-2026-011", "Minera Las Bambas S.A.", "Fabricación de Spools de Tubería Revestida", 92000.00, "ENVIADA", 4},
                {"COT-2026-012", "Southern Perú Copper Corporation", "Suministro e Instalación de Tableros MCC", 68000.00, "BORRADOR", 2}
        };

        for (Object[] row : list) {
            String num = (String) row[0];
            String client = (String) row[1];
            String service = (String) row[2];
            BigDecimal total = BigDecimal.valueOf((double) row[3]);
            String status = (String) row[4];
            int daysAgo = (int) row[5];

            if (!quotationRepository.existsByCompanyIdAndQuotationNumber(company.getId(), num)) {
                QuotationEntity q = new QuotationEntity();
                q.setCompany(company);
                q.setQuotationNumber(num);
                q.setClientName(client);
                q.setServiceType(service);
                q.setCurrency("PEN");
                q.setExchangeRate(BigDecimal.valueOf(3.75));
                q.setSubtotalCosts(total.multiply(BigDecimal.valueOf(0.75)));
                q.setProfitMarginPercentage(BigDecimal.valueOf(25.0));
                q.setTotalAmount(total);
                q.setStatus(status);
                q.setIsDeleted(false);
                q.setCreatedAt(now.minusDays(daysAgo));
                q.setUpdatedAt(now.minusDays(daysAgo));
                quotationRepository.save(q);
            }
        }
        return quotationRepository.findByCompanyId(company.getId());
    }

    private List<ProjectEntity> seedProjects(CompanyEntity company, List<QuotationEntity> quotations, LocalDate today, OffsetDateTime now) {
        Map<String, QuotationEntity> qMap = new HashMap<>();
        for (QuotationEntity q : quotations) {
            qMap.put(q.getQuotationNumber(), q);
        }

        Object[][] list = {
                {"PROJ-2026-001", "COT-2026-001", "Montaje Electromecánico de Flotación", "Montaje integral de celdas de flotación y piping", "COMPLETADO", -85, -25},
                {"PROJ-2026-002", "COT-2026-002", "Mantenimiento de Planta de Chancado", "Mantenimiento preventivo mayor y cambio de revestimientos", "COMPLETADO", -70, -15},
                {"PROJ-2026-003", "COT-2026-003", "Línea HDPE de Relaves 12\"", "Termofusión y tendido de tubería de relaves 12\"", "COMPLETADO", -55, -5},
                {"PROJ-2026-004", "COT-2026-004", "Reparación y Soldadura de Tolvas", "Soldadura especializada 6G y refuerzo estructural de tolvas", "EN_PROCESO", -45, 15},
                {"PROJ-2026-005", "COT-2026-005", "Montaje de Puente Grúa Nave Laminación", "Izaje, alineamiento y pruebas de carga de puente grúa", "EN_PROCESO", -35, 25},
                {"PROJ-2026-006", "COT-2026-006", "Piping Batería de Tanques La Pampilla", "Reemplazo de colectores y válvulas en tanques de crudo", "EN_PROCESO", -25, 35},
                {"PROJ-2026-007", "COT-2026-007", "Sistema Drenaje Estación 18 Metro", "Instalación de bombas sumergibles y líneas de descarga", "EN_PROCESO", -15, 45},
                {"PROJ-2026-008", "COT-2026-008", "Tendido de Cable Media Tensión", "Zanjado, tendido y empalmes de cable N2XOH 3x10 AWG", "PENDIENTE", 5, 55},
                {"PROJ-2026-009", "COT-2026-009", "Mantenimiento Turbobombas Planta Talara", "Desmontaje, calibración y pruebas hidrostáticas", "PENDIENTE", 10, 60},
                {"PROJ-2026-010", "COT-2026-010", "Overhaul Mecánico Turbina Hidráulica", "Inspección no destructiva y alineación de eje", "PENDIENTE", 15, 75}
        };

        for (Object[] row : list) {
            String code = (String) row[0];
            String cotNum = (String) row[1];
            String title = (String) row[2];
            String desc = (String) row[3];
            String status = (String) row[4];
            int startDays = (int) row[5];
            int endDays = (int) row[6];

            QuotationEntity q = qMap.get(cotNum);
            if (q != null && !projectRepository.existsByCompanyIdAndCode(company.getId(), code)) {
                ProjectEntity p = new ProjectEntity();
                p.setCompany(company);
                p.setQuotation(q);
                p.setCode(code);
                p.setTitle(title);
                p.setDescription(desc);
                p.setClientName(q.getClientName());
                p.setStatus(status);
                p.setStartDate(today.plusDays(startDays));
                p.setEndDate(today.plusDays(endDays));
                p.setIsDeleted(false);
                p.setCreatedAt(now.plusDays(startDays));
                p.setUpdatedAt(now);
                projectRepository.save(p);
            }
        }
        return projectRepository.findByCompanyId(company.getId());
    }

    private void seedInvoicesAndPayments(
            CompanyEntity company,
            List<QuotationEntity> quotations,
            List<ProjectEntity> projects,
            LocalDate today,
            OffsetDateTime now) {

        Map<String, QuotationEntity> qMap = new HashMap<>();
        for (QuotationEntity q : quotations) {
            qMap.put(q.getQuotationNumber(), q);
        }

        Map<String, ProjectEntity> pMap = new HashMap<>();
        for (ProjectEntity p : projects) {
            pMap.put(p.getCode(), p);
        }

        Object[][] invoiceData = {
                {"F001-000120", "COT-2026-001", "PROJ-2026-001", 145000.00, "PAGADA", -60, -50},
                {"F001-000121", "COT-2026-002", "PROJ-2026-002", 88500.00, "PAGADA", -50, -40},
                {"F001-000122", "COT-2026-003", "PROJ-2026-003", 120000.00, "PAGADA", -40, -30},
                {"F001-000123", "COT-2026-004", "PROJ-2026-004", 54000.00, "PAGADA", -30, -20},
                {"F001-000124", "COT-2026-005", "PROJ-2026-005", 96000.00, "PARCIAL", -25, -15},
                {"F001-000125", "COT-2026-006", "PROJ-2026-006", 175000.00, "PARCIAL", -20, -10},
                {"F001-000126", "COT-2026-007", "PROJ-2026-007", 65000.00, "PAGADA", -15, -5},
                {"F001-000127", "COT-2026-008", "PROJ-2026-008", 78000.00, "PENDIENTE", -8, 0},
                {"F001-000128", "COT-2026-009", "PROJ-2026-009", 115000.00, "PENDIENTE", -5, 0},
                {"F001-000129", "COT-2026-010", "PROJ-2026-010", 130000.00, "PENDIENTE", -2, 0}
        };

        for (Object[] row : invoiceData) {
            String invNum = (String) row[0];
            String cotNum = (String) row[1];
            String projCode = (String) row[2];
            BigDecimal amount = BigDecimal.valueOf((double) row[3]);
            String paymentStatus = (String) row[4];
            int issueDaysAgo = (int) row[5];
            int payDaysAgo = (int) row[6];

            QuotationEntity q = qMap.get(cotNum);
            ProjectEntity p = pMap.get(projCode);

            if (q != null && !invoiceRepository.existsByCompanyIdAndInvoiceNumber(company.getId(), invNum)) {
                InvoiceEntity inv = new InvoiceEntity();
                inv.setCompany(company);
                inv.setQuotation(q);
                inv.setProject(p);
                inv.setInvoiceNumber(invNum);
                inv.setCurrency("PEN");
                inv.setTotalAmount(amount);
                inv.setPaymentStatus(paymentStatus);
                inv.setIssueDate(today.plusDays(issueDaysAgo));
                inv.setDueDate(today.plusDays(issueDaysAgo + 30));
                inv.setIsDeleted(false);
                inv.setCreatedAt(now.plusDays(issueDaysAgo));
                inv.setUpdatedAt(now);
                InvoiceEntity savedInv = invoiceRepository.save(inv);

                // Add payment record for Dashboard and Reports revenue charts
                if ("PAGADA".equals(paymentStatus)) {
                    InvoicePaymentEntity pay = new InvoicePaymentEntity();
                    pay.setInvoice(savedInv);
                    pay.setAmountPaid(amount);
                    pay.setPaymentMethod("TRANSFERENCIA");
                    pay.setReferenceCode("OP-BCP-" + Math.abs(invNum.hashCode() % 900000 + 100000));
                    pay.setPaymentDate(now.plusDays(payDaysAgo));
                    invoicePaymentRepository.save(pay);
                } else if ("PARCIAL".equals(paymentStatus)) {
                    BigDecimal halfAmount = amount.divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP);
                    InvoicePaymentEntity pay = new InvoicePaymentEntity();
                    pay.setInvoice(savedInv);
                    pay.setAmountPaid(halfAmount);
                    pay.setPaymentMethod("TRANSFERENCIA");
                    pay.setReferenceCode("OP-BBVA-" + Math.abs(invNum.hashCode() % 900000 + 100000));
                    pay.setPaymentDate(now.plusDays(payDaysAgo));
                    invoicePaymentRepository.save(pay);
                }
            }
        }
    }

    private void seedPurchaseOrders(CompanyEntity company, List<SupplierEntity> suppliers, LocalDate today, OffsetDateTime now) {
        if (suppliers.isEmpty()) return;

        Object[][] poData = {
                {"OC-2026-001", 0, 38000.00, "RECIBIDA", -80},
                {"OC-2026-002", 1, 45000.00, "RECIBIDA", -65},
                {"OC-2026-003", 2, 28000.00, "RECIBIDA", -50},
                {"OC-2026-004", 3, 32000.00, "RECIBIDA", -38},
                {"OC-2026-005", 4, 14500.00, "RECIBIDA", -28},
                {"OC-2026-006", 5, 12000.00, "RECIBIDA", -22},
                {"OC-2026-007", 6, 9800.00, "RECIBIDA", -18},
                {"OC-2026-008", 7, 16500.00, "RECIBIDA", -12},
                {"OC-2026-009", 8, 11200.00, "PENDIENTE", -6},
                {"OC-2026-010", 9, 8400.00, "PENDIENTE", -2}
        };

        for (Object[] row : poData) {
            String orderNum = (String) row[0];
            int supIdx = Math.min((int) row[1], suppliers.size() - 1);
            BigDecimal total = BigDecimal.valueOf((double) row[2]);
            String status = (String) row[3];
            int daysAgo = (int) row[4];

            if (!purchaseOrderRepository.existsByCompanyIdAndOrderNumber(company.getId(), orderNum)) {
                PurchaseOrderEntity po = new PurchaseOrderEntity();
                po.setCompany(company);
                po.setSupplier(suppliers.get(supIdx));
                po.setOrderNumber(orderNum);
                po.setCurrency("PEN");
                po.setExchangeRate(BigDecimal.valueOf(3.75));
                po.setTotalAmount(total);
                po.setStatus(status);
                po.setIsDeleted(false);
                po.setCreatedAt(now.plusDays(daysAgo));
                po.setUpdatedAt(now.plusDays(daysAgo));
                purchaseOrderRepository.save(po);
            }
        }
    }
}
