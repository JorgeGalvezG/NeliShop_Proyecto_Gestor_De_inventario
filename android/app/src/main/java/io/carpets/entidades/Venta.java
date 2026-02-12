package io.carpets.entidades;

import java.util.Date;

public class Venta {
    private int id;
    private Date fecha;
    private double monto;
    private String descripcion;
    private String clienteDni;
    private int vendedorId;

    // Nuevos campos para boleta
    private String numeroBoleta;
    private double igv;
    private double igvAplicado;
    private double totalFinal;

    public Venta() {}

    public Venta(int id, Date fecha, double monto, String descripcion, String clienteDni, int vendedorId) {
        this.id = id;
        this.fecha = fecha;
        this.monto = monto;
        this.descripcion = descripcion;
        this.clienteDni = clienteDni;
        this.vendedorId = vendedorId;
    }

    // Getters y Setters existentes
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getClienteDni() { return clienteDni; }
    public void setClienteDni(String clienteDni) { this.clienteDni = clienteDni; }
    public int getVendedorId() { return vendedorId; }
    public void setVendedorId(int vendedorId) { this.vendedorId = vendedorId; }

    // Nuevos Getters y Setters para boleta
    public String getNumeroBoleta() { return numeroBoleta; }
    public void setNumeroBoleta(String numeroBoleta) { this.numeroBoleta = numeroBoleta; }
    public double getIgv() { return igv; }
    public void setIgv(double igv) { this.igv = igv; }
    public double getIgvAplicado() { return igvAplicado; }
    public void setIgvAplicado(double igvAplicado) { this.igvAplicado = igvAplicado; }
    public double getTotalFinal() { return totalFinal; }
    public void setTotalFinal(double totalFinal) { this.totalFinal = totalFinal; }
}