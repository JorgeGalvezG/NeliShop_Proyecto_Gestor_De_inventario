package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Usuario;
import io.carpets.repositories.UsuarioRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioRepositoryImplementacion implements UsuarioRepository {

    /**
     * Registra un usuario.
     * @param usuario Almacena toda la información del usuario a insertar.
     * @return True si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean save(Usuario usuario) {
        String sql = "INSERT INTO vendedor (nombre, rol, password) VALUES (?, ?, ?)";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getRol());
            stmt.setString(3, usuario.getPassword());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Actualiza la información del usuario.
     * @param usuario Contiene la información nueva del usuario. Esta info reemplaza a la de la base de datos.
     * @return True si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean update(Usuario usuario) {
        String sql = "UPDATE vendedor SET nombre=?, rol=?, password=? WHERE id_vendedor=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getRol());
            stmt.setString(3, usuario.getPassword());
            stmt.setInt(4, usuario.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Elimina el registro de un vendedor usando su id.
     * @param id Identificador del vendedor, se usa para ubicarlo.
     * @return True si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM vendedor WHERE id_vendedor=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Encuentra un vendedor usando su Id.
     * @param id Identificador, usado para ubicar al usuario.
     * @return Objeto usuario, si no es encontrado, retorna null.
     */
    @Override
    public Usuario findById(int id) {
        String sql = "SELECT * FROM vendedor WHERE id_vendedor=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getInt("idvendedor"));
                u.setNombre(rs.getString("nombre"));
                u.setRol(rs.getString("rol"));
                u.setPassword(rs.getString("password"));
                return u;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encuentra al usuario usando su nombre.
     * @param username Nombre completo del usuario. Debe ser identico al registrado.
     * @return Objeto usuario con la información obtenida. Si no es encontrado, retorna null.
     */
    @Override
    public Usuario findByUsername(String username) {
        String sql = "SELECT * FROM vendedor WHERE nombre=?"; // Mantenemos la consulta por "nombre" en la BD
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username); // Buscamos por el username que viene como parámetro
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getInt("id_vendedor"));
                u.setNombre(rs.getString("nombre"));
                u.setRol(rs.getString("rol"));
                u.setPassword(rs.getString("password"));
                return u;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Busca a todos los usuarios registrados.
     * @return Lista tipo Usuario con todos los datos de los mismos.
     */
    @Override
    public List<Usuario> findAll() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM vendedor";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getInt("id_vendedor"));
                u.setNombre(rs.getString("nombre"));
                u.setRol(rs.getString("rol"));
                u.setPassword(rs.getString("password"));
                lista.add(u);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}