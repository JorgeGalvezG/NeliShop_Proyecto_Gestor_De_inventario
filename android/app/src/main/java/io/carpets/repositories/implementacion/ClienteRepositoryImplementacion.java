package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Cliente;
import io.carpets.repositories.ClienteRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteRepositoryImplementacion implements ClienteRepository {

    /**
     * Registra a algun cliente
     * @param cliente Información del cliente
     * @return true si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean save(Cliente cliente) {
        String sql = "INSERT INTO cliente (nombre, dni) VALUES (?, ?)";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getDni());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Actualiza la información de un cliente
     * @param cliente Contiene la información de un cliente.
     * @return true si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean update(Cliente cliente) {
        String sql = "UPDATE cliente SET nombre=? WHERE dni=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getDni());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Eliminar a un cliente
     * @param dni Dni del cliente, se usa para identificar al usuario
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public boolean delete(String dni) {
        String sql = "DELETE FROM cliente WHERE dni=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, dni);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Encuentra al usuario usando su dni como identificador.
     * @param dni dni del usuario
     * @return Si todo sale bien retorna un cliente, sino un nulo.
     */
    @Override
    public Cliente findByDni(String dni) {
        String sql = "SELECT * FROM cliente WHERE dni=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Cliente cliente = new Cliente();
                cliente.setNombre(rs.getString("nombre"));
                cliente.setDni(rs.getString("dni"));
                return cliente;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encuentra todos los clientes registrados. No se para que se usaría.
     * @return Una lista con los clientes, si no hay, una lista vacía.
     */
    @Override
    public List<Cliente> findAll() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM cliente";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Cliente cliente = new Cliente();
                cliente.setNombre(rs.getString("nombre"));
                cliente.setDni(rs.getString("dni"));
                lista.add(cliente);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Encuentra una lista de clientes que contengan cierto nombre.
     * @param nombre nombre del usuario
     * @return Retorna una lista de coincidencias, si no hay, una lista vacía.
     */
    public List<Cliente> findByNombre(String nombre) {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM cliente WHERE nombre LIKE ?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Cliente cliente = new Cliente();
                cliente.setNombre(rs.getString("nombre"));
                cliente.setDni(rs.getString("dni"));
                lista.add(cliente);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Se usa el dni para verificar si un cliente existe en la base de datos.
     * @param dni Identificador del cliente.
     * @return Retorna true o false segun exista el cliente.
     */
    public boolean existePorDni(String dni) {
        String sql = "SELECT COUNT(*) FROM cliente WHERE dni=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}