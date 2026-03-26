import 'package:flutter/foundation.dart';

/// Define un mapa JSON estándar: String -> dynamic
/// Úsalo cuando envíes o recibas objetos individuales.
typedef JsonMap = Map<String, dynamic>;

/// Define una lista de mapas: útil para listas de productos o ventas.
typedef JsonList = List<Map<String, dynamic>>;

/// Clase central para estandarizar las respuestas del backend (MethodChannels).
class BridgeResponse {
  //  status no-nullable y todas las propiedades finales
  final String status;
  final String? mensaje;
  final dynamic data;

  static const String EXITO          = 'ok';
  static const String INTERNAL_ERROR = 'internal_error';
  static const String MESSAGE_ERROR  = 'error';

  const BridgeResponse({
    required this.status,
    this.mensaje,
    this.data,
  });

  /// Getter para evaluar rápidamente el éxito de la transacción.
  bool get isSuccess => status == EXITO;

  /// Fábrica inteligente: extrae status, mensaje y contenido enviado por Java.
  factory BridgeResponse.fromMap(Map<dynamic, dynamic> map) {
    return BridgeResponse(
      status:  map['status']?.toString() ?? INTERNAL_ERROR,
      mensaje: map['mensaje']?.toString(),
      data:    map['Content'] ?? map['data'],
    );
  }

  // ===========================================================================
  // Constructores con nombre estáticos (patrón idiomático Dart)
  // Reemplazan los métodos de instancia con PascalCase que eran inutilizables.
  // ===========================================================================

  /// Respuesta de éxito sin datos.
  static BridgeResponse exito() {
    return const BridgeResponse(status: EXITO);
  }

  /// Respuesta de éxito con datos adjuntos.
  static BridgeResponse exitoConDatos(dynamic dataPayload) {
    return BridgeResponse(status: EXITO, data: dataPayload);
  }

  /// Error interno inesperado (bug, excepción no controlada, etc.).
  static BridgeResponse errorInterno(String msg) {
    debugPrint("❌ Error Interno (Bridge): $msg"); // debugPrint
    return BridgeResponse(status: INTERNAL_ERROR, mensaje: msg);
  }

  /// Error de negocio con mensaje para mostrar al usuario.
  static BridgeResponse errorMensaje(String msg) {
    return BridgeResponse(status: MESSAGE_ERROR, mensaje: msg);
  }

  List<JsonMap> get dataList {
    if (data is List) {
      return List<JsonMap>.from(data as List);
    }
    return [];
  }

  // ===========================================================================
  // MÉTODOS DE COMPATIBILIDAD (Legacy) — DEPRECADOS
  // Se mantienen temporalmente para no romper archivos existentes.
  // Migrar a los métodos estáticos de arriba y eliminar en la próxima versión.
  // ===========================================================================

  @Deprecated('Usar BridgeResponse.exito() en su lugar')
  static BridgeResponse Exito() => exito();

  @Deprecated('Usar BridgeResponse.exitoConDatos(data) en su lugar')
  static BridgeResponse ExitoWithData(dynamic dataPayload) => exitoConDatos(dataPayload);

  @Deprecated('Usar BridgeResponse.errorInterno(msg) en su lugar')
  static BridgeResponse Internal_Error(String msg) => errorInterno(msg);

  @Deprecated('Usar BridgeResponse.errorMensaje(msg) en su lugar')
  static BridgeResponse Message_Error(String msg) => errorMensaje(msg);

  @override
  String toString() =>
      'BridgeResponse(status: $status, mensaje: $mensaje, data: $data)';
}