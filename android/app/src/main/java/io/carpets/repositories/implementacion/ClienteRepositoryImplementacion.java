package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Cliente;
import io.carpets.repositories.ClienteRepository;
import io.carpets.util.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteRepositoryImplementacion implements ClienteRepository {

    /**
     * Registra a algún cliente en la base de datos.
     */
    @Override
    public Response save(Cliente cliente) {
        Response response = new Response();
        String sql = "INSERT INTO cliente (nombre, dni) VALUES (?, ?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getDni());

            if (stmt.executeUpdate() > 0) {
                response.exito();
            } else {
                response.message_error("No se pudo registrar al cliente.");
            }

        } catch (SQLException e) {
            response.internal_error("CRI.save: " + e.getMessage());
        }
        return response;
    }

    /**
     * Actualiza la información de un cliente existente.
     */
    @Override
    public Response update(Cliente cliente) {
        Response response = new Response();
        String sql = "UPDATE cliente SET nombre=? WHERE dni=?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getDni());

            if (stmt.executeUpdate() > 0) {
                response.exito();
            } else {
                response.message_error("No se pudo actualizar. El cliente no existe.");
            }

        } catch (SQLException e) {
            response.internal_error("CRI.update: " + e.getMessage());
        }
        return response;
    }

    /**
     * Elimina a un cliente de la base de datos.
     */
    @Override
    public Response delete(String dni) {
        Response response = new Response();
        String sql = "DELETE FROM cliente WHERE dni=?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, dni);

            if (stmt.executeUpdate() > 0) {
                response.exito();
            } else {
                response.message_error("No se pudo eliminar. El cliente no fue encontrado.");
            }

        } catch (SQLException e) {
            response.internal_error("CRI.delete: " + e.getMessage());
        }
        return response;
    }

    /**
     * Encuentra al usuario usando su DNI como identificador.
     */
    @Override
    public Response<Cliente> findByDni(String dni) {
        Response<Cliente> response = new Response<>();
        String sql = "SELECT * FROM cliente WHERE dni=?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Cliente cliente = new Cliente();
                    cliente.setNombre(rs.getString("nombre"));
                    cliente.setDni(rs.getString("dni"));
                    response.exito(cliente);
                } else {
                    response.message_error("Cliente no encontrado con DNI: " + dni);
                }
            }

        } catch (SQLException e) {
            response.internal_error("CRI.findByDni: " + e.getMessage());
        }
        return response;
    }

    /**
     * Encuentra todos los clientes registrados.
     */
    @Override
    public Response<List<Cliente>> findAll() {
        Response<List<Cliente>> response = new Response<>();
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
            response.exito(lista);

        } catch (SQLException e) {
            response.internal_error("CRI.findAll: " + e.getMessage());
        }
        return response;
    }

    /**
     * Encuentra una lista de clientes que contengan cierto nombre.
     */
    public Response<List<Cliente>> findByNombre(String nombre) {
        Response<List<Cliente>> response = new Response<>();
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM cliente WHERE nombre LIKE ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cliente cliente = new Cliente();
                    cliente.setNombre(rs.getString("nombre"));
                    cliente.setDni(rs.getString("dni"));
                    lista.add(cliente);
                }
                response.exito(lista);
            }

        } catch (SQLException e) {
            response.internal_error("CRI.findByNombre: " + e.getMessage());
        }
        return response;
    }

    /**
     * Verifica de forma booleana si un cliente existe en la base de datos.
     */
    public Response<Boolean> existePorDni(String dni) {
        Response<Boolean> response = new Response<>();
        String sql = "SELECT COUNT(*) FROM cliente WHERE dni=?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    response.exito(rs.getInt(1) > 0);
                } else {
                    response.exito(false);
                }
            }

        } catch (SQLException e) {
            response.internal_error("CRI.existePorDni: " + e.getMessage());
        }
        return response;
    }
}