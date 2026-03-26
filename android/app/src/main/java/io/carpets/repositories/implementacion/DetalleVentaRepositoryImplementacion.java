package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.DetalleVenta;
import io.carpets.repositories.DetalleVentaRepository;
import io.carpets.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del repositorio para la gestión de detalles de venta.
 * Maneja todas las operaciones CRUD sobre la tabla detalle_venta.
 */
public class DetalleVentaRepositoryImplementacion implements DetalleVentaRepository {

    /**
     * Registra un nuevo detalle de venta en la base de datos.
     *
     * IMPORTANTE: Este método modifica el objeto 'detalle' pasado como parámetro,
     * asignándole el ID generado por la base de datos.
     *
     * @param detalle Detalle de venta a registrar (cantidad, precio, subtotal, ventaId, productoId)
     * @return Response indicando éxito o error
     */
    @Override
    public Response save(DetalleVenta detalle) {
        Response response = new Response();

        // Validación de entrada
        if (detalle == null) {
            response.internal_error("DVRI.save: El detalle de venta no puede ser nulo");
            return response;
        }

        if (detalle.getCantidad() <= 0) {
            response.internal_error("DVRI.save: La cantidad debe ser mayor a 0");
            return response;
        }

        if (detalle.getPrecioUnitario() <= 0) {
            response.internal_error("DVRI.save: El precio unitario debe ser mayor a 0");
            return response;
        }

        if (detalle.getVentaId() <= 0) {
            response.internal_error("DVRI.save: El ID de venta debe ser mayor a 0");
            return response;
        }

        if (detalle.getProductoId() <= 0) {
            response.internal_error("DVRI.save: El ID de producto debe ser mayor a 0");
            return response;
        }

        String sql = "INSERT INTO detalle_venta (cantidad, precio_unitario, subtotal, id_venta, id_producto) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, detalle.getCantidad());
            stmt.setDouble(2, detalle.getPrecioUnitario());
            stmt.setDouble(3, detalle.getSubtotal());
            stmt.setInt(4, detalle.getVentaId());
            stmt.setInt(5, detalle.getProductoId());

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        detalle.setId(generatedId);
                        response.exito();
                        return response;
                    }
                }
                // Si llegamos aquí, se insertó pero no se obtuvo el ID
                response.internal_error("DVRI.save: Registro insertado pero no se pudo recuperar el ID");
                return response;
            }

            response.internal_error("DVRI.save: No se insertó ningún registro");

        } catch (SQLException e) {
            response.internal_error("DVRI.save: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Actualiza un detalle de venta existente en la base de datos.
     *
     * @param detalle Detalle de venta con los datos actualizados (debe incluir el ID)
     * @return Response indicando éxito o error
     */
    @Override
    public Response update(DetalleVenta detalle) {
        Response response = new Response();

        // Validación de entrada
        if (detalle == null) {
            response.internal_error("DVRI.update: El detalle de venta no puede ser nulo");
            return response;
        }

        if (detalle.getId() <= 0) {
            response.internal_error("DVRI.update: El ID del detalle debe ser mayor a 0");
            return response;
        }

        if (detalle.getCantidad() <= 0) {
            response.internal_error("DVRI.update: La cantidad debe ser mayor a 0");
            return response;
        }

        if (detalle.getPrecioUnitario() <= 0) {
            response.internal_error("DVRI.update: El precio unitario debe ser mayor a 0");
            return response;
        }

        if (detalle.getVentaId() <= 0) {
            response.internal_error("DVRI.update: El ID de venta debe ser mayor a 0");
            return response;
        }

        if (detalle.getProductoId() <= 0) {
            response.internal_error("DVRI.update: El ID de producto debe ser mayor a 0");
            return response;
        }

        String sql = "UPDATE detalle_venta " +
                "SET cantidad = ?, precio_unitario = ?, subtotal = ?, id_venta = ?, id_producto = ? " +
                "WHERE id_detalle_venta = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, detalle.getCantidad());
            stmt.setDouble(2, detalle.getPrecioUnitario());
            stmt.setDouble(3, detalle.getSubtotal());
            stmt.setInt(4, detalle.getVentaId());
            stmt.setInt(5, detalle.getProductoId());
            stmt.setInt(6, detalle.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                response.exito();
                return response;
            }

            // Si no se actualizó ninguna fila, el ID no existe
            response.internal_error("DVRI.update: No existe un detalle de venta con id = " + detalle.getId());

        } catch (SQLException e) {
            response.internal_error("DVRI.update: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Elimina un detalle de venta de la base de datos.
     *
     * @param id Identificador del detalle de venta a eliminar (debe ser > 0)
     * @return Response indicando éxito o error
     */
    @Override
    public Response delete(int id) {
        Response response = new Response();

        // Validación de entrada
        if (id <= 0) {
            response.internal_error("DVRI.delete: El ID debe ser mayor a 0");
            return response;
        }

        String sql = "DELETE FROM detalle_venta WHERE id_detalle_venta = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                response.exito();
                return response;
            }

            // Si no se eliminó ninguna fila, el ID no existe
            response.internal_error("DVRI.delete: No existe un detalle de venta con id = " + id);

        } catch (SQLException e) {
            response.internal_error("DVRI.delete: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Encuentra un detalle de venta por su ID.
     *
     * @param id Identificador del detalle de venta (debe ser > 0)
     * @return Response<DetalleVenta> con el detalle encontrado o error
     */
    @Override
    public Response<DetalleVenta> findById(int id) {
        Response<DetalleVenta> response = new Response<>();

        // Validación de entrada
        if (id <= 0) {
            response.internal_error("DVRI.findById: El ID debe ser mayor a 0");
            return response;
        }

        String sql = "SELECT " +
                "  id_detalle_venta, " +
                "  cantidad, " +
                "  precio_unitario, " +
                "  subtotal, " +
                "  id_venta, " +
                "  id_producto " +
                "FROM detalle_venta " +
                "WHERE id_detalle_venta = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    DetalleVenta d = new DetalleVenta();
                    d.setId(rs.getInt("id_detalle_venta"));
                    d.setCantidad(rs.getInt("cantidad"));
                    d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    d.setSubtotal(rs.getDouble("subtotal"));
                    d.setVentaId(rs.getInt("id_venta"));
                    d.setProductoId(rs.getInt("id_producto"));

                    response.exito(d);
                    return response;
                }
            }

            response.internal_error("DVRI.findById: No se encontró detalle de venta con id = " + id);

        } catch (SQLException e) {
            response.internal_error("DVRI.findById: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Encuentra todos los detalles de venta asociados a una venta específica.
     *
     * @param ventaId Identificador de la venta (debe ser > 0)
     * @return Response<List<DetalleVenta>> con los detalles encontrados o error
     */
    @Override
    public Response<List<DetalleVenta>> findByVenta(int ventaId) {
        Response<List<DetalleVenta>> response = new Response<>();
        List<DetalleVenta> lista = new ArrayList<>();

        // Validación de entrada
        if (ventaId <= 0) {
            response.internal_error("DVRI.findByVenta: El ID de venta debe ser mayor a 0");
            return response;
        }

        String sql = "SELECT " +
                "  id_detalle_venta, " +
                "  cantidad, " +
                "  precio_unitario, " +
                "  subtotal, " +
                "  id_venta, " +
                "  id_producto " +
                "FROM detalle_venta " +
                "WHERE id_venta = ? " +
                "ORDER BY id_detalle_venta ASC";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ventaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DetalleVenta d = new DetalleVenta();
                    d.setId(rs.getInt("id_detalle_venta"));
                    d.setCantidad(rs.getInt("cantidad"));
                    d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    d.setSubtotal(rs.getDouble("subtotal"));
                    d.setVentaId(rs.getInt("id_venta"));
                    d.setProductoId(rs.getInt("id_producto"));
                    lista.add(d);
                }
            }

            if (lista.isEmpty()) {
                response.internal_error("DVRI.findByVenta: No se encontraron detalles para la venta id = " + ventaId);
                return response;
            }

            response.exito(lista);

        } catch (SQLException e) {
            response.internal_error("DVRI.findByVenta: Error SQL - " + e.getMessage());
        }

        return response;
    }
}