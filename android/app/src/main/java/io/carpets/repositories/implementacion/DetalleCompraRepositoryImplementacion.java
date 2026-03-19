package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.DetalleCompra;
import io.carpets.repositories.DetalleCompraRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetalleCompraRepositoryImplementacion implements DetalleCompraRepository {

    /**
     * Registra un detalle de compra.
     * @param detalle Contiene todos los datos sobre el detalle de compra.
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public boolean save(DetalleCompra detalle) {

        String sql = "INSERT INTO `detalle_compra` (unidades, id_producto, id_compra, precio_unitario) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, detalle.getUnidades());
            stmt.setInt(2, detalle.getProductoId());
            stmt.setInt(3, detalle.getCompraId());
            stmt.setDouble(4, detalle.getPrecioUnitario());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Actualiza los datos de un detalle de compra específico.
     * @param detalle Contiene los datos nuevos del detalle de compra.
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public boolean update(DetalleCompra detalle) {

        String sql = "UPDATE `detalle_compra` SET unidades=?, id_producto=?, id_compra=?, precio_unitario=? WHERE id_detalle_compra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, detalle.getUnidades());
            stmt.setInt(2, detalle.getProductoId());
            stmt.setInt(3, detalle.getCompraId());
            stmt.setDouble(4, detalle.getPrecioUnitario());
            stmt.setInt(5, detalle.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Elimina un detalle de compra especifico.
     * @param id Identificador del detalle de compra, se usa para ubicarlo.
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public boolean delete(int id) {

        String sql = "DELETE FROM `detalle_compra` WHERE id_detalle_compra=?";
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
     * Encuentra los datos de un detalle de compra específico.
     * @param id Identificador, se usa para ubicar al detalle de compra.
     * @return Un objeto DetalleCompra con los datos obtenidos, si hubo un problema se devuelve un nulo.
     */
    @Override
    public DetalleCompra findById(int id) {

        String sql = "SELECT * FROM `detalle_compra` WHERE id_detalle_compra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                DetalleCompra d = new DetalleCompra();
                d.setId(rs.getInt("id_detalle_compra"));
                d.setUnidades(rs.getInt("unidades"));
                d.setProductoId(rs.getInt("id_producto"));
                d.setCompraId(rs.getInt("id_compra"));
                d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                return d;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Busca los detalles de compra hechos en una compra específica.
     * @param compraId Id de la compra, se usa como identificador.
     * @return Una lista con los detalles de compra. Si no hay o hubo errores, devuelve un nulo.
     */
    @Override
    public List<DetalleCompra> findByCompraId(int compraId) {
        List<DetalleCompra> lista = new ArrayList<>();
        String sql = "SELECT * FROM `detalle_compra` WHERE id_compra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, compraId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                DetalleCompra d = new DetalleCompra();
                d.setId(rs.getInt("id_detalle_compra"));
                d.setUnidades(rs.getInt("unidades"));
                d.setProductoId(rs.getInt("id_producto"));
                d.setCompraId(rs.getInt("id_compra"));
                d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                lista.add(d);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Retorna todos los detalles de compra.
     * @return Una lista con los detalles de compra.
     */
    @Override
    public List<DetalleCompra> findAll() {

        List<DetalleCompra> lista = new ArrayList<>();
        String sql = "SELECT * FROM `detalle_compra`";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                DetalleCompra d = new DetalleCompra();
                d.setId(rs.getInt("id_detalle_compra"));
                d.setUnidades(rs.getInt("unidades"));
                d.setProductoId(rs.getInt("id_producto"));
                d.setCompraId(rs.getInt("id_compra"));
                d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                lista.add(d);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}