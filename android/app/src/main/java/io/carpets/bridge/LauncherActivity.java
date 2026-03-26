package io.carpets.bridge;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import io.carpets.bridge.BridgeCompra;
import io.carpets.bridge.BridgeMain;
import io.carpets.bridge.BridgeProducto;
import io.carpets.bridge.BridgeVenta;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

/**
 * LauncherActivity
 * - Actividad mínima que actúa como punto de entrada (LAUNCHER) pero no infla vistas.
 * - Está pensada para permitir que el equipo de frontend (Flutter u otro) implemente la UI
 *   por separado sin depender de los layouts XML actuales.
 */
public class LauncherActivity extends FlutterActivity {

    //Nombres de los canales :VvVVvV
    private static final String PRODUCT = "samples.flutter.dev/Productos";
    private static final String VENTA = "samples.flutter.dev/Venta";
    private static final String LOGIN = "samples.flutter.dev/Login";
    private static final String COMPRA = "samples.flutter.dev/Compra";

    //inicializamos una sola vez la memoria
    private BridgeProducto bridgeProducto;
    private BridgeVenta bridgeVenta;
    private BridgeMain bridgeMain;
    private BridgeCompra bridgeCompra;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        //inicializmos los puentes
        bridgeProducto = new BridgeProducto();
        bridgeVenta = new BridgeVenta();
        bridgeMain = new BridgeMain();
        bridgeCompra = new BridgeCompra();
        //CANAL DE PRODUCTOS
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), PRODUCT)
                .setMethodCallHandler((call, result) -> {
                    // Creamos un hilo secundario para no congelar la pantalla
                    new Thread(() -> {
                        try {
                            Object respuesta = bridgeProducto.Dirigir(call.method, (List<Object>) call.arguments);
                            runOnUiThread(() -> result.success(respuesta));
                        } catch (Exception e) {
                            runOnUiThread(() -> result.error("ERROR", e.getMessage(), null));
                        }
                    }).start();
                });

        //CANAL DE VENTAS
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), VENTA)
                .setMethodCallHandler((call, result) -> {
                    // Creamos un hilo secundario para no congelar la pantalla
                    new Thread(() -> {
                        try {
                            Object respuesta = bridgeVenta.Dirigir(call.method, (List<Object>) call.arguments);
                            runOnUiThread(() -> result.success(respuesta));
                        } catch (Exception e) {
                            runOnUiThread(() -> result.error("ERROR", e.getMessage(), null));
                        }
                    }).start();
                });
        
        //CANAL PRINCIPAL
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), LOGIN)
                .setMethodCallHandler((call, result) -> {
                    // Creamos un hilo secundario para no congelar la pantalla
                    new Thread(() -> {
                        try {
                            Object respuesta = bridgeMain.Dirigir(call.method, (List<Object>) call.arguments);
                            runOnUiThread(() -> result.success(respuesta));
                        } catch (Exception e) {
                            runOnUiThread(() -> result.error("ERROR", e.getMessage(), null));
                        }
                    }).start();
                });

        //CANAL DE COMPRAS
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), COMPRA)
                .setMethodCallHandler((call, result) -> {
                    // Creamos un hilo secundario para no congelar la pantalla
                    new Thread(() -> {
                        try {
                            Object respuesta = bridgeCompra.Dirigir(call.method, (List<Object>) call.arguments);
                            runOnUiThread(() -> result.success(respuesta));
                        } catch (Exception e) {
                            runOnUiThread(() -> result.error("ERROR", e.getMessage(), null));
                        }
                    }).start();
                });
    }

}
