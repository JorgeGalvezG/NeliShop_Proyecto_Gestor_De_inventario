import 'package:flutter/material.dart';

import '../pages/compras_page.dart';
import '../pages/home_page.dart';
import '../pages/productos_page.dart';
import '../pages/reportes_page.dart';
import '../pages/ventas_page.dart';
import '../pages/promociones_page.dart';
import '../pages/configuracion_page.dart';

class MainLayout extends StatefulWidget {
  final String userRole;
  const MainLayout({super.key, required this.userRole});

  @override
  State<MainLayout> createState() => _MainLayoutState();
}

class _MainLayoutState extends State<MainLayout> {
  late final PageController _pageController;
  int _selectedIndex = 0;

  late final List<Widget> _pages;
  late final List<BottomNavigationBarItem> _navBarItems;

  @override
  void initState() {
    super.initState();
    _pageController = PageController();

    if (widget.userRole == 'admin') {
      _pages = [
        HomePage(userRole: widget.userRole),
        const ProductosPage(),
        const VentasPage(),
        const ComprasPage(),
        const PromocionesPage(),
        const ReportesPage(),
        const ConfiguracionPage(),
      ];
      _navBarItems = const [
        BottomNavigationBarItem(icon: Icon(Icons.home), label: "Inicio"),
        BottomNavigationBarItem(
            icon: Icon(Icons.inventory_2), label: "Productos"),
        BottomNavigationBarItem(
            icon: Icon(Icons.receipt_long), label: "Ventas"),
        BottomNavigationBarItem(
            icon: Icon(Icons.local_shipping), label: "Compras"),
        BottomNavigationBarItem(icon: Icon(Icons.local_offer), label: "Promos"),
        BottomNavigationBarItem(icon: Icon(Icons.bar_chart), label: "Reportes"),
        BottomNavigationBarItem(icon: Icon(Icons.settings), label: "Config"),
      ];
    } else {
      // Usuario normal
      _pages = [
        HomePage(userRole: widget.userRole),
        const ProductosPage(),
        const VentasPage(),
      ];
      _navBarItems = const [
        BottomNavigationBarItem(icon: Icon(Icons.home), label: "Inicio"),
        BottomNavigationBarItem(
            icon: Icon(Icons.inventory_2), label: "Productos"),
        BottomNavigationBarItem(
            icon: Icon(Icons.receipt_long), label: "Ventas"),
      ];
    }
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
    _pageController.animateToPage(
      index,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.background,
      body: PageView(
        controller: _pageController,
        onPageChanged: (index) {
          setState(() {
            _selectedIndex = index;
          });
        },
        children: _pages,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex,
        onTap: _onItemTapped,
        items: _navBarItems,
        type: BottomNavigationBarType
            .fixed, // Adding fixed type to ensure all 7 items are visible
        selectedItemColor: Theme.of(context).colorScheme.primary,
        unselectedItemColor: Colors.grey,
      ),
    );
  }
}
