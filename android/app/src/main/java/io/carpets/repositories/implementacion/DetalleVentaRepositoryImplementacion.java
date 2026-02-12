package io.carpets.repositories.implementacion;

import io.carpets.entidades.DetalleVenta;
import io.carpets.repositories.DetalleVentaRepository;
import io.carpets.Configuracion.ConfiguracionBaseDatos;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetalleVentaRepositoryImplementacion implements DetalleVentaRepository {

    @Override
    public boolean save(DetalleVenta detalle) { // ✅ Cambiado a boolean
        String sql = "INSERT INTO detalle_venta (cantidad, precio_unitario, subtotal, venta_idventa, idproducto) VALUES (?, ?, ?, ?, ?)";
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
                return true; // ✅ Retorna true si se insertó
            }
            return false; // ✅ Retorna false si no se insertó

        } catch (SQLException e) {
            e.printStackTrace();
            return false; // ✅ Retorna false en caso de error
        }
    }

    @Override
    public boolean update(DetalleVenta detalle) { // ✅ Cambiado a boolean
        String sql = "UPDATE detalle_venta SET cantidad=?, precio_unitario=?, subtotal=?, venta_idventa=?, idproducto=? WHERE iddetalle_venta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, detalle.getCantidad());
            stmt.setDouble(2, detalle.getPrecioUnitario());
            stmt.setDouble(3, detalle.getSubtotal());
            stmt.setInt(4, detalle.getVentaId());
            stmt.setInt(5, detalle.getProductoId());
            stmt.setInt(6, detalle.getId());

            int rows = stmt.executeUpdate();
            return rows > 0; // ✅ Retorna true si se actualizó

        } catch (SQLException e) {
            e.printStackTrace();
            return false; // ✅ Retorna false en caso de error
        }
    }

    @Override
    public boolean delete(int id) { // ✅ Cambiado parámetro a int (no DetalleVenta)
        String sql = "DELETE FROM detalle_venta WHERE iddetalle_venta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            return rows > 0; // ✅ Retorna true si se eliminó

        } catch (SQLException e) {
            e.printStackTrace();
            return false; // ✅ Retorna false en caso de error
        }
    }

    @Override
    public DetalleVenta findById(int id) { // ✅ Cambiado parámetro a int
        String sql = "SELECT * FROM detalle_venta WHERE iddetalle_venta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                DetalleVenta d = new DetalleVenta();
                d.setId(rs.getInt("iddetalle_venta"));
                d.setCantidad(rs.getInt("cantidad"));
                d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                d.setSubtotal(rs.getDouble("subtotal"));
                d.setVentaId(rs.getInt("venta_idventa"));
                d.setProductoId(rs.getInt("idproducto"));
                return d;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<DetalleVenta> findByVenta(int ventaId) { // ✅ Cambiado nombre del método
        List<DetalleVenta> lista = new ArrayList<>();
        String sql = "SELECT * FROM detalle_venta WHERE venta_idventa=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ventaId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                DetalleVenta d = new DetalleVenta();
                d.setId(rs.getInt("iddetalle_venta"));
                d.setCantidad(rs.getInt("cantidad"));
                d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                d.setSubtotal(rs.getDouble("subtotal"));
                d.setVentaId(rs.getInt("venta_idventa"));
                d.setProductoId(rs.getInt("idproducto"));
                lista.add(d);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }


}