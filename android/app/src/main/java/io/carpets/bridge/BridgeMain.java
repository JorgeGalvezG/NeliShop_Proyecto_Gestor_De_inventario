package io.carpets.bridge;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import io.carpets.flutterbridge.MethodChannelHandler;

public class BridgeMain {
    MethodChannelHandler MCH;

    public BridgeMain() {
        MCH = new MethodChannelHandler();
        CargarFunciones();
    }

    HashMap<String, Function<Object, Object>> VoidFunc = new HashMap<>();
    HashMap<String, Function<Object, Object>> Funct = new HashMap<>();
    HashMap<String, BiFunction<Object, Object, Object>> Bifunc = new HashMap<>();

    private final String login = "login";
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
        Bifunc.put(login, (Object dni, Object password) -> MCH.login(dni.toString(), password.toString()));
    }
}