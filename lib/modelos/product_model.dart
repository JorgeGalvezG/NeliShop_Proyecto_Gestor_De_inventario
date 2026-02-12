import '../config/core.dart';

class Product {
  final String id;
  String name;
  String category;
  double price;
  String? imagePath; // Para la imagen
  double? salePrice; // Para promociones
  int stock;

  Product({
    required this.id,
    required this.name,
    required this.category,
    required this.price,
    this.imagePath,
    this.salePrice,
    this.stock = 0, // Default 0
  });

  // Helper para saber si está en oferta
  bool get onSale => salePrice != null;

  //constructor inteligente
factory Product.fromJson(JsonMap json){
  return Product(
    id: json['id'].toString(),
    name: json['nombre']?.toString() ?? 'Sin Nombre',
    category: json['categoriaNombre'] ?? 'General',
    //manejo seguro de numeros (aveces manda int aveces manda double)
    price: (json['precioVenta'] ?? 0).toDouble(),
    //price: json['precio']?.toDouble() ?? 0.0,
    imagePath: json['imagen']?.toString(),
    stock: json['stock']?.toInt() ?? 0,
    //Logica de oferta encapsulada para mejorar la legibilidad
    salePrice: json['precioOferta'] != null
        ? (json['precioOferta'] as num).toDouble()
        : null,
    );
  }
}

