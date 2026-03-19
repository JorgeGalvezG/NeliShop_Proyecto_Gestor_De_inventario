package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Venta;
import io.carpets.repositories.VentaRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VentaRepositoryImplementacion implements VentaRepository {

    /**
     * Registra una venta en la base de datos.
     * @param venta Contiene la información a registrar.
     * @return True si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean save(Venta venta) {
        String sql = "INSERT INTO venta (numero_boleta, fecha, monto, descripcion, id_vendedor) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, venta.getNumeroBoleta());
            stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            stmt.setDouble(3, venta.getMonto());
            stmt.setString(4, venta.getDescripcion());
            stmt.setInt(5, venta.getVendedorId());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    venta.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Actualiza una venta registrada.
     * @param venta Contiene la información nueva de la venta.
     * @return True si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean update(Venta venta) {
        String sql = "UPDATE venta SET numero_boleta=?, fecha=?, monto=?, descripcion=?, id_vendedor=? WHERE id_venta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, venta.getNumeroBoleta());
            // 🟢 Timestamp aplicado
            stmt.setTimestamp(2, new java.sql.Timestamp(venta.getFecha() != null ? venta.getFecha().getTime() : System.currentTimeMillis()));
            stmt.setDouble(3, venta.getMonto());
            stmt.setString(4, venta.getDescripcion());
            stmt.setInt(5, venta.getVendedorId());
            stmt.setInt(6, venta.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Elimina una venta usando su identificador.
     * @param id Identificador de la venta.
     * @return True si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM venta WHERE id_venta=?";
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
     * Encuentra una venta usando su Id para localizarla.
     * @param id Identificador de la venta.
     * @return Objeto venta con la información encontrada. Si no existe o hubo problemas, devuelve un nulo.
     */
    @Override
    public Venta findById(int id) {
        String sql = "SELECT * FROM venta WHERE id_venta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Venta v = new Venta();
                v.setId(rs.getInt("id_venta"));
                // 🟢 getTimestamp aplicado
                v.setFecha(rs.getTimestamp("fecha"));
                v.setMonto(rs.getDouble("monto"));
                v.setDescripcion(rs.getString("descripcion"));
                // 🟢 Extraemos numero_boleta
                v.setNumeroBoleta(rs.getString("numero_boleta"));
                v.setVendedorId(rs.getInt("id_vendedor"));
                return v;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encuentra todas las ventas registradas.
     * @return Listado con todas las ventas, si hay un error o no hay ventas, devuelve un nulo.
     */
    @Override
    public List<Venta> findAll() {
        List<Venta> lista = new ArrayList<>();
        String sql = "SELECT * FROM venta";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Venta v = new Venta();
                v.setId(rs.getInt("idventa"));
                v.setFecha(rs.getTimestamp("fecha"));
                v.setMonto(rs.getDouble("monto"));
                v.setDescripcion(rs.getString("descripcion"));
                v.setNumeroBoleta(rs.getString("numero_boleta"));
                v.setVendedorId(rs.getInt("vendedor_idvendedor"));
                lista.add(v);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Busca los detalles de venta de una venta usando el número de boleta.
     * @param numeroBoleta Identificador de la boleta.
     * @return Lista con las ventas hechas. Si no hay boleta o hay algún error, devuelve una lista vacía.
     */
    public List<Venta> findByNumeroBoleta(String numeroBoleta) {
        List<Venta> lista = new ArrayList<>();
        String sql = "SELECT * FROM venta WHERE numero_boleta=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, numeroBoleta);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Venta v = new Venta();
                v.setId(rs.getInt("idventa"));
                v.setFecha(rs.getTimestamp("fecha"));
                v.setMonto(rs.getDouble("monto"));
                v.setDescripcion(rs.getString("descripcion"));
                v.setNumeroBoleta(rs.getString("numero_boleta"));
                v.setVendedorId(rs.getInt("vendedor_idvendedor"));
                lista.add(v);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Registra un producto no encontrado. (Por qué está acá y no en ProductoRepository?)
     * @param idProductoSolicitado Id del producto no encontrado.
     * @param nombreProductoSolicitado Nombre del producto no encontrado.
     * @param vendedorId Quién hizo la venta.
     */
    @Override
    public void registrarProductoNoEncontrado(Integer idProductoSolicitado, String nombreProductoSolicitado, Integer vendedorId) {
        String sql = "INSERT INTO log_producto_no_encontrado (id_producto_solicitado, nombre_producto_solicitado, fecha_solicitud, vendedor_id) VALUES (?, ?, NOW(), ?)";
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}