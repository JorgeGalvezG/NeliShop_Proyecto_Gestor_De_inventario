package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Venta;
import io.carpets.repositories.VentaRepository;
import io.carpets.DTOs.VentaCompletaDTO;
import io.carpets.DTOs.DetalleVentaDTO;
import io.carpets.util.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VentaRepositoryImplementacion implements VentaRepository {

    /**
     * Registra una nueva venta en la base de datos.
     */
    @Override
    public Response save(Venta venta) {
        Response response = new Response();
        // Integrado id_cliente de la rama entrante
        String sql = "INSERT INTO venta (numero_boleta, fecha, monto, descripcion, id_vendedor, id_cliente) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, venta.getNumeroBoleta());
            stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            stmt.setDouble(3, venta.getMonto());
            stmt.setString(4, venta.getDescripcion());
            stmt.setInt(5, venta.getVendedorId());
            // Valor por defecto temporal para evitar romper la integridad referencial
            stmt.setInt(6, 1);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        venta.setId(rs.getInt(1));
                    }
                }
                response.exito();
            } else {
                response.message_error("No se pudo registrar la venta en la base de datos.");
            }
        } catch (SQLException e) {
            response.internal_error("VRI.save: " + e.getMessage());
        }
        return response;
    }

    /**
     * Actualiza una venta registrada.
     */
    @Override
    public Response update(Venta venta) {
        Response response = new Response();
        String sql = "UPDATE venta SET numero_boleta=?, fecha=?, monto=?, descripcion=?, id_vendedor=?, id_cliente=? WHERE id_venta=?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, venta.getNumeroBoleta());
            stmt.setTimestamp(2, new java.sql.Timestamp(venta.getFecha() != null ? venta.getFecha().getTime() : System.currentTimeMillis()));
            stmt.setDouble(3, venta.getMonto());
            stmt.setString(4, venta.getDescripcion());
            stmt.setInt(5, venta.getVendedorId());
            stmt.setInt(6, 1); // Hardcoded temporal
            stmt.setInt(7, venta.getId());

            if (stmt.executeUpdate() > 0) {
                response.exito();
            } else {
                response.message_error("No se actualizó. La venta no existe.");
            }
        } catch (SQLException e) {
            response.internal_error("VRI.update: " + e.getMessage());
        }
        return response;
    }

    /**
     * Elimina una venta usando su identificador.
     */
    @Override
    public Response delete(int id) {
        Response response = new Response();
        String sql = "DELETE FROM venta WHERE id_venta=?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            if (stmt.executeUpdate() > 0) {
                response.exito();
            } else {
                response.message_error("No se pudo eliminar. La venta no existe.");
            }
        } catch (SQLException e) {
            response.internal_error("VRI.delete: " + e.getMessage());
        }
        return response;
    }

    /**
     * Encuentra una venta usando su ID.
     */
    @Override
    public Response<Venta> findById(int id) {
        Response<Venta> response = new Response<>();
        String sql = "SELECT * FROM venta WHERE id_venta=?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Venta v = new Venta();
                    v.setId(rs.getInt("id_venta"));
                    v.setFecha(rs.getTimestamp("fecha"));
                    v.setMonto(rs.getDouble("monto"));
                    v.setDescripcion(rs.getString("descripcion"));
                    v.setNumeroBoleta(rs.getString("numero_boleta"));
                    v.setVendedorId(rs.getInt("id_vendedor"));
                    response.exito(v);
                } else {
                    response.message_error("Venta no encontrada con ID: " + id);
                }
            }
        } catch (SQLException e) {
            response.internal_error("VRI.findById: " + e.getMessage());
        }
        return response;
    }

    /**
     * Encuentra todas las ventas registradas.
     */
    @Override
    public Response<List<Venta>> findAll() {
        Response<List<Venta>> response = new Response<>();
        List<Venta> lista = new ArrayList<>();
        String sql = "SELECT * FROM venta";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Venta v = new Venta();
                v.setId(rs.getInt("id_venta"));
                v.setFecha(rs.getTimestamp("fecha"));
                v.setMonto(rs.getDouble("monto"));
                v.setDescripcion(rs.getString("descripcion"));
                v.setNumeroBoleta(rs.getString("numero_boleta"));
                v.setVendedorId(rs.getInt("id_vendedor"));
                lista.add(v);
            }
            response.exito(lista);

        } catch (SQLException e) {
            response.internal_error("VRI.findAll: " + e.getMessage());
        }
        return response;
    }

    @Override
    public Response<List<Venta>> findByNumeroBoleta(String numeroBoleta) {
        return null;
    }

    /**
     * Registra un intento de compra de un producto que no está en el inventario.
     *
     * @return
     */
    @Override
    public Response registrarProductoNoEncontrado(Integer idProductoSolicitado, String nombreProductoSolicitado, Integer vendedorId) {
        Response response = new Response();
        String sql = "INSERT INTO log_producto_no_encontrado (id_producto_solicitado, nombre_producto_solicitado, fecha_solicitud, id_vendedor) VALUES (?, ?, NOW(), ?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (idProductoSolicitado != null) {
                stmt.setInt(1, idProductoSolicitado);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }

            stmt.setString(2, nombreProductoSolicitado);

            if (vendedorId != null) {
                stmt.setInt(3, vendedorId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.executeUpdate();
            response.exito();

        } catch (SQLException e) {
            response.internal_error("VRI.registrarProductoNoEncontrado: " + e.getMessage());
        }
        return response;
    }

    /**
     * Retorna el listado completo de ventas junto con los detalles de cada una.
     */
    @Override
    public Response<List<VentaCompletaDTO>> listarVentasConDetalles() {
        Response<List<VentaCompletaDTO>> response = new Response<>();
        String sql = "SELECT v.id_venta, v.monto, v.fecha, " +
                "d.cantidad, d.precio_unitario, " +
                "p.nombre AS producto_nombre, p.image_path " +
                "FROM venta v " +
                "LEFT JOIN detalle_venta d ON v.id_venta = d.id_venta " +
                "LEFT JOIN producto p ON d.id_producto = p.id_producto " +
                "ORDER BY v.id_venta DESC";

        Map<Integer, VentaCompletaDTO> ventasMap = new LinkedHashMap<>();

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int idVenta = rs.getInt("id_venta");

                VentaCompletaDTO ventaDTO = ventasMap.computeIfAbsent(idVenta, k -> {
                    VentaCompletaDTO nuevaVenta = new VentaCompletaDTO();
                    nuevaVenta.setId(idVenta);
                    try {
                        nuevaVenta.setNumeroBoleta("V-" + idVenta);
                        nuevaVenta.setMonto(rs.getDouble("monto"));

                        java.sql.Timestamp ts = rs.getTimestamp("fecha");
                        nuevaVenta.setFecha(ts != null ? ts.toString() : "");

                        nuevaVenta.setClienteDni("Cliente General");
                    } catch (SQLException e) {
                        // Error interno de lectura de fila
                    }
                    return nuevaVenta;
                });

                // Si el id del producto en el detalle no es nulo, agregamos el detalle
                if (rs.getObject("cantidad") != null) {
                    DetalleVentaDTO detalleDTO = new DetalleVentaDTO();
                    detalleDTO.setCantidad(rs.getInt("cantidad"));
                    detalleDTO.setPrecio(rs.getDouble("precio_unitario"));
                    detalleDTO.setNombreProducto(rs.getString("producto_nombre"));
                    detalleDTO.setImagePath(rs.getString("image_path"));

                    ventaDTO.getDetalles().add(detalleDTO);
                }
            }

            List<VentaCompletaDTO> resultList = new ArrayList<>(ventasMap.values());
            response.exito(resultList);

        } catch (SQLException e) {
            response.internal_error("VRI.listarVentasConDetalles: " + e.getMessage());
        }

        return response;
    }

    /**
     * Retorna un reporte agrupado por días de los últimos 30 días de operación.
     */
    public Response<List<Map<String, Object>>> obtenerReporteDiario() {
        Response<List<Map<String, Object>>> response = new Response<>();
        String sql = "SELECT DATE(fecha) AS dia, SUM(monto) AS total_dia, COUNT(id_venta) AS cant_ventas " +
                "FROM venta GROUP BY DATE(fecha) ORDER BY dia DESC LIMIT 30";

        List<Map<String, Object>> reporte = new ArrayList<>();

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("fecha", rs.getString("dia"));
                fila.put("totalVendido", rs.getDouble("total_dia"));
                fila.put("cantidadVentas", rs.getInt("cant_ventas"));
                reporte.add(fila);
            }
            response.exito(reporte);

        } catch (SQLException e) {
            response.internal_error("VRI.obtenerReporteDiario: " + e.getMessage());
        }

        return response;
    }
}