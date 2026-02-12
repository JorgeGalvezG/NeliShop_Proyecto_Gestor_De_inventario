import 'package:flutter/material.dart';
// 1. IMPORTANTE: Importamos tu nuevo archivo de configuración/core
import '../config/core.dart';
import '../modelos/product_model.dart';
import '../bridge_flutter.dart';
import '../widgets/optimized_image.dart';
import '../widgets/animated_list_item.dart';
import '../widgets/stitch_loader.dart';

class ProductosPage extends StatefulWidget {
  const ProductosPage({super.key});

  @override
  State<ProductosPage> createState() => _ProductosPageState();
}

// Enum para los tipos de ordenamiento
enum SortType { nombreAZ, precioMayorMenor, precioMenorMayor }

class _ProductosPageState extends State<ProductosPage> {
  final BridgeFlutter _bridge = BridgeFlutter();

  // Datos
  List<Product> _allProducts = [];
  List<String> _categories = [];

  // Filtros
  bool _isLoading = true;
  String _searchQuery = '';
  String? _selectedCategory;
  SortType _selectedSort = SortType.nombreAZ;

  @override
  void initState() {
    super.initState();
    _loadProducts();
  }

  // --- CARGA DE DATOS ---
  Future<void> _loadProducts() async {
    if (!mounted) return;
    setState(() => _isLoading = true);

    try {
      // Llamamos al bridge. Como devuelve JsonList (definido en core.dart), es compatible.
      final rawProducts = await _bridge.obtenerProductos();

      setState(() {
        _allProducts = rawProducts
            .map((json) => Product.fromJson(json))
            .toList();

        // Extraemos categorías
        _categories = _allProducts
            .map((p) => p.category)
            .toSet()
            .toList();
        _categories.insert(0, "Todos");

        _isLoading = false;
      });
    } catch (e) {
      print("Error cargando productos: $e");
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // --- LÓGICA DE FILTRADO ---
  List<Product> _getFilteredProducts() {
    List<Product> temp = _allProducts.where((product) {
      final nameMatch = product.name.toLowerCase().contains(_searchQuery.toLowerCase());
      final categoryMatch = _selectedCategory == null || _selectedCategory == "Todos" || product.category == _selectedCategory;
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

  // --- GESTIÓN: STOCK ---
  Future<void> _editStock(Product product) async {
    final controller = TextEditingController();
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
              keyboardType: TextInputType.numberWithOptions(signed: true),
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
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          ElevatedButton(onPressed: () => Navigator.pop(context, true), child: const Text('Guardar')),
        ],
      ),
    );

    if (confirm == true && controller.text.isNotEmpty) {
      int addQty = int.tryParse(controller.text) ?? 0;
      int newTotal = product.stock + addQty;

      if (newTotal < 0) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Error: El stock no puede ser negativo')));
        return;
      }

      // Preparamos el mapa usando JsonMap (ahora reconocido por importar core.dart)
      final JsonMap productMap = {
        'id': int.parse(product.id),
        'nombre': product.name,
        'precioCompra': 0.0,
        'precioVenta': product.price,
        'cantidad': newTotal,
        'categoriaNombre': product.category,
        'imagePath': product.imagePath,
        // Agregamos salePrice si existe para no perder la oferta al editar stock
        'salePrice': product.salePrice
      };

      setState(() => _isLoading = true);

      // Llamada al Bridge
      final response = await _bridge.actualizarProducto(productMap);

      if (mounted) {
        // Ahora .isSuccess es reconocido gracias a importar core.dart
        if (response.isSuccess){
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Stock actualizado'), backgroundColor: Colors.green));
          _loadProducts();
        } else {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: ${response.mensaje}')));
          setState(() => _isLoading = false);
        }
      }
    }
  }

  // --- GESTIÓN: ELIMINAR ---
  Future<void> _deleteProduct(Product product) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar Producto'),
        content: Text('¿Estás seguro de eliminar "${product.name}"?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Eliminar', style: TextStyle(color: Colors.red))),
        ],
      ),
    );

    if (confirm == true) {
      setState(() => _isLoading = true);

      // Llamada al Bridge
      final response = await _bridge.eliminarProducto(int.parse(product.id));

      if (mounted) {
        if (response.isSuccess){
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Producto eliminado')));
          _loadProducts();
        } else {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: ${response.mensaje}')));
          setState(() => _isLoading = false);
        }
      }
    }
  }

  // --- FILTROS DIALOG ---
  void _showFilterDialog() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (context) => StatefulBuilder(
        builder: (context, setStateInModal) => Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
          child: Container(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text('Filtros y Orden', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                    if (_selectedCategory != null || _selectedSort != SortType.nombreAZ)
                      TextButton(
                          onPressed: () {
                            setState(() { _selectedCategory = null; _selectedSort = SortType.nombreAZ; });
                            // Actualizamos también el modal para que se vea el cambio visual
                            setStateInModal(() { _selectedCategory = null; _selectedSort = SortType.nombreAZ; });
                            Navigator.pop(context);
                          },
                          child: const Text("Limpiar")
                      )
                  ],
                ),
                const Divider(),
                const SizedBox(height: 10),

                const Text("Categoría:", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.grey)),
                DropdownButtonFormField<String>(
                  value: _selectedCategory,
                  isExpanded: true,
                  hint: const Text('Todas'),
                  items: _categories.map((c) => DropdownMenuItem(value: c == "Todos" ? null : c, child: Text(c))).toList(),
                  onChanged: (v) => setStateInModal(() => _selectedCategory = v),
                ),
                const SizedBox(height: 20),

                const Text("Ordenar por:", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.grey)),
                _buildRadioTile("Nombre (A - Z)", SortType.nombreAZ, setStateInModal),
                _buildRadioTile("Precio (Mayor a Menor)", SortType.precioMayorMenor, setStateInModal),
                _buildRadioTile("Precio (Menor a Mayor)", SortType.precioMenorMayor, setStateInModal),

                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton(
                    onPressed: () {
                      setState(() {}); // Aplicar cambios a la pantalla principal
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

  Widget _buildProductCard(Product product) {
    final theme = Theme.of(context);
    bool outOfStock = product.stock <= 0;

    return Card(
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
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
                      Container(color: Colors.white54, child: const Center(child: Text("AGOTADO", style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold)))),
                    if (product.onSale)
                      Positioned(top: 5, right: 5, child: Container(padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2), color: Colors.red, child: const Text("OFERTA", style: TextStyle(color: Colors.white, fontSize: 10)))),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(product.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.bold)),
                    Text(product.category, style: const TextStyle(fontSize: 11, color: Colors.grey)),
                    const SizedBox(height: 4),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text("Stock: ${product.stock}", style: TextStyle(fontSize: 12, color: outOfStock ? Colors.red : Colors.green)),
                        if (product.onSale)
                          Text("S/${product.salePrice!.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.red))
                        else
                          Text("S/${product.price.toStringAsFixed(2)}", style: TextStyle(fontWeight: FontWeight.bold, color: theme.primaryColor)),
                      ],
                    )
                  ],
                ),
              )
            ],
          ),
          Positioned(
            top: 0, right: 0,
            child: PopupMenuButton<String>(
              icon: const Icon(Icons.more_vert, color: Colors.black54),
              onSelected: (v) {
                if (v == 'edit') _editStock(product);
                if (v == 'del') _deleteProduct(product);
              },
              itemBuilder: (c) => [
                const PopupMenuItem(value: 'edit', child: Row(children: [Icon(Icons.edit, color: Colors.blue), SizedBox(width: 8), Text("Editar Stock")])),
                const PopupMenuItem(value: 'del', child: Row(children: [Icon(Icons.delete, color: Colors.red), SizedBox(width: 8), Text("Eliminar")])),
              ],
            ),
          )
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final filteredList = _getFilteredProducts();

    return Scaffold(
      appBar: AppBar(
        title: const Text("Productos"),
        automaticallyImplyLeading: false,
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadProducts),
          Stack(
            children: [
              IconButton(icon: const Icon(Icons.filter_list), onPressed: _showFilterDialog),
              if (_selectedCategory != null || _selectedSort != SortType.nombreAZ)
                Positioned(right: 8, top: 8, child: Container(padding: const EdgeInsets.all(4), decoration: const BoxDecoration(color: Colors.red, shape: BoxShape.circle))),
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
                filled: true, fillColor: Colors.grey[100],
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(30), borderSide: BorderSide.none),
                contentPadding: const EdgeInsets.symmetric(horizontal: 20),
              ),
              onChanged: (val) => setState(() => _searchQuery = val),
            ),
          ),
        ),
      ),
      body: _isLoading
          ? const StitchLoader()
          : filteredList.isEmpty
          ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(Icons.search_off, size: 60, color: Colors.grey[300]), const Text("No hay productos")]))
          : RefreshIndicator(
        onRefresh: _loadProducts,
        child: GridView.builder(
          padding: const EdgeInsets.all(12),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 2,
            mainAxisSpacing: 12, crossAxisSpacing: 12,
            childAspectRatio: 0.70,
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