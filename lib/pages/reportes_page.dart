import 'dart:io';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw; // Librería para crear el PDF
import 'package:open_file/open_file.dart'; // Librería para abrir el PDF
import '../modelos/report_model.dart';
import '../bridge_flutter.dart';
import '../widgets/animated_list_item.dart';
import '../widgets/optimized_image.dart'; // Para las imágenes en el detalle
import '../widgets/stitch_loader.dart';
import '../config/core.dart';

class ReportesPage extends StatefulWidget {
  const ReportesPage({super.key});

  @override
  State<ReportesPage> createState() => _ReportesPageState();
}

class _ReportesPageState extends State<ReportesPage> {
  final BridgeFlutter _bridge = BridgeFlutter();
  List<Report> _allReports = [];
  bool _isLoading = true;

  // Formato de fecha para Perú (día/mes/año hora:minuto AM/PM)
  final DateFormat _dateFormatter = DateFormat('dd/MM/yyyy hh:mm a');

  @override
  void initState() {
    super.initState();
    _loadReports();
  }

  Future<void> _loadReports() async {
    if (!mounted) return;
    setState(() => _isLoading = true);

    try {
      final response = await _bridge.listarVentas();

      if (response['status'] == 'ok') {
        final List<dynamic> ventas = response['ventas'] ?? [];

        _allReports = ventas.map((v) {
          DateTime date = DateTime.now();
          try {
            if (v['fecha'] != null) {
              date = DateTime.parse(v['fecha'].toString());
            }
          } catch (_) {}

          double total = (v['monto'] ?? 0).toDouble();
          double subtotal = total / 1.18;
          double igv = total - subtotal;

          return Report(
            id: v['id'].toString(),
            productName: v['numeroBoleta'] ?? "Venta #${v['id']}",
            quantity: 1,
            total: total,
            subtotal: subtotal,
            igv: igv,
            date: date,
            detalles: v['detalles'] ?? [], // Lista de productos vendidos
          );
        }).toList();

        // Ordenar por fecha: más recientes arriba
        _allReports.sort((a, b) => b.date.compareTo(a.date));
      }
    } catch (e) {
      print("Error loading reports: $e");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // --- 1. FUNCIÓN DE GENERAR PDF REAL (IMPLEMENTADA) ---
  Future<void> _generateAndOpenPdf() async {
    if (_allReports.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('No hay datos para generar el reporte'))
      );
      return;
    }

    // Mostrar feedback visual
    ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Generando PDF...'), duration: Duration(seconds: 1))
    );

    final pdf = pw.Document();

    // Construcción del documento PDF
    pdf.addPage(
      pw.MultiPage(
        pageFormat: PdfPageFormat.a4,
        margin: const pw.EdgeInsets.all(32),
        build: (pw.Context context) {
          return [
            // Encabezado del PDF
            pw.Header(
                level: 0,
                child: pw.Row(
                    mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                    children: [
                      pw.Text('Reporte General de Ventas', style: pw.TextStyle(fontSize: 24, fontWeight: pw.FontWeight.bold)),
                      pw.Text('NeliShop', style: pw.TextStyle(fontSize: 18, color: PdfColors.grey)),
                    ]
                )
            ),
            pw.SizedBox(height: 20),
            pw.Text('Fecha de emisión: ${_dateFormatter.format(DateTime.now())}'),
            pw.Text('Total de transacciones: ${_allReports.length}'),
            pw.SizedBox(height: 20),

            // Tabla de Datos
            pw.Table.fromTextArray(
              context: context,
              border: null, // Sin bordes verticales feos
              headerStyle: pw.TextStyle(fontWeight: pw.FontWeight.bold, color: PdfColors.white),
              headerDecoration: const pw.BoxDecoration(color: PdfColors.teal),
              rowDecoration: const pw.BoxDecoration(border: pw.Border(bottom: pw.BorderSide(color: PdfColors.grey300, width: 0.5))),
              cellAlignment: pw.Alignment.centerLeft,
              cellPadding: const pw.EdgeInsets.all(5),

              // Columnas
              headers: ['Fecha', 'Boleta / ID', 'Subtotal', 'IGV', 'Total'],

              // Filas (Datos)
              data: _allReports.map((report) => [
                _dateFormatter.format(report.date),
                report.productName,
                'S/${report.subtotal.toStringAsFixed(2)}',
                'S/${report.igv.toStringAsFixed(2)}',
                'S/${report.total.toStringAsFixed(2)}',
              ]).toList(),
            ),

            pw.Padding(padding: const pw.EdgeInsets.all(10)),

            // Pie de página con Total General
            pw.Row(
              mainAxisAlignment: pw.MainAxisAlignment.end,
              children: [
                pw.Text(
                  'Ingreso Total: S/${_allReports.fold(0.0, (sum, r) => sum + r.total).toStringAsFixed(2)}',
                  style: pw.TextStyle(fontSize: 18, fontWeight: pw.FontWeight.bold, color: PdfColors.teal),
                ),
              ],
            )
          ];
        },
      ),
    );

    try {
      // 1. Obtener ruta de documentos
      final output = await getApplicationDocumentsDirectory();
      // 2. Crear archivo con nombre único
      final file = File("${output.path}/reporte_ventas_${DateTime.now().millisecondsSinceEpoch}.pdf");
      // 3. Escribir los bytes del PDF
      await file.writeAsBytes(await pdf.save());

      // 4. Abrir el archivo
      await OpenFile.open(file.path);

      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('PDF generado correctamente'), backgroundColor: Colors.green)
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error al abrir PDF: $e'), backgroundColor: Colors.red)
      );
      print(e);
    }
  }

  // --- 2. MOSTRAR DETALLE CON FOTOS ---
  void _showReportDetail(Report report) {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: Column(
            children: [
              const Icon(Icons.receipt_long, size: 40, color: Colors.teal),
              const SizedBox(height: 8),
              Text(report.productName, style: const TextStyle(fontWeight: FontWeight.bold)),
            ],
          ),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text("Fecha: ${_dateFormatter.format(report.date)}", style: const TextStyle(color: Colors.grey, fontSize: 12)),
                  const Divider(),
                  const Text("Productos:", style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 10),

                  // LISTA DE PRODUCTOS VENDIDOS
                  if (report.detalles.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(8.0),
                      child: Text("Sin detalles disponibles", style: TextStyle(fontStyle: FontStyle.italic)),
                    )
                  else
                    ...report.detalles.map((d) {
                      return Container(
                        margin: const EdgeInsets.only(bottom: 8),
                        child: Row(
                          children: [
                            // IMAGEN DEL PRODUCTO
                            ClipRRect(
                              borderRadius: BorderRadius.circular(6),
                              child: SizedBox(
                                width: 40,
                                height: 40,
                                // Si viene la imagen, la mostramos, si no, un icono gris
                                child: (d['imagePath'] != null && d['imagePath'].toString().isNotEmpty)
                                    ? OptimizedImage(imagePath: d['imagePath'])
                                    : Container(color: Colors.grey[200], child: const Icon(Icons.image, size: 20, color: Colors.grey)),
                              ),
                            ),
                            const SizedBox(width: 10),

                            // DATOS DEL PRODUCTO
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(d['nombre'] ?? 'Producto', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                                  Text("x${d['cantidad']} unid.", style: const TextStyle(fontSize: 11, color: Colors.grey)),
                                ],
                              ),
                            ),
                            Text("S/${d['precio']}", style: const TextStyle(fontWeight: FontWeight.bold)),
                          ],
                        ),
                      );
                    }),

                  const Divider(),
                  _buildSummaryRow("Subtotal:", report.subtotal),
                  _buildSummaryRow("IGV (18%):", report.igv),
                  const SizedBox(height: 10),
                  Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text("TOTAL:", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                        Text("S/${report.total.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: Colors.green))
                      ]
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: const Text("Cerrar")),
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
          Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
          Text("S/${value.toStringAsFixed(2)}", style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Reportes de Ventas'),
        automaticallyImplyLeading: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadReports,
            tooltip: "Actualizar",
          ),
          IconButton(
            icon: const Icon(Icons.picture_as_pdf),
            onPressed: _generateAndOpenPdf, // <--- AQUÍ SE LLAMA A LA FUNCIÓN
            tooltip: "Exportar PDF",
          ),
        ],
      ),
      body: _isLoading
          ? const StitchLoader()
          : _allReports.isEmpty
          ? Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.bar_chart, size: 80, color: Colors.grey[300]),
            const SizedBox(height: 16),
            Text("No hay ventas registradas", style: TextStyle(fontSize: 18, color: Colors.grey[500])),
          ],
        ),
      )
          : RefreshIndicator(
        onRefresh: _loadReports,
        child: ListView.builder(
          padding: const EdgeInsets.all(12),
          itemCount: _allReports.length,
          itemBuilder: (context, index) {
            final report = _allReports[index];
            return AnimatedListItem(
              index: index,
              child: Card(
                elevation: 2,
                margin: const EdgeInsets.only(bottom: 10),
                child: ListTile(
                  onTap: () => _showReportDetail(report), // Al tocar, abre el detalle
                  leading: CircleAvatar(
                    backgroundColor: Colors.green[50],
                    child: const Icon(Icons.receipt_long, color: Colors.green),
                  ),
                  title: Text(report.productName, style: const TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text(_dateFormatter.format(report.date)),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        'S/${report.total.toStringAsFixed(2)}',
                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Theme.of(context).colorScheme.primary),
                      ),
                      const SizedBox(width: 8),
                      const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.grey)
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}