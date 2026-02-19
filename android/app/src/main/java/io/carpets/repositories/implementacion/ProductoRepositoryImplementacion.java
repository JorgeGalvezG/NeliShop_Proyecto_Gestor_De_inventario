package io.carpets.repositories.implementacion;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Producto;
import io.carpets.repositories.ProductoRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoRepositoryImplementacion implements ProductoRepository {

    /**
     * Verifica si la categoría existe. Si no existe, la inserta.
     * Mover este método privado arriba o abajo no importa, pero debe estar fuera de otros métodos.
     */
    private boolean asegurarCategoria(String categoriaNombre) throws SQLException {
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
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    @Override
    public boolean save(@NonNull Producto producto) {
        // 1. Asegurar categoría
        try {
            if (!asegurarCategoria(producto.getCategoriaNombre())) {
                System.out.println("Error: Categoría inválida o no se pudo crear.");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        // 2. SQL Correcto (8 parámetros)
        String sql = "INSERT INTO producto (nombre, fecha_ingreso, precio_compra, precio_venta, cantidad, categoria_nombre, image_path, precio_oferta) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Param 1: Nombre
            stmt.setString(1, producto.getNombre());

            // Param 2: Fecha
            if (producto.getFechaIngreso() != null) {
                stmt.setDate(2, new java.sql.Date(producto.getFechaIngreso().getTime()));
            } else {
                stmt.setDate(2, new java.sql.Date(new java.util.Date().getTime()));
            }

            // Param 3, 4, 5, 6, 7
            stmt.setDouble(3, producto.getPrecioCompra());
            stmt.setDouble(4, producto.getPrecioVenta());
            stmt.setInt(5, producto.getCantidad());
            stmt.setString(6, producto.getCategoriaNombre());
            stmt.setString(7, producto.getImagePath());

            // Param 8: Precio Oferta (Puede ser NULL)
            if (producto.getPrecioOferta() != null && producto.getPrecioOferta() > 0) {
                stmt.setDouble(8, producto.getPrecioOferta());
            } else {
                stmt.setNull(8, java.sql.Types.DECIMAL);
            }

            int rows = stmt.executeUpdate();

            // Recuperar ID generado (opcional pero recomendado)
            if (rows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        producto.setId(rs.getInt(1));
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean update(Producto producto) {
        // SQL: 8 campos a actualizar + 1 condición WHERE = 9 parámetros en total
        String sql = "UPDATE producto SET nombre=?, fecha_ingreso=?, precio_compra=?, precio_venta=?, cantidad=?, categoria_nombre=?, image_path=?, precio_oferta=? WHERE idproducto=?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 1. Nombre
            stmt.setString(1, producto.getNombre());

            // 2. Fecha
            if (producto.getFechaIngreso() != null) {
                stmt.setDate(2, new java.sql.Date(producto.getFechaIngreso().getTime()));
            } else {
                stmt.setDate(2, new java.sql.Date(new java.util.Date().getTime()));
            }

            // 3, 4, 5, 6, 7
            stmt.setDouble(3, producto.getPrecioCompra());
            stmt.setDouble(4, producto.getPrecioVenta());
            stmt.setInt(5, producto.getCantidad());
            stmt.setString(6, producto.getCategoriaNombre());
            stmt.setString(7, producto.getImagePath());

            // 8. Precio Oferta
            if (producto.getPrecioOferta() != null && producto.getPrecioOferta() > 0) {
                stmt.setDouble(8, producto.getPrecioOferta());
            } else {
                stmt.setNull(8, java.sql.Types.DECIMAL);
            }

            // 9. ID (Condición WHERE)
            stmt.setInt(9, producto.getId());

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
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearProducto(rs);
                }
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
    public List<Producto> findByCategoria(String categoriaNombre) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE categoria_nombre=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, categoriaNombre);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearProducto(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public List<Producto> findByNombre(String nombre) {
        List<Producto> lista = new ArrayList<>();
        // Usamos LIKE ? para buscar coincidencias parciales
        String sql = "SELECT * FROM producto WHERE nombre LIKE ?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Agregamos los % aquí, Java maneja las comillas
            stmt.setString(1, "%" + nombre + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearProducto(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public double getGananciaTotal() {
        // Corrección en la lógica: Usar nombres de tablas consistentes
        String sql = "SELECT SUM((d.precio_unitario - p.precio_compra) * d.cantidad) AS ganancia_total " +
                "FROM detalle_venta d " +
                "JOIN producto p ON d.idproducto = p.idproducto";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("ganancia_total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public boolean existeIdById(int id) {
        String sql = "SELECT COUNT(*) FROM producto WHERE idproducto = ?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Método auxiliar para mapear ResultSet a Objeto
    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("idproducto"));
        p.setNombre(rs.getString("nombre"));
        p.setFechaIngreso(rs.getDate("fecha_ingreso"));
        p.setPrecioCompra(rs.getDouble("precio_compra"));
        p.setPrecioVenta(rs.getDouble("precio_venta"));
        p.setCantidad(rs.getInt("cantidad"));
        p.setCategoriaNombre(rs.getString("categoria_nombre"));
        p.setImagePath(rs.getString("image_path"));

        // Manejo seguro de nulos para precio_oferta
        double oferta = rs.getDouble("precio_oferta");
        if (!rs.wasNull() && oferta > 0) {
            p.setPrecioOferta(oferta);
        } else {
            p.setPrecioOferta(null);
        }

        return p;
    }
}