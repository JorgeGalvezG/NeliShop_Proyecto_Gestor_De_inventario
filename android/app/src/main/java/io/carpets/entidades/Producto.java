package io.carpets.entidades;

import java.util.Date;


public class Producto {
    private int id;
    private String nombre;
    private Date fechaIngreso;
    private double precioCompra;
    private double precioVenta;
    private int cantidad;
    private String categoriaNombre;
    private String imagePath;
    private Double precioOferta;

    public Producto() {}

    public Producto(int id, String nombre, Date fechaIngreso, double precioCompra,
                    double precioVenta, int cantidad, String categoriaNombre, String codigo) {
        this.id = id;
        this.nombre = nombre;
        this.fechaIngreso = fechaIngreso;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.cantidad = cantidad;
        this.categoriaNombre = categoriaNombre;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Date getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Date fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public double getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(double precioCompra) {
        this.precioCompra = precioCompra;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public Double getPrecioOferta() { return precioOferta; }
    public void setPrecioOferta(Double precioOferta) { this.precioOferta = precioOferta; }





}