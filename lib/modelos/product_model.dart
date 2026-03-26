import 'dart:collection';

import '../config/core.dart';

class Product {
  final String id;
  String name;
  String category;
  double price;
  double? purchasePrice;

  String? imagePath; // Para la imagen
  double? salePrice; // Para promociones
  int stock;

  Product({
    required this.id,
    required this.name,
    required this.category,
    required this.price,
    this.purchasePrice,
    this.imagePath,
    this.salePrice,
    this.stock = 0, // Default 0
  });

  // Helper para saber si está en oferta
  bool get onSale => salePrice != null;

  //Devuelve una lista de los productos a partir de una lista de mapas.
  static List<Product> ListOfProducts(JsonList Lista){

    //Crea una lista
    List<Product> Productos =
      //La lista vendrá de la lista de mapas (.map aplica una funcion a cada elemento de la lista)
      Lista.map((mapa) => new Product(
        id: mapa['id'].toString(),
        name: mapa['nombre']?.toString() ?? 'Sin Nombre',
        category: mapa['categoriaNombre'].toString() ?? 'General',
        //manejo seguro de numeros (aveces manda int aveces manda double)
        price: (mapa['precioVenta'] ?? 0).toDouble(),
        purchasePrice: (mapa['precioCompra']?? 0).toDouble(),
        //price: Lista[i]['precio']?.toDouble() ?? 0.0,
        imagePath: mapa['imagen']?.toString(),
        stock: mapa['stock']?.toInt() ?? 0,
        //Logica de oferta encapsulada para mejorar la legibilidad
        salePrice: mapa['precioOferta'] != null
            ? (mapa['precioOferta'] as num).toDouble()
            : null,
      )).toList();

    return Productos ?? [];
  }


  //constructor inteligente
  factory Product.fromJson(JsonMap json){
  return Product(
    id: json['id'].toString(),
    name: json['nombre']?.toString() ?? 'Sin Nombre',
    category: json['categoriaNombre'].toString() ?? 'General',
    //manejo seguro de numeros (aveces manda int aveces manda double)
    price: (json['precioVenta'] ?? 0).toDouble(),
    purchasePrice: (json['precioCompra'] ?? 0).toDouble(),
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

