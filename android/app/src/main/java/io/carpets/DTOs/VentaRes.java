package io.carpets.DTOs;

import java.util.List;

// DTO para la respuesta de una venta

public class VentaRes {
    private int idVenta;
    private String fecha;
    private double montoTotal;
    private double igv;
    private List<VentaRequest.DetalleVentaDTO> productos;

    public VentaRes() {}

    public VentaRes(int idVenta, String fecha, double montoTotal, double igv, List<VentaRequest.DetalleVentaDTO> productos) {
        this.idVenta = idVenta;
        this.fecha = fecha;
        this.montoTotal = montoTotal;
        this.igv = igv;
        this.productos = productos;
    }

    public int getIdVenta() { return idVenta; }
    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(double montoTotal) { this.montoTotal = montoTotal; }

    public double getIgv() { return igv; }
    public void setIgv(double igv) { this.igv = igv; }

    public List<VentaRequest.DetalleVentaDTO> getProductos() { return productos; }
    public void setProductos(List<VentaRequest.DetalleVentaDTO> productos) { this.productos = productos; }
}