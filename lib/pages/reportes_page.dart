import 'dart:io';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:open_file/open_file.dart';

// Imports de tu proyecto
import '../config/core.dart';
import '../config/db_helper.dart';
import '../modelos/report_model.dart';
import '../bridge_flutter.dart';
import '../widgets/stitch_loader.dart';
import '../widgets/optimized_image.dart';
import '../widgets/animated_list_item.dart';

class ReportesPage extends StatefulWidget {
  const ReportesPage({super.key});

  @override
  State<ReportesPage> createState() => _ReportesPageState();
}

enum SortOption { fechaReciente, fechaAntigua, montoMayor, montoMenor }

class _ReportesPageState extends State<ReportesPage> with SingleTickerProviderStateMixin {
  final BridgeFlutter _bridge = BridgeFlutter();

  // ==========================================
  // 1. CONSTANTES (estáticas y constantes)
  // ==========================================
  static const String _nombreNegocio = "NeliShop Store";
  static const String _rucNegocio = "20601234567";
  static const String _direccion = "Av. Principal 123, Lima - Perú";

  // ==========================================
  // 2. VARIABLES DE ESTADO Y CONTROLADORES
  // ==========================================
  late TabController _tabController;
  final TextEditingController _searchCtrl = TextEditingController();

  // Datos
  List<Report> _allReports = [];
  List<Report> _filteredReports = [];

  // Variables de Resumen Financiero
  double _ventasHoy = 0.0;
  double _ventasMes = 0.0;
  double _ventasTotal = 0.0;
  int _boletasHoy = 0;

  // Selección ( _isSelectionMode ahora es un getter)
  final Set<String> _selectedIds = {};
  bool get _isSelectionMode => _selectedIds.isNotEmpty;

  // Filtros y Estados de UI
  bool _isLoading = true;
  bool _isRefreshing = false; // Flag para recargas manuales
  bool _isExporting = false;  // Flag para evitar múltiples PDFs simultáneos
  SortOption _currentSort = SortOption.fechaReciente;
  static final DateFormat _dateFormatter = DateFormat('dd/MM/yyyy HH:mm');

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);

    // Sincronía total entre el TextField y la búsqueda
    _searchCtrl.addListener(() {
      _applyFilters();
    });

    _loadReportsOfflineFirst();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _searchCtrl.dispose(); // El listener se destruye automáticamente aquí
    super.dispose();
  }

  // ==========================================
  // 3. CARGA DE DATOS (OFFLINE-FIRST)
  // ==========================================

  Future<void> _loadReportsOfflineFirst() async {
    if (!mounted) return;

    if (_allReports.isEmpty) {
      setState(() => _isLoading = true);
    } else {
      setState(() => _isRefreshing = true);
    }

    try {
      final localData = await DBHelper.instance.getVentasHistorialLocal();
      if (localData.isNotEmpty && mounted) {
        _processData(localData);
      }

      final response = await _bridge.listarVentas();
      final rawVentas = response.dataList;
      if (rawVentas.isNotEmpty && mounted) {
        await DBHelper.instance.syncVentasHistorial(rawVentas);
        final updatedLocal = await DBHelper.instance.getVentasHistorialLocal();
        if (mounted) _processData(updatedLocal);
      } else if (rawVentas.isEmpty && mounted) {
        _processData([]);
      }
    } catch (e) {
      debugPrint("Error en Offline-First (Reportes): $e");
      if (mounted) {
        setState(() {
          _isLoading = false;
          _isRefreshing = false;
        });
      }
    }
  }

  void _processData(List<JsonMap> ventasRaw) {
    List<Report> tempReports = ventasRaw.map((v) {
      DateTime date = DateTime.now();
      try {
        if (v['fecha'] != null) date = DateTime.parse(v['fecha'].toString());
      } catch (e) {
        //  Log explícito del error silencioso
        debugPrint("Fecha inválida: ${v['fecha']} — $e");
      }

      double total = (v['monto'] ?? 0).toDouble();
      double subtotal = total / 1.18;
      double igv = total - subtotal;

      return Report(
        id: v['id'].toString(),
        productName: v['numeroBoleta'] ?? "Ticket #${v['id']}",
        quantity: 1,
        total: total,
        subtotal: subtotal,
        igv: igv,
        date: date,
        detalles: v['detalles'] ?? [],
      );
    }).toList();

    _calculateMetrics(tempReports);
    _allReports = tempReports; // Asignamos sin setState aún

    //  _applyFilters se encarga del único setState
    if(mounted) _applyFilters();
  }

  // ==========================================
  // 4. LÓGICA DE NEGOCIO Y CÁLCULOS
  // ==========================================

  void _calculateMetrics(List<Report> reports) {
    final now = DateTime.now();
    double hVentas = 0, mVentas = 0, tVentas = 0;
    int hBoletas = 0;

    for (var r in reports) {
      tVentas += r.total;
      if (r.date.year == now.year && r.date.month == now.month) {
        mVentas += r.total;
      }
      if (r.date.year == now.year && r.date.month == now.month && r.date.day == now.day) {
        hVentas += r.total;
        hBoletas++;
      }
    }

    _ventasHoy = hVentas;
    _ventasMes = mVentas;
    _ventasTotal = tVentas;
    _boletasHoy = hBoletas;
  }

  void _applyFilters() {
    //  Único setState que maneja la UI completa post-procesamiento
    setState(() {
      final searchLower = _searchCtrl.text.trim().toLowerCase();

      _filteredReports = _allReports.where((report) {
        return report.productName.toLowerCase().contains(searchLower) ||
            report.id.contains(searchLower);
      }).toList();

      switch (_currentSort) {
        case SortOption.fechaReciente: _filteredReports.sort((a, b) => b.date.compareTo(a.date)); break;
        case SortOption.fechaAntigua: _filteredReports.sort((a, b) => a.date.compareTo(b.date)); break;
        case SortOption.montoMayor: _filteredReports.sort((a, b) => b.total.compareTo(a.total)); break;
        case SortOption.montoMenor: _filteredReports.sort((a, b) => a.total.compareTo(b.total)); break;
      }

      _isLoading = false;
      _isRefreshing = false;
    });
  }

  void _toggleSelection(String id) {
    setState(() {
      if (_selectedIds.contains(id)) _selectedIds.remove(id);
      else _selectedIds.add(id);
    });
  }

  void _selectAll() {
    setState(() {
      if (_selectedIds.length == _filteredReports.length) _selectedIds.clear();
      else _selectedIds.addAll(_filteredReports.map((e) => e.id));
    });
  }

  // ==========================================
  // 5. EXPORTACIÓN PDF
  // ==========================================

  Future<void> _exportSelectedPdf() async {
    //  Bloqueo de concurrencia para no generar múltiples PDFs
    if (_selectedIds.isEmpty || _isExporting) return;

    setState(() => _isExporting = true);
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Generando Boletas...')));

    try {
      final pdf = pw.Document();
      final selectedReports = _allReports.where((r) => _selectedIds.contains(r.id)).toList();

      for (var report in selectedReports) {
        pdf.addPage(
          pw.Page(
            pageFormat: PdfPageFormat.a5,
            margin: const pw.EdgeInsets.all(20),
            // Renombrado a pdfContext para evitar shadowing
            build: (pw.Context pdfContext) {
              return pw.Column(
                crossAxisAlignment: pw.CrossAxisAlignment.start,
                children: [
                  pw.Center(
                      child: pw.Column(children: [
                        pw.Text(_nombreNegocio, style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 18)),
                        pw.Text("RUC: $_rucNegocio", style: const pw.TextStyle(fontSize: 10)),
                        pw.Text(_direccion, style: const pw.TextStyle(fontSize: 10)),
                        pw.SizedBox(height: 10),
                        pw.Text("BOLETA DE VENTA", style: pw.TextStyle(fontWeight: pw.FontWeight.bold)),
                        pw.Text(report.productName, style: const pw.TextStyle(fontSize: 14)),
                      ])
                  ),
                  pw.Divider(),
                  pw.Row(mainAxisAlignment: pw.MainAxisAlignment.spaceBetween, children: [
                    pw.Text("Fecha: ${_dateFormatter.format(report.date)}", style: const pw.TextStyle(fontSize: 10)),
                    pw.Text("Moneda: PEN", style: const pw.TextStyle(fontSize: 10)),
                  ]),
                  pw.SizedBox(height: 10),
                  pw.Table.fromTextArray(
                      context: pdfContext,
                      border: null,
                      headerStyle: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 9),
                      cellStyle: const pw.TextStyle(fontSize: 9),
                      headerDecoration: const pw.BoxDecoration(color: PdfColors.grey200),
                      cellAlignment: pw.Alignment.centerLeft,
                      headers: ['Cant', 'Descripción', 'P.Unit', 'Total'],
                      data: report.detalles.map((d) {
                        double precio = (d['precio'] ?? 0).toDouble();
                        int cant = (d['cantidad'] ?? 1);
                        return [cant.toString(), d['nombre'] ?? 'Producto', precio.toStringAsFixed(2), (precio * cant).toStringAsFixed(2)];
                      }).toList(),
                      columnWidths: {0: const pw.FlexColumnWidth(1), 1: const pw.FlexColumnWidth(4), 2: const pw.FlexColumnWidth(2), 3: const pw.FlexColumnWidth(2)}
                  ),
                  pw.Divider(),
                  pw.Align(
                      alignment: pw.Alignment.centerRight,
                      child: pw.Column(
                          crossAxisAlignment: pw.CrossAxisAlignment.end,
                          children: [
                            pw.Text("Subtotal: S/${report.subtotal.toStringAsFixed(2)}", style: const pw.TextStyle(fontSize: 10)),
                            pw.Text("IGV (18%): S/${report.igv.toStringAsFixed(2)}", style: const pw.TextStyle(fontSize: 10)),
                            pw.SizedBox(height: 4),
                            pw.Text("TOTAL: S/${report.total.toStringAsFixed(2)}", style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 12)),
                          ]
                      )
                  ),
                  pw.Spacer(),
                  pw.Center(child: pw.Text("Gracias por su preferencia", style: const pw.TextStyle(color: PdfColors.grey, fontSize: 8))),
                ],
              );
            },
          ),
        );
      }

      final output = await getApplicationDocumentsDirectory();
      final file = File("${output.path}/boletas_${DateTime.now().millisecondsSinceEpoch}.pdf");
      await file.writeAsBytes(await pdf.save());
      await OpenFile.open(file.path);

      if (mounted) {
        setState(() {
          _selectedIds.clear();
        });
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error al crear PDF: $e'), backgroundColor: Colors.red));
    } finally {
      if (mounted) setState(() => _isExporting = false);
    }
  }

  // ==========================================
  // 6. INTERFAZ GRÁFICA PRINCIPAL
  // ==========================================

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: _isSelectionMode
          ? AppBar(
        backgroundColor: Colors.blueGrey[900],
        iconTheme: const IconThemeData(color: Colors.white),
        leading: IconButton(icon: const Icon(Icons.close), onPressed: () => setState(() => _selectedIds.clear())),
        title: Text("${_selectedIds.length} seleccionados", style: const TextStyle(color: Colors.white, fontSize: 16)),
        actions: [
          IconButton(icon: const Icon(Icons.select_all), onPressed: _selectAll, tooltip: "Seleccionar Todo"),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 8),
            child: ElevatedButton.icon(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
              icon: _isExporting ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2)) : const Icon(Icons.picture_as_pdf, size: 16),
              label: const Text("EXPORTAR PDF"),
              onPressed: _isExporting ? null : _exportSelectedPdf,
            ),
          )
        ],
      )
          : AppBar(
        title: const Text('Reportes y Finanzas'),
        automaticallyImplyLeading: false,
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadReportsOfflineFirst)
        ],
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.dashboard), text: 'Resumen'),
            Tab(icon: Icon(Icons.receipt_long), text: 'Boletas & PDF'),
          ],
        ),
      ),
      body: _isLoading
          ? const StitchLoader()
          : Column(
        children: [
          //Indicador visual sutil para recargas manuales
          if (_isRefreshing) const LinearProgressIndicator(minHeight: 3),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildDashboardTab(),
                _buildHistorialPdfTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ==========================================
  // 7. WIDGETS DE PESTAÑAS Y DIÁLOGOS
  // ==========================================

  Widget _buildDashboardTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text("Rendimiento de Ventas", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),

          Row(
            children: [
              Expanded(child: _buildMetricCard("Ventas de Hoy", _ventasHoy, Icons.today, Colors.orange)),
              const SizedBox(width: 12),
              Expanded(child: _buildMetricCard("Ventas del Mes", _ventasMes, Icons.calendar_month, Colors.blue)),
            ],
          ),
          const SizedBox(height: 12),

          Card(
            elevation: 2,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text("Total Histórico", style: TextStyle(color: Colors.grey, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      Text("S/${_ventasTotal.toStringAsFixed(2)}", style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: Colors.green)),
                    ],
                  ),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(color: Colors.green.withValues(alpha: 0.1), shape: BoxShape.circle),
                    child: const Icon(Icons.account_balance_wallet, color: Colors.green, size: 32),
                  )
                ],
              ),
            ),
          ),

          const SizedBox(height: 24),
          const Text("Métricas del Día", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),

          ListTile(
            tileColor: Colors.white,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            leading: const CircleAvatar(backgroundColor: Colors.amber, child: Icon(Icons.receipt, color: Colors.white)),
            title: const Text("Boletas emitidas hoy"),
            trailing: Text("$_boletasHoy", style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
          )
        ],
      ),
    );
  }

  Widget _buildMetricCard(String title, double amount, IconData icon, Color color) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: color),
            const SizedBox(height: 12),
            Text(title, style: const TextStyle(color: Colors.grey, fontSize: 13, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text("S/${amount.toStringAsFixed(2)}", style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
          ],
        ),
      ),
    );
  }

  Widget _buildHistorialPdfTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _searchCtrl,
                  decoration: InputDecoration(
                    hintText: 'Buscar boleta...',
                    prefixIcon: const Icon(Icons.search, color: Colors.grey),
                    filled: true, fillColor: Colors.grey[100],
                    contentPadding: EdgeInsets.zero,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(30), borderSide: BorderSide.none),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              PopupMenuButton<SortOption>(
                icon: const Icon(Icons.filter_list_alt, color: Colors.black87),
                onSelected: (SortOption result) {
                  _currentSort = result;
                  _applyFilters();
                },
                itemBuilder: (BuildContext context) => <PopupMenuEntry<SortOption>>[
                  const PopupMenuItem(value: SortOption.fechaReciente, child: Text('Más Recientes')),
                  const PopupMenuItem(value: SortOption.fechaAntigua, child: Text('Más Antiguos')),
                  const PopupMenuItem(value: SortOption.montoMayor, child: Text('Mayor Monto')),
                  const PopupMenuItem(value: SortOption.montoMenor, child: Text('Menor Monto')),
                ],
              ),
            ],
          ),
        ),
        Expanded(
          child: _filteredReports.isEmpty
              ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(Icons.search_off, size: 60, color: Colors.grey[300]), const Text("No se encontraron ventas")]))
              : RefreshIndicator(
            onRefresh: _loadReportsOfflineFirst,
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              itemCount: _filteredReports.length,
              itemBuilder: (context, index) {
                final report = _filteredReports[index];
                final isSelected = _selectedIds.contains(report.id);

                return AnimatedListItem(
                  index: index,
                  child: Card(
                    elevation: isSelected ? 4 : 1,
                    color: isSelected ? Colors.blue[50] : Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12), side: isSelected ? const BorderSide(color: Colors.blue, width: 2) : BorderSide.none),
                    margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 2),
                    child: ListTile(
                      onLongPress: () => _toggleSelection(report.id),
                      leading: Checkbox(value: isSelected, onChanged: (v) => _toggleSelection(report.id), shape: const CircleBorder(), activeColor: Colors.blue),
                      title: Text(report.productName, style: const TextStyle(fontWeight: FontWeight.bold)),
                      subtitle: Text("${_dateFormatter.format(report.date)} • ${report.detalles.length} items", style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                      trailing: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text("S/${report.total.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Colors.green)),
                          if (report.detalles.length > 1) Text("Multi-item", style: TextStyle(fontSize: 10, color: Colors.grey[400])),
                        ],
                      ),
                      onTap: () {
                        if (_isSelectionMode) _toggleSelection(report.id);
                        else _showReportDetail(report);
                      },
                    ),
                  ),
                );
              },
            ),
          ),
        ),
      ],
    );
  }

  void _showReportDetail(Report report) {
    showDialog(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: Row(
            children: [
              const Icon(Icons.receipt, color: Colors.teal),
              const SizedBox(width: 8),
              Expanded(child: Text(report.productName, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold))),
            ],
          ),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text("Fecha: ${_dateFormatter.format(report.date)}", style: TextStyle(color: Colors.grey[600], fontSize: 12)),
                  const Divider(height: 20),
                  if (report.detalles.isEmpty)
                    const Text("Sin detalles registrados", style: TextStyle(fontStyle: FontStyle.italic))
                  else
                    ...report.detalles.map((d) {
                      final nombre = d['nombre'] ?? 'Producto';
                      final cant = d['cantidad'] ?? 1;
                      final precio = (d['precio'] ?? 0).toDouble();
                      final subTotalItem = precio * cant;

                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 6),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Container(
                              width: 40, height: 40,
                              decoration: BoxDecoration(color: Colors.grey[100], borderRadius: BorderRadius.circular(8), border: Border.all(color: Colors.grey.shade300)),
                              child: ClipRRect(
                                borderRadius: BorderRadius.circular(8),
                                child: (d['imagePath'] != null && d['imagePath'].toString().trim().isNotEmpty)
                                    ? OptimizedImage(imagePath: d['imagePath'])
                                    : const Icon(Icons.shopping_bag_outlined, color: Colors.grey, size: 20),
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(nombre, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                                  Text("$cant unids. x S/$precio", style: const TextStyle(fontSize: 11, color: Colors.grey)),
                                ],
                              ),
                            ),
                            Text("S/${subTotalItem.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold)),
                          ],
                        ),
                      );
                    }).toList(),
                  const Divider(height: 20),
                  Padding(padding: const EdgeInsets.symmetric(vertical: 2), child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [const Text("Subtotal", style: TextStyle(color: Colors.grey, fontSize: 12)), Text("S/${report.subtotal.toStringAsFixed(2)}", style: const TextStyle(fontSize: 12))])),
                  Padding(padding: const EdgeInsets.symmetric(vertical: 2), child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [const Text("IGV (18%)", style: TextStyle(color: Colors.grey, fontSize: 12)), Text("S/${report.igv.toStringAsFixed(2)}", style: const TextStyle(fontSize: 12))])),
                  const SizedBox(height: 8),
                  Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [const Text("TOTAL", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)), Text("S/${report.total.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.teal))]),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text("Cerrar")),
            ElevatedButton.icon(
              icon: const Icon(Icons.print, size: 16),
              label: const Text("Imprimir"),
              onPressed: () {
                setState(() { _selectedIds.clear(); _selectedIds.add(report.id); });
                Navigator.pop(dialogContext);
                _exportSelectedPdf();
              },
            )
          ],
        );
      },
    );
  }
}