package io.carpets;

import android.os.Bundle; // ✅ Import necesario
import android.os.StrictMode; // ✅ Import necesario
import androidx.annotation.NonNull;

import java.util.List;

import io.carpets.bridge.BridgeCompra;
import io.carpets.bridge.BridgeMain;
import io.carpets.bridge.BridgeProducto;
import io.carpets.bridge.BridgeVenta;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

/**
 * LauncherActivity Corregido
 * - Incluye StrictMode para permitir BD en hilo principal.
 * - Incluye result.success() en TODOS los canales para que Flutter reciba los datos.
 */
public class LauncherActivity extends FlutterActivity {

    // Nombres de los canales
    private static final String PRODUCT = "samples.flutter.dev/Productos";
    private static final String VENTA = "samples.flutter.dev/Venta";
    private static final String LOGIN = "samples.flutter.dev/Login";
    private static final String COMPRA = "samples.flutter.dev/Compra";

    // 🔴 1. CORRECCIÓN CRÍTICA: Permitir conexión a BD (AWS) en el hilo principal
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        // StrictMode.setThreadPolicy(policy);
    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        // Canal Productos
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), PRODUCT)
                .setMethodCallHandler(
                        (call, result) -> {

                            //creamos hilo para no congelar
                            new Thread(() -> {
                                //acceso a la base de datos
                                BridgeProducto BP = new BridgeProducto();
                                Object respuesta = BP.Dirigir(call.method, (List<Object>) call.arguments);

                                //entregar la respuesta
                                runOnUiThread(() -> {
                                    result.success(respuesta);
                                });
                            }).start();
                        });
        //esti estaba mal
        // BridgeProducto BP = new BridgeProducto();
        // ✅ Esto estaba bien
        //result.success(BP.Dirigir(call.method, (List<Object>) call.arguments));

        // Canal Ventas
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), VENTA)
                .setMethodCallHandler((call, result) -> {

                    // 1. Abrimos un hilo secundario para que la app no se congele
                    new Thread(() -> {
                        try {
                            // 2. Hacemos el trabajo pesado (Base de Datos)
                            BridgeVenta BV = new BridgeVenta();
                            Object respuesta = BV.Dirigir(call.method, (List<Object>) call.arguments);

                            // 3. Volvemos al hilo principal SOLO para entregar la respuesta a Flutter
                            runOnUiThread(() -> {
                                result.success(respuesta);
                            });
                        } catch (Exception e) {
                            // Si algo falla, avisamos a Flutter
                            runOnUiThread(() -> {
                                result.error("ERROR_VENTA", "Error en hilo de venta: " + e.getMessage(), null);
                            });
                        }
                    }).start(); // ¡Importante! No olvides el .start()

                });

        // Canal Login
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), LOGIN)
                .setMethodCallHandler((call, result) -> {

                    new Thread(() -> {
                        try {
                            BridgeMain BM = new BridgeMain();
                            Object respuesta = BM.Dirigir(call.method, (List<Object>) call.arguments);

                            runOnUiThread(() -> {
                                result.success(respuesta);
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                result.error("ERROR_LOGIN", "Error en hilo de login: " + e.getMessage(), null);
                            });
                        }
                    }).start();

                });
        //new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), LOGIN)
        //      .setMethodCallHandler(
        //           (call, result) -> {
        //             BridgeMain BM = new BridgeMain();
        // ✅ Esto estaba bien
        //           result.success(BM.Dirigir(call.method, (List<Object>) call.arguments));
        //   }
        //);

        // Canal Compras
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), COMPRA)
                .setMethodCallHandler((call, result) -> {

                    new Thread(() -> {
                        try {
                            BridgeCompra BC = new BridgeCompra();
                            Object respuesta = BC.Dirigir(call.method, (List<Object>) call.arguments);

                            runOnUiThread(() -> {
                                result.success(respuesta);
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                result.error("ERROR_COMPRA", "Error en hilo de compra: " + e.getMessage(), null);
                            });
                        }
                    }).start();

                });
        /*new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), COMPRA)
                .setMethodCallHandler(
                        (call, result) -> {
                            BridgeCompra BC = new BridgeCompra();
                            // 🔴 3. CORRECCIÓN: Faltaba result.success()
                            result.success(BC.Dirigir(call.method, (List<Object>) call.arguments));
                        }
                );
    }*/
    }
}