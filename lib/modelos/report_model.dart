class Report {
  final String id;
  final String productName; // Numero de boleta
  final int quantity;
  final double total;
  final double subtotal;
  final double igv;
  final DateTime date;
  //Campo para los detalles
  final List<dynamic> detalles;

  Report({
    required this.id,
    required this.productName,
    required this.quantity,
    required this.subtotal,
    required this.igv,
    required this.total,
    required this.date,
    this.detalles = const [], // Por defecto lista vacía
  });
}
