/// Define un mapa JSON estándar: String -> dynamic
/// Úsalo cuando envíes o recibas objetos individuales.
typedef JsonMap = Map<String, dynamic>;

/// Define una lista de mapas: Útil para listas de productos o ventas.
typedef JsonList = List<dynamic>;
//typedef ListOfMaps = List<Map<String, dynamic>>;

/// Esta clase existe por si el mapa de MethodChannels se recibe como nulo.
/// Además, apoya con el log.
class BridgeResponse {
  String? status;
  String? mensaje;
  dynamic data;

  static final String EXITO = 'ok';
  static final String INTERNAL_ERROR = 'internal_error';
  static final String MESSAGE_ERROR = 'error';

  BridgeResponse({
    this.status,
    this.mensaje,
    this.data
  });

  //Si la respuesta es la esperada, usa esta función.
  BridgeResponse Exito(){
    return new BridgeResponse(status: EXITO);
  }

  //Si la respuesta es la esperada y hay elementos que retornar, usa esta función.
  BridgeResponse ExitoWithData(dynamic Data){
    return new BridgeResponse(status: EXITO, data: Data);
  }

  //Si la respuesta contuvo errores que quieras imprimir en consola o
  // que quieras identificar después, usa esta función.
  BridgeResponse Internal_Error(String mensaje){
    print(mensaje);
    return new BridgeResponse(status: INTERNAL_ERROR, mensaje: mensaje);
  }

  //Si el error se mostrará en la pantalla del usuario, usa este código.
  BridgeResponse Message_Error(String mensaje){
    return new BridgeResponse(status: MESSAGE_ERROR, mensaje: mensaje);
  }

  // Un "getter" para saber rápido si todo salió bien
  bool get isSuccess => status == EXITO;

  // Fábrica inteligente: Convierte lo que llegue de Java en una respuesta segura
  factory BridgeResponse.fromMap(Map<dynamic, dynamic> map) {
    return BridgeResponse(
      status: map['status']?.toString() ?? 'internal_error',
      mensaje: map['mensaje']?.toString(),
      data: map['Content'],
    );
  }
}