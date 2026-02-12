import 'package:flutter/foundation.dart';
import '../modelos/product_model.dart';

// Este servicio centraliza toda la lógica de productos
// Reemplaza la lista estática que estaba en 'productos_page.dart'

class ProductService {
  // Singleton
  ProductService._privateConstructor();
  static final ProductService instance = ProductService._privateConstructor();

  // Notificador para que la UI reaccione a los cambios
  final ValueNotifier<List<Product>> productsNotifier =
  ValueNotifier<List<Product>>(_seedProducts);

  // Método para obtener la lista actual
  List<Product> getProducts() {
    return productsNotifier.value;
  }

  // Método para añadir un producto (desde 'compras_page.dart')
  void addProduct(Product product) {
    final currentProducts = productsNotifier.value;
    productsNotifier.value = [product, ...currentProducts];
  }

  // --- Lógica de Promociones ---

  // Método para poner un producto en oferta (desde 'promociones_page.dart')
  void setPromotion(String productId, double salePrice) {
    final products = getProducts();
    try {
      final product = products.firstWhere((p) => p.id == productId);
      product.salePrice = salePrice;
      productsNotifier.value = List.from(products); // Notifica a la UI
    } catch (e) {
      debugPrint("Error: Producto no encontrado - $e");
    }
  }

  // Método para quitar una oferta
  void removePromotion(String productId) {
    final products = getProducts();
    try {
      final product = products.firstWhere((p) => p.id == productId);
      product.salePrice = null; // Quita la oferta
      productsNotifier.value = List.from(products); // Notifica a la UI
    } catch (e) {
      debugPrint("Error: Producto no encontrado - $e");
    }
  }

  // Método para obtener solo los productos en oferta
  List<Product> getPromotions() {
    return getProducts().where((p) => p.onSale).toList();
  }
}

// Datos de ejemplo
final List<Product> _seedProducts = [
  Product(
      id: "1",
      name: "Blusa de Seda",
      category: "Blusas",
      price: 55.00,
      salePrice: 45.00), // En oferta
  Product(
      id: "2",
      name: "Falda Plisada",
      category: "Faldas",
      price: 45.50),
  Product(
      id: "3",
      name: "Vestido de Noche",
      category: "Vestidos",
      price: 120.00),
  Product(
      id: "4",
      name: "Jeans Skinny",
      category: "Pantalones",
      price: 60.00,
      salePrice: 50.00), // En oferta
  Product(
      id: "5",
      name: "Chaqueta de Cuero",
      category: "Abrigos",
      price: 95.00),
  Product(
      id: "6",
      name: "Zapatillas Urbanas",
      category: "Calzado",
      price: 75.00),
];