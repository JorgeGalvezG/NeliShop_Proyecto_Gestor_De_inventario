package io.carpets.flutterbridge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.DTOs.BoletaVentaDTO;
import io.carpets.DTOs.CompraCompletaDTO;
import io.carpets.DTOs.DetalleCompraDTO;
import io.carpets.DTOs.DetalleVentaDTO;
import io.carpets.DTOs.MontosCalculados;
import io.carpets.DTOs.VentaCompletaDTO;
import io.carpets.entidades.Compra;
import io.carpets.entidades.DetalleCompra;
import io.carpets.entidades.DetalleVenta;
import io.carpets.entidades.Producto;
import io.carpets.entidades.Venta;
import io.carpets.servicios.ServicioCompra;
import io.carpets.servicios.ServicioProducto;
import io.carpets.servicios.ServicioUsuario;
import io.carpets.servicios.ServicioVenta;
import io.carpets.servicios.implementacion.ServicioCompraImplementacion;
import io.carpets.servicios.implementacion.ServicioProductoImplementacion;
import io.carpets.servicios.implementacion.ServicioUsuarioImplementacion;
import io.carpets.servicios.implementacion.ServicioVentaImplementacion;
import io.carpets.util.Response;

/**
 * Clase central que maneja todas las llamadas desde Flutter a través de Method Channels.
 * Actúa como intermediario entre la capa de presentación (Flutter) y la capa de negocio (Servicios).
 *
 * Características principales:
 * - Convierte objetos Java a Maps que Flutter puede entender
 * - Maneja el sistema de caché en memoria RAM
 * - Retorna objetos Response consistentes
 */
public class MethodChannelHandler {

    // ========================================================================
    // SERVICIOS
    // ========================================================================
    private final ServicioUsuario usuarioService = new ServicioUsuarioImplementacion();
    private final ServicioProducto productoService = new ServicioProductoImplementacion();
    private final ServicioVenta ventaService = new ServicioVentaImplementacion();
    private final ServicioCompra compraService = new ServicioCompraImplementacion();

    // ========================================================================
    // SECCIÓN 1: AUTENTICACIÓN (LOGIN)
    // ========================================================================

    /**
     * Autentica a un usuario en el sistema.
     *
     * @param username DNI o nombre de usuario
     * @param password Contraseña (debe estar cifrada desde Flutter)
     * @return Response con los datos del usuario autenticado o error
     */
    public Response<Map<String, Object>> login(String username, String password) {
        Response<Map<String, Object>> response = usuarioService.login(username, password);

        if (!response.isOk()) {
            response.message_error("Usuario o contraseña incorrectos. Verifique sus credenciales.");
        }

        return response;
    }

    // ========================================================================
    // SECCIÓN 2: GESTIÓN DE PRODUCTOS
    // ========================================================================

    /**
     * Obtiene todos los productos de la base de datos.
     *
     * @return Response<List<Map>> con los productos convertidos a formato Map para Flutter
     */
    public Response<List<Map<String, Object>>> obtenerProductos() {
        Response<List<Producto>> request = productoService.obtenerTodos();
        Response<List<Map<String, Object>>> response = new Response<>();

        if (!request.isOk()) {
            response.message_error("Error al obtener productos. Verifique su conexión a internet.");
            return response;
        }

        List<Producto> productos = request.getContent();
        List<Map<String, Object>> productosMap = productos.stream()
                .map(this::productoToMap)
                .collect(Collectors.toList());

        response.exito(productosMap);
        return response;
    }

    /**
     * Convierte un objeto Producto a Map para que Flutter lo pueda interpretar.
     *
     * @param p Producto a convertir
     * @return Map con todos los campos del producto
     */
    private Map<String, Object> productoToMap(Producto p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("nombre", p.getNombre());
        map.put("precioCompra", p.getPrecioCompra());
        map.put("precioVenta", p.getPrecioVenta());
        map.put("cantidad", p.getCantidad());
        map.put("stock", p.getCantidad()); // Alias para compatibilidad
        map.put("categoriaNombre", p.getCategoriaNombre());
        map.put("imagen", p.getImagePath()); // Flutter espera "imagen"
        map.put("precioOferta", p.getPrecioOferta());
        return map;
    }

    /**
     * Agrega un nuevo producto al inventario.
     *
     * @param producto Producto a agregar
     * @return Response indicando éxito o error
     */
    public Response agregarProducto(Producto producto) {
        Response response = productoService.agregarProducto(producto);

        if (!response.isOk()) {
            response.message_error("Error al agregar producto. Verifique que los datos sean válidos.");
        }

        return response;
    }

    /**
     * Actualiza un producto existente en el inventario.
     *
     * @param producto Producto con los datos actualizados
     * @return Response indicando éxito o error
     */
    public Response actualizarProducto(Producto producto) {
        Response response = productoService.actualizarInventario(producto);

        if (!response.isOk()) {
            response.message_error("Error al actualizar producto. Verifique los datos y su conexión.");
        }

        return response;
    }

    /**
     * Elimina un producto del inventario.
     *
     * @param idProducto ID del producto a eliminar
     * @return Response indicando éxito o error
     */
    public Response eliminarProducto(int idProducto) {
        Response response = productoService.eliminarProducto(idProducto);

        if (!response.isOk()) {
            response.message_error("Error al eliminar producto. Verifique su conexión a internet.");
        }

        return response;
    }

    /**
     * Busca productos según un criterio y tipo de búsqueda.
     *
     * @param criterio Texto a buscar
     * @param tipo Tipo de búsqueda (nombre, categoria, etc.)
     * @return Response<List<Map>> con los productos encontrados
     */
    public Response<List<Map<String, Object>>> buscarProductos(String criterio, String tipo) {
        Response<List<Producto>> request = productoService.buscarProductos(criterio, tipo);
        Response<List<Map<String, Object>>> response = new Response<>();

        if (!request.isOk()) {
            response.message_error("Error al buscar productos. Verifique su conexión a internet.");
            return response;
        }

        List<Producto> productos = request.getContent();
        List<Map<String, Object>> productosMap = productos.stream()
                .map(this::productoToMap)
                .collect(Collectors.toList());

        response.exito(productosMap);
        return response;
    }

    /**
     * Obtiene la ganancia total de todos los productos.
     *
     * @return Response<Double> con la ganancia total calculada
     */
    public Response<Double> getGananciaTotal() {
        Response<Double> response = productoService.getGananciaTotal();

        if (!response.isOk()) {
            response.message_error("Error al obtener la ganancia total. Revise su conexión a internet.");
        }

        return response;
    }

    // ========================================================================
    // SECCIÓN 3: GESTIÓN DE VENTAS
    // ========================================================================

    /**
     * Calcula montos (subtotal, IGV, total) para un detalle de venta.
     *
     * @param precioUnitario Precio unitario del producto
     * @param cantidad Cantidad de productos
     * @return Response<Map> con subtotal, igv y total
     */
    public Response<Map<String, Object>> calcularMontos(double precioUnitario, int cantidad) {
        Response<Map<String, Object>> response = new Response<>();

        try {
            MontosCalculados montos = ventaService.calcularMontos(precioUnitario, cantidad);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("subtotal", montos.getSubtotal());
            resultado.put("igv", montos.getIgvSolo());
            resultado.put("total", montos.getTotalConIGV());

            response.exito(resultado);
        } catch (Exception e) {
            response.internal_error("MCH.calcularMontos: " + e.getMessage());
        }

        return response;
    }

    /**
     * Calcula montos totales para una venta completa con múltiples detalles.
     *
     * @param detalles Lista de detalles de venta
     * @return Response<Map> con subtotal, igv y total
     */
    public Response<Map<String, Object>> calcularMontosVentaCompleta(List<DetalleVenta> detalles) {
        Response<Map<String, Object>> response = new Response<>();

        try {
            MontosCalculados montos = ventaService.calcularMontosVentaCompleta(detalles);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("subtotal", montos.getSubtotal());
            resultado.put("igv", montos.getIgvSolo());
            resultado.put("total", montos.getTotalConIGV());

            response.exito(resultado);
        } catch (Exception e) {
            response.internal_error("MCH.calcularMontosVentaCompleta: " + e.getMessage());
        }

        return response;
    }

    /**
     * Calcula el total de una venta (sin desglose).
     *
     * @param detalles Lista de detalles de venta
     * @return Response<Double> con el total
     */
    public Response<Double> calcularTotalVenta(List<DetalleVenta> detalles) {
        Response<Double> response = new Response<>();

        try {
            double total = ventaService.calcularTotalVenta(detalles);
            response.exito(total);
        } catch (Exception e) {
            response.internal_error("MCH.calcularTotalVenta: " + e.getMessage());
        }

        return response;
    }

    /**
     * Registra una nueva venta en el sistema.
     *
     * @param venta Objeto Venta con los datos de cabecera
     * @param detalles Lista de DetalleVenta
     * @return Response con el ID de la venta generada
     */
    public Response<Integer> registrarVenta(Venta venta, List<DetalleVenta> detalles) {
        Response<Integer> response = new Response<>();

        try {
            int idVenta = ventaService.registrarVenta(venta, detalles);
            response.exito(idVenta);
        } catch (Exception e) {
            response.internal_error("MCH.registrarVenta: " + e.getMessage());
        }

        return response;
    }

    /**
     * Lista todas las ventas con sus detalles.
     *
     * @return Response<List<Map>> con las ventas en formato Map
     */
    public Response<List<Map<String, Object>>> listarVentas() {
        Response<List<Map<String, Object>>> response = new Response<>();

        try {
            List<VentaCompletaDTO> ventasCompletas = ventaService.listarVentasConDetalles();
            List<Map<String, Object>> ventasMap = new ArrayList<>();

            for (VentaCompletaDTO v : ventasCompletas) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", v.getId());
                m.put("numeroBoleta", v.getNumeroBoleta());
                m.put("monto", v.getMonto());
                m.put("fecha", v.getFecha() != null ? v.getFecha() : "");
                m.put("clienteDni", v.getClienteDni());

                List<Map<String, Object>> detallesMap = new ArrayList<>();
                for (DetalleVentaDTO d : v.getDetalles()) {
                    Map<String, Object> detMap = new HashMap<>();
                    detMap.put("cantidad", d.getCantidad());
                    detMap.put("precio", d.getPrecio());
                    detMap.put("nombre", d.getNombreProducto() != null ? d.getNombreProducto() : "Producto Eliminado");
                    detMap.put("imagePath", d.getImagePath() != null ? d.getImagePath() : "");
                    detallesMap.add(detMap);
                }

                m.put("detalles", detallesMap);
                ventasMap.add(m);
            }

            response.exito(ventasMap);

        } catch (Exception e) {
            response.internal_error("MCH.listarVentas: " + e.getMessage());
        }

        return response;
    }

    /**
     * Genera una boleta de venta.
     *
     * @param ventaId ID de la venta
     * @param detalles Lista de detalles
     * @return Response<Map> con los datos de la boleta
     */
    public Response<Map<String, Object>> generarBoleta(int ventaId, List<DetalleVenta> detalles) {
        Response<Map<String, Object>> response = new Response<>();

        try {
            BoletaVentaDTO boleta = ventaService.generarBoleta(ventaId, detalles);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("numeroBoleta", boleta.getVenta().getNumeroBoleta());
            resultado.put("total", boleta.getTotal());

            response.exito(resultado);
        } catch (Exception e) {
            response.internal_error("MCH.generarBoleta: " + e.getMessage());
        }

        return response;
    }

    /**
     * Elimina una venta del sistema.
     *
     * @param id ID de la venta a eliminar
     * @return Response indicando éxito o error
     */
    public Response eliminarVenta(int id) {
        Response response = ventaService.eliminarVenta(id);

        if (!response.isOk()) {
            response.message_error("Error al eliminar venta. Verifique su conexión.");
        }

        return response;
    }

    // ========================================================================
    // SECCIÓN 4: GESTIÓN DE COMPRAS
    // ========================================================================

    /**
     * Registra una nueva compra en el sistema.
     *
     * Este método recibe objetos desde Flutter (Maps) y los convierte a entidades Java.
     *
     * @param compraMapObj Map con los datos de la compra
     * @param detallesListObj List<Map> con los detalles de la compra
     * @return Response indicando éxito o error
     */
    public Response registrarCompra(Object compraMapObj, Object detallesListObj) {
        Response response = new Response();

        try {
            // Validación de entrada
            if (compraMapObj == null || detallesListObj == null) {
                response.internal_error("MCH.registrarCompra: Parámetros nulos");
                return response;
            }

            // Conversión de Maps a entidades
            @SuppressWarnings("unchecked")
            Map<String, Object> compraMap = (Map<String, Object>) compraMapObj;
            Compra compra = Compra.CompraFromMap(compraMap);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> detallesList = (List<Map<String, Object>>) detallesListObj;

            List<DetalleCompra> detalles = new ArrayList<>();
            for (Map<String, Object> detMap : detallesList) {
                detalles.add(DetalleCompra.DetCompraFromMap(detMap));
            }

            // Validación de negocio
            if (detalles.isEmpty()) {
                response.message_error("La compra debe tener al menos un detalle");
                return response;
            }

            // Registro en BD
            Response registroResponse = compraService.registrarCompra(compra, detalles);

            if (!registroResponse.isOk()) {
                response.message_error("Error al registrar compra. Revise su conexión.");
                return response;
            }

            response.exito();

        } catch (ClassCastException e) {
            response.internal_error("MCH.registrarCompra: Error de conversión de tipos - " + e.getMessage());
        } catch (Exception e) {
            response.internal_error("MCH.registrarCompra: " + e.getMessage());
        }

        return response;
    }

    /**
     * Lista todas las compras con sus detalles.
     *
     * @return Response<List<Map>> con las compras en formato Map
     */
    public Response<List<Map<String, Object>>> listarCompras() {
        Response<List<Map<String, Object>>> response = compraService.listarCompras();

        if (!response.isOk()) {
            response.message_error("Error al listar compras. Revise su conexión.");
        }

        return response;
    }

    /**
     * Modifica la descripción de una compra existente.
     *
     * @param compraMap Map con id y descripcion
     * @return Response indicando éxito o error
     */
    public Response modificarDescripcionCompra(Object compraMap) {
        Response response = new Response();

        try {
            if (compraMap == null) {
                response.internal_error("MCH.modificarDescripcionCompra: Parámetro nulo");
                return response;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = (Map<String, Object>) compraMap;

            Compra compra = new Compra();

            Object idObj = mapa.get("id");
            if(idObj instanceof Number) {
                compra.setId(Integer.parseInt(idObj.toString()));
            }

            compra.setDescripcion(mapa.get("descripcion") != null ? mapa.get("descripcion").toString() : "");

            response = compraService.actualizarDescripcionCompra(compra);

            if (!response.isOk()) {
                response.message_error("Error al modificar descripción. Revise su conexión.");
            }

        } catch (Exception e) {
            response.internal_error("MCH.modificarDescripcionCompra: " + e.getMessage());
        }

        return response;
    }

    /**
     * Elimina una compra del sistema.
     *
     * @param compraId ID de la compra a eliminar
     * @return Response indicando éxito o error
     */
    public Response eliminarCompra(int compraId) {
        Response response = compraService.eliminarCompra(compraId);

        if (!response.isOk()) {
            response.message_error("Error al eliminar compra. Revise su internet.");
        }

        return response;
    }

    /**
     * Elimina un detalle de compra específico.
     *
     * @param detalleId ID del detalle a eliminar
     * @return Response indicando éxito o error
     */
    public Response eliminarDetalleCompra(int detalleId) {
        Response response = compraService.eliminarDetalleCompra(detalleId);

        if (!response.isOk()) {
            response.message_error("Error al eliminar el detalle de compra. Revise su conexión.");
        }

        return response;
    }

    /**
     * Edita un detalle de compra existente.
     *
     * @param detalleId ID del detalle
     * @param cantidad Nueva cantidad
     * @param precio Nuevo precio unitario
     * @return Response indicando éxito o error
     */
    public Response editarDetalleCompra(int detalleId, int cantidad, double precio) {
        DetalleCompra detalleCompra = new DetalleCompra();
        detalleCompra.setId(detalleId);
        detalleCompra.setUnidades(cantidad);
        detalleCompra.setPrecioUnitario(precio);

        Response response = compraService.editarDetalleCompra(detalleCompra);

        if (!response.isOk()) {
            response.message_error("Error al editar el detalle de compra. Revise su internet.");
        }

        return response;
    }

    // ========================================================================
    // SECCIÓN 5: MÉTODOS AUXILIARES
    // ========================================================================

    /**
     * Obtiene ventas filtradas por fecha.
     *
     * @param fecha Fecha en formato String
     * @return Response<Map> con las ventas del día (implementación pendiente)
     */
    public Response<Map<String, Object>> obtenerVentasPorDia(String fecha) {
        Response<Map<String, Object>> response = new Response<>();
        response.internal_error("MCH.obtenerVentasPorDia: Método no implementado aún");
        return response;
    }
}