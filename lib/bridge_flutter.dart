import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'config/core.dart';
import 'config/constants.dart';

class BridgeFlutter {
  static final BridgeFlutter _instance = BridgeFlutter._internal();
  factory BridgeFlutter() => _instance;
  BridgeFlutter._internal();

  final _channelLogin    = const MethodChannel(AppConstants.channelLogin);
  final _channelProductos = const MethodChannel(AppConstants.channelProductos);
  final _channelVenta    = const MethodChannel(AppConstants.channelVenta);
  final _channelCompra   = const MethodChannel(AppConstants.channelCompra);

  // CACHE LOCAL EN MEMORIA RAM
  List<JsonMap>? _cachedProducts;
  List<JsonMap>? _cachedVentas;
  List<JsonMap>? _cachedCompras;
  DateTime? _lastProductsFetch;
  DateTime? _lastVentasFetch;
  DateTime? _lastComprasFetch;

  final Duration _cacheValidDuration = const Duration(minutes: 5);

  // Usar kDebugMode para que los logs se apaguen automáticamente en release
  bool get _enableCacheLogs => kDebugMode;

  // ---------------------------------------------------------------------------
  // HELPER PRIVADO DE LOG
  // ---------------------------------------------------------------------------
  void _log(String msg) {
    if (_enableCacheLogs) debugPrint(msg); // debugPrint en lugar de print
  }

  // ---------------------------------------------------------------------------
  // HELPER DE LECTURA (GET) CON CACHÉ — RETORNA BridgeResponse
  // ---------------------------------------------------------------------------
  Future<BridgeResponse> _invokeListCached({
    required MethodChannel channel,
    required String method,
    required List<JsonMap>? cache,
    required DateTime? lastFetch,
    required Function(List<JsonMap>, DateTime) onCacheUpdate,
    bool forceRefresh = false,
  }) async {
    // 1. Verificar si tenemos caché válido
    if (!forceRefresh && cache != null && lastFetch != null) {
      final cacheAge = DateTime.now().difference(lastFetch);

      if (cacheAge < _cacheValidDuration) {
        _log("📦 Usando caché para $method (edad: ${cacheAge.inSeconds}s)");
        return BridgeResponse(status: 'ok', data: cache);
      } else {
        _log("⏰ Caché expirado para $method (edad: ${cacheAge.inMinutes}m)");
      }
    }

    // 2. Llamar al canal nativo
    try {
      _log("🌐 Llamando al servidor nativo: $method");

      final result = await channel.invokeMethod(method, []);

      if (result == null) {
        return BridgeResponse(
          status: 'error',
          mensaje: 'Respuesta nula del servidor en $method',
        );
      }

      List<dynamic> rawList = [];

      // 3. Parseo inteligente: detecta Response de Java (Map) o lista directa
      if (result is Map) {
        final status = result['status']?.toString() ?? 'error';

        if (status != 'ok') {
          final msg = result['mensaje']?.toString() ?? 'Error desconocido en $method';
          _log("Error backend en $method: $msg");
          // Fallback silencioso con caché obsoleto si existe
          if (cache != null) {
            _log("Usando caché obsoleto como fallback");
            return BridgeResponse(status: 'ok', data: cache, mensaje: 'datos_cache');
          }
          return BridgeResponse(status: 'error', mensaje: msg);
        }

        final content = result['Content'] ?? result['data'];
        if (content is List) {
          rawList = content;
        } else {
          _log("Formato inesperado en Response de $method");
          return BridgeResponse(
            status: 'error',
            mensaje: 'Formato de respuesta inesperado en $method',
          );
        }
      } else if (result is List) {
        rawList = result;
      } else {
        _log("⚠️ Tipo de resultado inesperado en $method: ${result.runtimeType}");
        return BridgeResponse(
          status: 'error',
          mensaje: 'Tipo de respuesta inesperado: ${result.runtimeType}',
        );
      }

      // 4. Conversión segura a lista de mapas
      final listaSegura = rawList
          .map((e) => Map<String, dynamic>.from(e as Map))
          .toList();

      // 5. Actualizar caché
      final now = DateTime.now();
      onCacheUpdate(listaSegura, now);
      _log("Caché actualizado para $method (${listaSegura.length} registros)");

      return BridgeResponse(status: 'ok', data: listaSegura);

    } on PlatformException catch (e) {
      // Fallback con caché obsoleto si existe
      if (cache != null && lastFetch != null) {
        final cacheAge = DateTime.now().difference(lastFetch);
        _log("⚠️ Error de red en $method: ${e.message}");
        _log("📦 Usando caché obsoleto (edad: ${cacheAge.inMinutes}m)");
        return BridgeResponse(status: 'ok', data: cache, mensaje: 'datos_cache');
      }
      _log("❌ Error en $method sin caché disponible: ${e.message}");
      return BridgeResponse(
        status: 'error',
        mensaje: e.message ?? 'Error nativo de plataforma',
      );

    } catch (e) {
      if (cache != null) {
        _log("⚠️ Error desconocido en $method, usando caché: $e");
        return BridgeResponse(status: 'ok', data: cache, mensaje: 'datos_cache');
      }
      _log("❌ Error desconocido en $method: $e");
      return BridgeResponse(
        status: 'error',
        mensaje: 'Excepción local: $e',
      );
    }
  }

  // ---------------------------------------------------------------------------
  // HELPER PRIVADO PARA TRANSACCIONES (ESCRITURA)
  // ---------------------------------------------------------------------------
  Future<BridgeResponse> _ejecutarTransaccion(
      MethodChannel channel,
      String method,
      dynamic args,
      ) async {
    try {
      final result = await channel.invokeMethod(method, args);

      if (result == null) {
        return BridgeResponse(
          status: 'error',
          mensaje: 'Respuesta nula del servidor',
        );
      }

      if (result is Map) {
        return BridgeResponse(
          status: result['status']?.toString() ?? 'error',
          mensaje: result['mensaje']?.toString(),
          data: result['Content'] ?? result['data'] ?? result,
        );
      } else {
        return BridgeResponse(status: 'ok', data: result);
      }

    } on PlatformException catch (e) {
      return BridgeResponse(
        status: 'error',
        mensaje: e.message ?? 'Error nativo de plataforma',
      );
    } catch (e) {
      return BridgeResponse(
        status: 'error',
        mensaje: 'Excepción local: $e',
      );
    }
  }

  // ---------------------------------------------------------------------------
  // MÉTODOS DE INVALIDACIÓN DE CACHÉ
  // ---------------------------------------------------------------------------

  void _invalidarCacheProductos() {
    _cachedProducts = null;
    _lastProductsFetch = null;
    _log("🗑️ Caché de productos invalidado");
  }

  void _invalidarCacheVentas() {
    _cachedVentas = null;
    _lastVentasFetch = null;
    _log("🗑️ Caché de ventas invalidado");
  }

  void _invalidarCacheCompras() {
    _cachedCompras = null;
    _lastComprasFetch = null;
    _log("🗑️ Caché de compras invalidado");
  }

  /// Limpia toda la memoria. Recomendado al hacer log-out.
  void invalidarTodoElCache() {
    _invalidarCacheProductos();
    _invalidarCacheVentas();
    _invalidarCacheCompras();
    _log("🗑️ Todos los cachés invalidados");
  }

  // ---------------------------------------------------------------------------
  // MÉTODOS DE NEGOCIO
  // ---------------------------------------------------------------------------

  // -------- LOGIN --------
  Future<BridgeResponse> login(String dni, String password) async {
    final response = await _ejecutarTransaccion(
      _channelLogin,
      AppConstants.methodLogin,
      [dni, password],
    );

    if (response.isSuccess) {
      invalidarTodoElCache();
    }

    return response;
  }

  // -------- PRODUCTOS --------

  Future<BridgeResponse> obtenerProductos({bool forceRefresh = false}) async {
    return _invokeListCached(
      channel: _channelProductos,
      method: AppConstants.methodGetProducts,
      cache: _cachedProducts,
      lastFetch: _lastProductsFetch,
      forceRefresh: forceRefresh,
      onCacheUpdate: (lista, timestamp) {
        _cachedProducts = lista;
        _lastProductsFetch = timestamp;
      },
    );
  }

  Future<BridgeResponse> agregarProducto(JsonMap producto) async {
    final res = await _ejecutarTransaccion(
      _channelProductos,
      AppConstants.methodAddProduct,
      [producto],
    );
    if (res.isSuccess) _invalidarCacheProductos();
    return res;
  }

  Future<BridgeResponse> actualizarProducto(JsonMap producto) async {
    final res = await _ejecutarTransaccion(
      _channelProductos,
      AppConstants.methodEditProduct,
      [producto],
    );
    if (res.isSuccess) _invalidarCacheProductos();
    return res;
  }

  Future<BridgeResponse> eliminarProducto(int id) async {
    final res = await _ejecutarTransaccion(
      _channelProductos,
      AppConstants.methodDeleteProduct,
      [id],
    );
    if (res.isSuccess) _invalidarCacheProductos();
    return res;
  }

  Future<double> getGananciaTotal() async {
    try {
      final result = await _channelProductos.invokeMethod(
        AppConstants.methodSumGanancia,
      );

      if (result is Map) {
        if (result['status'] == 'ok') {
          final content = result['Content'] ?? result['data'];
          if (content is num) return content.toDouble();
        }
        return 0.0;
      }

      return result is num ? result.toDouble() : 0.0;
    } catch (e) {
      _log("⚠️ Error obteniendo ganancia: $e");
      return 0.0;
    }
  }

  // -------- VENTAS --------

  Future<BridgeResponse> registrarVenta(JsonMap venta, JsonList detalles) async {
    final response = await _ejecutarTransaccion(
      _channelVenta,
      AppConstants.methodRegVenta,
      [venta, detalles],
    );

    if (response.isSuccess) {
      _invalidarCacheProductos();
      _invalidarCacheVentas();
    }

    return response;
  }

  //  Retorna BridgeResponse en lugar de List<JsonMap>
  Future<BridgeResponse> listarVentas({bool forceRefresh = false}) async {
    return _invokeListCached(
      channel: _channelVenta,
      method: AppConstants.methodListVentas,
      cache: _cachedVentas,
      lastFetch: _lastVentasFetch,
      forceRefresh: forceRefresh,
      onCacheUpdate: (lista, timestamp) {
        _cachedVentas = lista;
        _lastVentasFetch = timestamp;
      },
    );
  }

  // -------- COMPRAS --------

  // Retorna BridgeResponse en lugar de List<JsonMap>
  Future<BridgeResponse> listarCompras({bool forceRefresh = false}) async {
    return _invokeListCached(
      channel: _channelCompra,
      method: AppConstants.methodListCompras,
      cache: _cachedCompras,
      lastFetch: _lastComprasFetch,
      forceRefresh: forceRefresh,
      onCacheUpdate: (lista, timestamp) {
        _cachedCompras = lista;
        _lastComprasFetch = timestamp;
      },
    );
  }

  Future<BridgeResponse> registrarCompra(JsonMap compra, JsonList detalles) async {
    final res = await _ejecutarTransaccion(
      _channelCompra,
      AppConstants.methodRegCompra,
      [compra, detalles],
    );

    if (res.isSuccess) {
      _invalidarCacheProductos();
      _invalidarCacheCompras();
    }

    return res;
  }

  // ---------------------------------------------------------------------------
  // MÉTODOS ÚTILES PARA DEBUGGING
  // ---------------------------------------------------------------------------

  Map<String, dynamic> getCacheStatus() {
    return {
      'productos': {
        'cached': _cachedProducts != null,
        'count': _cachedProducts?.length ?? 0,
        'age_seconds': _lastProductsFetch != null
            ? DateTime.now().difference(_lastProductsFetch!).inSeconds
            : null,
      },
      'ventas': {
        'cached': _cachedVentas != null,
        'count': _cachedVentas?.length ?? 0,
        'age_seconds': _lastVentasFetch != null
            ? DateTime.now().difference(_lastVentasFetch!).inSeconds
            : null,
      },
      'compras': {
        'cached': _cachedCompras != null,
        'count': _cachedCompras?.length ?? 0,
        'age_seconds': _lastComprasFetch != null
            ? DateTime.now().difference(_lastComprasFetch!).inSeconds
            : null,
      },
    };
  }

  void printCacheStatus() {
    final status = getCacheStatus();
    _log("📊 Estado del Caché:");
    _log("  Productos: ${status['productos']}");
    _log("  Ventas: ${status['ventas']}");
    _log("  Compras: ${status['compras']}");
  }
}