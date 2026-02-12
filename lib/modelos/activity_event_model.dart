import 'package:flutter/material.dart';

// Este modelo define la estructura de un evento de actividad
// (como una transacción de Yape)
class ActivityEvent {
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final DateTime timestamp;

  ActivityEvent({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.timestamp,
  });
}