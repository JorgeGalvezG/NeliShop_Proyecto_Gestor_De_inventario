package io.carpets.DTOs;

import java.util.List;

// DTO para la solicitud de creación de una venta

public class VentaRequest {
    private String clienteDni;
    private int vendedorId;
    private List<DetalleVentaDTO> productos;

    public VentaRequest() {}

    public VentaRequest(String clienteDni, int vendedorId, List<DetalleVentaDTO> productos) {
        this.clienteDni = clienteDni;
        this.vendedorId = vendedorId;
        this.productos = productos;
    }

    public String getClienteDni() { return clienteDni; }
    public void setClienteDni(String clienteDni) { this.clienteDni = clienteDni; }

    public int getVendedorId() { return vendedorId; }
    public void setVendedorId(int vendedorId) { this.vendedorId = vendedorId; }

    public List<DetalleVentaDTO> getProductos() { return productos; }
    public void setProductos(List<DetalleVentaDTO> productos) { this.productos = productos; }

    // Clase interna para representar cada producto en la venta
    public static class DetalleVentaDTO {
        private int productoId;
        private int cantidad;
        private double precio;

        public DetalleVentaDTO() {}

        public DetalleVentaDTO(int productoId, int cantidad, double precio) {
            this.productoId = productoId;
            this.cantidad = cantidad;
            this.precio = precio;
        }

        public int getProductoId() { return productoId; }
        public void setProductoId(int productoId) { this.productoId = productoId; }

        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }

        public double getPrecio() { return precio; }
        public void setPrecio(double precio) { this.precio = precio; }
    }
}