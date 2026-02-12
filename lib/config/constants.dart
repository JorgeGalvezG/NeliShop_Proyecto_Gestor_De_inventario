class AppConstants {
  // --- Canales (Channels) ---
  // Estos deben coincidir EXACTAMENTE con los que definiste en LauncherActivity.java
  static const String channelLogin = 'samples.flutter.dev/Login';
  static const String channelProductos = 'samples.flutter.dev/Productos';
  static const String channelVenta = 'samples.flutter.dev/Venta';
  static const String channelCompra = 'samples.flutter.dev/Compra';

  // --- Métodos (Methods) ---
  // Login
  static const String methodLogin = 'login';

  // Productos
  static const String methodGetProducts = 'getProduct';
  static const String methodAddProduct = 'addProduct';
  static const String methodEditProduct = 'editProduct';
  static const String methodDeleteProduct = 'deleteProduct';
  static const String methodSumGanancia = 'SumGanancia';

  // Ventas
  static const String methodListVentas = 'listVentas';
  static const String methodRegVenta = 'regVenta';

  // Compras
  static const String methodListCompras = 'listCompras';
  static const String methodRegCompra = 'RegCompra';
}