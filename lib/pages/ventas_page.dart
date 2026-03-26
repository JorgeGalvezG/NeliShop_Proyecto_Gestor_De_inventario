import 'dart:async' show unawaited;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:audioplayers/audioplayers.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../config/core.dart';
import '../config/db_helper.dart';
import '../modelos/product_model.dart';
import '../bridge_flutter.dart';
import '../widgets/optimized_image.dart';
import '../widgets/animated_list_item.dart';
import '../servicios/activity_service.dart';
import '../modelos/activity_event_model.dart';
import '../servicios/preferences_service.dart';
import '../widgets/stitch_loader.dart';

class VentasPage extends StatefulWidget {
  const VentasPage({super.key});

  @override
  State<VentasPage> createState() => _VentasPageState();
}

class _VentasPageState extends State<VentasPage> with SingleTickerProviderStateMixin {
  final BridgeFlutter _bridge = BridgeFlutter();

  // ==========================================
  // CONSTANTES
  // ==========================================
  static const String _currency = 'S/';
  static const String _soundSale = 'sounds/sale.mp3';
  static const String _defaultClienteDni = '00000000';
  static const double _igvRate = 0.18;
  static const int _tabCount = 5; // ✅ FIX #9: Constante para evitar magic number

  // ==========================================
  // VARIABLES DE ESTADO Y CONTROLADORES
  // ==========================================

  late TabController _tabController;
  late final AudioPlayer _audioPlayer;

  // Controladores de Venta Rápida
  final _nameCtrl = TextEditingController();
  final _priceCtrl = TextEditingController();
  final _qtyCtrl = TextEditingController(text: '1');
  final _quickSaleFormKey = GlobalKey<FormState>();

  // Variables del Punto de Venta (POS)
  List<Product> _products = [];
  final List<Map<String, dynamic>> _cart = [];
  bool _isLoadingProducts = true;
  bool _isProcessingSale = false;
  int _vendedorId = 1;

  // Variables del Historial de Ventas
  List<JsonMap> _allVentas = [];
  List<JsonMap> _filteredVentas = [];
  bool _isLoadingVentas = true;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: _tabCount, vsync: this); // ✅ FIX #9
    _audioPlayer = AudioPlayer();
    _initAsync();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _audioPlayer.dispose();
    _nameCtrl.dispose();
    _priceCtrl.dispose();
    _qtyCtrl.dispose();
    super.dispose();
  }

  Future<void> _initAsync() async {
    await _cargarIdVendedor();
    await _loadInitialData();
  }

  Future<void> _cargarIdVendedor() async {
    final prefs = await SharedPreferences.getInstance();
    if (mounted) {
      setState(() {
        _vendedorId = prefs.getInt('vendedorId') ?? 1;
      });
    }
  }

  // ==========================================
  // CARGA DE DATOS (PARALELISMO)
  // ==========================================

  Future<void> _loadInitialData() async {
    setState(() {
      _isLoadingProducts = true;
      _isLoadingVentas = true;
    });

    try {
      await Future.wait([
        _loadProducts(),
        _loadVentasHistorial(),
      ]);
    } catch (e) {
      debugPrint("❌ Error en carga inicial: $e");
      if (mounted) {
        setState(() {
          _isLoadingProducts = false;
          _isLoadingVentas = false;
        });
      }
    }
  }

  Future<void> _loadProducts() async {
    if (!mounted) return;
    setState(() => _isLoadingProducts = true);

    try {
      // 1. Intentar cargar desde SQLite primero
      final localData = await DBHelper.instance.getProductosLocal();
      if (localData.isNotEmpty && mounted) {
        final localProducts = Product.ListOfProducts(localData);
        setState(() {
          _products = localProducts;
          _isLoadingProducts = false;
        });
      }

      // 2. Actualizar desde el servidor
      final response = await _bridge.obtenerProductos(forceRefresh: true);

      if (response.isSuccess && mounted) {
        final rawProducts = List<JsonMap>.from(response.data ?? []);

        if (rawProducts.isNotEmpty) {
          // ✅ FIX #1: Usar unawaited correctamente en lugar de .ignore()
          unawaited(DBHelper.instance.syncProductos(rawProducts));

          final freshProducts = Product.ListOfProducts(rawProducts);
          setState(() {
            _products = freshProducts;
            _isLoadingProducts = false;
          });
        } else {
          if (mounted) setState(() => _isLoadingProducts = false);
        }
      } else {
        if (mounted) setState(() => _isLoadingProducts = false);
        debugPrint("⚠️ No se pudieron cargar productos frescos: ${response.mensaje}");
      }
    } catch (e) {
      debugPrint("❌ Error loading products: $e");
      if (mounted) setState(() => _isLoadingProducts = false);
    }
  }

  Future<void> _loadVentasHistorial() async {
    if (!mounted) return;

    // ✅ FIX #6: Loading state consistente — siempre mostrar indicador al refrescar
    setState(() => _isLoadingVentas = true);

    try {
      // 1. Cargar desde SQLite primero
      final localData = await DBHelper.instance.getVentasHistorialLocal();
      if (localData.isNotEmpty && mounted) {
        setState(() {
          _allVentas = localData;
          _filteredVentas = List.from(localData);
          _isLoadingVentas = false;
        });
        if (_searchQuery.isNotEmpty) _filtrarVentas(_searchQuery);
      }

      // 2. Actualizar desde servidor
      final response = await _bridge.listarVentas();

      if (response.isSuccess && mounted) {
        final rawVentas = List<JsonMap>.from(response.data ?? []);

        if (rawVentas.isNotEmpty) {
          await DBHelper.instance.syncVentasHistorial(rawVentas);
          final updatedLocal = await DBHelper.instance.getVentasHistorialLocal();

          if (mounted) {
            setState(() {
              _allVentas = updatedLocal;
              _filteredVentas = List.from(updatedLocal);
              _isLoadingVentas = false;
            });
            if (_searchQuery.isNotEmpty) _filtrarVentas(_searchQuery);
          }
        }
      } else {
        if (mounted) setState(() => _isLoadingVentas = false);
        debugPrint("⚠️ No se pudieron cargar ventas frescas");
      }
    } catch (e) {
      debugPrint("❌ Error en Offline-First (Ventas): $e");
      if (mounted) setState(() => _isLoadingVentas = false);
    }
  }

  void _filtrarVentas(String query) {
    setState(() {
      _searchQuery = query;
      if (query.isEmpty) {
        _filteredVentas = List.from(_allVentas);
      } else {
        final queryStr = query.trim().toLowerCase();
        _filteredVentas = _allVentas.where((venta) {
          final boleta = (venta['numeroBoleta'] ?? '').toString().toLowerCase();
          final fecha = (venta['fecha'] ?? '').toString().toLowerCase();
          return boleta.contains(queryStr) || fecha.contains(queryStr);
        }).toList();
      }
    });
  }

  // ==========================================
  // LÓGICA DEL CARRITO Y CHECKOUT
  // ==========================================

  void _addToCart(Product product, int quantity) {
    setState(() {
      final existingIndex = _cart.indexWhere((item) => item['product'].id == product.id);
      if (existingIndex >= 0) {
        int newQty = _cart[existingIndex]['qty'] + quantity;
        if (newQty > product.stock) newQty = product.stock;
        _cart[existingIndex]['qty'] = newQty;
      } else {
        _cart.add({
          'product': product,
          'qty': quantity,
          'price': product.onSale ? product.salePrice : product.price
        });
      }
    });
    _showSnackBar('${product.name} agregado al carrito', Colors.green, duration: 1);
  }

  void _removeFromCart(int index) => setState(() => _cart.removeAt(index));

  double _calculateTotal() => _cart.fold(0.0, (sum, item) => sum + (item['price'] * item['qty']));

  // ✅ FIX #8: Getter en lugar de método que solo devuelve constante
  String get _clienteDni => _defaultClienteDni;

  Future<void> _processCheckout() async {
    if (_cart.isEmpty) return;

    setState(() => _isProcessingSale = true);
    final double totalVenta = _calculateTotal();

    try {
      final List<JsonMap> detallesList = [];
      for (var item in _cart) {
        Product p = item['product'];
        final int? productId = int.tryParse(p.id);

        if (productId == null) {
          _showSnackBar('Error: ID de producto inválido (${p.name})', Colors.red);
          return;
        }

        detallesList.add({
          'productoId': productId,
          'cantidad': item['qty'],
          'precioUnitario': item['price'],
          'subtotal': item['qty'] * item['price']
        });
      }

      final JsonMap ventaMap = {
        'descripcion': 'Venta Carrito (${_cart.length} items)',
        'clienteDni': _clienteDni, // ✅ FIX #8: Usar getter
        'monto': totalVenta,
        'vendedorId': _vendedorId
      };

      final response = await _bridge.registrarVenta(ventaMap, detallesList);

      if (response.isSuccess) {
        _playSuccessSound();
        ActivityService.instance.addActivity(ActivityEvent(
          title: 'Venta Carrito',
          subtitle: '${_cart.length} items por $_currency${totalVenta.toStringAsFixed(2)}',
          icon: Icons.shopping_cart_checkout,
          color: Colors.green,
          timestamp: DateTime.now(),
        ));

        setState(() => _cart.clear());

        await Future.wait([
          _loadProducts(),
          _loadVentasHistorial()
        ]);

        if (mounted) {
          String ventaId = response.data?['id']?.toString() ?? "---";
          _showOldStyleInvoiceDialog(ventaId, detallesList, totalVenta, isManual: false);
        }
      } else {
        _showSnackBar('Error Backend: ${response.mensaje}', Colors.red);
      }
    } catch (e) {
      _showSnackBar('Error de conexión: $e', Colors.red);
    } finally {
      if (mounted) setState(() => _isProcessingSale = false);
    }
  }

  Future<void> _processQuickSale(String name, double price, int qty) async {
    setState(() => _isProcessingSale = true);

    try {
      final product = _products.firstWhere(
            (p) => p.name.toLowerCase() == name.toLowerCase(),
        orElse: () => Product(id: '0', name: name, category: '', price: price),
      );

      final int? productId = int.tryParse(product.id);
      if (productId == null) {
        _showSnackBar('Error: ID de producto inválido', Colors.red);
        return;
      }

      final JsonMap ventaMap = {
        'descripcion': 'Venta Rápida: $name',
        'clienteDni': _clienteDni, // ✅ FIX #8: Usar getter
        'monto': price * qty,
        'vendedorId': _vendedorId
      };

      final JsonMap detalleMap = {
        'productoId': productId,
        'cantidad': qty,
        'precioUnitario': price,
        'subtotal': price * qty
      };

      final response = await _bridge.registrarVenta(ventaMap, [detalleMap]);

      if (response.isSuccess) {
        _playSuccessSound();
        ActivityService.instance.addActivity(ActivityEvent(
          title: 'Venta Rápida',
          subtitle: '$name (x$qty)',
          icon: Icons.flash_on,
          color: Colors.orange,
          timestamp: DateTime.now(),
        ));

        await Future.wait([
          _loadProducts(),
          _loadVentasHistorial()
        ]);

        if (mounted) {
          _showOldStyleInvoiceDialog(
            response.data?['id']?.toString() ?? "---",
            [detalleMap],
            price * qty,
            isManual: true,
            manualName: name,
            onClose: () {
              _nameCtrl.clear();
              _priceCtrl.clear();
              _qtyCtrl.text = '1';
            },
          );
        }
      } else {
        _showSnackBar('Error: ${response.mensaje}', Colors.red);
      }
    } catch (e) {
      _showSnackBar('Error: $e', Colors.red);
    } finally {
      if (mounted) setState(() => _isProcessingSale = false);
    }
  }

  // ✅ FIX #2: Manejo correcto de errores en toda la cadena async de audio
  void _playSuccessSound() {
    if (PreferencesService.instance.soundNotifier.value) {
      Future(() async {
        try {
          await _audioPlayer.stop();
          await _audioPlayer.play(AssetSource(_soundSale));
        } catch (e) {
          debugPrint("⚠️ Audio Error: $e");
        }
      });
    }
  }

  /// Helper: SnackBar centralizado
  void _showSnackBar(String msg, Color backgroundColor, {int duration = 3}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(msg),
        backgroundColor: backgroundColor,
        duration: Duration(seconds: duration),
      ),
    );
  }

  // ==========================================
  // INTERFAZ GRÁFICA PRINCIPAL
  // ==========================================

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Punto de Venta'),
        automaticallyImplyLeading: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: "Actualizar Datos",
            onPressed: _loadInitialData,
          )
        ],
        bottom: TabBar(
          controller: _tabController,
          isScrollable: true,
          tabAlignment: TabAlignment.start,
          tabs: [
            const Tab(icon: Icon(Icons.store), text: 'Catálogo'),
            const Tab(icon: Icon(Icons.star), text: 'Promociones'),
            const Tab(icon: Icon(Icons.flash_on), text: 'Venta Rápida'),
            Tab(
              icon: Badge(
                isLabelVisible: _cart.isNotEmpty,
                label: Text('${_cart.length}'),
                child: const Icon(Icons.shopping_cart),
              ),
              text: 'Carrito',
            ),
            const Tab(icon: Icon(Icons.history), text: 'Historial'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildCatalogTab(isPromosOnly: false),
          _buildCatalogTab(isPromosOnly: true),
          _buildQuickSaleTab(),
          _buildCartTab(),
          _buildHistorialTab(),
        ],
      ),
    );
  }

  // ==========================================
  // WIDGETS DE PESTAÑAS
  // ==========================================

  Widget _buildCatalogTab({required bool isPromosOnly}) {
    if (_isLoadingProducts) return const StitchLoader();

    final productList = isPromosOnly
        ? _products.where((p) => p.onSale).toList()
        : _products;

    if (productList.isEmpty) {
      return Center(
        child: Text(
          isPromosOnly ? "No hay promociones activas" : "Catálogo vacío",
        ),
      );
    }

    return GridView.builder(
      padding: const EdgeInsets.all(8),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        childAspectRatio: 0.60,
        crossAxisSpacing: 10,
        mainAxisSpacing: 10,
      ),
      itemCount: productList.length,
      itemBuilder: (context, index) {
        final product = productList[index];
        bool outOfStock = product.stock <= 0;

        return AnimatedListItem(
          index: index,
          child: Card(
            elevation: 3,
            clipBehavior: Clip.antiAlias,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: Stack(
                    children: [
                      Positioned.fill(
                        child: OptimizedImage(imagePath: product.imagePath),
                      ),
                      if (outOfStock)
                        Container(
                          color: Colors.white54,
                          child: const Center(
                            child: Text(
                              "AGOTADO",
                              style: TextStyle(
                                color: Colors.red,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ),
                      if (product.onSale)
                        Positioned(
                          top: 5,
                          right: 5,
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 6,
                              vertical: 2,
                            ),
                            color: Colors.red,
                            child: const Text(
                              "OFERTA",
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 10,
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        product.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                      if (product.onSale) ...[
                        Text(
                          "$_currency${product.price.toStringAsFixed(2)}",
                          style: const TextStyle(
                            decoration: TextDecoration.lineThrough,
                            fontSize: 11,
                            color: Colors.grey,
                          ),
                        ),
                        Text(
                          "$_currency${product.salePrice!.toStringAsFixed(2)}",
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Colors.red,
                            fontSize: 16,
                          ),
                        ),
                      ] else
                        Text(
                          "$_currency${product.price.toStringAsFixed(2)}",
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                      Text(
                        "Stock: ${product.stock}",
                        style: TextStyle(
                          fontSize: 11,
                          color: outOfStock ? Colors.red : Colors.green,
                        ),
                      ),
                      const SizedBox(height: 5),
                      SizedBox(
                        height: 35,
                        width: double.infinity,
                        child: ElevatedButton(
                          style: ElevatedButton.styleFrom(
                            padding: EdgeInsets.zero,
                          ),
                          onPressed: outOfStock ? null : () => _showQuantityDialog(product),
                          child: const Text("Agregar"),
                        ),
                      )
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildHistorialTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: TextField(
            decoration: InputDecoration(
              hintText: 'Buscar por boleta o fecha...',
              prefixIcon: const Icon(Icons.search),
              filled: true,
              fillColor: Colors.grey[100],
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(30),
                borderSide: BorderSide.none,
              ),
              contentPadding: const EdgeInsets.symmetric(horizontal: 20),
            ),
            onChanged: _filtrarVentas,
          ),
        ),
        if (_searchQuery.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Text(
              "${_filteredVentas.length} ventas encontradas",
              style: const TextStyle(color: Colors.grey, fontSize: 12),
            ),
          ),
        Expanded(
          child: _isLoadingVentas
              ? const Center(child: CircularProgressIndicator())
              : _filteredVentas.isEmpty
              ? Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.search_off, size: 60, color: Colors.grey[300]),
                const SizedBox(height: 10),
                const Text("No se encontraron ventas"),
              ],
            ),
          )
              : RefreshIndicator(
            onRefresh: _loadVentasHistorial,
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              itemCount: _filteredVentas.length,
              itemBuilder: (context, index) =>
                  _buildVentaCard(_filteredVentas[index]),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildVentaCard(JsonMap venta) {
    final List detalles = venta['detalles'] ?? [];
    final double total = (venta['monto'] ?? 0.0).toDouble();
    final String boleta = venta['numeroBoleta'] ?? 'Boleta sin número';
    final String fecha = venta['fecha'] ?? '';
    final primaryColor = Theme.of(context).primaryColor;

    return Card(
      elevation: 2,
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: ExpansionTile(
        leading: CircleAvatar(
          // ✅ FIX #4: withOpacity deprecado → withAlpha (0.1 * 255 ≈ 25)
          backgroundColor: primaryColor.withAlpha(25),
          child: Icon(Icons.receipt_long, color: primaryColor),
        ),
        title: Text(
          boleta,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Text(
          fecha,
          style: const TextStyle(fontSize: 12, color: Colors.grey),
        ),
        trailing: Text(
          "$_currency${total.toStringAsFixed(2)}",
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 16,
            color: Colors.green,
          ),
        ),
        children: detalles.map((d) {
          return ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 24),
            leading: const Icon(
              Icons.check_circle_outline,
              size: 18,
              color: Colors.grey,
            ),
            title: Text(
              d['nombre'] ?? 'Producto',
              style: const TextStyle(fontSize: 14),
            ),
            trailing: Text(
              "${d['cantidad']}x $_currency${(d['precio'] ?? 0.0).toStringAsFixed(2)}",
              style: const TextStyle(fontSize: 13, color: Colors.black87),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildQuickSaleTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Form(
        key: _quickSaleFormKey,
        child: Column(
          children: [
            const Icon(Icons.flash_on, size: 60, color: Colors.orange),
            const SizedBox(height: 10),
            const Text(
              "Venta Manual Directa",
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 30),
            TextFormField(
              controller: _nameCtrl,
              decoration: const InputDecoration(
                labelText: 'Nombre del Producto',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.label),
              ),
              validator: (v) => v == null || v.isEmpty ? 'Requerido' : null,
            ),
            const SizedBox(height: 15),
            Row(
              children: [
                Expanded(
                  child: TextFormField(
                    controller: _priceCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'Precio ($_currency)',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.attach_money),
                    ),
                    validator: (v) {
                      if (v == null || v.isEmpty) return 'Requerido';
                      if (double.tryParse(v) == null) return 'Inválido';
                      if (double.parse(v) <= 0) return 'Mayor a 0';
                      return null;
                    },
                  ),
                ),
                const SizedBox(width: 15),
                Expanded(
                  child: TextFormField(
                    controller: _qtyCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'Cantidad',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.numbers),
                    ),
                    validator: (v) {
                      if (v == null || v.isEmpty) return 'Requerido';
                      if (int.tryParse(v) == null) return 'Entero válido';
                      if (int.parse(v) <= 0) return 'Mínimo 1';
                      return null;
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 30),
            SizedBox(
              width: double.infinity,
              height: 55,
              child: ElevatedButton.icon(
                icon: _isProcessingSale ? const SizedBox() : const Icon(Icons.check),
                label: _isProcessingSale
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text("VENDER AHORA"),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.orange,
                  foregroundColor: Colors.white,
                ),
                onPressed: _isProcessingSale
                    ? null
                    : () {
                  if (_quickSaleFormKey.currentState!.validate()) {
                    _processQuickSale(
                      _nameCtrl.text,
                      double.parse(_priceCtrl.text),
                      int.parse(_qtyCtrl.text),
                    );
                  }
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCartTab() {
    if (_cart.isEmpty) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.shopping_cart_outlined, size: 80, color: Colors.grey),
            SizedBox(height: 10),
            Text("El carrito está vacío"),
          ],
        ),
      );
    }

    return Column(
      children: [
        Expanded(
          child: ListView.separated(
            padding: const EdgeInsets.all(12),
            itemCount: _cart.length,
            separatorBuilder: (c, i) => const Divider(),
            itemBuilder: (context, index) {
              final item = _cart[index];
              final Product p = item['product'];
              return ListTile(
                leading: SizedBox(
                  width: 50,
                  child: OptimizedImage(imagePath: p.imagePath),
                ),
                title: Text(p.name),
                subtitle: Text(
                  '${item['qty']} x $_currency${item['price'].toStringAsFixed(2)}',
                ),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      '$_currency${(item['qty'] * item['price']).toStringAsFixed(2)}',
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    IconButton(
                      icon: Icon(
                        Icons.delete,
                        color: _isProcessingSale ? Colors.grey : Colors.red,
                      ),
                      onPressed: _isProcessingSale
                          ? null
                          : () => _removeFromCart(index),
                    )
                  ],
                ),
              );
            },
          ),
        ),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: const BoxDecoration(
            color: Colors.white,
            boxShadow: [BoxShadow(blurRadius: 10, color: Colors.black12)],
          ),
          child: Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    "TOTAL:",
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  Text(
                    "$_currency${_calculateTotal().toStringAsFixed(2)}",
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Colors.green,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 15),
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: _isProcessingSale ? null : _processCheckout,
                  child: _isProcessingSale
                      ? const CircularProgressIndicator(color: Colors.white)
                      : const Text("CONFIRMAR VENTA"),
                ),
              )
            ],
          ),
        )
      ],
    );
  }

  // ==========================================
  // DIÁLOGOS
  // ==========================================

  void _showQuantityDialog(Product product) {
    int qty = 1;
    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setStateDialog) => AlertDialog(
          title: Text("Agregar: ${product.name}"),
          content: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              IconButton(
                icon: const Icon(Icons.remove),
                onPressed: qty > 1 ? () => setStateDialog(() => qty--) : null,
              ),
              Text("$qty", style: const TextStyle(fontSize: 24)),
              IconButton(
                icon: const Icon(Icons.add),
                onPressed: qty < product.stock
                    ? () => setStateDialog(() => qty++)
                    : null,
              )
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text("Cancelar"),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.pop(context);
                _addToCart(product, qty);
              },
              child: const Text("Agregar"),
            )
          ],
        ),
      ),
    );
  }

  void _showOldStyleInvoiceDialog(
      String id,
      List<dynamic> items,
      double total, {
        required bool isManual,
        String? manualName,
        VoidCallback? onClose,
      }) {
    final double subtotal = total / (1 + _igvRate);
    final double igv = total - subtotal;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) {
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          titlePadding: const EdgeInsets.all(0),
          title: Container(
            padding: const EdgeInsets.only(top: 24, bottom: 16),
            width: double.infinity,
            decoration: BoxDecoration(
              color: Colors.green[50],
              borderRadius: const BorderRadius.vertical(
                top: Radius.circular(16),
              ),
            ),
            child: const Column(
              children: [
                Icon(Icons.check_circle_outline, color: Colors.green, size: 60),
                SizedBox(height: 12),
                Text(
                  '¡Venta Exitosa!',
                  style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                )
              ],
            ),
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                const Text(
                  "TOTAL PAGADO",
                  style: TextStyle(color: Colors.grey, fontSize: 12),
                ),
                Text(
                  '$_currency${total.toStringAsFixed(2)}',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 36,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
                const SizedBox(height: 16),
                const Divider(),
                const SizedBox(height: 16),

                if (isManual)
                  _buildDetailRow("Producto:", "$manualName (Venta Rápida)")
                else ...[
                  const Text(
                    "Detalles:",
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 5),
                  ...items.map((item) {
                    // ✅ FIX #5: Eliminado try-catch innecesario; orElse nunca lanza excepción
                    final productId = item['productoId'].toString();
                    final foundProduct = _products.firstWhere(
                          (p) => p.id == productId,
                      orElse: () => Product(
                        id: '0',
                        name: 'Producto',
                        category: '',
                        price: 0,
                      ),
                    );

                    return Padding(
                      padding: const EdgeInsets.only(bottom: 4.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: Text(
                              foundProduct.name,
                              style: const TextStyle(fontSize: 13),
                            ),
                          ),
                          Text(
                            "x${item['cantidad']}  $_currency${item['precioUnitario']}",
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.bold,
                            ),
                          )
                        ],
                      ),
                    );
                  })
                ],
                const SizedBox(height: 16),
                const Divider(),
                _buildDetailRow(
                  "Subtotal:",
                  "$_currency${subtotal.toStringAsFixed(2)}",
                ),
                const SizedBox(height: 8),
                _buildDetailRow(
                  "IGV (${(_igvRate * 100).toStringAsFixed(0)}%):",
                  "$_currency${igv.toStringAsFixed(2)}",
                ),
                const SizedBox(height: 8),
                _buildDetailRow(
                  "Fecha:",
                  DateFormat('dd/MM/yy hh:mma').format(DateTime.now()),
                ),
                const SizedBox(height: 8),
                _buildDetailRow("Nro Boleta:", "B-$id"),
                const SizedBox(height: 24),
                const Center(
                  child: Text(
                    '¡Gracias por tu compra!',
                    style: TextStyle(
                      fontStyle: FontStyle.italic,
                      color: Colors.grey,
                    ),
                  ),
                )
              ],
            ),
          ),
          actionsPadding: const EdgeInsets.all(16),
          actions: [
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(30),
                  ),
                ),
                child: const Text('Cerrar', style: TextStyle(fontSize: 16)),
                onPressed: () {
                  Navigator.of(context).pop();
                  if (onClose != null) onClose();
                },
              ),
            )
          ],
        );
      },
    );
  }

  Widget _buildDetailRow(String title, String value) => Row(
    mainAxisAlignment: MainAxisAlignment.spaceBetween,
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        title,
        style: const TextStyle(color: Colors.grey, fontSize: 14),
      ),
      const SizedBox(width: 10),
      Flexible(
        child: Text(
          value,
          textAlign: TextAlign.end,
          style: const TextStyle(
            fontWeight: FontWeight.w600,
            fontSize: 14,
          ),
        ),
      )
    ],
  );
}