package io.carpets.entidades;


public class Usuario {
    private int id;
    private String nombre;
    private String rol;
    private String password;

    public Usuario() {}

    public Usuario(int id, String nombre, String rol, String password) {
        this.id = id;
        this.nombre = nombre;
        this.rol = rol;
        this.password = password;
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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}