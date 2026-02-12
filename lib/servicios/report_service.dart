import 'package:flutter/foundation.dart';
import '../modelos/report_model.dart';

class ReportService {
  // Singleton
  ReportService._privateConstructor();
  static final ReportService instance = ReportService._privateConstructor();

  final ValueNotifier<List<Report>> reportsNotifier = ValueNotifier<List<Report>>([]);

  void addReport(Report report) {
    final currentReports = reportsNotifier.value;
    reportsNotifier.value = [report, ...currentReports];
  }

  List<Report> getReports() {
    return reportsNotifier.value;
  }
}