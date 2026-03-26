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

    Response<Map<String, Object>> login(String username, String password);

    // Cambiado de findUsuarioById a findById para mantener el estándar CRUD
    Response<Usuario> findById(int id);

    Response<Usuario> findByUsername(String username);

    Response<List<Usuario>> findAll();

    Response update(Usuario usuario);

    Response delete(int id);
}