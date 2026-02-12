package io.carpets.DTOs;


// DTO para la respuesta de inicio de sesión


public class LoginResponse {

    private boolean success;
    private String mensaje;
    private String nombreUsuario; // ejemplo
    private String rol;           // ejemplo

    public LoginResponse() {}

    public LoginResponse(boolean success, String mensaje, String nombreUsuario, String rol) {
        this.success = success;
        this.mensaje = mensaje;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    
}