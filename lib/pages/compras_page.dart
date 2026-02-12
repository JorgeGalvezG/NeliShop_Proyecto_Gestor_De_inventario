import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as path;

// 1. Importamos tu configuración core para los tipos
import '../config/core.dart';

import '../bridge_flutter.dart';
import '../servicios/activity_service.dart';
import '../modelos/activity_event_model.dart';
import '../widgets/optimized_image.dart';
import '../widgets/stitch_loader.dart';

class ComprasPage extends StatefulWidget {
  const ComprasPage({super.key});

  @override
  State<ComprasPage> createState() => _ComprasPageState();
}

class _ComprasPageState extends State<ComprasPage> {
  final BridgeFlutter _bridge = BridgeFlutter();

  // Usamos el tipo definido JsonList
  JsonList _compras = [];
  bool _isLoading = true;
  File? _selectedImage;

  @override
  void initState() {
    super.initState();
    _loadCompras();
  }

  // Carga optimizada
  Future<void> _loadCompras() async {
    if (!mounted) return;
    setState(() => _isLoading = true);

    try {
      // El bridge ya devuelve una JsonList limpia
      final result = await _bridge.listarCompras();
      _compras = result;
    } catch (e) {
      print("Error loading purchases: $e");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _pickImage() async {
    final ImagePicker picker = ImagePicker();
    final XFile? image = await picker.pickImage(source: ImageSource.gallery);

    if (image != null) {
      setState(() {
        _selectedImage = File(image.path);
      });
    }
  }

  Future<String?> _saveImagePermanently(File imageFile) async {
    try {
      final directory = await getApplicationDocumentsDirectory();
      final fileName = path.basename(imageFile.path);
      final savedImage = await imageFile.copy('${directory.path}/$fileName');
      return savedImage.path;
    } catch (e) {
      print("Error guardando imagen: $e");
      return null;
    }
  }

  void _showAddProductDialog(BuildContext context) {
    final nameController = TextEditingController();
    final categoryController = TextEditingController();
    final purchasePriceController = TextEditingController();
    final salePriceController = TextEditingController();
    final quantityController = TextEditingController();
    final formKey = GlobalKey<FormState>();

    _selectedImage = null; // Reiniciar imagen al abrir

    showDialog(
      context: context,
      builder: (BuildContext dialogContext) {
        return StatefulBuilder(
          builder: (context, setStateInDialog) {
            return AlertDialog(
              title: const Text('Registrar Compra / Ingreso'),
              content: Form(
                key: formKey,
                child: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      // SELECTOR DE IMAGEN
                      GestureDetector(
                        onTap: () async {
                          await _pickImage();
                          setStateInDialog(() {}); // Actualiza el diálogo
                        },
                        child: Container(
                          width: 100,
                          height: 100,
                          decoration: BoxDecoration(
                            color: Colors.grey[200],
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: Colors.grey.shade400),
                            image: _selectedImage != null
                                ? DecorationImage(
                              image: FileImage(_selectedImage!),
                              fit: BoxFit.cover,
                            )
                                : null,
                          ),
                          child: _selectedImage == null
                              ? const Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.camera_alt, color: Colors.grey),
                              Text("Foto", style: TextStyle(color: Colors.grey, fontSize: 10)),
                            ],
                          )
                              : null,
                        ),
                      ),
                      const SizedBox(height: 15),
                      TextFormField(
                        controller: nameController,
                        decoration: const InputDecoration(labelText: 'Nombre Producto', prefixIcon: Icon(Icons.tag)),
                        validator: (v) => v!.isEmpty ? "Requerido" : null,
                      ),
                      TextFormField(
                        controller: categoryController,
                        decoration: const InputDecoration(labelText: 'Categoría', prefixIcon: Icon(Icons.category)),
                        validator: (v) => v!.isEmpty ? "Requerido" : null,
                      ),
                      Row(
                        children: [
                          Expanded(
                            child: TextFormField(
                              controller: purchasePriceController,
                              decoration: const InputDecoration(labelText: 'P. Compra', prefixIcon: Icon(Icons.money_off)),
                              keyboardType: TextInputType.number,
                              validator: (v) => v!.isEmpty ? "Falta" : null,
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: TextFormField(
                              controller: salePriceController,
                              decoration: const InputDecoration(labelText: 'P. Venta', prefixIcon: Icon(Icons.attach_money)),
                              keyboardType: TextInputType.number,
                              validator: (v) => v!.isEmpty ? "Falta" : null,
                            ),
                          ),
                        ],
                      ),
                      TextFormField(
                        controller: quantityController,
                        decoration: const InputDecoration(labelText: 'Cantidad', prefixIcon: Icon(Icons.numbers)),
                        keyboardType: TextInputType.number,
                        validator: (v) => v!.isEmpty ? "Requerido" : null,
                      ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancelar'),
                ),
                ElevatedButton(
                  onPressed: () async {
                    if (formKey.currentState!.validate()) {

                      // 1. Guardar imagen localmente
                      String? savedPath;
                      if (_selectedImage != null) {
                        savedPath = await _saveImagePermanently(_selectedImage!);
                      }

                      // 2. Preparar datos usando JsonMap (Tipado fuerte)
                      final JsonMap productMap = {
                        'nombre': nameController.text,
                        'precioCompra': double.parse(purchasePriceController.text),
                        'precioVenta': double.parse(salePriceController.text),
                        'cantidad': int.parse(quantityController.text),
                        'categoriaNombre': categoryController.text,
                        'imagePath': savedPath ?? '',
                      };

                      // 3. Enviar usando el Bridge Optimizado (Devuelve BridgeResponse)
                      final response = await _bridge.agregarProducto(productMap);

                      if (mounted) {
                        // Cerramos el diálogo primero
                        Navigator.of(context).pop();

                        // 4. Verificamos el éxito con la nueva propiedad .isSuccess
                        if (response.isSuccess) {

                          // Registrar en dashboard de actividad
                          ActivityService.instance.addActivity(ActivityEvent(
                            title: 'Ingreso Producto',
                            subtitle: '${nameController.text} (+${quantityController.text})',
                            icon: Icons.inventory,
                            color: Colors.blue,
                            timestamp: DateTime.now(),
                          ));

                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Producto registrado correctamente'), backgroundColor: Colors.green),
                          );

                          _loadCompras(); // Recargar lista
                        } else {
                          // Mostrar error exacto que viene de Java o del Bridge
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text('Error: ${response.mensaje}'), backgroundColor: Colors.red),
                          );
                        }
                      }
                    }
                  },
                  child: const Text('Guardar'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Compras / Ingresos'),
        automaticallyImplyLeading: false,
      ),
      body: _isLoading
          ? const StitchLoader()
          : _compras.isEmpty
          ? Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.local_shipping_outlined, size: 80, color: Colors.grey[300]),
            const SizedBox(height: 16),
            const Text("No hay historial de compras", style: TextStyle(fontSize: 18, color: Colors.grey)),
          ],
        ),
      )
          : ListView.builder(
        itemCount: _compras.length,
        itemBuilder: (context, index) {
          final compra = _compras[index];
          // Nota: Asegúrate de que tu backend Java (listarCompras) devuelva 'imagePath'
          // si quieres mostrar fotos aquí. Si no, saldrá el icono por defecto.
          final imagePath = compra['imagePath'];

          return Card(
            margin: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            clipBehavior: Clip.antiAlias,
            child: ListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
              leading: SizedBox(
                width: 50,
                height: 50,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: (imagePath != null && imagePath.isNotEmpty)
                      ? OptimizedImage(imagePath: imagePath)
                      : Container(
                    color: Colors.blue[50],
                    child: const Icon(Icons.inventory_2, color: Colors.blue),
                  ),
                ),
              ),
              title: Text(
                  "Compra #${compra['id']}",
                  style: const TextStyle(fontWeight: FontWeight.bold)
              ),
              subtitle: Text(
                compra['descripcion'] ?? 'Sin descripción',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              trailing: Text(
                "S/${(compra['monto'] ?? 0).toStringAsFixed(2)}",
                style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.blue, fontSize: 15),
              ),
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddProductDialog(context),
        backgroundColor: Colors.blue,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }
}