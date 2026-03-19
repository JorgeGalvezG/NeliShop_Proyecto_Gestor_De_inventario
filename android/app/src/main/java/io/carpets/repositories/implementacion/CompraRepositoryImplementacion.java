package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Compra;
import io.carpets.repositories.CompraRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
    Implementación del repositorio para la entidad Compra
    se encarga de las operaciones CRUD en la base de datos.
*/


public class CompraRepositoryImplementacion implements CompraRepository {

    /**
     * Registra una compra dentro de la base de datos.
     * @param compra Contiene todos los datos de la compra.
     * @return Retorna 'true' si todo salió bien, 'false' si hubo algun problema.
     */
        @Override
        public boolean save(Compra compra) {
        String sql = "INSERT INTO compra (descripcion, monto) VALUES (?, ?)";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, compra.getDescripcion());
            stmt.setDouble(2, compra.getMonto());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    compra.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Actualiza los datos de la compra.
     * @param compra Contiene los datos de la compra a actualizar.
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public boolean update(Compra compra) {
        String sql = "UPDATE compra SET descripcion=?, monto=? WHERE id_compra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, compra.getDescripcion());
            stmt.setDouble(2, compra.getMonto());
            stmt.setInt(3, compra.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Elimina una compra usando su id.
     * @param id Identificador de la compra.
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM compra WHERE id_compra=?";
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
     * Encuentra una compra usando su id.
     * @param id Identificador de la compra.
     * @return true si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public Compra findById(int id) {
        String sql = "SELECT * FROM compra WHERE id_compra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("id_compra"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setMonto(rs.getDouble("monto"));
                return c;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encuentra todas las compras registradas.
     * @return Una lista con todas las compras y, si hubo algún error o no hay compras, una lista vacía.
     */
    @Override
    public List<Compra> findAll() {
        List<Compra> lista = new ArrayList<>();
        String sql = "SELECT * FROM compra";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("id_compra"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setMonto(rs.getDouble("monto"));
                lista.add(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}