package io.carpets.bridge;

import io.carpets.flutterbridge.MethodChannelHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList; // Importante
import java.util.function.BiFunction;
import java.util.function.Function;

public class BridgeVenta {
    private final String listarVentas = "listVentas";
    private final String registrarVenta = "regVenta";

    // ... (otros strings se mantienen igual) ...
    private final String obtenerVentasPorDia = "getVentaPorDay";
    private final String eliminarVenta = "deleteVenta";
    private final String calcularMontosVentaCompleta = "calcMontVentCom";
    private final String calcularTotalVenta = "calcTotVent";
    private final String generarBoleta = "genBoletaVenta";
    private final String calcularMontos = "calcMontos";

    HashMap<String, Function<Object, Object>> VoidFunc = new HashMap<>();
    HashMap<String, Function<Object, Object>> Funct = new HashMap<>();
    HashMap<String, BiFunction<Object, Object, Object>> Bifunc = new HashMap<>();

    MethodChannelHandler MCH;

    public BridgeVenta() {
        MCH = new MethodChannelHandler();
        CargarFunciones();
    }

    public Object Dirigir(String Funcion, List<Object> List) {
        if (List.isEmpty()) {
            return Redirigir(Funcion, List);
        } else if (List.size() == 1) {
            return RedirigirFunction(Funcion, List);
        } else {
            return RedirigirBifunction(Funcion, List);
        }
    }

    private Object Redirigir(String Funcion, List<Object> List) {
        Function<Object, Object> f = VoidFunc.get(Funcion);
        return (f != null) ? f.apply(List) : null;
    }

    private Object RedirigirFunction(String Funcion, List<Object> List) {
        Function<Object, Object> f = Funct.get(Funcion);
        return (f != null) ? f.apply(List.get(0)) : null;
    }

    private Object RedirigirBifunction(String Funcion, List<Object> List) {
        BiFunction<Object, Object, Object> f = Bifunc.get(Funcion);
        return (f != null) ? f.apply(List.get(0), List.get(1)) : null;
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
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("mensaje", "Error en Bridge Java: " + e.getMessage());
                return error;
            }
        });
    }
}