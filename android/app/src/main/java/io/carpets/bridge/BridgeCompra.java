package io.carpets.bridge;

import android.os.Build;
import androidx.annotation.RequiresApi;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import io.carpets.flutterbridge.MethodChannelHandler;

public class BridgeCompra {
    private final String listarCompras = "listCompras";
    private final String registrarCompra = "RegCompra";

    MethodChannelHandler MCH;

    public BridgeCompra() {
        MCH = new MethodChannelHandler();
        CargarFunciones();
    }

    HashMap<String, Function<Object, Object>> VoidFunc = new HashMap<>();
    HashMap<String, Function<Object, Object>> Funct = new HashMap<>();
    HashMap<String, BiFunction<Object, Object, Object>> Bifunc = new HashMap<>();

    public Object Dirigir(String Funcion, List<Object> List) {
        if (List.isEmpty()) {
            return Redirigir(Funcion, List);
        } else if (List.size() == 2) {
            return RedirigirBifunction(Funcion, List);
        } else {
            return RedirigirFunction(Funcion, List);
        }
    }

    private Object Redirigir(String Funcion, List<Object> List) {
        Function<Object, Object> f = VoidFunc.get(Funcion);
        return (f != null) ? f.apply(List) : null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Object RedirigirFunction(String Funcion, List<Object> List) {
        Function<Object, Object> f = Funct.get(Funcion);
        return (f != null) ? f.apply(List.get(0)) : null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Object RedirigirBifunction(String Funcion, List<Object> List) {
        BiFunction<Object, Object, Object> f = Bifunc.get(Funcion);
        return (f != null) ? f.apply(List.get(0), List.get(1)) : null;
    }

    void CargarFunciones() {
        VoidFunc.put(listarCompras, (Object l) -> MCH.listarCompras());

        Bifunc.put(registrarCompra, (Object compraMapObj, Object detallesListObj) -> {
            java.util.Map<String, Object> compraMap = (java.util.Map<String, Object>) compraMapObj;
            List<java.util.Map<String, Object>> detallesList = (List<java.util.Map<String, Object>>) detallesListObj;

            io.carpets.entidades.Compra compra = new io.carpets.entidades.Compra();
            if (compraMap.get("descripcion") != null)
                compra.setDescripcion((String) compraMap.get("descripcion"));
            if (compraMap.get("monto") != null)
                compra.setMonto(Double.parseDouble(compraMap.get("monto").toString()));

            java.util.List<io.carpets.entidades.DetalleCompra> detalles = new java.util.ArrayList<>();
            for (java.util.Map<String, Object> detMap : detallesList) {
                io.carpets.entidades.DetalleCompra d = new io.carpets.entidades.DetalleCompra();
                if (detMap.get("productoId") != null) d.setProductoId((int) detMap.get("productoId"));
                if (detMap.get("unidades") != null) d.setUnidades((int) detMap.get("unidades"));
                if (detMap.get("precioUnitario") != null) d.setPrecioUnitario(Double.parseDouble(detMap.get("precioUnitario").toString()));
                detalles.add(d);
            }
            return MCH.registrarCompra(compra, detalles);
        });
    }
}