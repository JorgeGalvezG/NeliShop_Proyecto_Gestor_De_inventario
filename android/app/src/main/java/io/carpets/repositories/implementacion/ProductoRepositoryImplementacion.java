package io.carpets.repositories.implementacion;

import android.os.Build;

import androidx.annotation.RequiresApi;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Producto;
import io.carpets.repositories.ProductoRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoRepositoryImplementacion implements ProductoRepository {

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    @Override
    public boolean save(Producto producto) {
        // 1. PRIMERO: Asegurar que la categoría existe
        if (!asegurarCategoria(producto.getCategoriaNombre())) {
            System.out.println("Error al gestionar la categoría: " + producto.getCategoriaNombre());
            return false;
        }

        // 2. SEGUNDO: Insertar el producto (AHORA CON IMAGEN)
        // Se agregó ", image_path" y un "?" extra
        String sql = "INSERT INTO producto (nombre, fecha_ingreso, precio_compra, precio_venta, cantidad, categoria_nombre, image_path, precio_oferta) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, producto.getNombre());

            // Manejo seguro de fecha
            if (producto.getFechaIngreso() != null) {
                stmt.setDate(2, new java.sql.Date(producto.getFechaIngreso().getTime()));
            } else {
                stmt.setDate(2, new java.sql.Date(new java.util.Date().getTime()));
            }

            stmt.setDouble(3, producto.getPrecioCompra());
            stmt.setDouble(4, producto.getPrecioVenta());
            stmt.setInt(5, producto.getCantidad());
            stmt.setString(6, producto.getCategoriaNombre());

            // --- NUEVO: GUARDAR RUTA DE IMAGEN ---
            stmt.setString(7, producto.getImagePath());

            if (producto.getPrecioOferta() != null && producto.getPrecioOferta() > 0) {
                stmt.setDouble(8, producto.getPrecioOferta());
            } else {
                stmt.setNull(8, java.sql.Types.DECIMAL);
            }

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    producto.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Verifica si la categoría existe. Si no existe, la inserta.
     */
    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    private boolean asegurarCategoria(String categoriaNombre) {
        if (categoriaNombre == null || categoriaNombre.trim().isEmpty()) {
            return false;
        }

        String sqlCheck = "SELECT COUNT(*) FROM categoria WHERE nombre = ?";
        String sqlInsert = "INSERT INTO categoria (nombre) VALUES (?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection()) {

            // 1. Verificar existencia
            try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheck)) {
                stmtCheck.setString(1, categoriaNombre);
                ResultSet rs = stmtCheck.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return true; // Ya existe
                }
            }

            // 2. Insertar si no existe
            try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert)) {
                stmtInsert.setString(1, categoriaNombre);
                return stmtInsert.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Producto producto) {
        // Se agregó "image_path=?" al SQL
        String sql = "UPDATE producto SET nombre=?, fecha_ingreso=?, precio_compra=?, precio_venta=?, cantidad=?, categoria_nombre=?, image_path=?, precio_oferta=? WHERE idproducto=?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, producto.getNombre());

            if (producto.getFechaIngreso() != null) {
                stmt.setDate(2, new java.sql.Date(producto.getFechaIngreso().getTime()));
            } else {
                stmt.setDate(2, new java.sql.Date(new java.util.Date().getTime()));
            }

            stmt.setDouble(3, producto.getPrecioCompra());
            stmt.setDouble(4, producto.getPrecioVenta());
            stmt.setInt(5, producto.getCantidad());
            stmt.setString(6, producto.getCategoriaNombre());

            // --- NUEVO: ACTUALIZAR RUTA DE IMAGEN ---
            stmt.setString(7, producto.getImagePath());

            // El ID ahora es el parámetro 8
            stmt.setInt(8, producto.getId());

            if (producto.getPrecioOferta() != null && producto.getPrecioOferta() > 0) {
                stmt.setDouble(8, producto.getPrecioOferta());
            } else {
                stmt.setNull(8, java.sql.Types.DECIMAL);
            }

            stmt.setInt(9, producto.getId()); // El ID pasa a ser el 9

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM producto WHERE idproducto=?";
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
    public Producto findById(int id) {
        String sql = "SELECT * FROM producto WHERE idproducto=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapearProducto(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Producto> findAll() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM producto";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public double getGananciaTotal() {
        // FORMULA CORREGIDA: (Precio Venta * Cantidad) - (Precio Compra * Cantidad)
        String sql = "SELECT SUM( (dv.precio_unitario * dv.cantidad) - (p.precio_compra * dv.cantidad) ) AS ganancia_real " +
                "FROM detalle_venta dv " +
                "INNER JOIN producto p ON dv.idproducto = p.idproducto";

        double ganancia = 0;
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                ganancia = rs.getDouble("ganancia_real");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error calculando ganancia: " + e.getMessage());
        }
        return ganancia;
    }

    @Override
    public List<Producto> findByCategoria(String categoriaNombre) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE categoria_nombre=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoriaNombre);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public List<Producto> findByNombre(String nombre) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE nombre LIKE ?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // Método auxiliar para evitar código duplicado
    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("idproducto"));
        p.setNombre(rs.getString("nombre"));
        p.setFechaIngreso(rs.getDate("fecha_ingreso"));
        p.setPrecioCompra(rs.getDouble("precio_compra"));
        p.setPrecioVenta(rs.getDouble("precio_venta"));
        p.setCantidad(rs.getInt("cantidad"));
        p.setCategoriaNombre(rs.getString("categoria_nombre"));
        p.setImagePath(rs.getString("image_path")); // Ya incluía la lectura de imagen
        p.setPrecioOferta(rs.getObject("precio_oferta") != null ? rs.getDouble("precio_oferta") : null);
        return p;
    }

    @Override
    public boolean existeIdById(int id) {
        String sql = "SELECT COUNT(*) AS cnt FROM producto WHERE idproducto = ?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("cnt") > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}