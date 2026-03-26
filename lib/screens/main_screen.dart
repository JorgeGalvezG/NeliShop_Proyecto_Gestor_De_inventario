import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

// Importa todas tus pantallas aquí
import '../pages/home_page.dart';
import '../pages/productos_page.dart';
import '../pages/ventas_page.dart';
import '../pages/compras_page.dart';


class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  _MainScreenState createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _indiceActual = 0;
  String _rolUsuario = 'vendedor'; // Por seguridad, asumimos vendedor por defecto
  bool _isLoadingRole = true;

  @override
  void initState() {
    super.initState();
    _cargarRol();
  }

  // Leemos el rol desde la memoria del celular
  Future<void> _cargarRol() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _rolUsuario = prefs.getString('rol') ?? 'vendedor';
      _isLoadingRole = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    // Mientras lee la memoria, mostramos un cargando
    if (_isLoadingRole) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    // 1. Construimos la lista de PANTALLAS dinámicamente
    final List<Widget> _pantallas = [
      HomePage(userRole: _rolUsuario), // Tu página actual

      if (_rolUsuario == 'admin')
        const ProductosPage(),

      const VentasPage(),
      // Puedes agregar Compras, Promos, etc.
    ];

    // 2. Construimos la lista de BOTONES de abajo dinámicamente
    final List<BottomNavigationBarItem> _botones = [
      const BottomNavigationBarItem(icon: Icon(Icons.home), label: 'Inicio'),

      if (_rolUsuario == 'admin')
        const BottomNavigationBarItem(icon: Icon(Icons.inventory_2), label: 'Productos'),

      const BottomNavigationBarItem(icon: Icon(Icons.receipt_long), label: 'Ventas'),
      // Agrega los demás botones...
    ];

    return Scaffold(
      // Mostramos la pantalla según el índice
      body: _pantallas[_indiceActual],

      // La barra de abajo
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _indiceActual,
        type: BottomNavigationBarType.fixed, // Evita que los íconos salten
        selectedItemColor: Theme.of(context).colorScheme.primary,
        unselectedItemColor: Colors.grey,
        onTap: (index) {
          setState(() {
            _indiceActual = index;
          });
        },
        items: _botones,
      ),
    );
  }
}