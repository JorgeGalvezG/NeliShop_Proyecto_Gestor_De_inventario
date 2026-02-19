import 'dart:ffi';
import 'package:flutter/services.dart';
import 'config/core.dart';
import 'config/constants.dart';

class BridgeFlutter {
  // Singleton
  static final BridgeFlutter _instance = BridgeFlutter._internal();
  factory BridgeFlutter() => _instance;
  BridgeFlutter._internal();

  // Canales de comunicación con Android
  // Usamos las constantes para evitar errores de escritura
  final _channelLogin = const MethodChannel(AppConstants.channelLogin);
  final _channelProductos = const MethodChannel(AppConstants.channelProductos);
  final _channelVenta = const MethodChannel(AppConstants.channelVenta);
  final _channelCompra = const MethodChannel(AppConstants.channelCompra);

  // ---------------------------------------------------------------------------
  // MÉTODO GENÉRICO DE LECTURA (GET)
  // ---------------------------------------------------------------------------
  /// Útil para obtener listas o datos simples donde si falla, devolver null es aceptable.
  Future<T?> _invoke<T>(MethodChannel channel, String method, [dynamic arguments]) async {
    try {
      final result = await channel.invokeMethod(method, arguments);
      return result as T?;
    } on PlatformException catch (e) {
      print("⚠️ Error de Plataforma en $method: ${e.message}");
      return null;
    } catch (e) {
      print("⚠️ Error Desconocido en $method: $e");
      return null;
    }
  }

  // ---------------------------------------------------------------------------
  // MÉTODOS DE NEGOCIO (OPTIMIZADOS)
  // ---------------------------------------------------------------------------

  // -------- LOGIN --------
  Future<BridgeResponse> login(String dni, String password) async {
    try {
      final result = await _channelLogin.invokeMethod(
          AppConstants.methodLogin,
          [dni, password]
      );
      // Si Java devuelve un mapa, lo convertimos
      if (result != null && result is Map) {
        return BridgeResponse.fromMap(result);
      }
      return BridgeResponse(status: 'error', mensaje: 'Respuesta inválida del servidor');

    } on PlatformException catch (e) {
      return BridgeResponse(status: 'error', mensaje: e.message ?? 'Error de conexión');
    } catch (e) {
      return BridgeResponse(status: 'error', mensaje: e.toString());
    }
  }

  // -------- PRODUCTOS --------

  // Obtener lista (Usa el genérico _invoke para ser más rápido)
  Future<JsonList> obtenerProductos() async {
    final result = await _invoke<JsonList>(
        _channelProductos,
        AppConstants.methodGetProducts,
        []
    );
    return result ?? []; // Si falla, devuelve lista vacía
  }

  Future<BridgeResponse> agregarProducto(JsonMap producto) async {
    return _ejecutarTransaccion(_channelProductos, AppConstants.methodAddProduct, [producto]);
  }

  Future<BridgeResponse> actualizarProducto(JsonMap producto) async {
    return _ejecutarTransaccion(_channelProductos, AppConstants.methodEditProduct, [producto]);
  }

  Future<BridgeResponse> eliminarProducto(int id) async {
    return _ejecutarTransaccion(_channelProductos, AppConstants.methodDeleteProduct, [id]);
  }

  Future<double> getGananciaTotal() async {
    final result = await _invoke<double>(
        _channelProductos,
        AppConstants.methodSumGanancia,
        []
    );
    return result ?? 0.0;
  }

  // -------- VENTAS (Aquí estaba tu error en VentasPage) --------

  Future<BridgeResponse> registrarVenta(JsonMap venta, JsonList detalles) async {
    try {
      // Llamamos a Java
      final result = await _channelVenta.invokeMethod(
          AppConstants.methodRegVenta,
          [venta, detalles]
      );

      // Java devuelve un entero (ID) si todo sale bien
      if (result is int) {
        return BridgeResponse(status: 'ok', data: {'id': result});
      }
      // O Java devuelve un Mapa si configuraste una respuesta compleja
      else if (result is Map) {
        return BridgeResponse.fromMap(result);
      }

      return BridgeResponse(status: 'ok', data: result);

    } on PlatformException catch (e) {
      // ¡IMPORTANTE! Aquí capturamos el mensaje "Stock insuficiente" de Java
      return BridgeResponse(status: 'error', mensaje: e.message);
    } catch (e) {
      return BridgeResponse(status: 'error', mensaje: e.toString());
    }
  }
//solo devuelve un jsonlist en vez de un mapa
 /* Future<JsonList> listarVentas() async {
    final result = await _invoke<JsonList>(_channelVenta, AppConstants.methodListVentas, []);
    return result ?? [];
  } */
  //trabajando
  Future<BridgeResponse> listarVentas() async {
    try {
      final result = await _channelVenta.invokeMethod(AppConstants.methodListVentas, []);

      if (result is Map) {
        return BridgeResponse.fromMap(result);
      }
      // Si Java devuelve null o algo raro
      return BridgeResponse(status: 'error', mensaje: 'Formato de respuesta inválido');

    } catch (e) {
      return BridgeResponse(status: 'error', mensaje: e.toString());
    }
  }

  // -------- COMPRAS --------

  Future<JsonList> listarCompras() async {
    final result = await _invoke<JsonList>(_channelCompra, AppConstants.methodListCompras);
    return result ?? [];
  }

  Future<BridgeResponse> registrarCompra(JsonMap compra, JsonList detalles) async {
    return _ejecutarTransaccion(_channelCompra, AppConstants.methodRegCompra, [compra, detalles]);
  }

  // ---------------------------------------------------------------------------
  // HELPER PRIVADO PARA TRANSACCIONES (ESCRITURA)
  // ---------------------------------------------------------------------------
  /// Este método maneja los try-catch de escritura para no repetir código
  Future<BridgeResponse> _ejecutarTransaccion(MethodChannel channel, String method, dynamic args) async {
    try {
      final result = await channel.invokeMethod(method, args);

      if (result is Map) {
        return BridgeResponse.fromMap(result);
      } else {
        // Si Java devolvió algo que no es mapa pero no falló, asumimos éxito
        return BridgeResponse(status: 'ok', data: result);
      }
    } on PlatformException catch (e) {
      // Error controlado desde Java (ej. Validación fallida)
      return BridgeResponse(status: 'error', mensaje: e.message);
    } catch (e) {
      // Error inesperado (ej. NullPointer en Flutter)
      return BridgeResponse(status: 'error', mensaje: e.toString());
    }
  }
}