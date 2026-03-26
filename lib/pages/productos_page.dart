import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../modelos/product_model.dart';
import '../bridge_flutter.dart';
import '../widgets/optimized_image.dart';
import '../widgets/animated_list_item.dart';
import '../widgets/stitch_loader.dart';
import '../config/core.dart';
import '../config/db_helper.dart';

class ProductosPage extends StatefulWidget {
  const ProductosPage({super.key});

  @override
  State<ProductosPage> createState() => _ProductosPageState();
}

/// Tipos de ordenamiento disponibles para los productos
enum SortType { nombreAZ, precioMayorMenor, precioMenorMayor }

class _ProductosPageState extends State<ProductosPage> {
  final BridgeFlutter _bridge = BridgeFlutter();

  // Breakpoints para diseño responsivo
  static const double _tabletBreakpoint = 600;
  static const double _desktopBreakpoint = 900;

  // Estado de datos
  List<Product> _allProducts = [];
  List<String> _categories = [];

  // Estado de UI
  bool _isLoading = true;
  String _searchQuery = '';
  String? _selectedCategory;
  SortType _selectedSort = SortType.nombreAZ;

  // Control de operaciones individuales
  final Set<String> _processingIds = {};
  Timer? _debounceTimer;

  @override
  void initState() {
    super.initState();
    _loadProducts(forceRefresh: false);
  }

  @override
  void dispose() {
    _debounceTimer?.cancel();
    super.dispose();
  }

  /// Carga los productos desde la base de datos local (SQLite) y luego desde el servidor.
  /// Implementa una estrategia de caché: primero muestra datos locales, luego actualiza con datos frescos.
  Future<void> _loadProducts({bool forceRefresh = true}) async {
    if (!mounted) return;

    // Si no hay refresh forzado y ya tenemos productos, no hacer nada
    if (!forceRefresh && _allProducts.isNotEmpty) return;

    // Mostrar loader solo si la lista está vacía
    if (_allProducts.isEmpty) {
      setState(() => _isLoading = true);
    }

    try {
      // 1. Intentar cargar desde SQLite primero (caché local)
      final localData = await DBHelper.instance.getProductosLocal();

      if (localData.isNotEmpty && mounted) {
        final localProducts = Product.ListOfProducts(localData);
        final localCategories = _extractCategories(localProducts);

        setState(() {
          _allProducts = localProducts;
          _categories = localCategories;
          _isLoading = false;
        });
      }

      // 2. Luego intentar actualizar desde el servidor
      final response = await _bridge.obtenerProductos(forceRefresh: forceRefresh);
      final rawProducts = response.dataList;

      if (rawProducts.isNotEmpty && mounted) {
        // Guardar en SQLite para la próxima vez (fire-and-forget)
        unawaited(DBHelper.instance.syncProductos(rawProducts));

        final freshProducts = Product.ListOfProducts(rawProducts);
        final freshCategories = _extractCategories(freshProducts);

        setState(() {
          _allProducts = freshProducts;
          _categories = freshCategories;
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint("❌ Error cargando productos: $e");
      if (!mounted) return;

      setState(() => _isLoading = false);

      // Mostrar mensaje apropiado según si tenemos datos locales o no
      if (_allProducts.isEmpty) {
        _showSnackBar(
          "Error de conexión y no hay datos guardados.",
          Colors.red,
        );
      } else {
        _showSnackBar(
          "Modo sin conexión. Mostrando inventario guardado.",
          Colors.orange,
          duration: 2,
        );
      }
    }
  }

  /// Extrae categorías únicas de la lista de productos y las ordena alfabéticamente
  List<String> _extractCategories(List<Product> products) {
    return products
        .map((p) => p.category)
        .where((c) => c != null && c.isNotEmpty)
        .cast<String>()
        .toSet()
        .toList()
      ..sort();
  }

  /// Implementa debounce para evitar sobrecarga durante la escritura rápida
  void _onSearchChanged(String value) {
    _debounceTimer?.cancel();

    _debounceTimer = Timer(const Duration(milliseconds: 300), () {
      if (mounted) {
        setState(() {
          _searchQuery = value;
        });
      }
    });
  }

  /// Filtra y ordena los productos según los criterios seleccionados
  List<Product> _getFilteredProducts() {
    List<Product> temp = _allProducts.where((product) {
      final nameMatch = product.name.toLowerCase().contains(_searchQuery.toLowerCase());
      final categoryMatch = _selectedCategory == null || product.category == _selectedCategory;
      return nameMatch && categoryMatch;
    }).toList();

    temp.sort((a, b) {
      switch (_selectedSort) {
        case SortType.nombreAZ:
          return a.name.compareTo(b.name);
        case SortType.precioMayorMenor:
          return b.price.compareTo(a.price);
        case SortType.precioMenorMayor:
          return a.price.compareTo(b.price);
      }
    });

    return temp;
  }

  /// Muestra un diálogo para editar el stock del producto
  Future<void> _editStock(Product product) async {
    final controller = TextEditingController();

    try {
      final confirm = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: Text('Stock: ${product.name}'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Actual: ${product.stock} unidades'),
              const SizedBox(height: 10),
              TextField(
                controller: controller,
                keyboardType: const TextInputType.numberWithOptions(signed: true),
                inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'^-?\d*'))],
                autofocus: true,
                decoration: const InputDecoration(
                  labelText: 'Cantidad a AGREGAR',
                  hintText: 'Ej: 5 (suma) o -2 (resta)',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Guardar'),
            ),
          ],
        ),
      );

      if (confirm == true && controller.text.isNotEmpty) {
        final int addQty = int.tryParse(controller.text) ?? 0;
        final int newTotal = product.stock + addQty;

        // Validación de stock negativo
        if (newTotal < 0) {
          if (mounted) {
            _showSnackBar('Error: El stock no puede ser negativo', Colors.red);
          }
          return;
        }

        // Validación de ID antes de enviar
        final int? productId = int.tryParse(product.id);
        if (productId == null) {
          if (mounted) {
            _showSnackBar('Error: ID de producto inválido', Colors.red);
          }
          return;
        }

        setState(() => _processingIds.add(product.id));

        final JsonMap productMap = {
          'id': productId,
          'nombre': product.name,
          'precioCompra': product.purchasePrice ?? 0.0,
          'precioVenta': product.price,
          'cantidad': newTotal,
          'categoriaNombre': product.category ?? 'General',
          'imagePath': product.imagePath,
          'precioOferta': product.salePrice,
        };

        final response = await _bridge.actualizarProducto(productMap);

        if (mounted) {
          if (response.isSuccess) {
            _showSnackBar('Stock actualizado correctamente', Colors.green);
            await _loadProducts(forceRefresh: true);
          } else {
            _showSnackBar('Error: ${response.mensaje}', Colors.red);
          }
        }
      }
    } finally {
      controller.dispose();
      if (mounted) setState(() => _processingIds.remove(product.id));
    }
  }

  /// Muestra confirmación y elimina un producto
  Future<void> _deleteProduct(Product product) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar Producto'),
        content: Text('¿Estás seguro de eliminar "${product.name}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Eliminar', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirm == true) {
      // Validación de ID
      final int? productId = int.tryParse(product.id);
      if (productId == null) {
        if (mounted) {
          _showSnackBar('Error: ID de producto inválido', Colors.red);
        }
        return;
      }

      setState(() => _processingIds.add(product.id));

      try {
        final response = await _bridge.eliminarProducto(productId);

        if (mounted) {
          if (response.isSuccess) {
            _showSnackBar('Producto eliminado correctamente', Colors.green);
            await _loadProducts(forceRefresh: true);
          } else {
            _showSnackBar('Error: ${response.mensaje}', Colors.red);
          }
        }
      } finally {
        if (mounted) setState(() => _processingIds.remove(product.id));
      }
    }
  }

  /// Fuerza actualización de la UI
  void _applyFilters() => setState(() {});

  /// Muestra el diálogo de filtros y ordenamiento
  void _showFilterDialog() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => StatefulBuilder(
        builder: (context, setStateInModal) => Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.of(context).viewInsets.bottom,
          ),
          child: Container(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header con botón limpiar
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Filtros y Orden',
                      style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                    ),
                    if (_selectedCategory != null || _selectedSort != SortType.nombreAZ)
                      TextButton(
                        onPressed: () {
                          setStateInModal(() {
                            _selectedCategory = null;
                            _selectedSort = SortType.nombreAZ;
                          });
                          _applyFilters();
                          Navigator.pop(context);
                        },
                        child: const Text("Limpiar"),
                      )
                  ],
                ),
                const Divider(),
                const SizedBox(height: 10),

                // Selector de categoría
                const Text(
                  "Categoría:",
                  style: TextStyle(fontWeight: FontWeight.bold, color: Colors.grey),
                ),
                DropdownButtonFormField<String>(
                  value: _selectedCategory,
                  isExpanded: true,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                  ),
                  hint: const Text('Todas'),
                  items: [
                    const DropdownMenuItem<String>(value: null, child: Text("Todas")),
                    ..._categories.map((c) => DropdownMenuItem<String>(value: c, child: Text(c))),
                  ],
                  onChanged: (v) => setStateInModal(() => _selectedCategory = v),
                ),
                const SizedBox(height: 20),

                // Opciones de ordenamiento
                const Text(
                  "Ordenar por:",
                  style: TextStyle(fontWeight: FontWeight.bold, color: Colors.grey),
                ),
                _buildRadioTile("Nombre (A - Z)", SortType.nombreAZ, setStateInModal),
                _buildRadioTile("Precio (Mayor a Menor)", SortType.precioMayorMenor, setStateInModal),
                _buildRadioTile("Precio (Menor a Mayor)", SortType.precioMenorMayor, setStateInModal),
                const SizedBox(height: 16),

                // Botón aplicar
                SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton(
                    onPressed: () {
                      _applyFilters();
                      Navigator.pop(context);
                    },
                    child: const Text("Aplicar"),
                  ),
                )
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Construye un RadioListTile para las opciones de ordenamiento
  Widget _buildRadioTile(String title, SortType val, StateSetter setModalState) {
    return RadioListTile<SortType>(
      title: Text(title),
      value: val,
      groupValue: _selectedSort,
      contentPadding: EdgeInsets.zero,
      activeColor: Theme.of(context).primaryColor,
      onChanged: (v) => setModalState(() => _selectedSort = v!),
    );
  }

  /// Construye la tarjeta de producto con toda su funcionalidad
  Widget _buildProductCard(Product product) {
    final theme = Theme.of(context);
    final bool outOfStock = product.stock <= 0;
    final bool isProcessingItem = _processingIds.contains(product.id);

    return Card(
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Imagen del producto
              Expanded(
                child: Stack(
                  children: [
                    Positioned.fill(
                      child: Opacity(
                        opacity: outOfStock ? 0.5 : 1.0,
                        child: OptimizedImage(imagePath: product.imagePath),
                      ),
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
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          color: Colors.red,
                          child: const Text(
                            "OFERTA",
                            style: TextStyle(color: Colors.white, fontSize: 10),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
              // Información del producto
              Padding(
                padding: const EdgeInsets.all(10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      product.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    Text(
                      product.category ?? "Sin categoría",
                      style: const TextStyle(fontSize: 11, color: Colors.grey),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          "Stock: ${product.stock}",
                          style: TextStyle(
                            fontSize: 12,
                            color: outOfStock ? Colors.red : Colors.green,
                          ),
                        ),
                        if (product.onSale)
                          Text(
                            "S/${product.salePrice!.toStringAsFixed(2)}",
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              color: Colors.red,
                            ),
                          )
                        else
                          Text(
                            "S/${product.price.toStringAsFixed(2)}",
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              color: theme.primaryColor,
                            ),
                          ),
                      ],
                    )
                  ],
                ),
              )
            ],
          ),
          // Menú de opciones
          Positioned(
            top: 0,
            right: 0,
            child: PopupMenuButton<String>(
              icon: isProcessingItem
                  ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
                  : const Icon(Icons.more_vert, color: Colors.black54),
              enabled: !isProcessingItem,
              onSelected: (v) {
                if (v == 'edit') _editStock(product);
                if (v == 'del') _deleteProduct(product);
              },
              itemBuilder: (c) => [
                const PopupMenuItem(
                  value: 'edit',
                  child: Row(
                    children: [
                      Icon(Icons.edit, color: Colors.blue),
                      SizedBox(width: 8),
                      Text("Editar Stock"),
                    ],
                  ),
                ),
                const PopupMenuItem(
                  value: 'del',
                  child: Row(
                    children: [
                      Icon(Icons.delete, color: Colors.red),
                      SizedBox(width: 8),
                      Text("Eliminar"),
                    ],
                  ),
                ),
              ],
            ),
          )
        ],
      ),
    );
  }

  /// Helper para mostrar SnackBars consistentes
  void _showSnackBar(String message, Color backgroundColor, {int duration = 3}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: backgroundColor,
        duration: Duration(seconds: duration),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final double width = MediaQuery.of(context).size.width;
    int crossAxisCount = 2;
    double aspectRatio = 0.70;

    // Diseño responsivo
    if (width > _desktopBreakpoint) {
      crossAxisCount = 4;
      aspectRatio = 0.85;
    } else if (width > _tabletBreakpoint) {
      crossAxisCount = 3;
      aspectRatio = 0.80;
    }

    final filteredList = _getFilteredProducts();

    return Scaffold(
      appBar: AppBar(
        title: const Text("Productos"),
        automaticallyImplyLeading: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => _loadProducts(forceRefresh: true),
          ),
          Stack(
            children: [
              IconButton(
                icon: const Icon(Icons.filter_list),
                onPressed: _showFilterDialog,
              ),
              if (_selectedCategory != null || _selectedSort != SortType.nombreAZ)
                Positioned(
                  right: 8,
                  top: 8,
                  child: Container(
                    padding: const EdgeInsets.all(4),
                    decoration: const BoxDecoration(
                      color: Colors.red,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
            ],
          )
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(60),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              decoration: InputDecoration(
                hintText: 'Buscar por nombre...',
                prefixIcon: const Icon(Icons.search),
                filled: true,
                fillColor: Colors.grey[100],
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(30),
                  borderSide: BorderSide.none,
                ),
                contentPadding: const EdgeInsets.symmetric(horizontal: 20),
              ),
              onChanged: _onSearchChanged,
            ),
          ),
        ),
      ),
      body: _isLoading
          ? const StitchLoader()
          : filteredList.isEmpty
          ? Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.search_off, size: 60, color: Colors.grey[300]),
            const SizedBox(height: 10),
            const Text("No hay productos"),
          ],
        ),
      )
          : RefreshIndicator(
        onRefresh: () => _loadProducts(forceRefresh: true),
        child: GridView.builder(
          padding: const EdgeInsets.all(12),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: crossAxisCount,
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            childAspectRatio: aspectRatio,
          ),
          itemCount: filteredList.length,
          itemBuilder: (context, index) {
            return AnimatedListItem(
              index: index,
              child: _buildProductCard(filteredList[index]),
            );
          },
        ),
      ),
    );
  }
}