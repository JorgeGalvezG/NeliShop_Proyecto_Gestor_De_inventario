package io.carpets.bridge;

import io.carpets.flutterbridge.MethodChannelHandler;
import io.carpets.util.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BridgeVenta {
    private final String listarVentas = "listVentas";
    private final String registrarVenta = "regVenta";

    private final String obtenerVentasPorDia = "getVentaPorDay";
    private final String eliminarVenta = "deleteVenta";
    private final String calcularMontosVentaCompleta = "calcMontVentCom";
    private final String calcularTotalVenta = "calcTotVent";
    private final String generarBoleta = "genBoletaVenta";
    private final String calcularMontos = "calcMontos";

    HashMap<String, Function<Object, Response>> VoidFunc = new HashMap<>();
    HashMap<String, Function<Object, Response>> Funct = new HashMap<>();
    HashMap<String, BiFunction<Object, Object, Response>> Bifunc = new HashMap<>();
    MethodChannelHandler MCH;

    public BridgeVenta() {
        MCH = new MethodChannelHandler();
        CargarFunciones();
    }

    public Object Dirigir(String Funcion, List<Object> List) {
        if (List == null || List.isEmpty()) {
            return Redirigir(Funcion, null).getMap();
        } else if (List.size() == 1) {
            return RedirigirFunction(Funcion, List).getMap();
        } else if (List.size() == 2) {
            return RedirigirBifunction(Funcion, List).getMap();
        } else {
            Response error = new Response();
            error.internal_error("Error de Puente Venta: demasiados argumentos");
            return error.getMap();
        }
    }

    private Response Redirigir(String Funcion, List<Object> List) {
        Function<Object, Response> f = VoidFunc.get(Funcion);
        if (f != null) return f.apply(null);
        return functionNotFound(Funcion);
    }

    private Response RedirigirFunction(String Funcion, List<Object> List) {
        Function<Object, Response> f = Funct.get(Funcion);
        if (f != null) return f.apply(List.get(0));
        return functionNotFound(Funcion);
    }

    private Response RedirigirBifunction(String Funcion, List<Object> List) {
        BiFunction<Object, Object, Response> f = Bifunc.get(Funcion);
        if (f != null) return f.apply(List.get(0), List.get(1));
        return functionNotFound(Funcion);
    }

    private Response functionNotFound(String funcionName) {
        Response res = new Response();
        res.internal_error("Función de Venta no registrada en Java: " + funcionName);
        return res;
    }

    void CargarFunciones() {
        // Funciones sin parámetros
        VoidFunc.put(listarVentas, (Object l) -> MCH.listarVentas());

        // Funciones con un parámetro
        Funct.put(obtenerVentasPorDia, (Object f) -> MCH.obtenerVentasPorDia((String) f));
        Funct.put(eliminarVenta, (Object id) -> MCH.eliminarVenta((int) id));

        // Funciones con dos parámetros (AQUÍ ESTÁ LA MAGIA DEL CARRITO)
        Bifunc.put(registrarVenta, (Object ventaMapObj, Object detallesListObj) -> {
            try {
                Map<String, Object> ventaMap = (Map<String, Object>) ventaMapObj;
                List<?> rawList = (List<?>) detallesListObj; // Recibimos lista genérica

                io.carpets.entidades.Venta venta = new io.carpets.entidades.Venta();

                // Mapeo seguro de la Cabecera
                if (ventaMap.get("clienteDni") != null)
                    venta.setClienteDni(String.valueOf(ventaMap.get("clienteDni")));

                if (ventaMap.get("descripcion") != null)
                    venta.setDescripcion(String.valueOf(ventaMap.get("descripcion")));

                venta.setFecha(new java.util.Date());
                venta.setVendedorId(1); // O el ID del usuario logueado si lo tuvieras

                // Mapeo seguro de la Lista de Detalles (Carrito)
                java.util.List<io.carpets.entidades.DetalleVenta> detalles = new ArrayList<>();

                for (Object itemObj : rawList) {
                    Map<String, Object> detMap = (Map<String, Object>) itemObj;
                    io.carpets.entidades.DetalleVenta d = new io.carpets.entidades.DetalleVenta();

                    // Conversión robusta de números (evita crash Integer vs Double)
                    if (detMap.get("productoId") != null)
                        d.setProductoId(Integer.parseInt(detMap.get("productoId").toString()));

                    if (detMap.get("cantidad") != null)
                        d.setCantidad(Integer.parseInt(detMap.get("cantidad").toString()));

                    if (detMap.get("precioUnitario") != null)
                        d.setPrecioUnitario(Double.parseDouble(detMap.get("precioUnitario").toString()));

                    detalles.add(d);
                }

                // Enviamos al servicio que ya sabe guardar en bloque y actualizar stock
                return MCH.registrarVenta(venta, detalles);

            } catch (Exception e) {
                e.printStackTrace();
                Response error = new Response();
                error.internal_error("Error en Bridge Java: " + e.getMessage());
                return error;
            }
        });
    }
}