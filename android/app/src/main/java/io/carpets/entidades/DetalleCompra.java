package io.carpets.entidades;

import java.util.Map;

public class DetalleCompra {
    private int id;
    private int unidades;
    private int productoId;
    private int compraId;

    private double precioUnitario;

    public DetalleCompra() {}

    public DetalleCompra(int id, int unidades, int productoId, int compraId) {
        this.id = id;
        this.unidades = unidades;
        this.productoId = productoId;
        this.compraId = compraId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUnidades() {
        return unidades;
    }

    public void setUnidades(int unidades) {
        this.unidades = unidades;
    }

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public int getCompraId() {
        return compraId;
    }

    public void setCompraId(int compraId) {
        this.compraId = compraId;
    }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public static DetalleCompra DetCompraFromMap(Map<String, Object> DetCompraMap){
        DetalleCompra detCompra = new DetalleCompra();
        if (DetCompraMap.get("productoId") != null)
            detCompra.setProductoId((int) DetCompraMap.get("productoId"));
        if (DetCompraMap.get("unidades") != null)
            detCompra.setUnidades((int) DetCompraMap.get("unidades"));
        if (DetCompraMap.get("precioUnitario") != null)
            detCompra.setPrecioUnitario(Double.parseDouble(DetCompraMap.get("precioUnitario").toString()));
        return detCompra;
    }

}