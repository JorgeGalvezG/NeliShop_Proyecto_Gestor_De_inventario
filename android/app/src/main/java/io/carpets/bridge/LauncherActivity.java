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


    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), PRODUCT)
                .setMethodCallHandler(
                        (call, result) -> {
                            BridgeProducto BP = new BridgeProducto();
                            result.success(BP.Dirigir (call.method, (List<Object>) call.arguments ));
                        }
                );
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), VENTA)
                .setMethodCallHandler(
                        (call, result) -> {
                            BridgeVenta BV = new BridgeVenta();
                            BV.Dirigir (call.method, (List<Object>) call.arguments );
                        }
                );
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), LOGIN)
                .setMethodCallHandler(
                        (call, result) -> {
                            BridgeMain BM = new BridgeMain();
                            result.success(BM.Dirigir (call.method, (List<Object>) call.arguments ));

                        }
                );
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), COMPRA)
                .setMethodCallHandler(
                        (call, result) -> {
                            BridgeCompra BC = new BridgeCompra();
                            BC.Dirigir (call.method, (List<Object>) call.arguments );
                        }
                );
    }

}
