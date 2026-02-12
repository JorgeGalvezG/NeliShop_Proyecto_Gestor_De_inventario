package io.carpets.entidades;

import java.util.Date;
import java.util.List;

public class Compra {
    private int id;
    private String descripcion;
    private double monto;

    public Compra() {}

    public Compra(int id, String descripcion, double monto) {
        this.id = id;
        this.descripcion = descripcion;
        this.monto = monto;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }
}