package com.sepisac.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import com.sepisac.backend.dto.ProjectResponseDTO;

public class DashboardResponseDTO {
    private BigDecimal ingresosMensuales;
    private BigDecimal ingresosTrend; // Percentage
    
    private BigDecimal egresosMensuales;
    private BigDecimal egresosTrend; // Percentage

    private int cotizacionesAprobadas;
    private int cotizacionesTrend; // absolute diff
    
    private int proyectosActivos;
    private int proyectosTerminanPronto;

    private List<ChartData> ingresosVsEgresos;
    private List<PieData> estadoProyectos;
    private List<ProjectResponseDTO> proyectosRecientes;

    public static class ChartData {
        private String name;
        private BigDecimal ingresos;
        private BigDecimal egresos;

        public ChartData(String name, BigDecimal ingresos, BigDecimal egresos) {
            this.name = name;
            this.ingresos = ingresos;
            this.egresos = egresos;
        }

        public String getName() { return name; }
        public BigDecimal getIngresos() { return ingresos; }
        public BigDecimal getEgresos() { return egresos; }
    }

    public static class PieData {
        private String name;
        private int value;
        private String color;

        public PieData(String name, int value, String color) {
            this.name = name;
            this.value = value;
            this.color = color;
        }

        public String getName() { return name; }
        public int getValue() { return value; }
        public String getColor() { return color; }
    }

    // Getters and Setters
    public BigDecimal getIngresosMensuales() { return ingresosMensuales; }
    public void setIngresosMensuales(BigDecimal ingresosMensuales) { this.ingresosMensuales = ingresosMensuales; }

    public BigDecimal getIngresosTrend() { return ingresosTrend; }
    public void setIngresosTrend(BigDecimal ingresosTrend) { this.ingresosTrend = ingresosTrend; }

    public BigDecimal getEgresosMensuales() { return egresosMensuales; }
    public void setEgresosMensuales(BigDecimal egresosMensuales) { this.egresosMensuales = egresosMensuales; }

    public BigDecimal getEgresosTrend() { return egresosTrend; }
    public void setEgresosTrend(BigDecimal egresosTrend) { this.egresosTrend = egresosTrend; }

    public int getCotizacionesAprobadas() { return cotizacionesAprobadas; }
    public void setCotizacionesAprobadas(int cotizacionesAprobadas) { this.cotizacionesAprobadas = cotizacionesAprobadas; }

    public int getCotizacionesTrend() { return cotizacionesTrend; }
    public void setCotizacionesTrend(int cotizacionesTrend) { this.cotizacionesTrend = cotizacionesTrend; }

    public int getProyectosActivos() { return proyectosActivos; }
    public void setProyectosActivos(int proyectosActivos) { this.proyectosActivos = proyectosActivos; }

    public int getProyectosTerminanPronto() { return proyectosTerminanPronto; }
    public void setProyectosTerminanPronto(int proyectosTerminanPronto) { this.proyectosTerminanPronto = proyectosTerminanPronto; }

    public List<ChartData> getIngresosVsEgresos() { return ingresosVsEgresos; }
    public void setIngresosVsEgresos(List<ChartData> ingresosVsEgresos) { this.ingresosVsEgresos = ingresosVsEgresos; }

    public List<PieData> getEstadoProyectos() { return estadoProyectos; }
    public void setEstadoProyectos(List<PieData> estadoProyectos) { this.estadoProyectos = estadoProyectos; }

    public List<ProjectResponseDTO> getProyectosRecientes() { return proyectosRecientes; }
    public void setProyectosRecientes(List<ProjectResponseDTO> proyectosRecientes) { this.proyectosRecientes = proyectosRecientes; }
}
