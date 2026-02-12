import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class PreferencesService {
  // Singleton
  static final PreferencesService _instance = PreferencesService._internal();
  static PreferencesService get instance => _instance;
  PreferencesService._internal();

  // Notificadores para que la UI reaccione
  final ValueNotifier<bool> darkModeNotifier = ValueNotifier(false);
  final ValueNotifier<bool> soundNotifier = ValueNotifier(true);
  final ValueNotifier<bool> notificationsNotifier = ValueNotifier(true);

  // Inicializar cargando datos guardados
  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    darkModeNotifier.value = prefs.getBool('darkMode') ?? false;
    soundNotifier.value = prefs.getBool('sound') ?? true;
    notificationsNotifier.value = prefs.getBool('notifications') ?? true;
  }

  // --- MÉTODOS PARA CAMBIAR Y GUARDAR ---

  Future<void> toggleDarkMode(bool value) async {
    darkModeNotifier.value = value;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('darkMode', value);
  }

  Future<void> toggleSound(bool value) async {
    soundNotifier.value = value;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('sound', value);
  }

  Future<void> toggleNotifications(bool value) async {
    notificationsNotifier.value = value;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('notifications', value);
  }
}