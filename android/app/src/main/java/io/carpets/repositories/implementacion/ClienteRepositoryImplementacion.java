package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Cliente;
import io.carpets.repositories.ClienteRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteRepositoryImplementacion implements ClienteRepository {

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

    // buscar clientes por nombre
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

    // verificar si un cliente existe por DNI
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