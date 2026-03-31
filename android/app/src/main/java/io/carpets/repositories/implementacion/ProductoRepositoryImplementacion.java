package io.carpets.repositories.implementacion;

import androidx.annotation.NonNull;
import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Producto;
import io.carpets.repositories.ProductoRepository;
import io.carpets.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del repositorio para la gestión de productos.
 * Maneja todas las operaciones CRUD sobre la tabla producto.
 */
public class ProductoRepositoryImplementacion implements ProductoRepository {

    // ========================================================================
    // MÉTODOS PRIVADOS AUXILIARES
    // ========================================================================

    /**
     * Verifica si una categoría existe en la base de datos. Si no existe, la crea automáticamente.
     *
     * @param categoriaNombre Nombre de la categoría a verificar/crear
     * @return Response indicando éxito o error
     * @throws SQLException Si hay un error en la conexión o consulta SQL
     */
    private Response asegurarCategoria(String categoriaNombre) throws SQLException {
        Response response = new Response();

        // Validación de entrada
        if (categoriaNombre == null || categoriaNombre.trim().isEmpty()) {
            response.internal_error("PRI.asegurarCategoria: El nombre de categoría no puede estar vacío");
            return response;
        }

        String sqlCheck = "SELECT COUNT(*) FROM categoria WHERE nombre = ?";
        String sqlInsert = "INSERT INTO categoria (nombre) VALUES (?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection()) {
            // 1. Verificar si existe
            try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheck)) {
                stmtCheck.setString(1, categoriaNombre.trim());
                try (ResultSet rs = stmtCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Ya existe, todo bien
                        response.exito();
                        return response;
                    }
                }
            }

            // 2. Insertar si no existe
            try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert)) {
                stmtInsert.setString(1, categoriaNombre.trim());
                int rows = stmtInsert.executeUpdate();

                if (rows > 0) {
                    response.exito();
                } else {
                    response.internal_error("PRI.asegurarCategoria: No se pudo crear la categoría");
                }

                return response;
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto Producto.
     *
     * @param rs ResultSet con los datos del producto
     * @return Producto mapeado
     * @throws SQLException Si hay error al leer el ResultSet
     */
    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id_producto"));
        p.setNombre(rs.getString("nombre"));
        p.setFechaIngreso(rs.getDate("fecha_ingreso"));
        p.setPrecioCompra(rs.getDouble("precio_compra"));
        p.setPrecioVenta(rs.getDouble("precio_venta"));
        p.setCantidad(rs.getInt("cantidad"));
        p.setCategoriaNombre(rs.getString("categoria_nombre"));
        p.setImagePath(rs.getString("image_path"));

        // Manejo seguro de precio_oferta (puede ser NULL)
        double oferta = rs.getDouble("precio_oferta");
        if (!rs.wasNull() && oferta > 0) {
            p.setPrecioOferta(oferta);
        } else {
            p.setPrecioOferta(null);
        }

        return p;
    }

    // ========================================================================
    // MÉTODOS CRUD
    // ========================================================================

    /**
     * Registra un nuevo producto en la base de datos.
     *
     * IMPORTANTE: Este método modifica el objeto 'producto' pasado como parámetro,
     * asignándole el ID generado por la base de datos.
     *
     * @param producto Producto a registrar (no puede ser nulo)
     * @return Response indicando éxito o error
     */
    @Override
    public Response save(@NonNull Producto producto) {
        Response response = new Response();

        // Validación de entrada
        if (producto == null) {
            response.internal_error("PRI.save: El producto no puede ser nulo");
            return response;
        }

        if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
            response.internal_error("PRI.save: El nombre del producto no puede estar vacío");
            return response;
        }

        if (producto.getPrecioCompra() < 0) {
            response.internal_error("PRI.save: El precio de compra no puede ser negativo");
            return response;
        }

        if (producto.getPrecioVenta() <= 0) {
            response.internal_error("PRI.save: El precio de venta debe ser mayor a 0");
            return response;
        }

        if (producto.getCantidad() < 0) {
            response.internal_error("PRI.save: La cantidad no puede ser negativa");
            return response;
        }

        // Asegurar que la categoría existe
        try {
            Response categoriaResponse = asegurarCategoria(producto.getCategoriaNombre());
            if (!categoriaResponse.isOk()) {
                response.internal_error("PRI.save: No se pudo asegurar la categoría - " + categoriaResponse.getMensaje());
                return response;
            }
        } catch (SQLException e) {
            response.internal_error("PRI.save: Error al verificar categoría - " + e.getMessage());
            return response;
        }

        String sql = "INSERT INTO producto " +
                "(nombre, fecha_ingreso, precio_compra, precio_venta, cantidad, categoria_nombre, image_path, precio_oferta) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // 1. Nombre
            stmt.setString(1, producto.getNombre().trim());

            // 2. Fecha de ingreso
            if (producto.getFechaIngreso() != null) {
                stmt.setDate(2, new java.sql.Date(producto.getFechaIngreso().getTime()));
            } else {
                stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            }

            // 3-7. Datos del producto
            stmt.setDouble(3, producto.getPrecioCompra());
            stmt.setDouble(4, producto.getPrecioVenta());
            stmt.setInt(5, producto.getCantidad());
            stmt.setString(6, producto.getCategoriaNombre().trim());
            stmt.setString(7, producto.getImagePath());

            // 8. Precio de oferta (puede ser NULL)
            if (producto.getPrecioOferta() != null && producto.getPrecioOferta() > 0) {
                stmt.setDouble(8, producto.getPrecioOferta());
            } else {
                stmt.setNull(8, java.sql.Types.DECIMAL);
            }

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                // Recuperar ID generado
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        producto.setId(generatedId);
                        response.exito();
                        return response;
                    }
                }
                // Se insertó pero no se obtuvo el ID
                response.internal_error("PRI.save: Producto insertado pero no se pudo recuperar el ID");
                return response;
            }

            response.internal_error("PRI.save: No se insertó ningún registro");

        } catch (SQLException e) {
            response.internal_error("PRI.save: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Actualiza la información de un producto existente.
     *
     * @param producto Producto con los datos actualizados (debe incluir el ID)
     * @return Response indicando éxito o error
     */
    @Override
    public Response update(Producto producto) {
        Response response = new Response();

        // Validación de entrada
        if (producto == null) {
            response.internal_error("PRI.update: El producto no puede ser nulo");
            return response;
        }

        if (producto.getId() <= 0) {
            response.internal_error("PRI.update: El ID del producto debe ser mayor a 0");
            return response;
        }

        if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
            response.internal_error("PRI.update: El nombre del producto no puede estar vacío");
            return response;
        }

        if (producto.getPrecioCompra() < 0) {
            response.internal_error("PRI.update: El precio de compra no puede ser negativo");
            return response;
        }

        if (producto.getPrecioVenta() <= 0) {
            response.internal_error("PRI.update: El precio de venta debe ser mayor a 0");
            return response;
        }

        if (producto.getCantidad() < 0) {
            response.internal_error("PRI.update: La cantidad no puede ser negativa");
            return response;
        }

        String sql = "UPDATE producto " +
                "SET nombre = ?, fecha_ingreso = ?, precio_compra = ?, precio_venta = ?, " +
                "    cantidad = ?, categoria_nombre = ?, image_path = ?, precio_oferta = ? " +
                "WHERE id_producto = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 1. Nombre
            stmt.setString(1, producto.getNombre().trim());

            // 2. Fecha
            if (producto.getFechaIngreso() != null) {
                stmt.setDate(2, new java.sql.Date(producto.getFechaIngreso().getTime()));
            } else {
                stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            }

            // 3-7. Datos del producto
            stmt.setDouble(3, producto.getPrecioCompra());
            stmt.setDouble(4, producto.getPrecioVenta());
            stmt.setInt(5, producto.getCantidad());
            stmt.setString(6, producto.getCategoriaNombre().trim());
            stmt.setString(7, producto.getImagePath());

            // 8. Precio de oferta
            if (producto.getPrecioOferta() != null && producto.getPrecioOferta() > 0) {
                stmt.setDouble(8, producto.getPrecioOferta());
            } else {
                stmt.setNull(8, java.sql.Types.DECIMAL);
            }

            // 9. ID (condición WHERE)
            stmt.setInt(9, producto.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                response.exito();
            } else {
                response.internal_error("PRI.update: No existe un producto con id = " + producto.getId());
            }

        } catch (SQLException e) {
            response.internal_error("PRI.update: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Elimina un producto de la base de datos.
     *
     * ADVERTENCIA: Esta operación puede fallar si existen detalles de venta o compra
     * asociados debido a restricciones de clave foránea.
     *
     * @param id ID del producto a eliminar (debe ser > 0)
     * @return Response indicando éxito o error
     */
    @Override
    public Response delete(int id) {
        Response response = new Response();

        // Validación de entrada
        if (id <= 0) {
            response.internal_error("PRI.delete: El ID debe ser mayor a 0");
            return response;
        }

        String sql = "DELETE FROM producto WHERE id_producto = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                response.exito();
            } else {
                response.internal_error("PRI.delete: No existe un producto con id = " + id);
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            response.internal_error("PRI.delete: No se puede eliminar el producto porque tiene ventas o compras asociadas");
        } catch (SQLException e) {
            response.internal_error("PRI.delete: Error SQL - " + e.getMessage());
        }

        return response;
    }

    // ========================================================================
    // MÉTODOS DE CONSULTA
    // ========================================================================

    /**
     * Encuentra un producto por su ID.
     *
     * @param id Identificador del producto (debe ser > 0)
     * @return Response<Producto> con el producto encontrado o error
     */
    @Override
    public Response<Producto> findById(int id) {
        Response<Producto> response = new Response<>();

        // Validación de entrada
        if (id <= 0) {
            response.internal_error("PRI.findById: El ID debe ser mayor a 0");
            return response;
        }

        String sql = "SELECT " +
                "  id_producto, nombre, fecha_ingreso, precio_compra, precio_venta, " +
                "  cantidad, categoria_nombre, image_path, precio_oferta " +
                "FROM producto " +
                "WHERE id_producto = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Producto producto = mapearProducto(rs);
                    response.exito(producto);
                    return response;
                }
            }

            response.internal_error("PRI.findById: No se encontró producto con id = " + id);

        } catch (SQLException e) {
            response.internal_error("PRI.findById: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Obtiene todos los productos de la base de datos.
     *
     * @return Response<List<Producto>> con todos los productos o error
     */
    @Override
    public Response<List<Producto>> findAll() {
        Response<List<Producto>> response = new Response<>();
        List<Producto> lista = new ArrayList<>();

        String sql = "SELECT " +
                "  id_producto, nombre, fecha_ingreso, precio_compra, precio_venta, " +
                "  cantidad, categoria_nombre, image_path, precio_oferta " +
                "FROM producto " +
                "ORDER BY nombre ASC";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }

            if (lista.isEmpty()) {
                response.internal_error("PRI.findAll: No hay productos registrados en la base de datos");
            } else {
                response.exito(lista);
            }

        } catch (SQLException e) {
            response.internal_error("PRI.findAll: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Busca productos por categoría.
     *
     * @param categoriaNombre Nombre de la categoría
     * @return Response<List<Producto>> con los productos de la categoría o error
     */
    @Override
    public Response<List<Producto>> findByCategoria(String categoriaNombre) {
        Response<List<Producto>> response = new Response<>();
        List<Producto> lista = new ArrayList<>();

        // Validación de entrada
        if (categoriaNombre == null || categoriaNombre.trim().isEmpty()) {
            response.internal_error("PRI.findByCategoria: El nombre de categoría no puede estar vacío");
            return response;
        }

        String sql = "SELECT " +
                "  id_producto, nombre, fecha_ingreso, precio_compra, precio_venta, " +
                "  cantidad, categoria_nombre, image_path, precio_oferta " +
                "FROM producto " +
                "WHERE categoria_nombre = ? " +
                "ORDER BY nombre ASC";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoriaNombre.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearProducto(rs));
                }

                if (lista.isEmpty()) {
                    response.internal_error("PRI.findByCategoria: No se encontraron productos en la categoría '" + categoriaNombre + "'");
                } else {
                    response.exito(lista);
                }
            }

        } catch (SQLException e) {
            response.internal_error("PRI.findByCategoria: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Busca productos cuyo nombre contenga el texto especificado (búsqueda parcial).
     *
     * @param nombre Texto a buscar en el nombre del producto
     * @return Response<List<Producto>> con los productos encontrados o error
     */
    @Override
    public Response<List<Producto>> findByNombre(String nombre) {
        Response<List<Producto>> response = new Response<>();
        List<Producto> lista = new ArrayList<>();

        // Validación de entrada
        if (nombre == null || nombre.trim().isEmpty()) {
            response.internal_error("PRI.findByNombre: El nombre a buscar no puede estar vacío");
            return response;
        }

        String sql = "SELECT " +
                "  id_producto, nombre, fecha_ingreso, precio_compra, precio_venta, " +
                "  cantidad, categoria_nombre, image_path, precio_oferta " +
                "FROM producto " +
                "WHERE nombre LIKE ? " +
                "ORDER BY nombre ASC";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre.trim() + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearProducto(rs));
                }

                if (lista.isEmpty()) {
                    response.internal_error("PRI.findByNombre: No se encontraron productos con nombre '" + nombre + "'");
                } else {
                    response.exito(lista);
                }
            }

        } catch (SQLException e) {
            response.internal_error("PRI.findByNombre: Error SQL - " + e.getMessage());
        }

        return response;
    }

    // ========================================================================
    // MÉTODOS DE UTILIDAD
    // ========================================================================

    /**
     * Calcula la ganancia total de todas las ventas realizadas.
     *
     * La ganancia se calcula como: (precio_venta - precio_compra) * cantidad vendida
     *
     * @return Response<Double> con la ganancia total o error
     */
    @Override
    public Response<Double> getGananciaTotal() {
        Response<Double> response = new Response<>();

        String sql = "SELECT SUM((d.precio_unitario - p.precio_compra) * d.cantidad) AS ganancia_total " +
                "FROM detalle_venta d " +
                "JOIN producto p ON d.id_producto = p.id_producto";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                double ganancia = rs.getDouble("ganancia_total");
                // Si no hay ventas, SUM retorna NULL
                if (rs.wasNull()) {
                    ganancia = 0.0;
                }
                response.exito(ganancia);
            } else {
                response.exito(0.0);
            }

        } catch (SQLException e) {
            response.internal_error("PRI.getGananciaTotal: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Verifica si existe un producto con el ID especificado.
     *
     * @param id Identificador del producto (debe ser > 0)
     * @return Response indicando si existe (exito) o no existe (error)
     */
    @Override
    public Response<Boolean> existeIdById(int id) {
        Response<Boolean> response = new Response<>();

        // Validación de entrada
        if (id <= 0) {
            response.internal_error("PRI.existeIdById: El ID debe ser mayor a 0");
            return response;
        }

        String sql = "SELECT COUNT(*) FROM producto WHERE id_producto = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count > 0) {
                        response.exito();
                    } else {
                        response.internal_error("PRI.existeIdById: No existe un producto con id = " + id);
                    }
                }
            }

        } catch (SQLException e) {
            response.internal_error("PRI.existeIdById: Error SQL - " + e.getMessage());
        }

        return response;
    }
}