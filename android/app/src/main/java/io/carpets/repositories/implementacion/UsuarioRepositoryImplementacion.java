package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Usuario;
import io.carpets.repositories.UsuarioRepository;
import io.carpets.util.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsuarioRepositoryImplementacion implements UsuarioRepository {

    // Ejecuta la autenticacion consultando credenciales en la base de datos
    public Response<Map<String, Object>> login(String username, String password) {
        Response<Map<String, Object>> response = new Response<>();
        String sql = "SELECT id_vendedor, nombre, rol FROM vendedor WHERE nombre = ? AND password = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            pst.setString(2, password);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> usuarioData = new HashMap<>();
                    usuarioData.put("id", rs.getInt("id_vendedor"));
                    usuarioData.put("rol", rs.getString("rol"));
                    usuarioData.put("nombre", rs.getString("nombre"));
                    response.exito(usuarioData);
                } else {
                    response.message_error("Credenciales incorrectas.");
                }
            }

        } catch (SQLException e) {
            response.internal_error("URI.login: " + e.getMessage());
        }
        return response;
    }

    // Encuentra a un vendedor por su identificador unico
    @Override
    public Response<Usuario> findById(int id) {
        Response<Usuario> response = new Response<>();
        String sql = "SELECT * FROM vendedor WHERE id_vendedor = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, id);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id_vendedor"));
                    u.setNombre(rs.getString("nombre"));
                    u.setRol(rs.getString("rol"));
                    u.setPassword(rs.getString("password"));
                    response.exito(u);
                } else {
                    response.message_error("Vendedor no encontrado con ID: " + id);
                }
            }

        } catch (SQLException e) {
            response.internal_error("URI.findById: " + e.getMessage());
        }
        return response;
    }

    // Busca a un vendedor especifico utilizando su nombre de usuario
    @Override
    public Response<Usuario> findByUsername(String username) {
        Response<Usuario> response = new Response<>();
        String sql = "SELECT * FROM vendedor WHERE nombre = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id_vendedor"));
                    u.setNombre(rs.getString("nombre"));
                    u.setRol(rs.getString("rol"));
                    u.setPassword(rs.getString("password"));
                    response.exito(u);
                } else {
                    response.message_error("Vendedor no encontrado con nombre: " + username);
                }
            }

        } catch (SQLException e) {
            response.internal_error("URI.findByUsername: " + e.getMessage());
        }
        return response;
    }

    // Recupera la lista completa de vendedores registrados
    @Override
    public Response<List<Usuario>> findAll() {
        Response<List<Usuario>> response = new Response<>();
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM vendedor";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getInt("id_vendedor"));
                u.setNombre(rs.getString("nombre"));
                u.setRol(rs.getString("rol"));
                u.setPassword(rs.getString("password"));
                lista.add(u);
            }
            response.exito(lista);

        } catch (SQLException e) {
            response.internal_error("URI.findAll: " + e.getMessage());
        }
        return response;
    }

    // Sobrescribe los datos de un vendedor existente
    @Override
    public Response update(Usuario usuario) {
        Response response = new Response();
        String sql = "UPDATE vendedor SET nombre = ?, rol = ?, password = ? WHERE id_vendedor = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, usuario.getNombre());
            pst.setString(2, usuario.getRol());
            pst.setString(3, usuario.getPassword());
            pst.setInt(4, usuario.getId());

            int filasAfectadas = pst.executeUpdate();
            if (filasAfectadas > 0) {
                response.exito();
            } else {
                response.message_error("No se pudo actualizar. El vendedor no existe.");
            }

        } catch (SQLException e) {
            response.internal_error("URI.update: " + e.getMessage());
        }
        return response;
    }

    // Elimina fisicamente el registro de un vendedor
    @Override
    public Response delete(int id) {
        Response response = new Response();
        String sql = "DELETE FROM vendedor WHERE id_vendedor = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, id);

            int filasAfectadas = pst.executeUpdate();
            if (filasAfectadas > 0) {
                response.exito();
            } else {
                response.message_error("No se pudo eliminar. El vendedor no existe.");
            }

        } catch (SQLException e) {
            response.internal_error("URI.delete: " + e.getMessage());
        }
        return response;
    }
}