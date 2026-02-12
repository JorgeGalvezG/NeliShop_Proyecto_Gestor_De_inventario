import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../servicios/theme_service.dart';
import '../servicios/preferences_service.dart'; //
import '../widgets/stitch_loader.dart';

class ConfiguracionPage extends StatelessWidget { // Ya no necesita ser StatefulWidget
  const ConfiguracionPage({super.key});

  @override
  Widget build(BuildContext context) {
    final prefs = PreferencesService.instance;

    return Scaffold(
      // El fondo se ajusta solo con el tema (claro/oscuro)
      appBar: AppBar(
        title: const Text('Ajustes'),
        centerTitle: true,
        elevation: 0,
        automaticallyImplyLeading: false,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // SECCIÓN 1: APARIENCIA
            _buildSectionHeader(context, "Apariencia", Icons.palette),
            const SizedBox(height: 10),

            // Tarjeta de Colores
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: _cardDecoration(context),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("Color del Tema", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 15),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: ThemeService.availableColors.map((color) {
                      return _buildColorCircle(color);
                    }).toList(),
                  ),
                ],
              ),
            ).animate().fadeIn().slideY(begin: 0.1, end: 0),

            const SizedBox(height: 25),

            // SECCIÓN 2: PREFERENCIAS (FUNCIONALES)
            _buildSectionHeader(context, "Preferencias", Icons.tune),
            const SizedBox(height: 10),

            Container(
              decoration: _cardDecoration(context),
              child: Column(
                children: [
                  // MODO OSCURO (CONECTADO)
                  ValueListenableBuilder<bool>(
                    valueListenable: prefs.darkModeNotifier,
                    builder: (context, isDark, _) => _buildSwitchTile(
                        context,
                        "Modo Oscuro",
                        Icons.dark_mode,
                        isDark,
                            (v) => prefs.toggleDarkMode(v)
                    ),
                  ),
                  const Divider(height: 1, indent: 20, endIndent: 20),

                  // SONIDOS (CONECTADO)
                  ValueListenableBuilder<bool>(
                    valueListenable: prefs.soundNotifier,
                    builder: (context, isSound, _) => _buildSwitchTile(
                        context,
                        "Sonidos de Venta",
                        Icons.volume_up,
                        isSound,
                            (v) => prefs.toggleSound(v)
                    ),
                  ),

                  const Divider(height: 1, indent: 20, endIndent: 20),

                  // NOTIFICACIONES (Guardar preferencia)
                  ValueListenableBuilder<bool>(
                    valueListenable: prefs.notificationsNotifier,
                    builder: (context, isNotif, _) => _buildSwitchTile(
                        context,
                        "Notificaciones",
                        Icons.notifications_active,
                        isNotif,
                            (v) => prefs.toggleNotifications(v)
                    ),
                  ),
                ],
              ),
            ).animate().fadeIn(delay: 200.ms).slideY(begin: 0.1, end: 0),
          ],
        ),
      ),
    );
  }

  // --- WIDGETS AUXILIARES ---

  BoxDecoration _cardDecoration(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return BoxDecoration(
      color: isDark ? const Color(0xFF2C2C2C) : Colors.white,
      borderRadius: BorderRadius.circular(20),
      boxShadow: [
        BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4))
      ],
    );
  }

  Widget _buildSectionHeader(BuildContext context, String title, IconData icon) {
    return Row(
      children: [
        Icon(icon, size: 20, color: Theme.of(context).colorScheme.primary),
        const SizedBox(width: 10),
        Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
      ],
    );
  }

  Widget _buildSwitchTile(BuildContext context, String title, IconData icon, bool value, Function(bool) onChanged) {
    return SwitchListTile(
      value: value,
      onChanged: onChanged,
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w500)),
      secondary: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
            color: Theme.of(context).brightness == Brightness.dark ? Colors.grey[800] : Colors.grey[100],
            borderRadius: BorderRadius.circular(8)
        ),
        child: Icon(icon, size: 20),
      ),
      activeColor: Theme.of(context).colorScheme.primary,
    );
  }

  Widget _buildColorCircle(Color color) {
    return GestureDetector(
      onTap: () => ThemeService.instance.updatePrimaryColor(color),
      child: ValueListenableBuilder<Color>(
        valueListenable: ThemeService.instance.primaryColorNotifier,
        builder: (context, currentColor, child) {
          final isSelected = currentColor.value == color.value;
          return AnimatedContainer(
            duration: const Duration(milliseconds: 300),
            width: isSelected ? 50 : 40,
            height: isSelected ? 50 : 40,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
              border: isSelected ? Border.all(color: Colors.white, width: 3) : null,
              boxShadow: [
                if (isSelected) BoxShadow(color: color.withOpacity(0.4), blurRadius: 10, offset: const Offset(0, 4))
              ],
            ),
            child: isSelected ? const Icon(Icons.check, color: Colors.white, size: 24) : null,
          );
        },
      ),
    );
  }
}