package io.carpets.bridge;

import android.os.Build;
import androidx.annotation.RequiresApi;
import io.carpets.flutterbridge.MethodChannelHandler;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import io.carpets.entidades.Producto;

public class BridgeProducto {

    // Listado de claves
    private final String obtenerProductos = "getProduct";
    private final String agregarProducto = "addProduct";
    private final String actualizarProducto = "editProduct";
    private final String eliminarProducto = "deleteProduct";
    private final String validarProductoExiste = "ProductoExists";
    private final String buscarProductoEnVentaPorIdONombre = "SearchIdNombre";
    private final String obtenerProductoPorId = "getProdID";
    private final String buscarProductos = "searchProducts";
    private final String validarStock = "ValStock";
    private final String getGananciaTotal = "SumGanancia";

    HashMap<String, Function<Object, Object>> VoidFunc = new HashMap<>();
    HashMap<String, Function<Object, Object>> Funct = new HashMap<>();
    HashMap<String, BiFunction<Object, Object, Object>> Bifunc = new HashMap<>();

    MethodChannelHandler MCH;

    public BridgeProducto() {
        MCH = new MethodChannelHandler();
        CargarFunciones();
    }

    public Object Dirigir(String Funcion, List<Object> List) {
        if (List.isEmpty()) { return Redirigir(Funcion, List); }
        else if (List.size() == 1) { return RedirigirFunction(Funcion, List); }
        else { return RedirigirBifunction(Funcion, List); }
    }

    private Object Redirigir(String Funcion, List<Object> List) {
        return VoidFunc.get(Funcion).apply(null);
    }

    private Object RedirigirFunction(String Funcion, List<Object> List) {
        return Funct.get(Funcion).apply(List.get(0));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Object RedirigirBifunction(String Funcion, List<Object> List) {
        return Bifunc.get(Funcion).apply(List.get(0), List.get(1));
    }

    void CargarFunciones() {
        // Funciones sin parámetros
        VoidFunc.put(obtenerProductos, (Object r) -> MCH.obtenerProductos());
        VoidFunc.put(getGananciaTotal, (Object l) -> MCH.getGananciaTotal());

        // Funciones con un parámetro
        Funct.put(agregarProducto, (Object MapObj) -> {
            Date D = new Date();
            Map<String, Object> Mapa = (Map<String, Object>) MapObj;

            // Creamos el producto para insertar
            Producto p = new Producto(
                    0, // ID autogenerado
                    (String) Mapa.get("nombre"),
                    D,
                    Double.parseDouble(Mapa.get("precioCompra").toString()),
                    Double.parseDouble(Mapa.get("precioVenta").toString()),
                    Integer.parseInt(Mapa.get("cantidad").toString()),
                    (String) Mapa.get("categoriaNombre"),
                    "" // Código vacío por defecto
            );
            // Asignamos la imagen si viene
            if (Mapa.get("imagePath") != null) {
                p.setImagePath((String) Mapa.get("imagePath"));
            }

            return MCH.agregarProducto(p);
        });

        Funct.put(actualizarProducto, (Object MapObj) -> {
            Date D = new Date();
            Map<String, Object> Mapa = (Map<String, Object>) MapObj;

            // CORRECCIÓN CRÍTICA: Leer el ID y la Imagen del mapa
            int id = Integer.parseInt(Mapa.get("id").toString());
            String imagePath = (String) Mapa.get("imagePath");

            Producto p = new Producto(
                    id, // <--- ID CORRECTO (Antes era 0)
                    (String) Mapa.get("nombre"),
                    D,
                    Double.parseDouble(Mapa.get("precioCompra").toString()),
                    Double.parseDouble(Mapa.get("precioVenta").toString()),
                    Integer.parseInt(Mapa.get("cantidad").toString()),
                    (String) Mapa.get("categoriaNombre"),
                    ""
            );
            p.setImagePath(imagePath); // <--- IMAGEN CORRECTA (Antes era "wasa")

            if (Mapa.get("salePrice") != null) {
                p.setPrecioOferta(Double.parseDouble(Mapa.get("salePrice").toString()));
            } else {
                p.setPrecioOferta(null);
            }

            return MCH.actualizarProducto(p);
        });

        Funct.put(eliminarProducto, (Object idProducto) -> MCH.eliminarProducto((int) idProducto));
        Funct.put(validarProductoExiste, (Object productoId) -> MCH.validarProductoExiste((int) productoId));
        Funct.put(buscarProductoEnVentaPorIdONombre, (Object criterio) -> MCH.buscarProductoEnVentaPorIdONombre((String) criterio));
        Funct.put(obtenerProductoPorId, (Object id) -> MCH.obtenerProductoPorId((int) id));

        // Funciones con dos parámetros
        Bifunc.put(buscarProductos, (Object criterio, Object tipo) -> MCH.buscarProductos((String) criterio, (String) tipo));
        Bifunc.put(validarStock, (Object productoId, Object cantidad) -> MCH.validarStock((int) productoId, (int) cantidad));
    }
}