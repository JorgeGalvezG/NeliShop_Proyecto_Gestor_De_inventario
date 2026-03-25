package io.carpets.bridge;

import android.os.Build;
import androidx.annotation.RequiresApi;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import io.carpets.flutterbridge.MethodChannelHandler;
import io.carpets.util.Response;

public class BridgeCompra {
    private final String listarCompras = "listCompras";
    private final String registrarCompra = "RegCompra";

    MethodChannelHandler MCH;

    public BridgeCompra() {
        MCH = new MethodChannelHandler();
        CargarFunciones();
    }

    HashMap<String, Function<Object, Response>> VoidFunc = new HashMap<>();
    HashMap<String, Function<Object, Response>> Funct = new HashMap<>();
    HashMap<String, BiFunction<Object, Object, Response>> Bifunc = new HashMap<>();

    public Object Dirigir(String Funcion, List<Object> List) {
        if (List.isEmpty()) {
            return Redirigir(Funcion, List).getMap();
        } else if (List.size() == 2) {
            return RedirigirBifunction(Funcion, List).getMap();
        } else {
            return RedirigirFunction(Funcion, List).getMap();
        }
    }

    private Response Redirigir(String Funcion, List<Object> List) {
        return VoidFunc.get(Funcion).apply(null);
    }

    private Response RedirigirFunction(String Funcion, List<Object> List) {
        return Funct.get(Funcion).apply(List.get(0));
    }

    private Response RedirigirBifunction(String Funcion, List<Object> List) {
        return Bifunc.get(Funcion).apply(List.get(0), List.get(1));
    }

    void CargarFunciones() {
        VoidFunc.put(listarCompras, (Object l) -> MCH.listarCompras());

        Bifunc.put(registrarCompra, (Object compraMapObj, Object detallesListObj) -> MCH.registrarCompra(compraMapObj, detallesListObj));

    }
}