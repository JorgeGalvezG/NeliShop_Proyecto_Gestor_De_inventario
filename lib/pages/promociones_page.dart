import 'package:flutter/material.dart';
import 'package:dropdown_search/dropdown_search.dart';

// 1. Imports clave
import '../config/core.dart';
import '../modelos/product_model.dart';
import '../bridge_flutter.dart';
import '../widgets/stitch_loader.dart';
import '../widgets/optimized_image.dart';

class PromocionesPage extends StatefulWidget {
  const PromocionesPage({super.key});

  @override
  State<PromocionesPage> createState() => _PromocionesPageState();
}

class _PromocionesPageState extends State<PromocionesPage> {
  final BridgeFlutter _bridge = BridgeFlutter();
  List<Product> _products = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadProducts();
  }

  // --- CARGA OPTIMIZADA ---
  Future<void> _loadProducts() async {
    if (!mounted) return;
    setState(() => _isLoading = true);

    try {
      final rawProducts = await _bridge.obtenerProductos();

      setState(() {
        // Usamos el factory inteligente
        _products = rawProducts.map((json) => Product.fromJson(json)).toList();
        _isLoading = false;
      });
    } catch (e) {
      print("Error loading products: $e");
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // --- GUARDAR PROMOCIÓN ---
  Future<void> _savePromotion(Product product, double? promoPrice) async {
    setState(() => _isLoading = true);

    try {
      // 1. Preparamos los datos con JsonMap (Tipado fuerte)
      final JsonMap updateData = {
        'id': int.parse(product.id),
        'nombre': product.name,
        'precioCompra': 0.0, // Backend requiere este campo aunque sea 0
        'precioVenta': product.price,
        'cantidad': product.stock,
        'categoriaNombre': product.category,
        'imagePath': product.imagePath,
        // Aquí está la magia: enviamos el precio oferta o null
        'salePrice': promoPrice
      };

      // 2. Llamada al Bridge
      final response = await _bridge.actualizarProducto(updateData);

      if (mounted) {
        if (response.isSuccess) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
                content: Text(promoPrice == null ? 'Oferta eliminada' : '¡Oferta aplicada!'),
                backgroundColor: Colors.green
            ),
          );
          _loadProducts(); // Recargar lista
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error: ${response.mensaje}'), backgroundColor: Colors.red),
          );
          setState(() => _isLoading = false);
        }
      }
    } catch (e) {
      print("Error updating promo: $e");
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // --- DIÁLOGO DE EDICIÓN ---
  void _showPromotionDialog({Product? productToEdit}) {
    Product? selectedProduct = productToEdit;

    // Si estamos editando, mostramos el precio actual. Si no, vacío.
    final priceCtrl = TextEditingController(
        text: productToEdit?.salePrice?.toString() ?? ''
    );

    // Filtramos la lista para el dropdown: solo mostramos productos que NO tienen oferta aún
    // (A menos que estemos editando uno específico)
    final availableProducts = _products.where((p) => !p.onSale || p.id == productToEdit?.id).toList();

    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(builder: (context, setStateDialog) {
          return AlertDialog(
            title: Text(productToEdit == null ? 'Nueva Oferta' : 'Editar Oferta'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (productToEdit == null)
                  DropdownSearch<Product>(
                    items: availableProducts,
                    itemAsString: (Product p) => "${p.name} (S/${p.price})",
                    selectedItem: selectedProduct,
                    compareFn: (i, s) => i.id == s.id,
                    onChanged: (Product? p) {
                      setStateDialog(() {
                        selectedProduct = p;
                        priceCtrl.text = ''; // Limpiar al cambiar
                      });
                    },
                    dropdownDecoratorProps: const DropDownDecoratorProps(
                      dropdownSearchDecoration: InputDecoration(
                          labelText: "Seleccionar Producto",
                          border: OutlineInputBorder(),
                          prefixIcon: Icon(Icons.search)
                      ),
                    ),
                    popupProps: const PopupProps.menu(
                      showSearchBox: true,
                      searchFieldProps: TextFieldProps(decoration: InputDecoration(hintText: "Buscar...")),
                    ),
                  )
                else
                  Column(
                    children: [
                      Text(selectedProduct!.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                      const SizedBox(height: 5),
                      Text("Precio Normal: S/${selectedProduct!.price.toStringAsFixed(2)}",
                          style: const TextStyle(color: Colors.grey)),
                    ],
                  ),

                const SizedBox(height: 20),

                if (selectedProduct != null)
                  TextField(
                    controller: priceCtrl,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: InputDecoration(
                      labelText: 'Nuevo Precio Oferta (S/)',
                      prefixIcon: const Icon(Icons.local_offer, color: Colors.red),
                      border: const OutlineInputBorder(),
                      helperText: "Debe ser menor a S/${selectedProduct!.price.toStringAsFixed(2)}",
                    ),
                  ),
              ],
            ),
            actions: [
              // Botón para eliminar oferta si ya existe
              if (productToEdit != null && productToEdit.onSale)
                TextButton(
                  onPressed: () {
                    Navigator.pop(context);
                    _savePromotion(productToEdit, null); // Null elimina la oferta
                  },
                  child: const Text('Quitar Oferta', style: TextStyle(color: Colors.red)),
                ),

              TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),

              ElevatedButton(
                onPressed: () {
                  if (selectedProduct == null) return;

                  final double? newPrice = double.tryParse(priceCtrl.text);

                  if (newPrice != null && newPrice > 0 && newPrice < selectedProduct!.price) {
                    Navigator.pop(context);
                    _savePromotion(selectedProduct!, newPrice);
                  } else {
                    ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('El precio de oferta debe ser válido y menor al normal'))
                    );
                  }
                },
                child: const Text('Guardar'),
              ),
            ],
          );
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    // Ordenar: Primero las ofertas
    final displayList = [..._products];
    displayList.sort((a, b) {
      if (a.onSale && !b.onSale) return -1;
      if (!a.onSale && b.onSale) return 1;
      return 0;
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Gestión de Promociones'),
        automaticallyImplyLeading: false,
      ),
      body: _isLoading
          ? const StitchLoader()
          : displayList.isEmpty
          ? const Center(child: Text("No hay productos disponibles."))
          : ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: displayList.length,
        itemBuilder: (context, index) {
          final product = displayList[index];
          final isPromo = product.onSale;

          return Card(
            elevation: isPromo ? 4 : 1,
            color: isPromo ? Colors.red[50] : Colors.white,
            margin: const EdgeInsets.only(bottom: 12),
            child: ListTile(
              leading: SizedBox(
                width: 50, height: 50,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: OptimizedImage(imagePath: product.imagePath),
                ),
              ),
              title: Text(
                product.name,
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: isPromo ? Colors.red[800] : Colors.black87,
                ),
              ),
              subtitle: isPromo
                  ? const Text("🔥🔥 ¡En Oferta! 🔥🔥", style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold, fontSize: 12))
                  : Text(product.category),

              trailing: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  if (isPromo) ...[
                    Text(
                      "S/${product.price.toStringAsFixed(2)}",
                      style: const TextStyle(
                        decoration: TextDecoration.lineThrough,
                        color: Colors.grey,
                        fontSize: 12,
                      ),
                    ),
                    Text(
                      "S/${product.salePrice!.toStringAsFixed(2)}",
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        color: Colors.red,
                        fontSize: 16,
                      ),
                    ),
                  ] else
                    Text(
                      "S/${product.price.toStringAsFixed(2)}",
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                    ),
                ],
              ),
              onTap: () => _showPromotionDialog(productToEdit: product),
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showPromotionDialog(),
        backgroundColor: Colors.red,
        icon: const Icon(Icons.add, color: Colors.white),
        label: const Text("Nueva Oferta", style: TextStyle(color: Colors.white)),
      ),
    );
  }
}