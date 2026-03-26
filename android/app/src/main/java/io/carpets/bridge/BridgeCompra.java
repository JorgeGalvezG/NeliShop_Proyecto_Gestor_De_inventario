package io.carpets.bridge;

import android.os.Build;
import androidx.annotation.RequiresApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import io.carpets.flutterbridge.MethodChannelHandler;
import io.carpets.util.Response;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BridgeCompra {

    // Constantes de Nombres de Métodos (CamelCase estándar)
    private static final String LISTAR_COMPRAS = "listCompras";
    private static final String REGISTRAR_COMPRA = "RegCompra";

    // Constantes de Llaves del Diccionario (Evita Strings Mágicos)
    private static final class CompraKeys {
        static final String DESCRIPCION = "descripcion";
        static final String MONTO = "monto";
    }

    private static final class DetalleKeys {
        static final String PRODUCTO_ID = "productoId";
        static final String CANTIDAD = "cantidad"; // Viene de Flutter
        static final String PRECIO_UNITARIO = "precioUnitario";
    }

    MethodChannelHandler MCH;

    HashMap<String, Function<Object, Response>> VoidFunc = new HashMap<>();
    HashMap<String, Function<Object, Response>> Funct = new HashMap<>();
    HashMap<String, BiFunction<Object, Object, Response>> Bifunc = new HashMap<>();

    public BridgeCompra() {
        MCH = new MethodChannelHandler();
        CargarFunciones();
    }

    // =========================================================================
    // ENRUTAMIENTO PRINCIPAL
    // =========================================================================

    public Object Dirigir(String Funcion, List<Object> List) {
        if (List == null || List.isEmpty()) {
            return Redirigir(Funcion, null).getMap();
        } else if (List.size() == 1) {
            return RedirigirFunction(Funcion, List).getMap();
        } else if (List.size() == 2) {
            return RedirigirBifunction(Funcion, List).getMap();
        } else {
            Response error = new Response();
            error.internal_error("Error de Puente: Función '" + Funcion + "' recibió demasiados argumentos (" + List.size() + ")");
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
        res.internal_error("Función no registrada en Java: " + funcionName);
        return res;
    }

    // =========================================================================
    // DECLARACIÓN DE FUNCIONES DE NEGOCIO
    // =========================================================================

    void CargarFunciones() {
        // --- LISTAR COMPRAS ---
        VoidFunc.put(LISTAR_COMPRAS, (Object l) -> {
            try {
                return MCH.listarCompras();
            } catch (Exception e) {
                Response error = new Response();
                error.internal_error("Error al listar compras: " + e.getMessage());
                return error;
            }
        });

        // --- REGISTRAR COMPRA ---
        Bifunc.put(REGISTRAR_COMPRA, (Object compraMapObj, Object detallesListObj) -> {
            try {
                // 1. Validación de Tipos (ClassCastException Prevention)
                if (!(compraMapObj instanceof Map)) {
                    Response error = new Response();
                    error.internal_error("El primer parámetro de la compra debe ser un Mapa/Diccionario válido.");
                    return error;
                }
                if (!(detallesListObj instanceof List)) {
                    Response error = new Response();
                    error.internal_error("El segundo parámetro de la compra debe ser una Lista de detalles válida.");
                    return error;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> compraMap = (Map<String, Object>) compraMapObj;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> detallesList = (List<Map<String, Object>>) detallesListObj;

                // 2. Validación de Contenido
                if (detallesList.isEmpty()) {
                    Response error = new Response();
                    error.message_error("No se puede registrar una compra sin productos en el detalle.");
                    return error;
                }

                // 3. Construcción Segura de la Cabecera
                io.carpets.entidades.Compra compra = new io.carpets.entidades.Compra();

                if (compraMap.get(CompraKeys.DESCRIPCION) != null) {
                    compra.setDescripcion(compraMap.get(CompraKeys.DESCRIPCION).toString());
                }
                compra.setMonto(parseDouble(compraMap.get(CompraKeys.MONTO), 0.0));

                // 4. Construcción Segura de los Detalles
                java.util.List<io.carpets.entidades.DetalleCompra> detalles = new java.util.ArrayList<>();
                for (Map<String, Object> detMap : detallesList) {
                    io.carpets.entidades.DetalleCompra d = new io.carpets.entidades.DetalleCompra();

                    d.setProductoId(parseInt(detMap.get(DetalleKeys.PRODUCTO_ID), 0));
                    // Flutter envía 'cantidad', el modelo Java espera 'unidades'
                    d.setUnidades(parseInt(detMap.get(DetalleKeys.CANTIDAD), 0));
                    d.setPrecioUnitario(parseDouble(detMap.get(DetalleKeys.PRECIO_UNITARIO), 0.0));

                    detalles.add(d);
                }

                // 5. Llamada al MCH (Protegida)
                return MCH.registrarCompra(compra, detalles);

            } catch (Exception e) {
                Response error = new Response();
                error.internal_error("Error grave procesando la compra en el Bridge: " + e.getMessage());
                return error;
            }
        });
    }

    // =========================================================================
    // HELPERS DE PARSEO SEGURO (Null & NumberFormat Prevention)
    // =========================================================================

    private Double parseDouble(Object obj, double defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer parseInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}