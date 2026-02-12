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

    @Override
    public boolean update(Compra compra) {
        String sql = "UPDATE compra SET descripcion=?, monto=? WHERE idcompra=?";
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

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM compra WHERE idcompra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Compra findById(int id) {
        String sql = "SELECT * FROM compra WHERE idcompra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("idcompra"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setMonto(rs.getDouble("monto"));
                return c;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Compra> findAll() {
        List<Compra> lista = new ArrayList<>();
        String sql = "SELECT * FROM compra";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("idcompra"));
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