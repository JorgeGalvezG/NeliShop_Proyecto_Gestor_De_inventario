package io.carpets.repositories;

import io.carpets.entidades.Usuario;
import io.carpets.util.Response;

import java.util.List;
import java.util.Map;

/*
    Interfaz del repositorio para la entidad Usuario
    define los métodos CRUD para interactuar con la base de datos.
*/
public interface UsuarioRepository {
    public Response<Map<String, Object>> login(String username, String password);
    public Response<Usuario> findUsuarioById(int id);
}