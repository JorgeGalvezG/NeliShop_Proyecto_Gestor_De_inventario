package io.carpets.repositories.implementacion;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Producto;
import io.carpets.repositories.ProductoRepository;
import io.carpets.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoRepositoryImplementacion implements ProductoRepository {

    /**
     * Verifica si la categoría existe. Si no existe, la inserta.
     * Mover este método privado arriba o abajo no importa, pero debe estar fuera de otros métodos.
     */
    private Response asegurarCategoria(String categoriaNombre) throws SQLException {
        Response response = new Response();
        if (categoriaNombre == null || categoriaNombre.trim().isEmpty()) {
            response.internal_error("PRI.asegurarCategoria: Parámetro nulo o vacío.");
            return response;
        }

        String sqlCheck = "SELECT COUNT(*) FROM categoria WHERE nombre = ?";
        String sqlInsert = "INSERT INTO categoria (nombre) VALUES (?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection()) {
            // 1. Verificar existencia
            try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheck)) {
                stmtCheck.setString(1, categoriaNombre);
                ResultSet rs = stmtCheck.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    response.internal_error("PRI.asegurarCategoria: La categoría no existe en la base de datos.");
                    return response;
                }
            }
            // 2. Insertar si no existe
            try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert)) {
                stmtInsert.setString(1, categoriaNombre);
                response.exito();
                return response;
            }
        }
    }

    /**
     * Registra un producto en la base de datos.
     * @param producto Contiene la información del producto a registrar.
     * @return Response indicará si todo salió bien o si hubo algún problema.
     */
    @Override
    public Response save(@NonNull Producto producto) {
        Response response = new Response();
        // Verificamos si la categoria existe
        try {
            if (!asegurarCategoria(producto.getCategoriaNombre()).isOk()) {
                response.internal_error("PRI.save: Categoría inválida o no se pudo crear.");
                return response;
            }
        } catch (SQLException e) {
            response.internal_error("PRI.save: " + e.getMessage());
            return response;
        }

        //Query molde con 8 parametros
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
                response.exito();
                return response;
            }

        } catch (SQLException e) {
            response.internal_error("PRI.save: " + e.getMessage());
        }
        return response;
    }

    /**
     * Actualiza la información de un producto específico.
     * @param producto Contiene toda la información del producto.
     * @return Response indica si todo salió correcto o si es que hubo problemas.
     */
    @Override
    public Response update(Producto producto) {
        Response response = new Response();

        // SQL: 8 campos a actualizar + 1 condición WHERE = 9 parámetros en total
        String sql = "UPDATE producto SET nombre=?, fecha_ingreso=?, precio_compra=?, precio_venta=?, cantidad=?, categoria_nombre=?, image_path=?, precio_oferta=? WHERE id_producto=?";

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

            if(stmt.executeUpdate() > 0){response.exito();}
            else{response.internal_error("PRI.update: No se actualizó ninguna fila.");}

        } catch (SQLException e) {
            response.internal_error("PRI.update: " + e.getMessage());
        }
        return response;
    }

    /**
     * Elimina el registro de un producto.
     * @param id Identificador del producto en la base de datos.
     * @return  Response registra si hubo algún problema.
     */
    @Override
    public Response delete(int id) {
        Response response = new Response();
        String sql = "DELETE FROM producto WHERE id_producto=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            if(stmt.executeUpdate() > 0){   response.exito();   }
            else{response.internal_error("PRI.delete: No se eliminó ninguna fila.");}

        } catch (SQLException e) {
            response.internal_error("PRI.delete: " + e.getMessage());
        }
        return response;
    }

    /**
     * Encuentra la información de un producto usando su Id.
     * @param id Identificador del producto.
     * @return Response registra al producto encontrado, si hubo algún problema, se devuelve un mensaje de error..
     */
    @Override
    public Response<Producto> findById(int id) {
        Response<Producto> response = new Response<Producto>();
        String sql = "SELECT * FROM producto WHERE id_producto=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    response.exito(mapearProducto(rs));
                }
            }
        } catch (SQLException e) {
            response.internal_error("PRI.findById: " + e.getMessage());
        }
        return response;
    }

    /**
     * Usa un llamado a la base de datos para obtener TODOS los productos
     * @return Response contiene una lista con todos los productos tipo List<Producto>.
     */
    @Override
    public Response<List<Producto>> findAll() {
        Response<List<Producto>> response = new Response<List<Producto>>();
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM producto";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }
            response.exito(lista);
        } catch (SQLException e) {
            response.internal_error("PRI.findAll: " + e.getMessage());
        }
        return response;
    }

    /**
     * Busca un listado de productos usando una categoria seleccionada.
     * @param categoriaNombre Categoria por la que se buscarán los productos.
     * @return Response tendrá una lista, si no hubo nada o hubo problemas en la busqueda, la lista estará vacia.
     */
    @Override
    public Response<List<Producto>> findByCategoria(String categoriaNombre) {
        Response<List<Producto>> response = new Response<List<Producto>>();
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE categoria_nombre=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, categoriaNombre);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearProducto(rs));
                }
                response.exito(lista);
            }
        } catch (SQLException e) {
            response.internal_error("PRI.findByCategoria: " + e.getMessage());
        }
        return response;
    }

    /**
     * Buscará los productos que CONTENGAN el texto buscado.
     * @param nombre Es el texto con el que filtraremos los productos.
     * @return Una lista con productos, si no hubo coincidencias o si es que hubo problemas, la lista estará vacia.
     */
    @Override
    public Response<List<Producto>> findByNombre(String nombre) {
        Response<List<Producto>> response = new Response<List<Producto>>();
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
                response.exito(lista);
            }
        } catch (SQLException e) {
            response.internal_error("PRI.findByNombre: " + e.getMessage());
        }
        return response;
    }

    /**
     * Obtienes la ganancia total obtenida.
     * ANOTACION: PQ CHCH TA ESTO EN ESTA CLASE???
     * @return retorna la ganancia total, si hubo problemas, retorna 0.0
     */
    @Override
    public Response<Double> getGananciaTotal() {
        Response<Double> response = new Response<Double>();
        // Corrección en la lógica: Usar nombres de tablas consistentes
        String sql = "SELECT SUM((d.precio_unitario - p.precio_compra) * d.cantidad) AS ganancia_total " +
                "FROM detalle_venta d " +
                "JOIN producto p ON d.id_producto = p.id_producto";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                response.exito(rs.getDouble("ganancia_total"));
            }
        } catch (SQLException e) {
            response.internal_error("PRI.getGananciaTotal: " + e.getMessage());
        }
        return response;
    }

    /**
     * Verifica si un producto sigue existiendo usando su Id como verificador.
     * @param id Identificador del producto
     * @return true o false segun si existe o no.
     */
    @Override
    public Response existeIdById(int id) {
        Response response = new Response();
        String sql = "SELECT COUNT(*) FROM producto WHERE id_producto = ?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if(rs.getInt(1) > 0){response.exito();}
                    else{
                        response.internal_error("PRI.existeIdById: No existe un producto con el ID indicado.");
                    }
                }
            }
        } catch (SQLException e) {
            response.internal_error("PRI.existeIdById: " + e.getMessage());

        }
        return response;
    }

    // Método auxiliar para mapear ResultSet a Objeto

    /**
     * Recopila los datos de ResultSet y los convierte a un objeto Producto
     * @param rs Es el set de datos de un producto
     * @return retorna el objeto Producto con los datos contenidos en el ResultSet.
     * @throws SQLException
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