/// Define un mapa JSON estándar: String -> dynamic
/// Úsalo cuando envíes o recibas objetos individuales.
typedef JsonMap = Map<String, dynamic>;

/// Define una lista de mapas: Útil para listas de productos o ventas.
typedef JsonList = List<dynamic>;

/// Esta clase estandariza CUALQUIER respuesta que venga de Java.
/// Ya no tendrás que adivinar si viene un Map o un null.
class BridgeResponse {
  final String status;
  final String? mensaje;
  final dynamic data;


  BridgeResponse({
    required this.status,
    this.mensaje,
    this.data
  });

  // Un "getter" para saber rápido si todo salió bien
  bool get isSuccess => status == 'ok';

  // Fábrica inteligente: Convierte lo que llegue de Java en una respuesta segura
  factory BridgeResponse.fromMap(Map<dynamic, dynamic> map) {
    return BridgeResponse(
      status: map['status']?.toString() ?? 'error',
      mensaje: map['mensaje']?.toString(),
      data: map,
    );
  }
}