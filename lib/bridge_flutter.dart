
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
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
      print(" Error de Plataforma en $method: ${e.message}");
      return null;
    } catch (e) {
      print(" Error Desconocido en $method: $e");
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
      if (result != null) {
        return BridgeResponse.fromMap(result);
      }
      return BridgeResponse().Internal_Error('BF.login: Respuesta inválida del servidor');

    } on PlatformException catch (e) {
      return BridgeResponse().Internal_Error('BF.login: ${ e.message ?? 'Error de conexión' }');
    } catch (e) {
      return BridgeResponse(status: 'error', mensaje: e.toString());
    }
  }

  // -------- PRODUCTOS --------

  // Obtener lista (Usa el genérico _invoke para ser más rápido)
  Future<BridgeResponse> obtenerProductos() async {
    //Recibe la lista de Mapas<Object?, Object?> de GetProducts
    // Hacer que el mapa tenga otros tipo de dato puede llevar a errores.
    // Es posible llamar a los elementos del mapa usando Mapa['llave']
    final request = await _invoke<dynamic>(
        _channelProductos,
        AppConstants.methodGetProducts,
        []
    );

    BridgeResponse response;
    
    if(request == null){
      response = BridgeResponse()
          .Internal_Error("BF.obtenerProductos: Elementos no recibidos.");
      return response;

    }
    response = BridgeResponse.fromMap(request);
    
    return response;
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

  Future<BridgeResponse> getGananciaTotal() async {
    final result = await _invoke<dynamic>(
        _channelProductos,
        AppConstants.methodSumGanancia,
        []
    );

    return result == null
        ? BridgeResponse().Internal_Error("BF.getGananciaTotal: Respuesta vacía.")
        : BridgeResponse.fromMap(result);
  }

  // -------- VENTAS --------

  Future<BridgeResponse> registrarVenta(JsonMap venta, JsonList detalles) async {
    try {
      // Llamamos a Java
      JsonMap? result = await _channelVenta.invokeMethod(
          AppConstants.methodRegVenta,
          [venta, detalles]
      );

      // Java devuelve un entero (ID) si todo sale bien
      if (result == null) {
        return BridgeResponse().Internal_Error("BF.registrarVenta: Resultado nulo.");
      }
      return BridgeResponse.fromMap(result);


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
  Future<List<JsonMap>> listarVentas() async {
    JsonList? result = await _invoke<JsonList>(
        _channelVenta,
        AppConstants.methodListVentas,
        []);

      if (result == null) {
        return BridgeResponse().Internal_Error("BF.listarVentas: Resultado nulo, imposible seguir.");
      }
      return BridgeResponse.fromMap(result);
      // Si Java devuelve null o algo raro

    } catch (e) {
      return BridgeResponse().Internal_Error("BF.listarVentas: $e");
    }
  }

  // -------- COMPRAS --------

  Future<BridgeResponse> listarCompras() async {
    try{
      //Se obtienen la Lista de compras (En forma de mapas..
      final result = await _invoke<dynamic>(_channelCompra, AppConstants.methodListCompras);

      //Se verifica si entregó algo.
      if(result == null){
        return BridgeResponse().Internal_Error("BF.listarCompras: Resultado nulo, imposible seguir.");
      }

      //En caso el flujo haya salido bien.
      return BridgeResponse.fromMap(result);
    }catch(e){
      return BridgeResponse().Internal_Error("BF.listarVentas: $e");
    }
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
      
      if(result == null){
        return BridgeResponse().Internal_Error("BF._ejecutarTransaccion: Transacción de tipo nulo.");
      }
      //Si hubo una respuesta, la regresamos.
      return BridgeResponse.fromMap(result);
      
    } on PlatformException catch (e) {
      String message = e.message ?? "Error controlado en Java.";
      
      return BridgeResponse().Internal_Error("BF._ejecutarTransaccion: $message");
    } catch (e) {
      //Errores inesperados.
      return BridgeResponse().Internal_Error("BF._ejecutarTransaccion: $e");
    }
  }
}