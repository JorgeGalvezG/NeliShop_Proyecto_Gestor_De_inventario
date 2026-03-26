import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../config/core.dart';
import '../config/db_helper.dart';
import '../bridge_flutter.dart';
import '../modelos/product_model.dart';
import '../widgets/activity_card.dart';
import '../widgets/summary_card.dart';
import '../screens/login_screen.dart';
import '../servicios/activity_service.dart';
import '../modelos/activity_event_model.dart';

class HomePage extends StatefulWidget {
  final String userRole;
  const HomePage({super.key, required this.userRole});

  static const String roleAdmin = 'admin';

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final BridgeFlutter _bridge = BridgeFlutter();

  // ==========================================
  // 1. CONSTANTES
  // ==========================================
  static const String _supportPhone = '920 325 196';
  static const String _supportEmail = 'enriqueespinosadioses@gmail.com';
  static const int    _maxRecentActivities = 5;
  static const double _promoCardHeight = 130;
  static const String _currency = 'S/';

  // ==========================================
  // 2. VARIABLES DE ESTADO
  // ==========================================

  bool _isLoadingAdmin = true;
  bool _hasAdminError  = false;
  int  _cantidadProductosAdmin = 0;

  _MetricasPeriodo _metricasHoy    = _MetricasPeriodo.vacio();
  _MetricasPeriodo _metricasSemana = _MetricasPeriodo.vacio();
  _MetricasPeriodo _metricasMes    = _MetricasPeriodo.vacio();

  bool          _isLoadingUser = true;
  List<Product> _promociones   = [];
  int _totalProductosCatalogo  = 0;

  @override
  void initState() {
    super.initState();
    if (widget.userRole == HomePage.roleAdmin) {
      _cargarDatosAdmin();
    } else {
      _cargarDatosUsuarioOfflineFirst();
    }
  }

  // ==========================================
  // 3. LÓGICA DE ADMIN
  // ==========================================

  Future<void> _cargarDatosAdmin() async {
    if (!mounted) return;
    setState(() {
      _isLoadingAdmin = true;
      _hasAdminError  = false;
    });

    // PASO 1: Caché local rápido
    try {
      final localProducts = await DBHelper.instance.getProductosLocal();
      final localVentas   = await DBHelper.instance.getVentasHistorialLocal();

      if (localProducts.isNotEmpty && mounted) {
        setState(() {
          _cantidadProductosAdmin = localProducts.length;
          _isLoadingAdmin = false;
        });
      }
      if (localVentas.isNotEmpty && mounted) {
        _calcularMetricas(localVentas, localProducts);
      }
    } catch (e) {
      debugPrint("⚠️ Error leyendo caché local (Admin): $e");
    }

    // PASO 2: Red remota en paralelo
    //  eagerError:false → una falla no cancela la otra
    try {
      final results = await Future.wait(
        [
          _bridge.obtenerProductos(forceRefresh: true),
          _bridge.listarVentas(forceRefresh: true),
        ],
        eagerError: false,
      );

      final responseProductos = results[0] as BridgeResponse;
      final responseVentas    = results[1] as BridgeResponse;

      //  validar isSuccess antes de leer data
      final productos = responseProductos.isSuccess
          ? List<JsonMap>.from(responseProductos.data ?? [])
          : <JsonMap>[];

      final ventas = responseVentas.isSuccess
          ? List<JsonMap>.from(responseVentas.data ?? [])
          : <JsonMap>[];

      if (productos.isNotEmpty) await DBHelper.instance.syncProductos(productos);
      if (ventas.isNotEmpty)    await DBHelper.instance.syncVentasHistorial(ventas);

      final productosActualizados = await DBHelper.instance.getProductosLocal();
      final ventasActualizadas    = await DBHelper.instance.getVentasHistorialLocal();

      if (mounted) {
        _calcularMetricas(ventasActualizadas, productosActualizados);
        setState(() {
          _cantidadProductosAdmin = productosActualizados.length;
          _isLoadingAdmin = false;
        });
      }
    } catch (e) {
      debugPrint("❌ Error cargando dashboard remoto (Admin): $e");
      if (mounted) setState(() { _hasAdminError = true; _isLoadingAdmin = false; });
    }
  }

  /// Calcula métricas para hoy, esta semana y este mes.
  ///
  /// Campos usados (según product_model.dart):
  ///   productos → 'id', 'precioCompra'
  ///   detalles  → 'precio', 'cantidad', 'nombre'
  void _calcularMetricas(List<JsonMap> ventas, List<JsonMap> productosRaw) {
    // índices O(1) con nombres de campo correctos del modelo
    final Map<String, double> costoPorId     = {};
    final Map<String, double> costoPorNombre = {};

    for (final p in productosRaw) {
      final id     = p['id']?.toString() ?? '';
      final nombre = (p['nombre'] ?? '').toString().toLowerCase();
      final costoRaw = p['precioCompra'] ?? 0.0; // campo real del modelo
      final costo    = costoRaw is num ? costoRaw.toDouble() : 0.0;

      if (id.isNotEmpty)     costoPorId[id]        = costo;
      if (nombre.isNotEmpty) costoPorNombre[nombre] = costo;
    }

    final now    = DateTime.now();
    final hoy    = DateTime(now.year, now.month, now.day);
    final semana = hoy.subtract(Duration(days: hoy.weekday - 1));
    final mes    = DateTime(now.year, now.month, 1);

    //  nombres de variable sin espacio
    double brutaHoy = 0,    gananciaNetaHoy = 0;
    double brutaSemana = 0, gananciaNetaSemana = 0;
    double brutaMes = 0,    gananciaNetaMes = 0;

    //  contador de ventas con fecha inválida
    int ventasIgnoradas = 0;

    for (final venta in ventas) {
      final fechaVenta = DateTime.tryParse(venta['fecha']?.toString() ?? '');
      if (fechaVenta == null) { ventasIgnoradas++; continue; }

      final List detalles = venta['detalles'] ?? [];
      double brutaVenta = 0;
      double netaVenta  = 0;

      for (final d in detalles) {
        final precio   = (d['precio']   is num) ? (d['precio']   as num).toDouble() : 0.0;
        final cantidad = (d['cantidad'] is num) ? (d['cantidad'] as num).toDouble() : 0.0;
        final nombre   = (d['nombre']  ?? '').toString().toLowerCase();

        // Lookup de costo: primero por id, luego por nombre
        double costo = 0.0;
        final pid = d['productoId']?.toString();
        if (pid != null && costoPorId.containsKey(pid)) {
          costo = costoPorId[pid]!;
        } else if (nombre.isNotEmpty && costoPorNombre.containsKey(nombre)) {
          costo = costoPorNombre[nombre]!;
        }

        brutaVenta += precio * cantidad;
        netaVenta  += (precio - costo) * cantidad;
      }

      final soloFecha = DateTime(fechaVenta.year, fechaVenta.month, fechaVenta.day);

      if (!soloFecha.isBefore(hoy))    { brutaHoy    += brutaVenta; gananciaNetaHoy    += netaVenta; }
      if (!soloFecha.isBefore(semana)) { brutaSemana += brutaVenta; gananciaNetaSemana += netaVenta; }
      if (!soloFecha.isBefore(mes))    { brutaMes    += brutaVenta; gananciaNetaMes    += netaVenta; }
    }

    if (ventasIgnoradas > 0) {
      debugPrint("⚠️ $ventasIgnoradas ventas ignoradas por fecha inválida");
    }

    if (mounted) {
      setState(() {
        _metricasHoy    = _MetricasPeriodo(bruto: brutaHoy,    ganancia: gananciaNetaHoy);
        _metricasSemana = _MetricasPeriodo(bruto: brutaSemana, ganancia: gananciaNetaSemana);
        _metricasMes    = _MetricasPeriodo(bruto: brutaMes,    ganancia: gananciaNetaMes);
      });
    }
  }

  // ==========================================
  // 4. LÓGICA DE VENDEDOR
  // ==========================================

  Future<void> _cargarDatosUsuarioOfflineFirst() async {
    if (!mounted) return;
    setState(() => _isLoadingUser = true);

    try {
      final localData = await DBHelper.instance.getProductosLocal();
      if (localData.isNotEmpty && mounted) _procesarDatosVendedor(localData);

      final response    = await _bridge.obtenerProductos();
      final rawProducts = response.isSuccess
          ? List<JsonMap>.from(response.data ?? [])
          : <JsonMap>[];

      if (rawProducts.isNotEmpty && mounted) {
        await DBHelper.instance.syncProductos(rawProducts);
        final updatedLocal = await DBHelper.instance.getProductosLocal();
        if (mounted) _procesarDatosVendedor(updatedLocal);
      } else if (mounted) {
        setState(() => _isLoadingUser = false);
      }
    } catch (e) {
      debugPrint(" Error cargando datos de vendedor: $e");
      if (mounted) setState(() => _isLoadingUser = false);
    }
  }

  void _procesarDatosVendedor(List<JsonMap> rawData) {
    if (!mounted) return;
    final products = Product.ListOfProducts(rawData);
    setState(() {
      _totalProductosCatalogo = products.length;
      _promociones = products.where((p) => p.onSale).toList();
      _isLoadingUser = false;
    });
  }

  // ==========================================
  // 5. BUILD PRINCIPAL
  // ==========================================

  @override
  Widget build(BuildContext context) {
    final theme   = Theme.of(context);
    final isAdmin = widget.userRole == HomePage.roleAdmin;

    return Scaffold(
      appBar: AppBar(
        title: Text(isAdmin ? "¡Hola, Neli!" : "Bienvenido a NeliShop"),
        automaticallyImplyLeading: false,
        actions: [
          PopupMenuButton<String>(
            onSelected: (value) {
              if (value == 'logout') {
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                      (route) => false,
                );
              } else if (value == 'support') {
                _mostrarDialogoSoporte();
              }
            },
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: CircleAvatar(
                backgroundColor: theme.colorScheme.primary.withAlpha(25),
                child: Icon(Icons.person, color: theme.colorScheme.primary),
              ),
            ),
            itemBuilder: (_) => [
              const PopupMenuItem(value: 'support', child: ListTile(leading: Icon(Icons.support_agent), title: Text('Soporte Técnico'))),
              const PopupMenuItem(value: 'logout',  child: ListTile(leading: Icon(Icons.logout, color: Colors.red), title: Text('Cerrar Sesión', style: TextStyle(color: Colors.red)))),
            ],
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: isAdmin ? _cargarDatosAdmin : _cargarDatosUsuarioOfflineFirst,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: isAdmin ? _buildAdminHome(context) : _buildUserHome(context),
        ),
      ),
    );
  }

  // ==========================================
  // 6. VISTA ADMIN
  // ==========================================

  Widget _buildAdminHome(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("Resumen del negocio",
            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
        const SizedBox(height: 4),
        Text(
          "Productos en catálogo: ${_isLoadingAdmin ? '...' : _cantidadProductosAdmin}",
          style: TextStyle(fontSize: 13, color: Colors.grey[600]),
        ),
        const SizedBox(height: 20),

        _buildMetricasAcordeon(),

        const SizedBox(height: 32),
        const Row(children: [
          Icon(Icons.history_toggle_off, color: Colors.grey),
          SizedBox(width: 8),
          Text("Actividad reciente",
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        ]),
        const SizedBox(height: 12),

        ValueListenableBuilder<List<ActivityEvent>>(
          valueListenable: ActivityService.instance.activityNotifier,
          builder: (context, activities, _) {
            if (activities.isEmpty) {
              return Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Center(child: Text("No hay actividad reciente.",
                    style: TextStyle(color: Colors.grey))),
              );
            }
            return Column(
              children: activities.take(_maxRecentActivities).map((event) {
                return ActivityCard(
                  icon: event.icon, title: event.title,
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

  Widget _buildMetricasAcordeon() {
    if (_isLoadingAdmin) {
      return const Center(child: Padding(
        padding: EdgeInsets.symmetric(vertical: 24),
        child: CircularProgressIndicator(),
      ));
    }

    if (_hasAdminError) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(color: Colors.red[50], borderRadius: BorderRadius.circular(12)),
        child: const Row(children: [
          Icon(Icons.wifi_off, color: Colors.red),
          SizedBox(width: 8),
          Text("Sin conexión — mostrando datos guardados"),
        ]),
      );
    }

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Column(children: [
        _buildPeriodoTile(titulo: "Hoy",         icono: Icons.today,          color: Colors.blue,   metricas: _metricasHoy),
        Divider(height: 1, color: Colors.grey.shade200),
        _buildPeriodoTile(titulo: "Esta semana", icono: Icons.date_range,     color: Colors.orange, metricas: _metricasSemana),
        Divider(height: 1, color: Colors.grey.shade200),
        _buildPeriodoTile(titulo: "Este mes",    icono: Icons.calendar_month, color: Colors.purple, metricas: _metricasMes, isLast: true),
      ]),
    ).animate().fadeIn(duration: 300.ms);
  }

  Widget _buildPeriodoTile({
    required String titulo,
    required IconData icono,
    required Color color,
    required _MetricasPeriodo metricas,
    bool isLast = false,
  }) {
    return Theme(
      data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
      child: ExpansionTile(
        leading: CircleAvatar(
          backgroundColor: color.withAlpha(30),
          child: Icon(icono, color: color, size: 20),
        ),
        title: Text(titulo,
            style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
        subtitle: Text(
          "Bruto: $_currency${metricas.bruto.toStringAsFixed(2)}  ·  "
              "Ganancia: $_currency${metricas.ganancia.toStringAsFixed(2)}",
          style: TextStyle(fontSize: 11, color: Colors.grey[600]),
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(
            top: const Radius.circular(16),
            bottom: isLast ? const Radius.circular(16) : Radius.zero,
          ),
        ),
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            child: Column(children: [
              const Divider(),
              const SizedBox(height: 8),
              _buildMetricaRow(
                label: "Monto Bruto",
                descripcion: "Total vendido (precio × cantidad)",
                valor: metricas.bruto,
                color: Colors.blue,
                icono: Icons.point_of_sale,
              ),
              const SizedBox(height: 12),
              _buildMetricaRow(
                label: "Ganancia Real",
                descripcion: "Precio venta − Precio compra",
                valor: metricas.ganancia,
                color: metricas.ganancia >= 0 ? Colors.green : Colors.red,
                icono: Icons.trending_up,
              ),
              const SizedBox(height: 12),
              _buildMargenRow(metricas),
            ]),
          ),
        ],
      ),
    );
  }

  Widget _buildMetricaRow({
    required String label,
    required String descripcion,
    required double valor,
    required Color color,
    required IconData icono,
  }) {
    return Row(children: [
      Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: color.withAlpha(25),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Icon(icono, color: color, size: 18),
      ),
      const SizedBox(width: 12),
      Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(label, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
        Text(descripcion, style: TextStyle(fontSize: 11, color: Colors.grey[500])),
      ])),
      Text(
        "$_currency${valor.toStringAsFixed(2)}",
        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: color),
      ),
    ]);
  }

  Widget _buildMargenRow(_MetricasPeriodo metricas) {
    final margen = metricas.bruto > 0
        ? (metricas.ganancia / metricas.bruto * 100)
        : 0.0;
    final color = margen >= 20 ? Colors.green : margen >= 10 ? Colors.orange : Colors.red;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: color.withAlpha(20),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(children: [
            Icon(Icons.percent, size: 16, color: color),
            const SizedBox(width: 6),
            Text("Margen de ganancia",
                style: TextStyle(fontSize: 12, color: color, fontWeight: FontWeight.w500)),
          ]),
          Text("${margen.toStringAsFixed(1)}%",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: color)),
        ],
      ),
    );
  }

  // ==========================================
  // 7. VISTA VENDEDOR
  // ==========================================

  Widget _buildUserHome(BuildContext context) {
    if (_isLoadingUser) {
      return const Center(child: Padding(
        padding: EdgeInsets.all(40), child: CircularProgressIndicator(),
      ));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("¡Promociones del Día!",
            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),

        if (_promociones.isEmpty)
          Container(
            padding: const EdgeInsets.all(24),
            width: double.infinity,
            decoration: BoxDecoration(color: Colors.blue[50], borderRadius: BorderRadius.circular(12)),
            child: const Column(children: [
              Icon(Icons.sentiment_satisfied_alt, color: Colors.blue, size: 40),
              SizedBox(height: 8),
              Text("No hay promociones hoy.\n¡Vuelve pronto!",
                  textAlign: TextAlign.center, style: TextStyle(color: Colors.blueGrey)),
            ]),
          )
        else
          SizedBox(
            height: _promoCardHeight,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              itemCount: _promociones.length,
              itemBuilder: (_, index) => _buildPromoCard(context, _promociones[index]),
            ),
          ),

        const SizedBox(height: 32),
        const Text("Catálogo de Productos",
            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),

        Card(
          elevation: 0,
          color: Colors.grey[100],
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: ListTile(
            leading: const CircleAvatar(
              backgroundColor: Colors.white,
              child: Icon(Icons.inventory, color: Colors.blueGrey),
            ),
            title: const Text("Total de productos en tienda"),
            trailing: Text("$_totalProductosCatalogo",
                style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
          ),
        ),
      ],
    ).animate().fadeIn(duration: 300.ms);
  }

  Widget _buildPromoCard(BuildContext context, Product product) {
    final theme = Theme.of(context);
    return Container(
      width: 250,
      margin: const EdgeInsets.only(right: 12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        gradient: LinearGradient(
          colors: [
            theme.colorScheme.primary.withAlpha(204),
            theme.colorScheme.primary,
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: [BoxShadow(
          color: theme.colorScheme.primary.withAlpha(76),
          blurRadius: 8, offset: const Offset(0, 4),
        )],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(product.name,
                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                maxLines: 1, overflow: TextOverflow.ellipsis),
            const Spacer(),
            Text("AHORA $_currency${product.salePrice!.toStringAsFixed(2)}",
                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20)),
            Text("Antes $_currency${product.price.toStringAsFixed(2)}",
                style: TextStyle(
                  color: Colors.white.withAlpha(204),
                  decoration: TextDecoration.lineThrough,
                  fontSize: 12,
                )),
          ],
        ),
      ),
    );
  }

  // ==========================================
  // 8. HELPERS
  // ==========================================

  void _mostrarDialogoSoporte() {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Row(children: [
          Icon(Icons.support_agent, color: Colors.blue),
          SizedBox(width: 10),
          Text('Soporte Técnico'),
        ]),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('¿Necesitas ayuda con el sistema?',
                style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            const Row(children: [
              Icon(Icons.phone, size: 16, color: Colors.grey),
              SizedBox(width: 8),
              Text(_supportPhone),
            ]),
            const SizedBox(height: 8),
            Row(children: [
              const Icon(Icons.email, size: 16, color: Colors.grey),
              const SizedBox(width: 8),
              Flexible(child: Text(_supportEmail, style: const TextStyle(fontSize: 13))),
            ]),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Cerrar'))
        ],
      ),
    );
  }

  String _formatTimeAgo(DateTime time) {
    final diff = DateTime.now().difference(time);
    if (diff.inDays > 0)    return 'Hace ${diff.inDays} d';
    if (diff.inHours > 0)   return 'Hace ${diff.inHours} h';
    if (diff.inMinutes > 0) return 'Hace ${diff.inMinutes} m';
    return 'Ahora';
  }
}

// ==========================================
// MODELO DE MÉTRICAS
// ==========================================

///  @immutable garantiza que no mute después de crearse
@immutable
class _MetricasPeriodo {
  final double bruto;    // Total vendido  = precio × cantidad
  final double ganancia; // Ganancia real  = (precio − precioCompra) × cantidad

  const _MetricasPeriodo({required this.bruto, required this.ganancia});

  factory _MetricasPeriodo.vacio() =>
      const _MetricasPeriodo(bruto: 0, ganancia: 0);
}