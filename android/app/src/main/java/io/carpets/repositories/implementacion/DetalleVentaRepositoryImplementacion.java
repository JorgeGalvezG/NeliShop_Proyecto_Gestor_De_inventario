package io.carpets.repositories.implementacion;

import io.carpets.entidades.DetalleVenta;
import io.carpets.repositories.DetalleVentaRepository;
import io.carpets.Configuracion.ConfiguracionBaseDatos;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetalleVentaRepositoryImplementacion implements DetalleVentaRepository {

    /**
     * Registra los detalles de venta de una venta.
     * @param detalle Tiene todos los detalles de la venta.
     * @return True si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean save(DetalleVenta detalle) {
        String sql = "INSERT INTO detalle_venta (cantidad, precio_unitario, subtotal, id_venta, id_producto) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, detalle.getCantidad());
            stmt.setDouble(2, detalle.getPrecioUnitario());
            stmt.setDouble(3, detalle.getSubtotal());
            stmt.setInt(4, detalle.getVentaId());
            stmt.setInt(5, detalle.getProductoId());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    detalle.setId(rs.getInt(1));
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Actualiza un detalle de venta ya existente.
     * @param detalle Contiene la información nueva.
     * @return True si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean update(DetalleVenta detalle) {
        String sql = "UPDATE detalle_venta SET cantidad=?, precio_unitario=?, subtotal=?, id_venta=?, id_producto=? WHERE id_detalle_venta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, detalle.getCantidad());
            stmt.setDouble(2, detalle.getPrecioUnitario());
            stmt.setDouble(3, detalle.getSubtotal());
            stmt.setInt(4, detalle.getVentaId());
            stmt.setInt(5, detalle.getProductoId());
            stmt.setInt(6, detalle.getId());

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina un detalle de venta de la base de datos.
     * @param id Identificador del detalle de venta.
     * @return True si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM detalle_venta WHERE id_detalle_venta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Encuentra un detalle de venta usando su Id
     * @param id Identificador del detalle de venta.
     * @return Objeto detalle de venta con la información encontrada. Si no existe retorna un null.
     */
    @Override
    public DetalleVenta findById(int id) {
        String sql = "SELECT * FROM detalle_venta WHERE id_detalle_venta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                DetalleVenta d = new DetalleVenta();
                d.setId(rs.getInt("id_detalle_venta"));
                d.setCantidad(rs.getInt("cantidad"));
                d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                d.setSubtotal(rs.getDouble("subtotal"));
                d.setVentaId(rs.getInt("id_venta"));
                d.setProductoId(rs.getInt("id_producto"));
                return d;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encuentra los detalles de venta usando el Id de una venta específica.
     * @param ventaId El identificador de la venta.
     * @return Listado con los detalles de venta de la venta especificada.
     */
    @Override
    public List<DetalleVenta> findByVenta(int ventaId) {
        List<DetalleVenta> lista = new ArrayList<>();
        String sql = "SELECT * FROM detalle_venta WHERE id_venta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ventaId);
            ResultSet rs = stmt.executeQuery();
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

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }


}