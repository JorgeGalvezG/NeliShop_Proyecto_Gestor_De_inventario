package io.carpets.servicios.implementacion;

import io.carpets.entidades.Venta;
import io.carpets.entidades.DetalleVenta;
import io.carpets.entidades.Producto;
import io.carpets.entidades.Cliente;
import io.carpets.entidades.Usuario;
import io.carpets.DTOs.MontosCalculados;
import io.carpets.DTOs.BoletaVentaDTO;
import io.carpets.repositories.VentaRepository;
import io.carpets.repositories.DetalleVentaRepository;
import io.carpets.repositories.ProductoRepository;
import io.carpets.repositories.ClienteRepository;
import io.carpets.repositories.UsuarioRepository;
import io.carpets.repositories.implementacion.VentaRepositoryImplementacion;
import io.carpets.repositories.implementacion.DetalleVentaRepositoryImplementacion;
import io.carpets.repositories.implementacion.ProductoRepositoryImplementacion;
import io.carpets.repositories.implementacion.ClienteRepositoryImplementacion;
import io.carpets.repositories.implementacion.UsuarioRepositoryImplementacion;
import io.carpets.servicios.ServicioVenta;
import io.carpets.util.ResponseUtil;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone; // <--- AGREGAR IMPORT
import java.util.Calendar; // <--- AGREGAR IMPORT

public class ServicioVentaImplementacion implements ServicioVenta {

    private VentaRepository ventaRepo = new VentaRepositoryImplementacion();
    private DetalleVentaRepository detalleVentaRepo = new DetalleVentaRepositoryImplementacion();
    private ProductoRepository productoRepo = new ProductoRepositoryImplementacion();
    private ClienteRepository clienteRepo = new ClienteRepositoryImplementacion();
    private UsuarioRepository usuarioRepo = new UsuarioRepositoryImplementacion();
    private io.carpets.servicios.ServicioProducto servicioProducto = new io.carpets.servicios.implementacion.ServicioProductoImplementacion();

    // Constante para el IGV (18%)
    private static final double IGV_PORCENTAJE = 0.18;

    // Límites para validación de precio razonable
    private static final double PRECIO_MINIMO = 0.01;
    private static final double PRECIO_MAXIMO = 100000.0; // Límite máximo razonable
    private static final double PORCENTAJE_DESVIACION_MAXIMA = 2.0; // 200% de desviación máxima

    @Override
    public int registrarVenta(Venta venta, List<DetalleVenta> detalles) {
        try {
            // 1. Validar DNI (Formato básico)
            if (venta.getClienteDni() == null || venta.getClienteDni().length() != 8) {
                // Si viene vacío o mal, forzamos uno genérico o lanzamos error.
                // Pero como vamos a generar aleatorios, asumimos que viene bien.
                throw new RuntimeException("DNI inválido: " + venta.getClienteDni());
            }

            // --- AUTO-REGISTRO DE CLIENTE (NUEVO) ---
            // Verificamos si el cliente existe. Si es null, lo creamos.
            Cliente clienteExistente = clienteRepo.findByDni(venta.getClienteDni());
            if (clienteExistente == null) {
                System.out.println("Cliente nuevo detectado (" + venta.getClienteDni() + "). Registrando automáticamente...");
                Cliente nuevoCliente = new Cliente();
                nuevoCliente.setDni(venta.getClienteDni());
                nuevoCliente.setNombre("Cliente " + venta.getClienteDni()); // Nombre genérico
                boolean clienteGuardado = clienteRepo.save(nuevoCliente);

                if (!clienteGuardado) {
                    throw new RuntimeException("No se pudo auto-registrar al cliente " + venta.getClienteDni());
                }
            }
            // ----------------------------------------

            // 2. Validar que todos los productos existen
            for (DetalleVenta detalle : detalles) {
                if (!validarProductoExiste(detalle.getProductoId())) {
                    throw new RuntimeException("Producto no encontrado ID: " + detalle.getProductoId());
                }
            }

            // ... (El resto del código sigue igual: validación de precios, stock, cálculos, etc.) ...

            // 3. Calcular montos totales
            MontosCalculados montosVenta = calcularMontosVentaCompleta(detalles);
            venta.setMonto(montosVenta.getTotalConIGV());

            // 4. Obtener ID y Numero Boleta
            int proximoId = obtenerProximoIdVenta(); // Asegúrate de tener este método auxiliar o usar auto-increment

            // ... (Resto de tu lógica original) ...

            // Si usas auto-increment en BD, el save llenará el ID.
            // Si usas tu lógica manual:
            String numeroBoletaReal = generarNumeroBoleta(proximoId);
            venta.setNumeroBoleta(numeroBoletaReal);

            // Guardar venta
            boolean ventaGuardada = ventaRepo.save(venta);
            if (!ventaGuardada) {
                throw new RuntimeException("Error al guardar la venta en BD");
            }

            // ... (Lógica de detalles y stock se mantiene igual) ...

            // Como ventaRepo.save puede no devolver el ID si no es autoincrement directo en el objeto,
            // usamos la lógica que tenías de obtenerVentaReciente o confiamos en que 'venta' se actualizó.
            Venta ventaConId = obtenerVentaReciente();

            // Guardar detalles
            for (DetalleVenta detalle : detalles) {
                detalle.setVentaId(ventaConId.getId());
                detalleVentaRepo.save(detalle);

                // Actualizar Stock
                Producto producto = productoRepo.findById(detalle.getProductoId());
                producto.setCantidad(producto.getCantidad() - detalle.getCantidad());
                productoRepo.update(producto);
            }

            return ventaConId.getId();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al registrar venta: " + e.getMessage());
        }
    }

    /**
     * Obtiene el próximo ID de venta (máximo ID actual + 1)
     */
    private int obtenerProximoIdVenta() {
        try {
            List<Venta> ventas = ventaRepo.findAll();
            if (ventas.isEmpty()) {
                return 1; // Primera venta
            }

            // Encontrar el máximo ID actual
            int maxId = ventas.stream()
                    .mapToInt(Venta::getId)
                    .max()
                    .orElse(0);

            return maxId + 1;

        } catch (Exception e) {
            System.err.println("Error al obtener próximo ID de venta: " + e.getMessage());
            return -1; // Fallback
        }
    }

    /**
     * Valida el DNI del cliente usando ResponseUtil
     */
    private boolean validarDNICliente(String dni) {
        return ResponseUtil.validarDNI(dni);
    }

    @Override
    public List<Venta> obtenerVentasPorDia(String fecha) {
        List<Venta> todasVentas = ventaRepo.findAll();
        return filtrarVentasPorFecha(todasVentas, fecha);
    }

    @Override
    public List<Venta> obtenerVentasPorRango(String fechaInicio, String fechaFin) {
        List<Venta> todasVentas = ventaRepo.findAll();
        return filtrarVentasPorRango(todasVentas, fechaInicio, fechaFin);
    }

    // obtener la venta más reciente
    private Venta obtenerVentaReciente() {
        List<Venta> ventas = ventaRepo.findAll();
        if (ventas.isEmpty()) {
            return null;
        }
        return ventas.get(ventas.size() - 1);
    }

    // Método para generar número de boleta único
    private String generarNumeroBoleta(int ventaId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fecha = sdf.format(new Date());
        return "B" + fecha + "-" + String.format("%04d", ventaId);
    }

    // Métodos para filtrar ventas por fecha
    private List<Venta> filtrarVentasPorFecha(List<Venta> ventas, String fecha) {
        List<Venta> resultado = new java.util.ArrayList<>();
        for (Venta venta : ventas) {
            if (venta.getFecha().toString().equals(fecha)) {
                resultado.add(venta);
            }
        }
        return resultado;
    }

    private List<Venta> filtrarVentasPorRango(List<Venta> ventas, String fechaInicio, String fechaFin) {
        List<Venta> resultado = new java.util.ArrayList<>();
        for (Venta venta : ventas) {
            String fechaVenta = venta.getFecha().toString();
            if (fechaVenta.compareTo(fechaInicio) >= 0 && fechaVenta.compareTo(fechaFin) <= 0) {
                resultado.add(venta);
            }
        }
        return resultado;
    }

    /**
     * Valida que el precio unitario sea válido y razonable
     * @param precioUnitario Precio ingresado por el usuario
     * @param productoId ID del producto para comparar con el precio original
     * @return true si el precio es válido, false si no lo es
     */
    private boolean validarPrecioUnitario(double precioUnitario, int productoId) {
        // 1. Validar que el precio sea mayor que el mínimo
        if (precioUnitario < PRECIO_MINIMO) {
            System.out.println("Precio demasiado bajo: " + precioUnitario);
            return false;
        }

        // 2. Validar que el precio no exceda el máximo razonable
        if (precioUnitario > PRECIO_MAXIMO) {
            System.out.println("Precio excesivamente alto: " + precioUnitario);
            return false;
        }

        // 3. Obtener el producto para comparar con el precio original
        Producto producto = productoRepo.findById(productoId);
        if (producto != null) {
            double precioOriginal = producto.getPrecioVenta();

            // 4. Validar que la desviación no sea excesiva (máximo 200% del precio original)
            double porcentajeDesviacion = Math.abs((precioUnitario - precioOriginal) / precioOriginal) * 100;

            if (porcentajeDesviacion > PORCENTAJE_DESVIACION_MAXIMA) {
                System.out.println("Desviación de precio excesiva: " + porcentajeDesviacion + "%");
                System.out.println("Precio original: " + precioOriginal + ", Precio ingresado: " + precioUnitario);
                // Podrías lanzar una excepción más específica o registrar una advertencia
                // Por ahora, solo retornamos false para rechazar el precio
                return false;
            }

            // 5. Registrar advertencia si hay una desviación significativa (más del 50%)
            if (porcentajeDesviacion > 50.0) {
                System.out.println("ADVERTENCIA: Desviación de precio significativa: " + porcentajeDesviacion + "%");
                System.out.println("Producto: " + producto.getNombre() + " (ID: " + productoId + ")");
                System.out.println("Precio original: " + precioOriginal + ", Precio ingresado: " + precioUnitario);

                // Aquí podrías registrar esta advertencia en un log de auditoría
                registrarAdvertenciaPrecio(productoId, precioOriginal, precioUnitario, porcentajeDesviacion);
            }
        }

        return true;
    }

    /**
     * Registra una advertencia por desviación significativa de precio
     */
    private void registrarAdvertenciaPrecio(int productoId, double precioOriginal, double precioIngresado, double porcentajeDesviacion) {

        System.out.println("=== ADVERTENCIA DE PRECIO ===");
        System.out.println("Producto ID: " + productoId);
        System.out.println("Precio original: " + precioOriginal);
        System.out.println("Precio ingresado: " + precioIngresado);
        System.out.println("Desviación: " + String.format("%.2f", porcentajeDesviacion) + "%");
        System.out.println("Fecha: " + new Date());
        System.out.println("=============================");


    }

    /**
     * Método más permisivo para casos especiales (descuentos, promociones, etc.)
     * que permite override con autorización
     */
    public boolean validarPrecioUnitarioConAutorizacion(double precioUnitario, int productoId, boolean autorizado) {
        // Validaciones básicas
        if (precioUnitario < PRECIO_MINIMO || precioUnitario > PRECIO_MAXIMO) {
            return false;
        }

        // Si está autorizado, permitir cualquier precio dentro de los límites
        if (autorizado) {
            System.out.println("Precio override autorizado para producto ID: " + productoId);
            return true;
        }

        // Si no está autorizado, aplicar las validaciones normales
        return validarPrecioUnitario(precioUnitario, productoId);
    }

    @Override
    public BoletaVentaDTO generarBoleta(int ventaId, List<DetalleVenta> detalles) {
        try {
            // 1. Obtener datos de la venta
            Venta venta = ventaRepo.findById(ventaId);
            if (venta == null) {
                throw new RuntimeException("Venta no encontrada con ID: " + ventaId);
            }

            // 2. Obtener datos del cliente
            Cliente cliente = clienteRepo.findByDni(venta.getClienteDni());

            // 3. Obtener datos del vendedor
            Usuario vendedor = usuarioRepo.findById(venta.getVendedorId());

            // 4. Calcular montos de la boleta
            MontosCalculados montos = calcularMontosVentaCompleta(detalles);

            // 5. Generar número de boleta si no existe
            if (venta.getNumeroBoleta() == null || venta.getNumeroBoleta().isEmpty()) {
                String numeroBoleta = generarNumeroBoleta(ventaId);
                venta.setNumeroBoleta(numeroBoleta);
            }

            // 6. Actualizar venta con los montos calculados
            venta.setIgv(montos.getIgvSolo());
            venta.setIgvAplicado(IGV_PORCENTAJE * 100); // 18%
            venta.setTotalFinal(montos.getTotalConIGV());

            // Persistir los cambios en la venta
            ventaRepo.update(venta);

            // 7. Crear y retornar el DTO de boleta
            return new BoletaVentaDTO(
                    venta,
                    cliente,
                    vendedor,
                    detalles,
                    montos.getSubtotal(),
                    montos.getIgvSolo(),
                    montos.getTotalConIGV()
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar boleta: " + e.getMessage());
        }
    }

    @Override
    public MontosCalculados calcularMontos(double precioUnitario, int cantidad) {
        double subtotal = precioUnitario * cantidad;
        double igvSolo = subtotal * IGV_PORCENTAJE;
        double totalConIGV = subtotal + igvSolo;

        return new MontosCalculados(subtotal, igvSolo, totalConIGV);
    }

    @Override
    public MontosCalculados calcularMontosVentaCompleta(List<DetalleVenta> detalles) {
        double subtotalTotal = 0.0;

        for (DetalleVenta detalle : detalles) {
            MontosCalculados montos = calcularMontos(detalle.getPrecioUnitario(), detalle.getCantidad());
            subtotalTotal += montos.getSubtotal();
        }

        double igvTotal = subtotalTotal * IGV_PORCENTAJE;
        double totalConIGV = subtotalTotal + igvTotal;

        return new MontosCalculados(subtotalTotal, igvTotal, totalConIGV);
    }

    @Override
    public double calcularTotalVenta(List<DetalleVenta> detalles) {
        MontosCalculados montos = calcularMontosVentaCompleta(detalles);
        return montos.getTotalConIGV();
    }

    @Override
    public double calcularGananciaTotal() {
        return productoRepo.getGananciaTotal();
    }

    @Override
    public boolean validarProductoExiste(int productoId) {
        Producto producto = productoRepo.findById(productoId);
        boolean existe = (producto != null);

        if (!existe) {
            ventaRepo.registrarProductoNoEncontrado(productoId, null, null);
        }

        return existe;
    }

    @Override
    public List<Producto> buscarProductoEnVentaPorIdONombre(String criterio) {
        List<Producto> resultado = new java.util.ArrayList<>();
        if (criterio == null || criterio.trim().isEmpty()) return resultado;

        try {
            int id = Integer.parseInt(criterio);
            Producto p = servicioProducto.obtenerPorId(id);
            if (p != null && p.getCantidad() > 0) {
                resultado.add(p);
            } else {
                ventaRepo.registrarProductoNoEncontrado(id, criterio, null);
            }
        } catch (NumberFormatException e) {
            List<Producto> productos = servicioProducto.buscarProductos(criterio, "nombre");
            for (Producto p : productos) {
                if (p.getCantidad() > 0) {
                    resultado.add(p);
                }
            }

            if (productos.isEmpty()) {
                ventaRepo.registrarProductoNoEncontrado(null, criterio, null);
            }
        }
        return resultado;
    }

    @Override
    public List<Venta> listarVentas() {
        try {

            List<Venta> ventas = ventaRepo.findAll();

            System.out.println("✅ Se obtuvieron " + ventas.size() + " ventas");
            return ventas;

        } catch (Exception e) {
            System.err.println("Error en listarVentas: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // Retorna lista vacía en caso de error
        }
    }

    @Override
    public boolean eliminarVenta(int ventaId) {
        try {
            // 1. Validar que la venta existe
            Venta venta = ventaRepo.findById(ventaId);
            if (venta == null) {
                System.out.println("Venta no encontrada con ID: " + ventaId);
                return false;
            }

            System.out.println("Eliminando venta ID: " + ventaId + " - Boleta: " + venta.getNumeroBoleta());

            // 2. Obtener todos los detalles de la venta
            List<DetalleVenta> detalles = detalleVentaRepo.findByVenta(ventaId);
            System.out.println("Encontrados " + detalles.size() + " detalles para la venta ID: " + ventaId);

            // 3. Revertir el stock de los productos (sumar lo que se había restado)
            for (DetalleVenta detalle : detalles) {
                Producto producto = productoRepo.findById(detalle.getProductoId());
                if (producto != null) {
                    int stockActual = producto.getCantidad();
                    int nuevoStock = stockActual + detalle.getCantidad();

                    System.out.println("Revirtiendo stock producto ID: " + producto.getId() +
                            " - Stock actual: " + stockActual +
                            " + Cantidad revertida: " + detalle.getCantidad() +
                            " = Nuevo stock: " + nuevoStock);

                    producto.setCantidad(nuevoStock);
                    boolean stockActualizado = productoRepo.update(producto);

                    if (!stockActualizado) {
                        System.err.println("Error al revertir stock del producto ID: " + producto.getId());
                        return false;
                    }
                } else {
                    System.err.println("Producto no encontrado ID: " + detalle.getProductoId() + " - continuando...");
                }
            }

            // 4. Eliminar los detalles de venta primero (por integridad referencial)
            for (DetalleVenta detalle : detalles) {
                boolean detalleEliminado = detalleVentaRepo.delete(detalle.getId());
                if (!detalleEliminado) {
                    System.err.println("Error al eliminar detalle ID: " + detalle.getId());
                    // Continuamos aunque falle uno, para intentar limpiar lo máximo posible
                }
            }

            // 5. Finalmente eliminar la venta
            boolean ventaEliminada = ventaRepo.delete(ventaId);

            if (ventaEliminada) {
                System.out.println("Venta eliminada exitosamente:");
                System.out.println("   - ID: " + ventaId);
                System.out.println("   - Número de boleta: " + venta.getNumeroBoleta());
                System.out.println("   - Stock revertido para " + detalles.size() + " productos");
            } else {
                System.err.println("Error al eliminar la venta de la base de datos");
            }

            return ventaEliminada;

        } catch (Exception e) {
            System.err.println("Error en eliminarVenta: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


}