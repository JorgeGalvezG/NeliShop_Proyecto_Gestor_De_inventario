package io.carpets.flutterbridge;

import java.sql.Connection;
import io.carpets.entidades.*;
import io.carpets.servicios.*;
import java.sql.PreparedStatement;
import io.carpets.servicios.implementacion.*;
import io.carpets.DTOs.BoletaVentaDTO;
import io.carpets.DTOs.MontosCalculados;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.util.Response;

public class MethodChannelHandler {

    // Instancias de tus servicios
    private ServicioUsuario usuarioService = new ServicioUsuarioImplementacion();
    private ServicioProducto productoService = new ServicioProductoImplementacion();
    private ServicioVenta ventaService = new ServicioVentaImplementacion();
    private ServicioCompra compraService = new ServicioCompraImplementacion();

    // ========================================================================
    // SECCIÓN 1: AUTENTICACIÓN (LOGIN)
    // ========================================================================


    //metodo sin capacidad ni cifrado
    /* public Map<String, Object> login(String dni, String password) {
        Usuario u = usuarioService.login(dni, password);
        if (u != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("mensaje", "Login exitoso");
            response.put("usuario", u.getNombre());
            response.put("rol", u.getRol());
            return response;
        }
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("mensaje", "Credenciales inválidas");
        return error;
    } */
    //metodo cambiado y cifrando para evitar vulneraciones

    public Response<Map<String, Object>> login(String dni, String password) {
        Response<Map<String, Object>> response = new Response<>();
        Map<String, Object> Usuario = new HashMap<>();
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = ConfiguracionBaseDatos.getConnection();
            // Construir la consulta SQL con la contraseña cifrada
            String sql = "SELECT * FROM vendedor WHERE nombre = ? AND password = ?";
            //preparacion de la consulta a la bd
            pst = conn.prepareStatement(sql);
            //rellenamos datos de forma segura
            pst.setString(1, dni); //el primer "hueco" es el dni
            pst.setString(2, password); //el segundo "hueco" es la contraseña cifrada

            //ejecutamos
            rs = pst.executeQuery(); //posible SQLException

            if (rs.next()) {
                Usuario.put("status", "ok");
                Usuario.put("mensaje", "Bienvenido" + rs.getString("nombre"));

                //guardamos datos utiles para flutter
                Usuario.put("id", rs.getInt("idvendedor"));
                Usuario.put("rol", rs.getString("rol")); // 'admin' o 'vendedor'
                Usuario.put("nombre", rs.getString("nombre"));
                response.exito(Usuario);
            } else {
                // Credenciales incorrectas
                response.internal_error("MCH.login: Credenciales incorrectas.");
            }
        } catch (SQLException e) {
            response.internal_error("MCH.login: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pst != null) pst.close();
                //no se cierra conN por si se hace uso de un pool compartido
                //si se abre una por consulta; solo descomentar la siguiente linea de codigo:
                //if (conn != null) conn.close();
            } catch (SQLException e) {
                response.internal_error("MCH.login: " + e.getMessage());
            }
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

    public Response<List<Map<String, Object>>> buscarProductoEnVentaPorIdONombre(String criterio) {
        // Retornamos lista vacía para evitar crash por ahora
        Response response = new Response();
        response.exito(new ArrayList<>());
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

    public Map<String, Object> listarVentas() {
        try {
            List<Venta> ventas = ventaService.listarVentas();
            List<Map<String, Object>> ventasMap = new ArrayList<>();

            // Repositorio para buscar los detalles de cada venta
            io.carpets.repositories.DetalleVentaRepository detalleRepo = new io.carpets.repositories.implementacion.DetalleVentaRepositoryImplementacion();
            io.carpets.repositories.ProductoRepository productoRepo = new io.carpets.repositories.implementacion.ProductoRepositoryImplementacion();

            for (Venta v : ventas) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", v.getId());
                m.put("numeroBoleta", v.getNumeroBoleta()); // Ahora será "Venta #..."
                m.put("monto", v.getMonto());
                m.put("fecha", v.getFecha() != null ? v.getFecha().toString() : "");
                m.put("clienteDni", v.getClienteDni());

                // --- AGREGAMOS LOS DETALLES (PRODUCTOS) A LA RESPUESTA ---
                List<io.carpets.entidades.DetalleVenta> detalles = detalleRepo.findByVenta(v.getId());
                List<Map<String, Object>> productosList = new ArrayList<>();

                for (io.carpets.entidades.DetalleVenta d : detalles) {
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
                // ---------------------------------------------------------

                ventasMap.add(m);
            }

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "ok");
            resultado.put("Content", ventasMap);
            return resultado;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
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

    public Map<String, Object> registrarCompra(Compra compra, List<DetalleCompra> detalles) {
        try {
            boolean exito = compraService.registrarCompra(compra, detalles);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", exito ? "ok" : "error");
            return resultado;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // 🟢 CORRECCIÓN: Devolvemos List<Map> en lugar de List<Compra>
    public List<Map<String, Object>> listarCompras() {
        List<Compra> compras = compraService.listarCompras();
        List<Map<String, Object>> listaMapas = new ArrayList<>();

        // Repositorios para buscar la imagen asociada
        io.carpets.repositories.DetalleCompraRepository detalleRepo = new io.carpets.repositories.implementacion.DetalleCompraRepositoryImplementacion();
        io.carpets.repositories.ProductoRepository productoRepo = new io.carpets.repositories.implementacion.ProductoRepositoryImplementacion();

        for (Compra c : compras) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("descripcion", c.getDescripcion());
            map.put("monto", c.getMonto());

            // --- BÚSQUEDA DE IMAGEN ---
            // Buscamos los detalles de esta compra para encontrar qué producto fue
            List<DetalleCompra> detalles = detalleRepo.findByCompraId(c.getId());
            if (detalles != null && !detalles.isEmpty()) {
                // Tomamos el primer producto de la compra para mostrar su foto
                Producto p = productoRepo.findById(detalles.get(0).getProductoId()).getContent();
                if (p != null) {
                    map.put("imagePath", p.getImagePath()); // Enviamos la ruta
                }
            }
            // --------------------------

            listaMapas.add(map);
        }
        return listaMapas;
    }
    public Map<String, Object> eliminarDetalleCompra(int detalleId) {
        boolean exito = compraService.eliminarDetalleCompra(detalleId);
        Map<String, Object> r = new HashMap<>();
        r.put("status", exito ? "ok" : "error");
        return r;
    }

    public Map<String, Object> editarDetalleCompra(int detalleId, int cantidad, double precio) {
        boolean exito = compraService.editarDetalleCompra(detalleId, cantidad, precio);
        Map<String, Object> r = new HashMap<>();
        r.put("status", exito ? "ok" : "error");
        return r;
    }

    public Map<String, Object> eliminarCompra(int compraId) {
        boolean exito = compraService.eliminarCompra(compraId);
        Map<String, Object> r = new HashMap<>();
        r.put("status", exito ? "ok" : "error");
        return r;
    }

    // Stubs para evitar errores de compilación
    public Map<String, Object> validarDatosProductoNuevo(String codigo, int cantidad, double precio) {
        return new HashMap<>();
    }

    public Map<String, Object> agregarProductoNuevoACompra(DetalleCompra detalle) {
        return new HashMap<>();
    }

    public Map<String, Object> agregarProductoExistenteACompra(int productoId, int cantidad) {
        return new HashMap<>();
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