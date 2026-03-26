import 'dart:async' show unawaited;
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as path;

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

class ComprasPage extends StatefulWidget {
  const ComprasPage({super.key});

  @override
  State<ComprasPage> createState() => _ComprasPageState();
}

class _ComprasPageState extends State<ComprasPage> with SingleTickerProviderStateMixin {
  final BridgeFlutter _bridge = BridgeFlutter();
  static const String _currency = 'S/';

  // ==========================================
  // 1. VARIABLES DE ESTADO Y CONTROLADORES
  // ==========================================

  late TabController _tabController;
  late final VoidCallback _tabListener;

  // Variables de Reabastecimiento (Carrito)
  List<Product> _products = [];
  final List<Map<String, dynamic>> _cart = [];
  bool _isLoadingProducts = true;
  bool _isProcessingPurchase = false;

  // Variables del Historial de Compras
  List<JsonMap> _allCompras = [];
  List<JsonMap> _filteredCompras = [];
  bool _isLoadingCompras = true;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);

    _tabListener = () {
      if (!_tabController.indexIsChanging) {
        setState(() {});
      }
    };
    _tabController.addListener(_tabListener);

    _loadInitialData();
  }

  @override
  void dispose() {
    _tabController.removeListener(_tabListener);
    _tabController.dispose();
    super.dispose();
  }

  // ====================
  // 2. CARGA DE DATOS
  // ====================

  Future<void> _loadInitialData() async {
    setState(() {
      _isLoadingProducts = true;
      _isLoadingCompras = true;
    });

    try {
      await Future.wait([
        _loadProducts(),
        _loadComprasHistorial(),
      ]);
    } catch (e) {
      debugPrint("Error en carga inicial de Compras: $e");
      if (mounted) {
        setState(() {
          _isLoadingProducts = false;
          _isLoadingCompras = false;
        });
      }
    }
  }

  Future<void> _loadProducts() async {
    if (!mounted) return;
    setState(() => _isLoadingProducts = true);

    try {
      // Usamos el método optimizado con caché
      final response = await _bridge.obtenerProductos();
      if (mounted) {
        setState(() {
          _products = response.dataList
              .map((json) => Product.fromJson(json))
              .toList();
          _isLoadingProducts = false;
        });
      }
    } catch (e) {
      debugPrint("Error loading products: $e");
      if (mounted) setState(() => _isLoadingProducts = false);
    }
  }

  Future<void> _loadComprasHistorial() async {
    if (!mounted) return;
    if (_allCompras.isEmpty) setState(() => _isLoadingCompras = true);

    try {
      final localData = await DBHelper.instance.getComprasHistorialLocal();
      if (localData.isNotEmpty && mounted) {
        setState(() {
          _allCompras = localData;
          _filteredCompras = List.from(localData);
          _isLoadingCompras = false;
        });
        if (_searchQuery.isNotEmpty) _filtrarCompras(_searchQuery);
      }

      final response = await _bridge.listarCompras();
      final rawCompras = response.dataList;

      if (rawCompras.isNotEmpty && mounted) {
        await DBHelper.instance.syncComprasHistorial(rawCompras);
        final updatedLocal = await DBHelper.instance.getComprasHistorialLocal();

        if (mounted) {
          setState(() {
            _allCompras = updatedLocal;
            _filteredCompras = List.from(updatedLocal);
            _isLoadingCompras = false;
          });
          if (_searchQuery.isNotEmpty) _filtrarCompras(_searchQuery);
        }
      } else if (mounted) {
        // En caso de que no haya compras
        setState(() => _isLoadingCompras = false);
      }
    } catch (e) {
      debugPrint("Error en Offline-First (Compras): $e");
      if (mounted) setState(() => _isLoadingCompras = false);
    }
  }

  void _filtrarCompras(String query) {
    setState(() {
      _searchQuery = query;
      if (query.isEmpty) {
        _filteredCompras = List.from(_allCompras);
      } else {
        final queryStr = query.trim().toLowerCase();
        _filteredCompras = _allCompras.where((compra) {
          final desc = (compra['descripcion'] ?? '').toString().toLowerCase();
          final fecha = (compra['fecha'] ?? '').toString().toLowerCase();
          return desc.contains(queryStr) || fecha.contains(queryStr);
        }).toList();
      }
    });
  }

  // ==========================================
  // 3. LÓGICA DE COMPRA (REABASTECIMIENTO)
  // ==========================================

  void _addToCart(Product product, int quantity, double costPrice) {
    setState(() {
      final existingIndex = _cart.indexWhere((item) => item['product'].id == product.id);
      if (existingIndex >= 0) {
        _cart[existingIndex]['qty'] += quantity;
        _cart[existingIndex]['costPrice'] = costPrice;
      } else {
        _cart.add({
          'product': product,
          'qty': quantity,
          'costPrice': costPrice
        });
      }
    });
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${product.name} agregado a la compra'), duration: const Duration(milliseconds: 600)));
  }

  void _removeFromCart(int index) => setState(() => _cart.removeAt(index));

  double _calculateTotal() => _cart.fold(0.0, (sum, item) => sum + (item['costPrice'] * item['qty']));

  Future<void> _processPurchase() async {
    if (_cart.isEmpty) return;
    setState(() => _isProcessingPurchase = true);

    final double totalCompra = _calculateTotal();

    try {
      final JsonMap compraMap = {
        'descripcion': 'Ingreso de mercadería (${_cart.length} items)',
        'monto': totalCompra,
      };

      final List<Map<String, dynamic>> detallesList = _cart.map((item) {
        Product p = item['product'];
        return {
          'productoId': int.tryParse(p.id) ?? 0,
          'cantidad': item['qty'],
          'precioUnitario': item['costPrice'],
          'subtotal': item['qty'] * item['costPrice']
        };
      }).toList();

      final response = await _bridge.registrarCompra(compraMap, detallesList);

      if (!mounted) return; // 🛡️ Protección de UI

      if (response.isSuccess) {
        ActivityService.instance.addActivity(ActivityEvent(
          title: 'Compra Registrada',
          subtitle: 'Ingreso de ${_cart.length} items por $_currency${totalCompra.toStringAsFixed(2)}',
          icon: Icons.inventory, color: Colors.blue, timestamp: DateTime.now(),
        ));

        setState(() => _cart.clear());

        // Forzamos actualización de ambos cachés
        await Future.wait([
          _bridge.obtenerProductos(forceRefresh: true).then((_) => _loadProducts()),
          _bridge.listarCompras(forceRefresh: true).then((_) => _loadComprasHistorial())
        ]);

        if (mounted) _showSuccessDialog(totalCompra);
      } else {
        _showErrorSnack('Error Backend: ${response.mensaje}');
      }
    } catch (e) {
      if (mounted) _showErrorSnack('Error de conexión: $e');
    } finally {
      if (mounted) setState(() => _isProcessingPurchase = false);
    }
  }

  void _showErrorSnack(String msg) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg), backgroundColor: Colors.red));

  // ==========================================
  // 4. LÓGICA DE CREACIÓN DE NUEVO PRODUCTO
  // ==========================================

  Future<String?> _saveImagePermanently(File imageFile) async {
    try {
      final directory = await getApplicationDocumentsDirectory();
      final fileName = path.basename(imageFile.path);
      final savedImage = await imageFile.copy('${directory.path}/$fileName');
      return savedImage.path;
    } catch (e) {
      debugPrint("Error guardando imagen: $e");
      return null;
    }
  }

  // ✅ CORRECCIÓN CRÍTICA: Extraemos el diálogo a su propio método/clase
  // o manejamos el ciclo de vida de los controladores dentro del Builder para evitar Dispose prematuro.
  void _showCreateNewProductDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext dialogContext) {
        return _CreateProductDialog(
          bridge: _bridge,
          onSaveImage: _saveImagePermanently,
          onSuccess: (String name, String qty) {
            ActivityService.instance.addActivity(ActivityEvent(
              title: 'Nuevo Producto Creado',
              subtitle: '$name (+$qty)',
              icon: Icons.inventory,
              color: Colors.blue,
              timestamp: DateTime.now(),
            ));
            ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Producto registrado correctamente'), backgroundColor: Colors.green));

            // Forzamos refresco
            unawaited(_bridge.obtenerProductos(forceRefresh: true).then((_) => _loadProducts()));
          },
        );
      },
    );
  }

  // ==========================================
  // 5. INTERFAZ GRÁFICA PRINCIPAL
  // ==========================================

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Gestión de Compras'),
        automaticallyImplyLeading: false,
        actions: [
          IconButton(icon: const Icon(Icons.refresh), tooltip: "Actualizar Datos", onPressed: _loadInitialData)
        ],
        bottom: TabBar(
          controller: _tabController,
          tabAlignment: TabAlignment.start,
          isScrollable: true,
          tabs: [
            const Tab(icon: Icon(Icons.inventory_2), text: 'Catálogo'),
            Tab(icon: Badge(isLabelVisible: _cart.isNotEmpty, label: Text('${_cart.length}'), child: const Icon(Icons.local_shipping)), text: 'Ingresar Stock'),
            const Tab(icon: Icon(Icons.history), text: 'Historial'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildCatalogTab(),
          _buildCartTab(),
          _buildHistorialTab(),
        ],
      ),
      floatingActionButton: _tabController.index == 0
          ? FloatingActionButton.extended(
        onPressed: _showCreateNewProductDialog,
        backgroundColor: Colors.blue,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add),
        label: const Text("Nuevo Producto"),
      )
          : null,
    );
  }

  // ==========================================
  // 6. WIDGETS DE PESTAÑAS
  // ==========================================

  Widget _buildCatalogTab() {
    if (_isLoadingProducts) return const StitchLoader();
    if (_products.isEmpty) return const Center(child: Text("Catálogo vacío. Agrega un nuevo producto."));

    return GridView.builder(
      padding: const EdgeInsets.all(8),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 0.60, crossAxisSpacing: 10, mainAxisSpacing: 10),
      itemCount: _products.length,
      itemBuilder: (context, index) {
        final product = _products[index];

        return AnimatedListItem(
          index: index,
          child: Card(
            elevation: 3,
            clipBehavior: Clip.antiAlias,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(child: OptimizedImage(imagePath: product.imagePath)),
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(product.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.bold)),
                      Text("Stock actual: ${product.stock}", style: const TextStyle(fontSize: 12, color: Colors.grey)),
                      const SizedBox(height: 5),
                      SizedBox(
                          height: 35, width: double.infinity,
                          child: ElevatedButton.icon(
                              icon: const Icon(Icons.add_shopping_cart, size: 16),
                              label: const Text("Comprar"),
                              style: ElevatedButton.styleFrom(padding: EdgeInsets.zero, backgroundColor: Colors.blue, foregroundColor: Colors.white),
                              onPressed: () => _showAddStockDialog(product)
                          )
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

  Widget _buildCartTab() {
    if (_cart.isEmpty) return const Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(Icons.widgets_outlined, size: 80, color: Colors.grey), SizedBox(height: 10), Text("Selecciona productos del catálogo para abastecer")]));

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
                      leading: SizedBox(width: 50, child: OptimizedImage(imagePath: p.imagePath)),
                      title: Text(p.name),
                      subtitle: Text('${item['qty']} uds. a $_currency${item['costPrice'].toStringAsFixed(2)} c/u'),
                      trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text('$_currency${(item['qty']*item['costPrice']).toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.bold)),
                            IconButton(
                                icon: Icon(Icons.delete, color: _isProcessingPurchase ? Colors.grey : Colors.red),
                                onPressed: _isProcessingPurchase ? null : () => _removeFromCart(index)
                            )
                          ]
                      )
                  );
                }
            )
        ),
        Container(
            padding: const EdgeInsets.all(20),
            decoration: const BoxDecoration(color: Colors.white, boxShadow: [BoxShadow(blurRadius: 10, color: Colors.black12)]),
            child: Column(
                children: [
                  Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text("COSTO TOTAL:", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                        Text("$_currency${_calculateTotal().toStringAsFixed(2)}", style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.blue))
                      ]
                  ),
                  const SizedBox(height: 15),
                  SizedBox(
                      width: double.infinity,
                      height: 50,
                      child: ElevatedButton.icon(
                          icon: _isProcessingPurchase ? const SizedBox() : const Icon(Icons.save),
                          style: ElevatedButton.styleFrom(backgroundColor: Colors.blue, foregroundColor: Colors.white),
                          onPressed: _isProcessingPurchase ? null : _processPurchase,
                          label: _isProcessingPurchase ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2)) : const Text("REGISTRAR COMPRA Y STOCK")
                      )
                  )
                ]
            )
        )
      ],
    );
  }

  Widget _buildHistorialTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: TextField(
            decoration: InputDecoration(
              hintText: 'Buscar compra por descripción o fecha...',
              prefixIcon: const Icon(Icons.search),
              filled: true,
              fillColor: Colors.grey[100],
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(30), borderSide: BorderSide.none),
              contentPadding: const EdgeInsets.symmetric(horizontal: 20),
            ),
            onChanged: _filtrarCompras,
          ),
        ),
        if (_searchQuery.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Text("${_filteredCompras.length} compras encontradas", style: const TextStyle(color: Colors.grey, fontSize: 12)),
          ),
        Expanded(
          child: _isLoadingCompras
              ? const Center(child: CircularProgressIndicator())
              : _filteredCompras.isEmpty
              ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(Icons.search_off, size: 60, color: Colors.grey[300]), const Text("No se encontraron compras")]))
              : RefreshIndicator(
            onRefresh: _loadComprasHistorial,
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              itemCount: _filteredCompras.length,
              itemBuilder: (context, index) => _buildCompraCard(_filteredCompras[index]),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildCompraCard(JsonMap compra) {
    final double total = (compra['monto'] ?? 0.0).toDouble();
    final String desc = compra['descripcion'] ?? 'Compra de mercadería';
    final String fecha = compra['fecha'] ?? '';
    final String img = compra['imagePath']?.toString().trim() ?? '';

    return Card(
      elevation: 2,
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: ListTile(
        contentPadding: const EdgeInsets.all(12),
        leading: SizedBox(width: 50, height: 50, child: OptimizedImage(imagePath: img)),
        title: Text(desc, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
        subtitle: Text(fecha, style: const TextStyle(fontSize: 12, color: Colors.grey)),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            const Text("Costo", style: TextStyle(fontSize: 10, color: Colors.grey)),
            Text("$_currency${total.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Colors.blue)),
          ],
        ),
      ),
    );
  }

  // ==========================================
  // 7. DIÁLOGOS ADICIONALES
  // ==========================================

  void _showAddStockDialog(Product product) {
    showDialog(
        context: context,
        builder: (dialogContext) {
          // ✅ Corregido: Delegando a un StatefulBuilder para encapsular la memoria
          return _AddStockDialog(
            product: product,
            currency: _currency,
            onAdd: (qty, cost) => _addToCart(product, qty, cost),
          );
        }
    );
  }

  void _showSuccessDialog(double total) {
    showDialog(
        context: context,
        builder: (dialogContext) {
          return AlertDialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            content: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  const Icon(Icons.inventory, color: Colors.blue, size: 60),
                  const SizedBox(height: 16),
                  const Text('¡Compra Registrada!', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  const Text('El inventario se ha actualizado exitosamente.', textAlign: TextAlign.center, style: TextStyle(color: Colors.grey)),
                  const SizedBox(height: 16),
                  const Divider(),
                  const SizedBox(height: 8),
                  const Text("INVERSIÓN TOTAL", style: TextStyle(color: Colors.grey, fontSize: 12)),
                  Text(
                      '$_currency${total.toStringAsFixed(2)}',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 32, color: Colors.blue)
                  ),
                  const SizedBox(height: 24),
                  SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                          style: ElevatedButton.styleFrom(
                              padding: const EdgeInsets.symmetric(vertical: 12),
                              backgroundColor: Colors.blue,
                              foregroundColor: Colors.white,
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30))
                          ),
                          child: const Text('Cerrar', style: TextStyle(fontSize: 16)),
                          onPressed: () => Navigator.of(dialogContext).pop()
                      )
                  )
                ]
            ),
          );
        }
    );
  }
}

// -----------------------------------------------------------------------------
// CLASES PRIVADAS PARA DIÁLOGOS (PREVIENE MEMORY LEAKS Y ERRORES DE BUILD)
// -----------------------------------------------------------------------------

class _CreateProductDialog extends StatefulWidget {
  final BridgeFlutter bridge;
  final Future<String?> Function(File) onSaveImage;
  final Function(String, String) onSuccess;

  const _CreateProductDialog({
    required this.bridge,
    required this.onSaveImage,
    required this.onSuccess
  });

  @override
  State<_CreateProductDialog> createState() => _CreateProductDialogState();
}

class _CreateProductDialogState extends State<_CreateProductDialog> {
  final _nameCtrl = TextEditingController();
  final _categoryCtrl = TextEditingController();
  final _purchasePriceCtrl = TextEditingController();
  final _salePriceCtrl = TextEditingController();
  final _quantityCtrl = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  File? _selectedLocalImage;
  bool _isSaving = false;

  @override
  void dispose() {
    _nameCtrl.dispose();
    _categoryCtrl.dispose();
    _purchasePriceCtrl.dispose();
    _salePriceCtrl.dispose();
    _quantityCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Registrar Nuevo Producto'),
      content: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              GestureDetector(
                onTap: _isSaving ? null : () async {
                  final picker = ImagePicker();
                  final image = await picker.pickImage(source: ImageSource.gallery);
                  if (image != null && mounted) {
                    setState(() => _selectedLocalImage = File(image.path));
                  }
                },
                child: Container(
                  width: 100, height: 100,
                  decoration: BoxDecoration(
                    color: Colors.grey[200],
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.grey.shade400),
                    image: _selectedLocalImage != null ? DecorationImage(image: FileImage(_selectedLocalImage!), fit: BoxFit.cover) : null,
                  ),
                  child: _selectedLocalImage == null
                      ? const Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(Icons.camera_alt, color: Colors.grey), Text("Foto", style: TextStyle(color: Colors.grey, fontSize: 10))])
                      : null,
                ),
              ),
              const SizedBox(height: 15),
              TextFormField(
                controller: _nameCtrl,
                decoration: const InputDecoration(labelText: 'Nombre Producto', prefixIcon: Icon(Icons.tag)),
                validator: (v) => v == null || v.isEmpty ? "Requerido" : null,
              ),
              TextFormField(
                controller: _categoryCtrl,
                decoration: const InputDecoration(labelText: 'Categoría', prefixIcon: Icon(Icons.category)),
                validator: (v) => v == null || v.isEmpty ? "Requerido" : null,
              ),
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                        controller: _purchasePriceCtrl,
                        decoration: const InputDecoration(labelText: 'Costo', prefixIcon: Icon(Icons.money_off)),
                        keyboardType: TextInputType.number,
                        validator: (v) {
                          if (v == null || v.isEmpty) return 'Falta';
                          if (double.tryParse(v) == null || double.parse(v) < 0) return 'Inválido';
                          return null;
                        }
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: TextFormField(
                        controller: _salePriceCtrl,
                        decoration: const InputDecoration(labelText: 'P. Venta', prefixIcon: Icon(Icons.attach_money)),
                        keyboardType: TextInputType.number,
                        validator: (v) {
                          if (v == null || v.isEmpty) return 'Falta';
                          if (double.tryParse(v) == null || double.parse(v) <= 0) return 'Inválido';
                          return null;
                        }
                    ),
                  ),
                ],
              ),
              TextFormField(
                  controller: _quantityCtrl,
                  decoration: const InputDecoration(labelText: 'Stock Inicial', prefixIcon: Icon(Icons.numbers)),
                  keyboardType: TextInputType.number,
                  validator: (v) {
                    if (v == null || v.isEmpty) return 'Requerido';
                    if (int.tryParse(v) == null || int.parse(v) < 0) return 'Inválido';
                    return null;
                  }
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isSaving ? null : () => Navigator.of(context).pop(),
          child: const Text('Cancelar', style: TextStyle(color: Colors.grey)),
        ),
        ElevatedButton(
          style: ElevatedButton.styleFrom(backgroundColor: Colors.blue, foregroundColor: Colors.white),
          onPressed: _isSaving ? null : () async {
            if (_formKey.currentState!.validate()) {
              setState(() => _isSaving = true);

              String? savedPath;
              if (_selectedLocalImage != null) {
                savedPath = await widget.onSaveImage(_selectedLocalImage!);
              }

              final JsonMap productMap = {
                'nombre': _nameCtrl.text,
                'precioCompra': double.parse(_purchasePriceCtrl.text),
                'precioVenta': double.parse(_salePriceCtrl.text),
                'cantidad': int.parse(_quantityCtrl.text),
                'categoriaNombre': _categoryCtrl.text,
                'imagePath': savedPath ?? '',
              };

              final response = await widget.bridge.agregarProducto(productMap);

              if (!mounted) return;

              if (response.isSuccess) {
                Navigator.of(context).pop();
                widget.onSuccess(_nameCtrl.text, _quantityCtrl.text);
              } else {
                setState(() => _isSaving = false);
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: ${response.mensaje}'), backgroundColor: Colors.red));
              }
            }
          },
          child: _isSaving ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2)) : const Text('Guardar Producto'),
        ),
      ],
    );
  }
}

class _AddStockDialog extends StatefulWidget {
  final Product product;
  final String currency;
  final Function(int, double) onAdd;

  const _AddStockDialog({required this.product, required this.currency, required this.onAdd});

  @override
  State<_AddStockDialog> createState() => _AddStockDialogState();
}

class _AddStockDialogState extends State<_AddStockDialog> {
  final _costCtrl = TextEditingController();
  final _qtyCtrl = TextEditingController(text: '1');
  final _formKey = GlobalKey<FormState>();

  @override
  void dispose() {
    _costCtrl.dispose();
    _qtyCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
        title: Text("Reabastecer: ${widget.product.name}"),
        content: Form(
          key: _formKey,
          child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text("Stock actual: ${widget.product.stock}", style: const TextStyle(color: Colors.grey)),
                const SizedBox(height: 16),
                TextFormField(
                    controller: _qtyCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(labelText: 'Cantidad a ingresar', border: OutlineInputBorder(), prefixIcon: Icon(Icons.numbers)),
                    validator: (v) {
                      if (v == null || v.isEmpty) return 'Requerido';
                      if (int.tryParse(v) == null || int.parse(v) <= 0) return 'Mayor a 0';
                      return null;
                    }
                ),
                const SizedBox(height: 16),
                TextFormField(
                    controller: _costCtrl,
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(labelText: 'Costo Unitario (${widget.currency})', border: const OutlineInputBorder(), prefixIcon: const Icon(Icons.attach_money)),
                    validator: (v) {
                      if (v == null || v.isEmpty) return 'Requerido';
                      if (double.tryParse(v) == null || double.parse(v) <= 0) return 'Mayor a 0';
                      return null;
                    }
                ),
              ]
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text("Cancelar")),
          ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.blue, foregroundColor: Colors.white),
              onPressed: () {
                if (_formKey.currentState!.validate()) {
                  Navigator.pop(context);
                  widget.onAdd(int.parse(_qtyCtrl.text), double.parse(_costCtrl.text));
                }
              },
              child: const Text("Añadir a la lista")
          )
        ]
    );
  }
}