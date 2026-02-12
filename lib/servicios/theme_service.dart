import 'package:flutter/material.dart';

class ThemeService {
  // Singleton instance
  static final ThemeService _instance = ThemeService._internal();
  static ThemeService get instance => _instance;

  ThemeService._internal();

  // Color principal por defecto (Verde Teal)
  final ValueNotifier<Color> primaryColorNotifier =
      ValueNotifier(const Color(0xFF00695C));

  // Método para cambiar el color
  void updatePrimaryColor(Color newColor) {
    primaryColorNotifier.value = newColor;
  }

  // Colores predefinidos para elegir
  static const List<Color> availableColors = [
    Color(0xFF00695C), // Teal (Default)
    Color(0xFF1976D2), // Blue
    Color(0xFFC2185B), // Pink
    Color(0xFF7B1FA2), // Purple
    Color(0xFFE64A19), // Deep Orange
    Color(0xFF455A64), // Blue Grey
    Color(0xFF388E3C), // Green
  ];
}
