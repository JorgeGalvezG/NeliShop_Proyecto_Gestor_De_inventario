import 'dart:io';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:open_file/open_file.dart';
import '../modelos/report_model.dart';
import '../bridge_flutter.dart';
import '../widgets/stitch_loader.dart';
import '../widgets/optimized_image.dart'; // Asegúrate de tener este widget
import '../widgets/animated_list_item.dart'; // Para la animación de la lista
import '../config/core.dart'; // Para tipos genéricos

class ReportesPage extends StatefulWidget {
  const ReportesPage({super.key});

  @override
  State<ReportesPage> createState() => _ReportesPageState();
}

// Enum para ordenamiento (Genérico)
enum SortOption { fechaReciente, fechaAntigua, montoMayor, montoMenor }

class _ReportesPageState extends State<ReportesPage> {
  final BridgeFlutter _bridge = BridgeFlutter();

  // Datos
  List<Report> _allReports = [];      // Datos originales
  List<Report> _filteredReports = []; // Datos visibles (filtrados)

  // Selección
  final Set<String> _selectedIds = {};
  bool _isSelectionMode = false;

  // Filtros
  bool _isLoading = true;
  String _searchQuery = '';
  SortOption _currentSort = SortOption.fechaReciente;

  // --- CONFIGURACIÓN DE PLANTILLA (Datos de la Empresa) ---
  final String _nombreNegocio = "NeliShop Store";
  final String _rucNegocio = "20601234567";
  final String _direccion = "Av. Principal 123, Lima - Perú";

  final DateFormat _dateFormatter = DateFormat('dd/MM/yyyy HH:mm');

  @override
  void initState() {
    super.initState();
    _loadReports();
  }

  // --- 1. CARGA DE DATOS ---
  Future<void> _loadReports() async {
    if (!mounted) return;
    setState(() => _isLoading = true);

    try {
      final response = await _bridge.listarVentas();

      // Validación flexible para soportar tanto Map como BridgeResponse
      if (response.isSuccess) {
        final dataMap = response.data as Map<String, dynamic>;
        final List<dynamic> ventas = dataMap['ventas'] ?? [];

        // Construir lista de Reportes
        _allReports = ventas.map((v) {
          DateTime date = DateTime.now();
          try {
            if (v['fecha'] != null) date = DateTime.parse(v['fecha'].toString());
          } catch (_) {
            print("Error parseando fecha: ${v['fecha']}");
          }

          double total = (v['monto'] ?? 0).toDouble();
          // Calculamos IGV asumiendo que el monto incluye IGV (1.18)
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

        _applyFilters();
      }
    } catch (e) {
      print("Error loading reports: $e");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // --- 2. FILTROS Y ORDENAMIENTO ---
  void _applyFilters() {
    setState(() {
      // Filtrar
      _filteredReports = _allReports.where((report) {
        final searchLower = _searchQuery.toLowerCase();
        final matchesName = report.productName.toLowerCase().contains(searchLower);
        final matchesId = report.id.contains(searchLower);
        return matchesName || matchesId;
      }).toList();

      // Ordenar
      switch (_currentSort) {
        case SortOption.fechaReciente:
          _filteredReports.sort((a, b) => b.date.compareTo(a.date));
          break;
        case SortOption.fechaAntigua:
          _filteredReports.sort((a, b) => a.date.compareTo(b.date));
          break;
        case SortOption.montoMayor:
          _filteredReports.sort((a, b) => b.total.compareTo(a.total));
          break;
        case SortOption.montoMenor:
          _filteredReports.sort((a, b) => a.total.compareTo(b.total));
          break;
      }
    });
  }

  // --- 3. LÓGICA DE SELECCIÓN ---
  void _toggleSelection(String id) {
    setState(() {
      if (_selectedIds.contains(id)) {
        _selectedIds.remove(id);
      } else {
        _selectedIds.add(id);
      }
      _isSelectionMode = _selectedIds.isNotEmpty;
    });
  }

  void _selectAll() {
    setState(() {
      if (_selectedIds.length == _filteredReports.length) {
        _selectedIds.clear();
      } else {
        _selectedIds.addAll(_filteredReports.map((e) => e.id));
      }
      _isSelectionMode = _selectedIds.isNotEmpty;
    });
  }

  // --- 4. EXPORTAR PDF (INDIVIDUAL POR BOLETA) ---
  Future<void> _exportSelectedPdf() async {
    if (_selectedIds.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Selecciona al menos una venta')));
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Generando Boletas...')));

    final pdf = pw.Document();

    // Obtenemos solo los reportes seleccionados
    final selectedReports = _allReports.where((r) => _selectedIds.contains(r.id)).toList();

    // Loop: Creamos una página nueva para CADA venta seleccionada
    for (var report in selectedReports) {
      pdf.addPage(
        pw.Page(
          pageFormat: PdfPageFormat.a5, // Formato A5 (mitad de hoja) ideal para boletas
          margin: const pw.EdgeInsets.all(20),
          build: (pw.Context context) {
            return pw.Column(
              crossAxisAlignment: pw.CrossAxisAlignment.start,
              children: [
                // Cabecera Centrada
                pw.Center(
                    child: pw.Column(children: [
                      pw.Text(_nombreNegocio, style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 18)),
                      pw.Text("RUC: $_rucNegocio", style: const pw.TextStyle(fontSize: 10)),
                      pw.Text(_direccion, style: const pw.TextStyle(fontSize: 10)),
                      pw.SizedBox(height: 10),
                      pw.Text("BOLETA DE VENTA ELECTRÓNICA", style: pw.TextStyle(fontWeight: pw.FontWeight.bold)),
                      pw.Text(report.productName, style: const pw.TextStyle(fontSize: 14)),
                    ])
                ),
                pw.Divider(),

                // Datos Cliente/Fecha
                pw.Row(mainAxisAlignment: pw.MainAxisAlignment.spaceBetween, children: [
                  pw.Text("Fecha: ${_dateFormatter.format(report.date)}", style: const pw.TextStyle(fontSize: 10)),
                  pw.Text("Moneda: PEN", style: const pw.TextStyle(fontSize: 10)),
                ]),
                pw.SizedBox(height: 10),

                // Tabla de Productos
                pw.Table.fromTextArray(
                    context: context,
                    border: null,
                    headerStyle: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 9),
                    cellStyle: const pw.TextStyle(fontSize: 9),
                    headerDecoration: const pw.BoxDecoration(color: PdfColors.grey200),
                    cellAlignment: pw.Alignment.centerLeft,
                    headers: ['Cant', 'Descripción', 'P.Unit', 'Total'],
                    data: report.detalles.map((d) {
                      double precio = (d['precio'] ?? 0).toDouble();
                      int cant = (d['cantidad'] ?? 1);
                      return [
                        cant.toString(),
                        d['nombre'] ?? 'Producto',
                        precio.toStringAsFixed(2),
                        (precio * cant).toStringAsFixed(2),
                      ];
                    }).toList(),
                    columnWidths: {
                      0: const pw.FlexColumnWidth(1),
                      1: const pw.FlexColumnWidth(4),
                      2: const pw.FlexColumnWidth(2),
                      3: const pw.FlexColumnWidth(2),
                    }
                ),

                pw.Divider(),

                // Totales Alineados a la Derecha
                pw.Align(
                    alignment: pw.Alignment.centerRight,
                    child: pw.Column(
                        crossAxisAlignment: pw.CrossAxisAlignment.end,
                        children: [
                          pw.Text("Op. Gravada: S/${report.subtotal.toStringAsFixed(2)}", style: const pw.TextStyle(fontSize: 10)),
                          pw.Text("IGV (18%): S/${report.igv.toStringAsFixed(2)}", style: const pw.TextStyle(fontSize: 10)),
                          pw.SizedBox(height: 4),
                          pw.Text("IMPORTE TOTAL: S/${report.total.toStringAsFixed(2)}", style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 12)),
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

    try {
      final output = await getApplicationDocumentsDirectory();
      // Nombre único con timestamp
      final file = File("${output.path}/boletas_${DateTime.now().millisecondsSinceEpoch}.pdf");
      await file.writeAsBytes(await pdf.save());
      await OpenFile.open(file.path);

      // Limpiar selección tras exportar con éxito
      setState(() {
        _selectedIds.clear();
        _isSelectionMode = false;
      });
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error al crear PDF: $e'), backgroundColor: Colors.red));
    }
  }

  // --- 5. DETALLE VISUAL (CON FOTOS) ---
  void _showReportDetail(Report report) {
    showDialog(
      context: context,
      builder: (context) {
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

                  // Lista de Productos
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
                            // Imagen
                            Container(
                              width: 40, height: 40,
                              decoration: BoxDecoration(
                                  color: Colors.grey[100],
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(color: Colors.grey.shade300)
                              ),
                              child: ClipRRect(
                                borderRadius: BorderRadius.circular(8),
                                child: (d['imagePath'] != null && d['imagePath'].toString().isNotEmpty)
                                    ? OptimizedImage(imagePath: d['imagePath'])
                                    : const Icon(Icons.shopping_bag_outlined, color: Colors.grey, size: 20),
                              ),
                            ),
                            const SizedBox(width: 10),
                            // Textos
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

                  // Resumen Económico
                  _buildSummaryRow("Subtotal", report.subtotal),
                  _buildSummaryRow("IGV (18%)", report.igv),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text("TOTAL", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                      Text("S/${report.total.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.teal)),
                    ],
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text("Cerrar"),
            ),
            ElevatedButton.icon(
              icon: const Icon(Icons.print, size: 16),
              label: const Text("Imprimir"),
              onPressed: () {
                // Truco: seleccionamos temporalmente este solo reporte y exportamos
                setState(() {
                  _selectedIds.clear();
                  _selectedIds.add(report.id);
                });
                Navigator.pop(context);
                _exportSelectedPdf();
              },
            )
          ],
        );
      },
    );
  }

  Widget _buildSummaryRow(String label, double value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
          Text("S/${value.toStringAsFixed(2)}", style: const TextStyle(fontSize: 12)),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // APPBAR CAMBIANTE (Modo Normal vs Modo Selección)
      appBar: _isSelectionMode
          ? AppBar(
        backgroundColor: Colors.black87,
        iconTheme: const IconThemeData(color: Colors.white),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => setState(() { _selectedIds.clear(); _isSelectionMode = false; }),
        ),
        title: Text("${_selectedIds.length} seleccionados", style: const TextStyle(color: Colors.white, fontSize: 16)),
        actions: [
          IconButton(
            icon: const Icon(Icons.select_all),
            onPressed: _selectAll,
            tooltip: "Seleccionar Todo",
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 8),
            child: ElevatedButton.icon(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
              icon: const Icon(Icons.picture_as_pdf, size: 16),
              label: const Text("EXPORTAR PDF"),
              onPressed: _exportSelectedPdf,
            ),
          )
        ],
      )
          : AppBar(
        title: const Text('Historial de Ventas'),
        automaticallyImplyLeading: false,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(60),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
            child: Row(
              children: [
                // BUSCADOR
                Expanded(
                  child: TextField(
                    decoration: InputDecoration(
                      hintText: 'Buscar boleta...',
                      prefixIcon: const Icon(Icons.search, color: Colors.grey),
                      filled: true,
                      fillColor: Colors.grey[100],
                      contentPadding: EdgeInsets.zero,
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(30), borderSide: BorderSide.none),
                    ),
                    onChanged: (val) {
                      _searchQuery = val;
                      _applyFilters();
                    },
                  ),
                ),
                const SizedBox(width: 8),
                // FILTRO (POPUP MENU)
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
        ),
      ),

      body: _isLoading
          ? const StitchLoader()
          : _filteredReports.isEmpty
          ? Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.search_off, size: 60, color: Colors.grey[300]),
            const SizedBox(height: 10),
            Text("No se encontraron ventas", style: TextStyle(color: Colors.grey[500])),
          ],
        ),
      )
          : RefreshIndicator(
        onRefresh: _loadReports,
        child: ListView.builder(
          padding: const EdgeInsets.all(8),
          itemCount: _filteredReports.length,
          itemBuilder: (context, index) {
            final report = _filteredReports[index];
            final isSelected = _selectedIds.contains(report.id);

            return AnimatedListItem( // Usando tu widget de animación
              index: index,
              child: Card(
                elevation: isSelected ? 4 : 1,
                color: isSelected ? Colors.blue[50] : Colors.white,
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                    side: isSelected ? const BorderSide(color: Colors.blue, width: 2) : BorderSide.none
                ),
                margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 2),
                child: ListTile(
                  // Pulsación larga activa el modo selección
                  onLongPress: () => _toggleSelection(report.id),

                  // Checkbox siempre visible para que sea intuitivo
                  leading: Checkbox(
                    value: isSelected,
                    onChanged: (v) => _toggleSelection(report.id),
                    shape: const CircleBorder(),
                    activeColor: Colors.blue,
                  ),
                  title: Text(report.productName, style: const TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text(
                    "${_dateFormatter.format(report.date)} • ${report.detalles.length} items",
                    style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                  ),
                  trailing: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        "S/${report.total.toStringAsFixed(2)}",
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Colors.green),
                      ),
                      if (report.quantity > 1)
                        Text("Multi-item", style: TextStyle(fontSize: 10, color: Colors.grey[400])),
                    ],
                  ),
                  onTap: () {
                    if (_isSelectionMode) {
                      _toggleSelection(report.id);
                    } else {
                      _showReportDetail(report);
                    }
                  },
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}