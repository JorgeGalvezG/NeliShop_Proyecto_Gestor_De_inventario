package io.carpets.servicios;

import io.carpets.entidades.Usuario;

public interface ServicioUsuario {

    /**
     * Valida las credenciales del usuario (DNI y password).
     * @param dni
     * @param password
     * @return Usuario si las credenciales son correctas, null si no.
     */
    Usuario login(String dni, String password);


}
