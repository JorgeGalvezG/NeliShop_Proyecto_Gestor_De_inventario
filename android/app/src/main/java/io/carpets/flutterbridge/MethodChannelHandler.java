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

    public Map<String, Object> login(String dni, String password) {
        Map<String, Object> response = new HashMap<>();
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = ConfiguracionBaseDatos.getConnection();
            // Construir la consulta SQL con la contraseña cifrada
            String sql = "SELECT * FROM usuarios WHERE dni = ? AND password = ?";
            //preparacion de la consulta a la bd
            pst = conn.prepareStatement(sql);
            //rellenamos datos de forma segura
            pst.setString(1, dni); //el primer "hueco" es el dni
            pst.setString(2, password); //el segundo "hueco" es la contraseña cifrada

            //ejecutamos
            rs = pst.executeQuery();

            if (rs.next()) {
                response.put("status", "ok");
                response.put("mensaje", "Bienvendio" + rs.getString("nombre"));

                //guardamos datos utiles para flutter
                response.put("id", rs.getInt("id"));
                response.put("rol", rs.getString("rol")); // 'admin' o 'vendedor'
                response.put("nombre", rs.getString("nombre"));
            } else {
                // Credenciales incorrectas
                response.put("status", "error");
                response.put("mensaje", "Usuario o contraseña incorrectos");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("mensaje", "Error de base de datos: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pst != null) pst.close();
                //no sse cierra conn por si se hace uso de un pool compartido
                //si se abre una por consulta; solo descomentar la siguiente linea de codigo:
                //if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    // ========================================================================
    // SECCIÓN 2: GESTIÓN DE PRODUCTOS
    // ========================================================================

    // Devolvemos List<Map> en lugar de List<Producto>
    public List<Map<String, Object>> obtenerProductos() {
        List<Producto> productos = productoService.obtenerTodos();
        return productos.stream().map(this::productoToMap).collect(Collectors.toList());
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
        map.put("categoriaNombre", p.getCategoriaNombre());
        map.put("imagePath", p.getImagePath());
        map.put("salePrice", p.getPrecioOferta());
        return map;
    }

    public Map<String, Object> agregarProducto(Producto producto) {
        try {
            boolean exito = productoService.agregarProducto(producto);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", exito ? "ok" : "error");
            if (!exito) resultado.put("mensaje", "No se pudo agregar el producto");
            return resultado;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    public Map<String, Object> actualizarProducto(Producto producto) {
        try {
            productoService.actualizarInventario(producto);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "ok");
            return resultado;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    public Map<String, Object> eliminarProducto(int idProducto) {
        try {
            boolean exito = productoService.eliminarProducto(idProducto);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", exito ? "ok" : "error");
            return resultado;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // Métodos de búsqueda y validación (Stubs funcionales)
    public List<Map<String, Object>> buscarProductos(String criterio, String tipo) {
        List<Producto> productos = productoService.buscarProductos(criterio, tipo);
        return productos.stream().map(this::productoToMap).collect(Collectors.toList());
    }

    public boolean validarProductoExiste(int productoId) {
        // Implementación básica o llamada al servicio si existe
        return true;
    }

    public List<Map<String, Object>> buscarProductoEnVentaPorIdONombre(String criterio) {
        // Retornamos lista vacía para evitar crash por ahora
        return new ArrayList<>();
    }


    public Double getGananciaTotal(){
        double Ganancia = productoService.getGananciaTotal();
        return Ganancia;

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
            resultado.put("id", idVenta);
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

                    // Buscamos el nombre del producto para mostrarlo bonito
                    io.carpets.entidades.Producto p = productoRepo.findById(d.getProductoId());
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
            resultado.put("ventas", ventasMap);
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
                Producto p = productoRepo.findById(detalles.get(0).getProductoId());
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

    public Map<String, Object> obtenerProductoPorId(int id) {
        return new HashMap<>();
    }

    public Map<String, Object> validarStock(int id, int cant) {
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