package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Compra;
import io.carpets.repositories.CompraRepository;
import io.carpets.util.Response;
import io.carpets.DTOs.CompraCompletaDTO;
import io.carpets.DTOs.DetalleCompraDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompraRepositoryImplementacion implements CompraRepository {

    /**
     * Registra una compra dentro de la base de datos.
     *
     * IMPORTANTE: Este método modifica el objeto 'compra' pasado como parámetro,
     * asignándole el ID generado por la base de datos.
     *
     * @param compra Contiene todos los datos de la compra (descripcion). El ID será asignado automáticamente.
     * @return Response con el resultado. Si es exitoso, el objeto compra contendrá el ID generado.
     */
    @Override
    public Response save(Compra compra) {
        Response response = new Response();

        // Validación de entrada
        if (compra == null) {
            response.internal_error("CRI.save: El objeto compra no puede ser nulo");
            return response;
        }

        if (compra.getDescripcion() == null || compra.getDescripcion().trim().isEmpty()) {
            response.internal_error("CRI.save: La descripción no puede estar vacía");
            return response;
        }

        String sql = "INSERT INTO compra (descripcion) VALUES (?)";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, compra.getDescripcion().trim());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        compra.setId(generatedId);
                        response.exito();
                        return response;
                    }
                }
                // Si llegamos aquí, se insertó pero no se obtuvo el ID
                response.internal_error("CRI.save: Registro insertado pero no se pudo recuperar el ID");
                return response;
            }

            response.internal_error("CRI.save: No se insertó ningún registro");

        } catch (SQLException e) {
            response.internal_error("CRI.save: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Actualiza los datos de una compra existente.
     *
     * @param compra Contiene los datos actualizados (id, descripcion, monto).
     * @return Response con el resultado de la operación
     */
    @Override
    public Response update(Compra compra) {
        Response response = new Response();

        // Validación de entrada
        if (compra == null) {
            response.internal_error("CRI.update: El objeto compra no puede ser nulo");
            return response;
        }

        if (compra.getId() <= 0) {
            response.internal_error("CRI.update: El ID de la compra debe ser mayor a 0");
            return response;
        }

        if (compra.getDescripcion() == null || compra.getDescripcion().trim().isEmpty()) {
            response.internal_error("CRI.update: La descripción no puede estar vacía");
            return response;
        }

        if (compra.getMonto() < 0) {
            response.internal_error("CRI.update: El monto no puede ser negativo");
            return response;
        }

        String sql = "UPDATE compra SET descripcion = ?, monto = ? WHERE id_compra = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, compra.getDescripcion().trim());
            stmt.setDouble(2, compra.getMonto());
            stmt.setInt(3, compra.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                response.exito();
                return response;
            }

            // Si no se actualizó ninguna fila, el ID no existe
            response.internal_error("CRI.update: No existe una compra con id = " + compra.getId());

        } catch (SQLException e) {
            response.internal_error("CRI.update: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Elimina una compra usando su id.
     *
     * ADVERTENCIA: Esta operación puede fallar si existen detalles de compra asociados
     * debido a restricciones de clave foránea. Considere eliminar primero los detalles
     * o usar DELETE CASCADE en la base de datos.
     *
     * @param id Identificador de la compra a eliminar (debe ser > 0).
     * @return Response con el resultado de la operación
     */
    @Override
    public Response delete(int id) {
        Response response = new Response();

        // Validación de entrada
        if (id <= 0) {
            response.message_error("CRI.delete: El ID debe ser mayor a 0");
            return response;
        }

        String sql = "DELETE FROM compra WHERE id_compra = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                response.exito();
                return response;
            }

            // Si no se eliminó ninguna fila, el ID no existe
            response.message_error("CRI.delete: No existe una compra con id = " + id);

        } catch (SQLIntegrityConstraintViolationException e) {
            // Error específico de restricción de clave foránea
            response.message_error("CRI.delete: No se puede eliminar la compra porque tiene detalles asociados. Elimine primero los detalles.");
        } catch (SQLException e) {
            response.message_error("CRI.delete: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Encuentra una compra por su ID.
     *
     * @param id Identificador de la compra (debe ser > 0).
     * @return Response<Compra> con la compra encontrada o mensaje de error
     */
    @Override
    public Response<Compra> findById(int id) {
        Response<Compra> response = new Response<>();

        // Validación de entrada
        if (id <= 0) {
            response.internal_error("CRI.findById: El ID debe ser mayor a 0");
            return response;
        }

        String sql = "SELECT id_compra, descripcion, monto, fecha FROM compra WHERE id_compra = ?";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Compra c = new Compra();
                    c.setId(rs.getInt("id_compra"));
                    c.setDescripcion(rs.getString("descripcion"));
                    c.setMonto(rs.getDouble("monto"));
                    c.setFecha(rs.getDate("fecha"));

                    response.exito(c);
                    return response;
                }
            }

            response.internal_error("CRI.findById: No se encontró compra con id = " + id);

        } catch (SQLException e) {
            response.internal_error("CRI.findById: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Encuentra todas las compras registradas, ordenadas por ID descendente (más recientes primero).
     *
     * @return Response<List<Compra>> con la lista de compras. Si no hay compras, retorna error con lista vacía.
     */
    @Override
    public Response<List<Compra>> findAll() {
        Response<List<Compra>> response = new Response<>();
        List<Compra> lista = new ArrayList<>();
        String sql = "SELECT id_compra, descripcion, monto, fecha FROM compra ORDER BY id_compra DESC";

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("id_compra"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setMonto(rs.getDouble("monto"));
                c.setFecha(rs.getDate("fecha"));
                lista.add(c);
            }

            if (lista.isEmpty()) {
                response.internal_error("CRI.findAll: No hay compras registradas en la base de datos");
                return response;
            }

            response.exito(lista);

        } catch (SQLException e) {
            response.internal_error("CRI.findAll: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Encuentra las compras realizadas en un rango de fechas.
     *
     * @param desde Fecha inicial del rango (inclusive). Si es null, no se aplica límite inferior.
     * @param hasta Fecha final del rango (inclusive). Si es null, no se aplica límite superior.
     * @return Response<List<Compra>> con las compras filtradas, ordenadas por fecha descendente.
     *         Si ambos parámetros son null, retorna todas las compras (equivalente a findAll).
     */
    @Override
    public Response<List<Compra>> findByDate(Date desde, Date hasta) {
        Response<List<Compra>> response = new Response<>();
        List<Compra> lista = new ArrayList<>();

        // Si ambos son null, usar findAll es más eficiente
        if (desde == null && hasta == null) {
            return findAll();
        }

        // Construcción dinámica de la consulta SQL
        StringBuilder sql = new StringBuilder(
                "SELECT id_compra, descripcion, monto, fecha FROM compra WHERE 1=1"
        );

        if (desde != null) {
            sql.append(" AND fecha >= ?");
        }
        if (hasta != null) {
            sql.append(" AND fecha <= ?");
        }
        sql.append(" ORDER BY fecha DESC");

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Asignar parámetros dinámicamente
            int paramIndex = 1;
            if (desde != null) {
                stmt.setDate(paramIndex++, desde);
            }
            if (hasta != null) {
                stmt.setDate(paramIndex, hasta);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Compra c = new Compra();
                    c.setId(rs.getInt("id_compra"));
                    c.setDescripcion(rs.getString("descripcion"));
                    c.setMonto(rs.getDouble("monto"));
                    c.setFecha(rs.getDate("fecha"));
                    lista.add(c);
                }
            }

            if (lista.isEmpty()) {
                response.internal_error("CRI.findByDate: No se encontraron compras en el rango especificado");
                return response;
            }

            response.exito(lista);

        } catch (SQLException e) {
            response.internal_error("CRI.findByDate: Error SQL - " + e.getMessage());
        }

        return response;
    }

    /**
     * Lista todas las compras con sus detalles en un solo query optimizado.
     * Útil para mostrar el historial completo sin realizar múltiples consultas.
     *
     * Este método usa un LEFT JOIN, por lo que también incluye compras sin detalles.
     *
     * @return Response<List<CompraCompletaDTO>> con las compras y sus detalles anidados,
     *         ordenadas por ID descendente (más recientes primero).
     */
    @Override
    public Response<List<CompraCompletaDTO>> listarComprasConDetalles() {
        Response<List<CompraCompletaDTO>> response = new Response<>();

        String sql = "SELECT " +
                "  c.id_compra, " +
                "  c.descripcion, " +
                "  c.monto, " +
                "  c.fecha, " +
                "  d.unidades AS cantidad, " +
                "  d.precio_unitario, " +
                "  p.nombre AS producto_nombre, " +
                "  p.image_path " +
                "FROM compra c " +
                "LEFT JOIN detalle_compra d ON c.id_compra = d.id_compra " +
                "LEFT JOIN producto p ON d.id_producto = p.id_producto " +
                "ORDER BY c.id_compra DESC";

        // LinkedHashMap para mantener orden de inserción
        Map<Integer, CompraCompletaDTO> comprasMap = new LinkedHashMap<>();

        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int idCompra = rs.getInt("id_compra");

                // computeIfAbsent: solo crea el DTO si no existe aún (agrupa por compra)
                CompraCompletaDTO compraDTO = comprasMap.computeIfAbsent(idCompra, k -> {
                    CompraCompletaDTO nueva = new CompraCompletaDTO();
                    nueva.setId(idCompra);
                    try {
                        nueva.setDescripcion(rs.getString("descripcion"));
                        nueva.setMonto(rs.getDouble("monto"));

                        // Manejo seguro de timestamp/fecha
                        Timestamp ts = rs.getTimestamp("fecha");
                        nueva.setFecha(ts != null ? ts.toString() : "");
                    } catch (SQLException e) {
                        // Log del error pero continuar con los demás registros
                        System.err.println("Error procesando compra " + idCompra + ": " + e.getMessage());
                    }
                    return nueva;
                });

                // Solo agregar detalle si hay datos de detalle (LEFT JOIN puede traer nulls)
                Integer cantidad = (Integer) rs.getObject("cantidad");
                if (cantidad != null) {
                    DetalleCompraDTO detalle = new DetalleCompraDTO();
                    detalle.setCantidad(cantidad);
                    detalle.setPrecio(rs.getDouble("precio_unitario"));
                    detalle.setNombreProducto(rs.getString("producto_nombre"));
                    detalle.setImagePath(rs.getString("image_path"));
                    compraDTO.getDetalles().add(detalle);
                }
            }

            List<CompraCompletaDTO> listaFinal = new ArrayList<>(comprasMap.values());

            if (listaFinal.isEmpty()) {
                response.internal_error("CRI.listarComprasConDetalles: No hay compras registradas");
                return response;
            }

            response.exito(listaFinal);

        } catch (SQLException e) {
            response.internal_error("CRI.listarComprasConDetalles: Error SQL - " + e.getMessage());
        }

        return response;
    }
}