import 'dart:ffi';

import 'package:compact_dir/bridge_flutter.dart';

import '../modelos/product_model.dart';
import '../servicios/product_service.dart';
import '../widgets/activity_card.dart';
import '../widgets/summary_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../screens/login_screen.dart';
import '../servicios/activity_service.dart';
import '../modelos/activity_event_model.dart';

class HomePage extends StatefulWidget {
  final String userRole;
  const HomePage({super.key, required this.userRole});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  // Variables de Estado para el Dashboard
  String _gananciaTexto = "Cargando...";
  String _cantidadProductos = "...";

  @override
  void initState() {
    super.initState();
    // Cargar datos solo si es admin
    if(widget.userRole == 'admin') {
      _cargarDatosDashboard();
    }
  }

  // Función que obtiene datos reales de la BD via Java Bridge
  Future<void> _cargarDatosDashboard() async {
    BridgeFlutter BF = BridgeFlutter();

    try {
      // 1. Obtener Ganancia Neta Real (Requiere la corrección SQL en Java)
      double? ganancia = await BF.getGananciaTotal();

      // 2. Obtener Cantidad Real de Productos en Base de Datos
      List<dynamic> productosRaw = await BF.obtenerProductos();
      int cantidad = productosRaw.length;

      if(mounted) {
        setState(() {
          _gananciaTexto = "S/${(ganancia ?? 0.0).toStringAsFixed(2)}";
          _cantidadProductos = cantidad.toString();
        });
      }
    } catch (e) {
      print("Error cargando dashboard: $e");
      if(mounted) {
        setState(() {
          _gananciaTexto = "Error";
          _cantidadProductos = "0";
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.userRole == 'admin' ? "Hola, Neli!" : "Bienvenido a NeliShop"),
        automaticallyImplyLeading: false,
        actions: [
          PopupMenuButton<String>(
            onSelected: (value) {
              if (value == 'logout') {
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (context) => const LoginScreen()),
                      (Route<dynamic> route) => false,
                );
              } else if (value == 'support') {
                showDialog(
                  context: context,
                  builder: (context) => AlertDialog(
                    title: const Text('Soporte Técnico'),
                    content: const Text('Para ayuda, contacta al:        920 325 196 o escribenos a: enriqueespinosadioses@gmail.com'),

                    actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cerrar'))],
                  ),
                );
              }
            },
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: CircleAvatar(
                backgroundColor: Theme.of(context).colorScheme.primary.withOpacity(0.1),
                child: Icon(Icons.person, color: Theme.of(context).colorScheme.primary),
              ),
            ),
            itemBuilder: (BuildContext context) => <PopupMenuEntry<String>>[
              const PopupMenuItem<String>(value: 'support', child: ListTile(leading: Icon(Icons.support_agent), title: Text('Soporte'))),
              const PopupMenuItem<String>(value: 'logout', child: ListTile(leading: Icon(Icons.logout, color: Colors.red), title: Text('Cerrar Sesión', style: TextStyle(color: Colors.red)))),
            ],
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: widget.userRole == 'admin'
            ? _buildAdminHome(context)
            : _buildUserHome(context),
      ),
    );
  }

  // 👩‍💼 VISTA DE ADMIN (Dashboard Actualizado)
  Widget _buildAdminHome(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          "Resumen del negocio",
          style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 12),
        GridView.count(
          crossAxisCount: 2,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          children: [
            SummaryCard(
              icon: Icons.attach_money,
              title: "Ganancia Neta",
              value: _gananciaTexto,  // ✅ Dato real desde BD
              color: Colors.green.withOpacity(0.1),
              iconColor: Colors.green,
            ),
            SummaryCard(
              icon: Icons.inventory_2,
              title: "Productos",
              value: _cantidadProductos, // ✅ Dato real desde BD
              color: theme.colorScheme.secondary.withOpacity(0.1),
              iconColor: theme.colorScheme.secondary,
            ),
          ],
        ).animate().fadeIn(duration: 300.ms),
        const SizedBox(height: 24),
        const Text("Actividad reciente", style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),

        ValueListenableBuilder<List<ActivityEvent>>(
          valueListenable: ActivityService.instance.activityNotifier,
          builder: (context, activities, child) {
            if (activities.isEmpty) {
              return const Center(child: Text("No hay actividad reciente.", style: TextStyle(color: Colors.grey)));
            }
            return Column(
              children: activities.take(5).map((event) {
                return ActivityCard(
                  icon: event.icon,
                  title: event.title,
                  subtitle: event.subtitle,
                  time: _formatTimeAgo(event.timestamp),
                  color: event.color,
                ).animate().slideX(begin: -0.2).fadeIn();
              }).toList(),
            );
          },
        ),
      ],
    );
  }

  // 🧑‍💻 VISTA DE USUARIO (Misma lógica anterior)
  Widget _buildUserHome(BuildContext context) {
    // Escucha los cambios del ProductService
    final productService = ProductService.instance;
    return ValueListenableBuilder<List<Product>>(
        valueListenable: productService.productsNotifier,
        builder: (context, products, child) {
          final promotions = productService.getPromotions();

          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Sección de Promociones
              const Text(
                "¡Promociones del Día!",
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              if (promotions.isEmpty)
                const Text("No hay promociones hoy, vuelve pronto.")
              else
              // Carrusel horizontal de promociones
                SizedBox(
                  height: 120,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    itemCount: promotions.length,
                    itemBuilder: (context, index) {
                      final product = promotions[index];
                      return _buildPromoCard(context, product);
                    },
                  ),
                ),
              const SizedBox(height: 24),

              // Sección de "Todos los Productos"
              const Text(
                "Catálogo de Productos",
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              Text("Total de productos en tienda: ${products.length}"),
            ],
          ).animate().fadeIn(duration: 300.ms);
        });
  }

  // Tarjeta de Promoción
  Widget _buildPromoCard(BuildContext context, Product product) {
    final theme = Theme.of(context);
    return Container(
      width: 250,
      margin: const EdgeInsets.only(right: 12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        gradient: LinearGradient(
          colors: [
            theme.colorScheme.primary.withOpacity(0.8),
            theme.colorScheme.primary,
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: [
          BoxShadow(
            color: theme.colorScheme.primary.withOpacity(0.3),
            blurRadius: 8,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              product.name,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 8),
            Text(
              "AHORA S/${product.salePrice!.toStringAsFixed(2)}",
              style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 20),
            ),
            Text(
              "Antes S/${product.price.toStringAsFixed(2)}",
              style: TextStyle(
                color: Colors.white.withOpacity(0.8),
                decoration: TextDecoration.lineThrough,
                fontSize: 12,
              ),
            ),
          ],
        ),
      ),
    );
  }

  // Helper para formatear la hora
  String _formatTimeAgo(DateTime time) {
    final difference = DateTime.now().difference(time);
    if (difference.inDays > 0) {
      return 'Hace ${difference.inDays} d';
    } else if (difference.inHours > 0) {
      return 'Hace ${difference.inHours} h';
    } else if (difference.inMinutes > 0) {
      return 'Hace ${difference.inMinutes} m';
    } else {
      return 'Ahora';
    }
  }
}