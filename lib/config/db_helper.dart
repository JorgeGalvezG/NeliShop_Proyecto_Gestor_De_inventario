import 'dart:convert';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'core.dart';

class DBHelper {
  static final DBHelper instance = DBHelper._init();
  static Database? _database;

  DBHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    // Nombre limpio y profesional
    _database = await _initDB('nelishop_local.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    // Esto asegura que si cambias la estructura, la app no crashee por tablas faltantes
    return await openDatabase(
      path,
      version: 2, // Subimos la versión para forzar la actualización
      onCreate: _createDB,
      onUpgrade: _upgradeDB,
    );
  }

  // ---------------------------------------------------------------------------
  // ESTRUCTURA DE LA BASE DE DATOS LOCAL
  // ---------------------------------------------------------------------------
  Future _createDB(Database db, int version) async {
    // 1. Tabla de Catálogo (Caché rápido)
    await db.execute('''
    CREATE TABLE productos (
      id TEXT PRIMARY KEY,
      nombre TEXT,
      categoriaNombre TEXT,
      precioVenta REAL,
      precioCompra REAL,
      stock INTEGER,
      imagePath TEXT, -- Corregido para mantener paridad con Java
      precioOferta REAL
    )
    ''');

    // 2. Cola de Ventas (Para ventas sin internet)
    await db.execute('''
    CREATE TABLE ventas_offline (
      id_local INTEGER PRIMARY KEY AUTOINCREMENT,
      fecha TEXT,
      montoTotal REAL,
      descripcion TEXT,
      vendedorId INTEGER,
      numeroBoleta TEXT,
      sincronizado INTEGER DEFAULT 0
    )
    ''');

    // 3. Detalles de las ventas offline
    await db.execute('''
    CREATE TABLE detalles_venta_offline (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      venta_local_id INTEGER,
      producto_id TEXT,
      cantidad INTEGER,
      precio_unitario REAL,
      subtotal REAL
    )
    ''');

    // 4. Historial de Ventas (Caché de lectura)
    await db.execute('''
    CREATE TABLE ventas_historial (
      id INTEGER PRIMARY KEY,
      numeroBoleta TEXT,
      monto REAL,
      fecha TEXT,
      clienteDni TEXT,
      detalles TEXT 
    )
    ''');

    // 5. Historial de Compras (Caché de lectura)
    await db.execute('''
    CREATE TABLE compras_historial (
      id INTEGER PRIMARY KEY,
      descripcion TEXT,
      monto REAL,
      fecha TEXT,
      imagePath TEXT
    )
    ''');
  }

  //  Permite alterar la base de datos limpiamente si cambiamos el código
  Future _upgradeDB(Database db, int oldVersion, int newVersion) async {
    // Como esto es un caché, la forma más sana de actualizar es borrar y recrear
    await db.execute('DROP TABLE IF EXISTS productos');
    await db.execute('DROP TABLE IF EXISTS ventas_offline');
    await db.execute('DROP TABLE IF EXISTS detalles_venta_offline');
    await db.execute('DROP TABLE IF EXISTS ventas_historial');
    await db.execute('DROP TABLE IF EXISTS compras_historial');
    await _createDB(db, newVersion);
  }

  // ===========================================================================
  // MÓDULO: PRODUCTOS (Sincronización)
  // ===========================================================================

  Future<void> syncProductos(List<JsonMap> productosNuevos) async {
    final db = await instance.database;
    final batch = db.batch();

    batch.delete('productos');

    for (var p in productosNuevos) {
      batch.insert('productos', {
        'id': p['id'].toString(),
        'nombre': p['nombre']?.toString() ?? 'Genérico',
        'categoriaNombre': p['categoriaNombre']?.toString() ?? 'General',
        'precioVenta': (p['precioVenta'] ?? 0).toDouble(),
        'precioCompra': (p['precioCompra'] ?? 0).toDouble(),
        'stock': p['cantidad'] ?? 0,
        'imagePath': p['imagePath']?.toString(),
        'precioOferta': p['precioOferta'] != null ? (p['precioOferta'] as num).toDouble() : null,
      });
    }
    await batch.commit();
  }

  Future<List<JsonMap>> getProductosLocal() async {
    final db = await instance.database;
    final result = await db.query('productos', orderBy: 'nombre ASC');

    return result.map((p) => {
      'id': p['id'],
      'nombre': p['nombre'],
      'categoriaNombre': p['categoriaNombre'],
      'precioVenta': p['precioVenta'],
      'precioCompra': p['precioCompra'],
      'cantidad': p['stock'],
      'imagePath': p['imagePath'],
      'precioOferta': p['precioOferta'],
    }).toList();
  }

  // ===========================================================================
  // MÓDULO: VENTAS OFFLINE
  // ===========================================================================

  Future<int> guardarVentaPendiente(JsonMap venta, JsonList detalles) async {
    final db = await instance.database;

    int ventaLocalId = await db.insert('ventas_offline', {
      'fecha': DateTime.now().toIso8601String(),
      'montoTotal': venta['monto'],
      'descripcion': venta['descripcion'],
      'vendedorId': venta['vendedorId'],
      'numeroBoleta': venta['numeroBoleta'],
      'sincronizado': 0
    });

    final batch = db.batch();
    for (var d in detalles) {
      batch.insert('detalles_venta_offline', {
        'venta_local_id': ventaLocalId,
        'producto_id': d['productoId'].toString(),
        'cantidad': d['cantidad'],
        'precio_unitario': d['precio'],
        'subtotal': (d['cantidad'] * d['precio']).toDouble(),
      });
    }
    await batch.commit();

    return ventaLocalId;
  }

  // ===========================================================================
  // MÓDULO: HISTORIAL DE VENTAS (Caché de lectura)
  // ===========================================================================

  Future<void> syncVentasHistorial(List<JsonMap> ventasNuevas) async {
    final db = await instance.database;
    final batch = db.batch();

    batch.delete('ventas_historial');

    for (var v in ventasNuevas) {
      batch.insert('ventas_historial', {
        'id': v['id'],
        'numeroBoleta': v['numeroBoleta']?.toString() ?? '',
        'monto': (v['monto'] ?? 0).toDouble(),
        'fecha': v['fecha']?.toString() ?? '',
        'clienteDni': v['clienteDni']?.toString() ?? '',
        'detalles': jsonEncode(v['detalles'] ?? []),
      });
    }
    await batch.commit();
  }

  Future<List<JsonMap>> getVentasHistorialLocal() async {
    final db = await instance.database;
    final result = await db.query('ventas_historial', orderBy: 'id DESC');

    return result.map((v) => {
      'id': v['id'],
      'numeroBoleta': v['numeroBoleta'],
      'monto': v['monto'],
      'fecha': v['fecha'],
      'clienteDni': v['clienteDni'],
      // Null-Safety al decodificar
      'detalles': jsonDecode((v['detalles'] as String?) ?? '[]'),
    }).toList();
  }

  // ===========================================================================
  // MÓDULO: HISTORIAL DE COMPRAS (Caché de lectura)
  // ===========================================================================

  Future<void> syncComprasHistorial(List<JsonMap> comprasNuevas) async {
    final db = await instance.database;
    final batch = db.batch();

    batch.delete('compras_historial');

    for (var c in comprasNuevas) {
      batch.insert('compras_historial', {
        'id': c['id'],
        'descripcion': c['descripcion']?.toString() ?? '',
        'monto': (c['monto'] ?? 0).toDouble(),
        'fecha': c['fecha']?.toString() ?? '',
        'imagePath': c['imagePath']?.toString() ?? ' ',
      });
    }
    await batch.commit();
  }

  Future<List<JsonMap>> getComprasHistorialLocal() async {
    final db = await instance.database;
    final result = await db.query('compras_historial', orderBy: 'id DESC');

    return result.map((c) => {
      'id': c['id'],
      'descripcion': c['descripcion'],
      'monto': c['monto'],
      'fecha': c['fecha'],
      'imagePath': c['imagePath'],
    }).toList();
  }
}