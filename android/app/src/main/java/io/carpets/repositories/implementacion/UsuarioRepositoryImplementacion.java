package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Usuario;
import io.carpets.repositories.UsuarioRepository;
import io.carpets.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsuarioRepositoryImplementacion implements UsuarioRepository {

    /**
     * Permiteo el logeo del vendedor usando usuario y contraseña.
     * @param username Nombre de usuario.
     * @param password Contraseña (Debe ser idéntica).
     * @return El usuario tal cual es.
     */
    public Response<Map<String, Object>> login(String username, String password) {
        Response<Map<String, Object>> response = new Response<>();
        Map<String, Object> Usuario = new HashMap<>();
        String sql = "SELECT id_vendedor, nombre, rol from VENDEDOR WHERE nombre = ? AND password = ?";

        //Abre una conexión
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)){

            pst.setString(1, username); //el primer "hueco" es el dni
            pst.setString(2, password); //el segundo "hueco" es la contraseña cifrada

            //Ejecuta el query
            try(ResultSet rs = pst.executeQuery();){
                if (rs.next()) {
                    Usuario.put("status", "ok");
                    Usuario.put("mensaje", "Bienvenido" + rs.getString("nombre"));

                    //guardamos datos utiles para flutter
                    Usuario.put("id", rs.getInt("id_vendedor"));
                    Usuario.put("rol", rs.getString("rol")); // 'admin' o 'vendedor'
                    Usuario.put("nombre", rs.getString("nombre"));
                    response.exito(Usuario);
                } else {
                    // Credenciales incorrectas
                    response.internal_error("URI.login: Credenciales incorrectas.");
                }
            }

        } catch (SQLException e) {
            response.internal_error("URI.login: " + e.getMessage());
        }
        return response;
    }

    /**
     * Encuentra al vendedor usando su id, normalmente usado para boletas.
     * @param id Identificador del vendedor
     * @return Objeto usuario con Nombre y Rol.
     */
    public Response<Usuario> findUsuarioById(int id) {
        Response<Usuario> response = new Response<>();
        String sql = "SELECT nombre, rol from VENDEDOR WHERE id_vendedor = ?";

        //Abre una conexión
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)){

            pst.setInt(1, id);

            //Ejecuta el query
            try(ResultSet rs = pst.executeQuery();){
                if (rs.next()) {
                    Usuario vendedor = new Usuario();
                    vendedor.setNombre(rs.getString("nombre"));
                    vendedor.setRol(rs.getString("rol"));
                    response.exito(vendedor);
                } else {
                    // Credenciales incorrectas
                    response.internal_error("URI.findUsuarioById: Id no encontrado.");
                }
            }

        } catch (SQLException e) {
            response.internal_error("URI.findUsuarioById: " + e.getMessage());
        }
        return response;
    }

}