package io.carpets.bridge;

import android.os.Build;
import androidx.annotation.RequiresApi;
import io.carpets.flutterbridge.MethodChannelHandler;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import io.carpets.entidades.Producto;
import io.carpets.util.Response;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BridgeProducto {

    // Nombres de los métodos llamados desde Flutter
    private static final String OBTENER_PRODUCTOS = "getProduct";
    private static final String AGREGAR_PRODUCTO = "addProduct";
    private static final String ACTUALIZAR_PRODUCTO = "editProduct";
    private static final String ELIMINAR_PRODUCTO = "deleteProduct";
    private static final String BUSCAR_PRODUCTOS = "searchProducts";
    private static final String GET_GANANCIA_TOTAL = "SumGanancia";
    private static final String MODIFICAR_DESCRIPCION_COMPRA = "updateDescription";

    HashMap<String, Function<Object, Response>> VoidFunc = new HashMap<>();
    HashMap<String, Function<Object, Response>> Funct = new HashMap<>();
    HashMap<String, BiFunction<Object, Object, Response>> Bifunc = new HashMap<>();

    MethodChannelHandler MCH;

    public BridgeProducto() {
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
        res.internal_error("Función de Producto no registrada en Java: " + funcionName);
        return res;
    }

    // =========================================================================
    // DECLARACIÓN DE FUNCIONES DE NEGOCIO
    // =========================================================================

    void CargarFunciones() {
        // Funciones sin parámetros
        VoidFunc.put(OBTENER_PRODUCTOS, (Object r) -> {
            try {
                return MCH.obtenerProductos();
            } catch (Exception e) {
                Response err = new Response();
                err.internal_error("Error obteniendo productos: " + e.getMessage());
                return err;
            }
        });

        VoidFunc.put(GET_GANANCIA_TOTAL, (Object l) -> {
            try {
                return MCH.getGananciaTotal();
            } catch (Exception e) {
                Response err = new Response();
                err.internal_error("Error calculando ganancia: " + e.getMessage());
                return err;
            }
        });

        // Funciones con un parámetro
        Funct.put(AGREGAR_PRODUCTO, (Object mapObj) -> {
            try {
                if (!(mapObj instanceof Map)) {
                    Response err = new Response();
                    err.internal_error("El parámetro debe ser un Mapa válido.");
                    return err;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> mapa = (Map<String, Object>) mapObj;

                Producto p = new Producto(
                        0, // ID autogenerado
                        mapa.get("nombre") != null ? mapa.get("nombre").toString() : "",
                        new Date(),
                        parseDouble(mapa.get("precioCompra"), 0.0),
                        parseDouble(mapa.get("precioVenta"), 0.0),
                        parseInt(mapa.get("cantidad"), 0),
                        mapa.get("categoriaNombre") != null ? mapa.get("categoriaNombre").toString() : "",
                        "" // Código vacío por defecto
                );

                if (mapa.get("imagePath") != null) {
                    p.setImagePath(mapa.get("imagePath").toString());
                }

                return MCH.agregarProducto(p);
            } catch (Exception e) {
                Response err = new Response();
                err.internal_error("Error procesando inserción de producto: " + e.getMessage());
                return err;
            }
        });

        Funct.put(ACTUALIZAR_PRODUCTO, (Object mapObj) -> {
            try {
                if (!(mapObj instanceof Map)) {
                    Response err = new Response();
                    err.internal_error("El parámetro debe ser un Mapa válido.");
                    return err;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> mapa = (Map<String, Object>) mapObj;

                Producto p = new Producto(
                        parseInt(mapa.get("id"), 0),
                        mapa.get("nombre") != null ? mapa.get("nombre").toString() : "",
                        new Date(),
                        parseDouble(mapa.get("precioCompra"), 0.0),
                        parseDouble(mapa.get("precioVenta"), 0.0),
                        parseInt(mapa.get("cantidad"), 0),
                        mapa.get("categoriaNombre") != null ? mapa.get("categoriaNombre").toString() : "",
                        ""
                );

                if (mapa.get("imagePath") != null) {
                    p.setImagePath(mapa.get("imagePath").toString());
                }

                if (mapa.get("salePrice") != null) {
                    p.setPrecioOferta(parseDouble(mapa.get("salePrice"), 0.0));
                } else {
                    p.setPrecioOferta(null);
                }

                return MCH.actualizarProducto(p);
            } catch (Exception e) {
                Response err = new Response();
                err.internal_error("Error procesando actualización de producto: " + e.getMessage());
                return err;
            }
        });

        Funct.put(ELIMINAR_PRODUCTO, (Object idProducto) -> {
            try {
                return MCH.eliminarProducto(parseInt(idProducto, 0));
            } catch (Exception e) {
                Response err = new Response();
                err.internal_error("Error eliminando producto: " + e.getMessage());
                return err;
            }
        });

        Funct.put(MODIFICAR_DESCRIPCION_COMPRA, (Object compraMap) -> {
            try {
                return MCH.modificarDescripcionCompra(compraMap);
            } catch (Exception e) {
                Response err = new Response();
                err.internal_error("Error modificando descripción de compra: " + e.getMessage());
                return err;
            }
        });

        // Funciones con dos parámetros
        Bifunc.put(BUSCAR_PRODUCTOS, (Object criterio, Object tipo) -> {
            try {
                String c = criterio != null ? criterio.toString() : "";
                String t = tipo != null ? tipo.toString() : "";
                return MCH.buscarProductos(c, t);
            } catch (Exception e) {
                Response err = new Response();
                err.internal_error("Error buscando productos: " + e.getMessage());
                return err;
            }
        });
    }

    // =========================================================================
    // HELPERS DE PARSEO SEGURO
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