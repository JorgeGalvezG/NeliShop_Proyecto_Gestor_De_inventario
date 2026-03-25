package io.carpets.flutterbridge;

import java.sql.Connection;
import io.carpets.entidades.*;
import io.carpets.servicios.*;
import java.sql.PreparedStatement;
import io.carpets.servicios.implementacion.*;
import io.carpets.DTOs.BoletaVentaDTO;
import io.carpets.DTOs.MontosCalculados;
import io.carpets.DTOs.DetalleVentaDTO;
import io.carpets.DTOs.VentaCompletaDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.util.Response;
import io.flutter.embedding.engine.systemchannels.PlatformChannel;

public class MethodChannelHandler {

    // Instancias de tus servicios
    private ServicioUsuario usuarioService = new ServicioUsuarioImplementacion();
    private ServicioProducto productoService = new ServicioProductoImplementacion();
    private ServicioVenta ventaService = new ServicioVentaImplementacion();
    private ServicioCompra compraService = new ServicioCompraImplementacion();

    private io.carpets.repositories.DetalleVentaRepository detalleVentaRepo =
            new io.carpets.repositories.implementacion.DetalleVentaRepositoryImplementacion();

    private io.carpets.repositories.ProductoRepository productoRepo =
            new io.carpets.repositories.implementacion.ProductoRepositoryImplementacion();
    private io.carpets.repositories.DetalleCompraRepository detalleCompraRepo =
            new io.carpets.repositories.implementacion.DetalleCompraRepositoryImplementacion();



    // ========================================================================
    // SECCIÓN 1: AUTENTICACIÓN (LOGIN)
    // ========================================================================

    public Response<Map<String, Object>> login(String username, String password) {
        Response <Map<String, Object>> response = usuarioService.login(username, password);
        if(!response.isOk()){
            response.message_error("Error. Su usuario y contraseña deben ser exactos.");
        }
        return response;
    }

    // ========================================================================
    // SECCIÓN 2: GESTIÓN DE PRODUCTOS
    // ========================================================================

    // Devolvemos List<Map> en lugar de List<Producto>

    /**
     * Devuelve todos los productos de la base de datos.
     * @return Devuelve una lista de mapas<> con los productos. Tipo List< Map<String, Object> >Siendo Object los datos del producto.
     */
    public Response<List<Map<String, Object>>> obtenerProductos() {
        //Llamamos al ProductoService.
        Response<List<Producto>> request = productoService.obtenerTodos();
        //Creamos otro para la respuesta de la actual función.
        Response<List<Map<String, Object>>> response = new Response<>();
        //Si hubo algún error al obtener los objetos.
        if(!request.isOk()){
            //Mandamos un mensaje al front.
            response.message_error("Petición no concedida. Verifique su conexión de internet");
            return response;
        }

        List<Producto> productos = request.getContent();  //Se obtienen todos los productos
        response.exito(
                   productos.stream()            //stream() es un flujo de trabajo, la
                           // función que le envíes se le aplicará a todos los
                           //elementos de la lista
                    .map(this::productoToMap)               //Se usa la función productoToMap para el stream
                    .collect(Collectors.toList())           //Recupera los datos y los hace una lista ( toList() )
        );

        return response;
    }

    // Helper para convertir Producto a Map (Lo que Flutter entiende)
    private Map<String, Object> productoToMap(Producto p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("nombre", p.getNombre());
        map.put("precioCompra", p.getPrecioCompra());
        map.put("precioVenta", p.getPrecioVenta());
        map.put("cantidad", p.getCantidad());
        map.put("stock", p.getCantidad()); // Enviamos ambos por compatibilidad
        map.put("categoria", p.getCategoriaNombre());
        //map.put("categoriaNombre", p.getCategoriaNombre());
        map.put("imagePath", p.getImagePath());
        map.put("salePrice", p.getPrecioOferta());
        return map;
    }

    public Response agregarProducto(Producto producto) {
        Response response = new Response();
        if(!productoService.agregarProducto(producto).isOk()){
            response.message_error("Error al ingresar producto, verifique que los datos sean válidos.");
            return response;
        }
        response.exito();
        return response;
    }

    public Response actualizarProducto(Producto producto) {
        Response response = new Response();
        if(!productoService.actualizarInventario(producto).isOk()){
            response.message_error("Error al actualizar producto, verifique que los datos sean válidos. Verifique su conexión a internet.");
            return response;
        }
        response.exito();
        return response;
    }

    public Response eliminarProducto(int idProducto) {
        Response response = new Response();
        if(!productoService.eliminarProducto(idProducto).isOk()){
            response.message_error("Error al eliminar producto. Verifique su conexión a internet.");
            return response;
        }
        response.exito();
        return response;
    }

    // Métodos de búsqueda y validación (Stubs funcionales)
    public Response<List<Map<String, Object>>> buscarProductos(String criterio, String tipo) {
        //Registramos la consulta.
        Response<List<Producto>> request = productoService.buscarProductos(criterio, tipo);
        //Creamos la respuesta.
        Response<List<Map<String, Object>>> response = new Response<>();

        //Si la consulta tuvo errores, los mostramos en el front.
        if(!request.isOk()){
            response.message_error("Error al buscar productos. Verifique su conexión a internet.");
            return response;
        }

        //Si no hubo errores, obtenemos la lista.
        List<Producto> productos = request.getContent();
        response.exito(
                productos.stream().map(this::productoToMap).collect(Collectors.toList())
        );
        return response;
    }


    public Response<Double> getGananciaTotal(){
        Response<Double> response = productoService.getGananciaTotal();
        if(!response.isOk()){
            response.message_error("Error al obtener la ganancia total. Revise su conexión a internet.");
        }
        return response;

    }
    // ========================================================================
    // SECCIÓN 3: GESTIÓN DE VENTAS
    // ========================================================================

    public Map<String, Object> calcularMontos(double precioUnitario, int cantidad) {
        MontosCalculados montos = ventaService.calcularMontos(precioUnitario, cantidad);
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("subtotal", montos.getSubtotal());
        resultado.put("igv", montos.getIgvSolo());
        resultado.put("total", montos.getTotalConIGV());
        return resultado;
    }

    public Map<String, Object> calcularMontosVentaCompleta(List<DetalleVenta> detalles) {
        MontosCalculados montos = ventaService.calcularMontosVentaCompleta(detalles);
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("subtotal", montos.getSubtotal());
        resultado.put("igv", montos.getIgvSolo());
        resultado.put("total", montos.getTotalConIGV());
        return resultado;
    }

    public double calcularTotalVenta(List<DetalleVenta> detalles) {
        return ventaService.calcularTotalVenta(detalles);
    }

    public Map<String, Object> registrarVenta(Venta venta, List<DetalleVenta> detalles) {
        try {
            int idVenta = ventaService.registrarVenta(venta, detalles);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "ok");
            resultado.put("Content", idVenta);
            return resultado;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    public List<Map<String, Object>> listarVentas() {
        try {
            // 1. Usamos tu nuevo método optimizado con DTOs
            List<VentaCompletaDTO> ventasCompletas = ventaService.listarVentasConDetalles();
            List<Map<String, Object>> ventasMap = new ArrayList<>();

            for (VentaCompletaDTO v : ventasCompletas) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", v.getId());
                m.put("numeroBoleta", v.getNumeroBoleta());
                m.put("monto", v.getMonto());
                m.put("fecha", v.getFecha() != null ? v.getFecha() : "");
                m.put("clienteDni", v.getClienteDni());

                // 2. Extraemos los detalles que ya armaste en la memoria
                List<Map<String, Object>> productosList = new ArrayList<>();
                for (DetalleVentaDTO d : v.getDetalles()) {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("cantidad", d.getCantidad());
                    pMap.put("precio", d.getPrecioUnitario());

                    // Buscamos el nombre del producto y confiamos en que existe.
                    Producto p = productoRepo.findById(d.getProductoId()).getContent();
                    pMap.put("nombre", p != null ? p.getNombre() : "Producto Eliminado");
                    pMap.put("imagePath", p != null ? p.getImagePath() : " ");


                    productosList.add(pMap);
                }

                m.put("detalles", productosList);
                ventasMap.add(m);
            }

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "ok");
            resultado.put("Content", ventasMap);
            return resultado;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Map<String, Object> generarBoleta(int ventaId, List<DetalleVenta> detalles) {
        try {
            BoletaVentaDTO boleta = ventaService.generarBoleta(ventaId, detalles);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "success");
            resultado.put("numeroBoleta", boleta.getVenta().getNumeroBoleta());
            resultado.put("total", boleta.getTotal());
            return resultado;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // ========================================================================
    // SECCIÓN 4: GESTIÓN DE COMPRAS
    // ========================================================================

    /**
     * Registrar una compra desde la base de datos.
     * @param compraMapObj Es el objeto contenedor mandado por flutter. Contiene la compra a registrar.
     * @param detallesListObj Es el objeto contenedor mandado por flutter. Contiene los detalles de compra a registrar.
     * @return Response, indica si la lógica sucedió según lo esperado.
     */
    public Response registrarCompra(Object compraMapObj, Object detallesListObj) {

        //Convertimos los objetos del bridge a Objetos propios.
        Compra compra = Compra.CompraFromMap((Map<String, Object>) compraMapObj);
        List<Map<String, Object>> detallesList = (List<Map<String, Object>>) detallesListObj;

        List<DetalleCompra> detalles = new ArrayList<>();
        for (Map<String, Object> detMap : detallesList) {
            detalles.add(DetalleCompra.DetCompraFromMap(detMap));
        }

        //Lógica.
        Response response = new Response();

        if(!compraService.registrarCompra(compra, detalles).isOk()){
            response.message_error("Error al registrar compra. Revise su conexión.");
            return response;
        }

        response.exito();
        return response;
    }

    public Response modificarDescripcionCompra(Object compraMap) {

        Response response = new Response();
        Compra compra = new Compra();
        Map<String, Object> Mapa = (Map<String, Object>) compraMap;

        if (Mapa == null){
            response.internal_error("MCH.modificarDescripcionCompra: Error en la conversión del mapa.");
            return response;
        }
            compra.setDescripcion((String) Mapa.get("descripcion"));
        response = compraService.actualizarDescripcionCompra(compra);

        if(!response.isOk()){
            response.message_error("Error. Revise su conexión a internet.");
        }

        return response;
    }


    public Response<List<Map<String, Object>>> listarCompras() {
        Response<List<Map<String, Object>>> response = new Response<>();

        //verificamos que se listen las compras correctamente.
        Response<List<Map<String, Object>>> request = compraService.listarCompras();
        if(!request.isOk()){
            response.message_error("Error al listar compras. Revise su conexión.");
            return response;
        }

        response.exito(request.getContent());
        return response;
    }
    public Response eliminarDetalleCompra(int detalleId) {
        Response response = compraService.eliminarDetalleCompra(detalleId);
        if(!response.isOk()){
            response.message_error("Error al eliminar el detalle de compra. Revise su conexión a internet.");
        }
        return response;
    }

    public Response editarDetalleCompra(int detalleId, int cantidad, double precio) {
        DetalleCompra detalleCompra = new DetalleCompra();
        detalleCompra.setId(detalleId);
        detalleCompra.setUnidades(cantidad);
        detalleCompra.setPrecioUnitario(precio);
        Response response = compraService.editarDetalleCompra(detalleCompra);
        if(!response.isOk()){response.message_error("Error al editar el detalle de compra. Revise su internet.");}
        return response;
    }

    public Response eliminarCompra(int compraId) {
        Response response = compraService.eliminarCompra(compraId);
        if(!response.isOk()){response.message_error("Error al eliminar su compra. Revise su internet.");}
        return response;
    }


    public Map<String, Object> obtenerVentasPorDia(String fecha) {
        return new HashMap<>();
    }

    public Map<String, Object> eliminarVenta(int id) {
        return new HashMap<>();
    }

    // Helper de error
    private Map<String, Object> errorResponse(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "error");
        r.put("mensaje", msg);
        return r;
    }


}