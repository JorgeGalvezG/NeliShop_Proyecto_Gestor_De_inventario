import 'package:flutter/material.dart';
import 'screens/login_screen.dart';
import 'servicios/theme_service.dart';
import 'servicios/preferences_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Inicializamos las preferencias antes de arrancar la app
  await PreferencesService.instance.init();
  runApp(const StitchApp());
}

class StitchApp extends StatelessWidget {
  const StitchApp({super.key});

  @override
  Widget build(BuildContext context) {
    // 1. Escuchamos el cambio de Color Principal
    return ValueListenableBuilder<Color>(
      valueListenable: ThemeService.instance.primaryColorNotifier,
      builder: (context, primaryColor, _) {

        // 2. Escuchamos el cambio de Modo Oscuro
        return ValueListenableBuilder<bool>(
          valueListenable: PreferencesService.instance.darkModeNotifier,
          builder: (context, isDark, _) {

            return MaterialApp(
              title: 'Stitch Design',
              debugShowCheckedModeBanner: false,
              // Definimos el Tema Claro
              theme: ThemeData(
                brightness: Brightness.light,
                fontFamily: 'Inter',
                colorScheme: ColorScheme.fromSeed(
                  seedColor: primaryColor,
                  brightness: Brightness.light,
                ),
                useMaterial3: true,
                appBarTheme: AppBarTheme(
                  backgroundColor: Colors.white,
                  foregroundColor: primaryColor,
                  iconTheme: IconThemeData(color: primaryColor),
                ),
              ),
              // Definimos el Tema Oscuro
              darkTheme: ThemeData(
                brightness: Brightness.dark,
                fontFamily: 'Inter',
                colorScheme: ColorScheme.fromSeed(
                  seedColor: primaryColor,
                  brightness: Brightness.dark, // Importante
                  surface: const Color(0xFF121212), // Fondo oscuro
                ),
                useMaterial3: true,
                appBarTheme: AppBarTheme(
                  backgroundColor: const Color(0xFF1E1E1E),
                  foregroundColor: Colors.white,
                  iconTheme: const IconThemeData(color: Colors.white),
                ),
                scaffoldBackgroundColor: const Color(0xFF121212),
              ),
              // Aquí la magia: Flutter decide cuál usar según la variable
              themeMode: isDark ? ThemeMode.dark : ThemeMode.light,
              home: const LoginScreen(),
            );
          },
        );
      },
    );
  }
}