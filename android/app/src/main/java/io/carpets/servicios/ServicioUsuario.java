package io.carpets.servicios;

import java.util.Map;

import io.carpets.entidades.Usuario;
import io.carpets.util.Response;

public interface ServicioUsuario {

    /**
     * Valida las credenciales del usuario (DNI y password).
     * @param username Nombre de usuario al que se intenta acceder.
     * @param password Contraseña usada
     * @return Usuario si las credenciales son correctas, null si no.
     */
    Response<Map<String, Object>> login(String username, String password);


}
