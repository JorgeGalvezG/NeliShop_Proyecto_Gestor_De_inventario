package io.carpets.repositories;

import io.carpets.entidades.Usuario;
import java.util.List;

/*
    Interfaz del repositorio para la entidad Usuario
    define los métodos CRUD para interactuar con la base de datos.
*/
public interface UsuarioRepository {
    boolean save(Usuario usuario);
    boolean update(Usuario usuario);
    boolean delete(int id);
    Usuario findById(int id);
    Usuario findByUsername(String username);
    List<Usuario> findAll();
}