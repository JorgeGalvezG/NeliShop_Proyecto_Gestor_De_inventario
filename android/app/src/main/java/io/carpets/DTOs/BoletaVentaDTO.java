package io.carpets.DTOs;

import io.carpets.entidades.Venta;
import io.carpets.entidades.DetalleVenta;
import io.carpets.entidades.Cliente;
import io.carpets.entidades.Usuario;
import java.util.List;

public class BoletaVentaDTO {
    private Venta venta;
    private Cliente cliente;
    private Usuario vendedor;
    private List<DetalleVenta> detalles;
    private double subtotal;
    private double igv;
    private double total;

    public BoletaVentaDTO() {}

    public BoletaVentaDTO(Venta venta, Cliente cliente, Usuario vendedor, List<DetalleVenta> detalles, double subtotal, double igv, double total) {
        this.venta = venta;
        this.cliente = cliente;
        this.vendedor = vendedor;
        this.detalles = detalles;
        this.subtotal = subtotal;
        this.igv = igv;
        this.total = total;
    }

    // Getters y Setters
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getIgv() { return igv; }
    public void setIgv(double igv) { this.igv = igv; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}