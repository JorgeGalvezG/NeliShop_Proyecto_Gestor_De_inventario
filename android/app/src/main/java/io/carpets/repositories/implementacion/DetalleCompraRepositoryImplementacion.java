package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.DetalleCompra;
import io.carpets.repositories.DetalleCompraRepository;
import io.carpets.util.Response;

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
    public Response save(DetalleCompra detalle) {
        Response response = new Response();
        String sql = "INSERT INTO detalle_compra (unidades, id_producto, id_compra, precio_unitario) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, detalle.getUnidades());
            stmt.setInt(2, detalle.getProductoId());
            stmt.setInt(3, detalle.getCompraId());
            stmt.setDouble(4, detalle.getPrecioUnitario());

            if(stmt.executeUpdate() > 0){
                response.exito();
                return response;
            }
            response.internal_error("DCRI.save: Error al insertar detalle compra");
        } catch (SQLException e) {
            response.internal_error("DCRI.save: " + e.getMessage());
        }
        return response;
    }

    /**
     * Actualiza los datos de un detalle de compra específico.
     * @param detalle Contiene los datos nuevos del detalle de compra.
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public Response update(DetalleCompra detalle) {
        Response response = new Response();
        String sql = "UPDATE detalle_compra SET unidades=?, id_producto=?, id_compra=?, precio_unitario=? WHERE id_detalle_compra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, detalle.getUnidades());
            stmt.setInt(2, detalle.getProductoId());
            stmt.setInt(3, detalle.getCompraId());
            stmt.setDouble(4, detalle.getPrecioUnitario());
            stmt.setInt(5, detalle.getId());

            if(stmt.executeUpdate() > 0){
                response.exito();
                return response;
            }
            response.internal_error("DCRI.update: Error al actualizar Detalle de Compra, Id = " + detalle.getId());

        } catch (SQLException e) {
            response.internal_error("DCRI.update: " + e.getMessage());

        }
        return response;
    }

    /**
     * Elimina un detalle de compra especifico.
     * @param id Identificador del detalle de compra, se usa para ubicarlo.
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public Response delete(int id) {
        Response response = new Response();
        String sql = "DELETE FROM detalle_compra WHERE id_detalle_compra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            if(stmt.executeUpdate() > 0){
                response.exito();
                return response;
            }
            response.internal_error("DCRI.delete: Error al eliminar Detalle de Compra, Id = " + id);

        } catch (SQLException e) {
            response.internal_error("DCRI.delete: " + e.getMessage());

        }
        return response;
    }

    /**
     * Encuentra los datos de un detalle de compra específico.
     * @param id Identificador, se usa para ubicar al detalle de compra.
     * @return Un objeto DetalleCompra con los datos obtenidos, si hubo un problema se devuelve un nulo.
     */
    @Override
    public Response<DetalleCompra> findById(int id) {
        Response<DetalleCompra> response = new Response<>();
        String sql = "SELECT * FROM detalle_compra WHERE id_detalle_compra=?";
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
                response.exito(d);
                return response;
            }
            response.internal_error("DCRI.findById: Id de detalle de compra no encontrado.");
        } catch (SQLException e) {
            response.internal_error("DCRI.findById: " + e.getMessage());
        }
        return response;
    }

    /**
     * Busca los detalles de compra hechos en una compra específica.
     * @param compraId Id de la compra, se usa como identificador.
     * @return Una lista con los detalles de compra. Si no hay o hubo errores, devuelve un nulo.
     */
    @Override
    public Response<List<DetalleCompra>> findByCompraId(int compraId) {
        Response<List<DetalleCompra>> response = new Response<>();
        List<DetalleCompra> lista = new ArrayList<>();
        String sql = "SELECT * FROM detalle_compra WHERE id_compra=?";
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
            if(!lista.isEmpty()){
                response.exito(lista);
                return response;
            }

            response.internal_error("DCRI.findByCompraId: Lista vacía, probablemente dicha compra no tiene detalles.");

        } catch (SQLException e) {
            response.internal_error("DCRI.findByCompraId: " + e.getMessage());
        }
        return response;
    }

    /**
     * Retorna todos los detalles de compra.
     * @return Una lista con los detalles de compra.
     */
    @Override
    public Response<List<DetalleCompra>> findAll() {
        Response<List<DetalleCompra>> response = new Response<>();
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

            if(!lista.isEmpty()){
                response.exito(lista);
                return response;
            }

            response.internal_error("DCRI.findAll: Lista vacía, probablemente no existe detalle de compra alguno.");

        } catch (SQLException e) {
            response.internal_error("DCRI.findAll: " + e.getMessage());
        }
        return response;
    }
}