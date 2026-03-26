import 'package:flutter/material.dart';

// Imports de tu proyecto
import '../config/core.dart';
import '../config/db_helper.dart';
import '../modelos/product_model.dart';
import '../bridge_flutter.dart';
import '../widgets/optimized_image.dart';
import '../widgets/animated_list_item.dart';
import '../servicios/activity_service.dart';
import '../modelos/activity_event_model.dart';
import '../widgets/stitch_loader.dart';

class PromocionesPage extends StatefulWidget {
  const PromocionesPage({super.key});

  @override
  State<PromocionesPage> createState() => _PromocionesPageState();
}

class _PromocionesPageState extends State<PromocionesPage> with SingleTickerProviderStateMixin {
  final BridgeFlutter _bridge = BridgeFlutter();
  static const String _currency = 'S/';

  // ==========================================
  // 1. VARIABLES DE ESTADO Y CONTROLADORES
  // ==========================================

  late TabController _tabController;
  final TextEditingController _searchCtrl = TextEditingController();

  List<Product> _allProducts = [];
  List<Product> _filteredProducts = [];
  List<Product> _activePromos = [];

  bool _isLoading = true;
  bool _isRefreshing = false;

  final Set<String> _processingIds = {};

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _searchCtrl.addListener(_filtrarProductos);
    _loadProductsOfflineFirst();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _searchCtrl.dispose();
    super.dispose();
  }

  // ==========================================
  // 2. CARGA DE DATOS (OFFLINE-FIRST)
  // ==========================================

  Future<void> _loadProductsOfflineFirst() async {
    if (!mounted) return;
    if (_allProducts.isEmpty) {
      setState(() => _isLoading = true);
    } else {
      setState(() => _isRefreshing = true);
    }

    try {
      final localData = await DBHelper.instance.getProductosLocal();
      if (localData.isNotEmpty && mounted) {
        _processData(localData);
      }
      final response = await _bridge.obtenerProductos();
      final rawProducts = response.dataList;
      if (rawProducts.isNotEmpty && mounted) {
        await DBHelper.instance.syncProductos(rawProducts);
        final updatedLocal = await DBHelper.instance.getProductosLocal();
        if (mounted) _processData(updatedLocal);
      }
    } catch (e) {
      debugPrint("Error en Offline-First (Promociones): $e");
      // Reset de ambos flags en el catch
      if (mounted) setState(() {
        _isLoading = false;
        _isRefreshing = false;
      });
    }
  }
//  Separar responsabilidades:
// _processData solo procesa datos locales (síncrono, sin bridge)
// La llamada al bridge va en el método de carga (_loadProducts)

  void _processData(List<JsonMap> rawData) {
    if (!mounted) return;

    final products = Product.ListOfProducts(rawData);
    final query = _searchCtrl.text.trim().toLowerCase();

    final filtered = query.isEmpty
        ? List<Product>.from(products)
        : products.where((p) {
      final cat = (p.category ?? '').toLowerCase();
      return p.name.toLowerCase().contains(query) || cat.contains(query);
    }).toList();

    final promos = products.where((p) => p.onSale).toList();

    setState(() {
      _allProducts = products;
      _filteredProducts = filtered;
      _activePromos = promos;
      _isLoading = false;
      _isRefreshing = false;
    });
  }

//  La llamada al bridge va aquí, separada y async
  Future<void> _loadProducts() async {
    if (!mounted) return;

    try {
      final response = await _bridge.obtenerProductos();   // BridgeResponse
      if (response.isSuccess) {
        _processData(response.dataList);                   // dataList tipado seguro
      } else {
        debugPrint(" PromPage._loadProducts: ${response.mensaje}"); // ✅ no más Internal_Error
        if (mounted) setState(() => _isLoading = false);
      }
    } catch (e) {
      debugPrint(" Error en _loadProducts: $e");
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _filtrarProductos() {
    setState(() {
      final queryStr = _searchCtrl.text.trim().toLowerCase();

      if (queryStr.isEmpty) {
        _filteredProducts = List.from(_allProducts);
      } else {
        _filteredProducts = _allProducts.where((p) {
          final cat = (p.category ?? '').toLowerCase();
          return p.name.toLowerCase().contains(queryStr) || cat.contains(queryStr);
        }).toList();
      }

      _activePromos = _allProducts.where((p) => p.onSale).toList();
    });
  }

  // ==========================================
  // 3. LÓGICA DE NEGOCIO (ACTUALIZAR PRECIOS)
  // ==========================================

  Future<void> _updatePromoPrice(Product product, double? newSalePrice) async {
    setState(() => _processingIds.add(product.id));

    try {
      final JsonMap productMap = {
        'id': int.tryParse(product.id) ?? 0,
        'nombre': product.name,
        'precioCompra': 0.0,
        'precioVenta': product.price,
        'cantidad': product.stock,
        'categoriaNombre': product.category,
        'imagePath': product.imagePath,
        'precioOferta': newSalePrice
      };

      final response = await _bridge.actualizarProducto(productMap);

      if (response.isSuccess && mounted) {
        final String accion = newSalePrice != null ? 'Promoción Aplicada' : 'Promoción Retirada';
        final String detalle = newSalePrice != null
            ? '${product.name} bajó a $_currency$newSalePrice'
            : '${product.name} regresó a precio normal';

        ActivityService.instance.addActivity(ActivityEvent(
          title: accion,
          subtitle: detalle,
          icon: newSalePrice != null ? Icons.local_offer : Icons.money_off,
          color: newSalePrice != null ? Colors.red : Colors.grey,
          timestamp: DateTime.now(),
        ));

        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(detalle),
          backgroundColor: newSalePrice != null ? Colors.green : Colors.blueGrey,
        ));

        await _loadProductsOfflineFirst();
      } else {
        _showErrorSnack('Error Backend: ${response.mensaje}');
      }
    } catch (e) {
      _showErrorSnack('Error de conexión: $e');
    } finally {
      if (mounted) setState(() => _processingIds.remove(product.id));
    }
  }

  void _showErrorSnack(String msg) => ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text(msg), backgroundColor: Colors.red),
  );

  // ==========================================
  // 4. DIÁLOGOS DE GESTIÓN
  // ==========================================

  void _showApplyPromoDialog(Product product) {
    final priceCtrl = TextEditingController();
    final formKey = GlobalKey<FormState>();

    if (product.onSale) {
      priceCtrl.text = product.salePrice!.toStringAsFixed(2);
    }

    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text("Promoción: ${product.name}", style: const TextStyle(fontSize: 18)),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                "Precio Normal: $_currency${product.price.toStringAsFixed(2)}",
                style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.grey),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: priceCtrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                autofocus: true,
                decoration: const InputDecoration(
                  labelText: 'Nuevo Precio de Oferta',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.local_offer, color: Colors.red),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Requerido';
                  final newPrice = double.tryParse(v);
                  if (newPrice == null) return 'Monto inválido';
                  if (newPrice <= 0) return 'Debe ser mayor a 0';
                  if (newPrice >= product.price) return 'Debe ser MENOR al precio normal';
                  return null;
                },
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text("Cancelar"),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
            onPressed: () {
              if (!mounted) return;
              if (formKey.currentState!.validate()) {
                Navigator.pop(dialogContext);
                _updatePromoPrice(product, double.parse(priceCtrl.text));
              }
            },
            child: const Text("Aplicar Descuento"),
          ),
        ],
      ),
    ).then((_) {
      priceCtrl.dispose();
    });
  }

  void _showRemovePromoDialog(Product product) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text("Quitar Promoción"),
        content: Text(
          "¿Deseas devolver '${product.name}' a su precio normal de $_currency${product.price.toStringAsFixed(2)}?",
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text("Cancelar"),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.blueGrey, foregroundColor: Colors.white),
            onPressed: () {
              if (!mounted) return;
              Navigator.pop(dialogContext);
              _updatePromoPrice(product, null);
            },
            child: const Text("Quitar Descuento"),
          ),
        ],
      ),
    );
  }

  // ==========================================
  // 5. INTERFAZ GRÁFICA PRINCIPAL
  // ==========================================

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Gestión de Promociones'),
        automaticallyImplyLeading: false,
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadProductsOfflineFirst),
        ],
        bottom: TabBar(
          controller: _tabController,
          tabs: [
            Tab(
              icon: Badge(
                isLabelVisible: _activePromos.isNotEmpty,
                label: Text('${_activePromos.length}'),
                child: const Icon(Icons.star),
              ),
              text: 'Ofertas Activas',
            ),
            const Tab(icon: Icon(Icons.list_alt), text: 'Catálogo General'),
          ],
        ),
      ),
      //  LinearProgressIndicator para recargas con datos existentes
      body: _isLoading
          ? const StitchLoader()
          : Column(
        children: [
          if (_isRefreshing) const LinearProgressIndicator(minHeight: 3),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildActivePromosTab(),
                _buildCatalogTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ==========================================
  // 6. WIDGETS DE PESTAÑAS
  // ==========================================

  Widget _buildActivePromosTab() {
    if (_activePromos.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.local_offer_outlined, size: 80, color: Colors.grey[300]),
            const SizedBox(height: 16),
            const Text("No hay promociones activas", style: TextStyle(fontSize: 18, color: Colors.grey)),
            const SizedBox(height: 8),
            TextButton(
              onPressed: () => _tabController.animateTo(1),
              child: const Text("Ir al Catálogo para crear una"),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadProductsOfflineFirst,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: _activePromos.length,
        itemBuilder: (context, index) {
          final p = _activePromos[index];
          final discountPercent = ((p.price - p.salePrice!) / p.price) * 100;
          final isItemProcessing = _processingIds.contains(p.id);

          return AnimatedListItem(
            index: index,
            child: Card(
              elevation: 2,
              margin: const EdgeInsets.only(bottom: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
                side: const BorderSide(color: Colors.redAccent, width: 1),
              ),
              child: ListTile(
                contentPadding: const EdgeInsets.all(12),
                leading: Stack(
                  clipBehavior: Clip.none,
                  children: [
                    SizedBox(width: 50, height: 50, child: OptimizedImage(imagePath: p.imagePath)),
                    Positioned(
                      top: -5,
                      left: -5,
                      child: Container(
                        padding: const EdgeInsets.all(4),
                        decoration: const BoxDecoration(color: Colors.red, shape: BoxShape.circle),
                        child: Text(
                          "-${discountPercent.toInt()}%",
                          style: const TextStyle(color: Colors.white, fontSize: 9, fontWeight: FontWeight.bold),
                        ),
                      ),
                    ),
                  ],
                ),
                title: Text(p.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                subtitle: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 4),
                    Text(
                      "Normal: $_currency${p.price.toStringAsFixed(2)}",
                      style: const TextStyle(decoration: TextDecoration.lineThrough, fontSize: 12, color: Colors.grey),
                    ),
                    Text(
                      "Oferta: $_currency${p.salePrice!.toStringAsFixed(2)}",
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.red),
                    ),
                  ],
                ),
                trailing: IconButton(
                  icon: isItemProcessing
                      ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.cancel, color: Colors.grey),
                  tooltip: "Quitar Promoción",
                  onPressed: isItemProcessing ? null : () => _showRemovePromoDialog(p),
                ),
                onTap: isItemProcessing ? null : () => _showApplyPromoDialog(p),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildCatalogTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: TextField(
            controller: _searchCtrl,
            decoration: InputDecoration(
              hintText: 'Buscar producto para aplicar oferta...',
              prefixIcon: const Icon(Icons.search, color: Colors.grey),
              filled: true,
              fillColor: Colors.grey[100],
              contentPadding: EdgeInsets.zero,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(30),
                borderSide: BorderSide.none,
              ),
            ),
          ),
        ),
        Expanded(
          child: _filteredProducts.isEmpty
              ? Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.search_off, size: 60, color: Colors.grey[300]),
                const Text("No se encontraron productos"),
              ],
            ),
          )
              : ListView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            itemCount: _filteredProducts.length,
            itemBuilder: (context, index) {
              final p = _filteredProducts[index];
              final isItemProcessing = _processingIds.contains(p.id);

              return Card(
                margin: const EdgeInsets.only(bottom: 8),
                child: ListTile(
                  leading: SizedBox(width: 40, height: 40, child: OptimizedImage(imagePath: p.imagePath)),
                  title: Text(p.name, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                  subtitle: Text(
                    "Precio: $_currency${p.price.toStringAsFixed(2)}",
                    style: const TextStyle(fontSize: 12),
                  ),
                  trailing: p.onSale
                      ? ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red[50],
                      foregroundColor: Colors.red,
                      elevation: 0,
                    ),
                    icon: isItemProcessing
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.red))
                        : const Icon(Icons.edit, size: 16),
                    label: Text("$_currency${p.salePrice!.toStringAsFixed(2)}"),
                    onPressed: isItemProcessing ? null : () => _showApplyPromoDialog(p),
                  )
                      : OutlinedButton.icon(
                    icon: isItemProcessing
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.local_offer, size: 16),
                    label: const Text("Aplicar"),
                    onPressed: isItemProcessing ? null : () => _showApplyPromoDialog(p),
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}