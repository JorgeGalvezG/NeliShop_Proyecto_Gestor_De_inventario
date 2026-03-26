package io.carpets.servicios.implementacion;

import io.carpets.DTOs.VentaCompletaDTO;
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
import io.carpets.util.Response;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;

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
                throw new RuntimeException("DNI inválido: " + venta.getClienteDni());
            }

            // --- AUTO-REGISTRO DE CLIENTE ---
            // Extraer el cliente verificando el Response
            Response<Cliente> resCliente = clienteRepo.findByDni(venta.getClienteDni());
            Cliente clienteExistente = resCliente.isOk() ? resCliente.getContent() : null;

            if (clienteExistente == null) {
                System.out.println("Cliente nuevo detectado (" + venta.getClienteDni() + "). Registrando automáticamente...");
                Cliente nuevoCliente = new Cliente();
                nuevoCliente.setDni(venta.getClienteDni());
                nuevoCliente.setNombre("Cliente " + venta.getClienteDni());

                Response resGuardar = clienteRepo.save(nuevoCliente);
                if (!resGuardar.isOk()) {
                    throw new RuntimeException("No se pudo auto-registrar al cliente " + venta.getClienteDni());
                }
            }

            // 2. Validar que todos los productos existen
            for (DetalleVenta detalle : detalles) {
                if (!validarProductoExiste(detalle.getProductoId())) {
                    throw new RuntimeException("Producto no encontrado ID: " + detalle.getProductoId());
                }
            }

            // 3. Calcular montos totales
            MontosCalculados montosVenta = calcularMontosVentaCompleta(detalles);
            venta.setMonto(montosVenta.getTotalConIGV());

            // 4. Obtener ID y Numero Boleta
            int proximoId = obtenerProximoIdVenta();
            String numeroBoletaReal = generarNumeroBoleta(proximoId);
            venta.setNumeroBoleta(numeroBoletaReal);

            // Guardar venta
            Response resVenta = ventaRepo.save(venta);
            if (!resVenta.isOk()) {
                throw new RuntimeException("Error al guardar la venta en BD");
            }

            Venta ventaConId = obtenerVentaReciente();
            if (ventaConId == null) {
                throw new RuntimeException("Error al recuperar la venta registrada");
            }

            // Guardar detalles
            for (DetalleVenta detalle : detalles) {
                detalle.setVentaId(ventaConId.getId());
                detalleVentaRepo.save(detalle);

                // Actualizar Stock mediante extracción segura del Response
                Response<Producto> resProducto = productoRepo.findById(detalle.getProductoId());
                if (resProducto.isOk()) {
                    Producto producto = resProducto.getContent();
                    producto.setCantidad(producto.getCantidad() - detalle.getCantidad());
                    productoRepo.update(producto);
                }
            }

            return ventaConId.getId();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al registrar venta: " + e.getMessage());
        }
    }

    @Override
    public List<VentaCompletaDTO> listarVentasConDetalles() {
        Response<List<VentaCompletaDTO>> res = ventaRepo.listarVentasConDetalles();
        return res.isOk() ? res.getContent() : new ArrayList<>();
    }

    @Override
    public boolean eliminarDetalleVenta(int detalleId) {
        return false;
    }

    private int obtenerProximoIdVenta() {
        try {
            Response<List<Venta>> res = ventaRepo.findAll();
            if (!res.isOk() || res.getContent().isEmpty()) {
                return 1;
            }

            List<Venta> ventas = res.getContent();
            int maxId = ventas.stream()
                    .mapToInt(Venta::getId)
                    .max()
                    .orElse(0);

            return maxId + 1;

        } catch (Exception e) {
            System.err.println("Error al obtener próximo ID de venta: " + e.getMessage());
            return -1;
        }
    }

    @Override
    public List<Venta> obtenerVentasPorDia(String fecha) {
        Response<List<Venta>> res = ventaRepo.findAll();
        List<Venta> todasVentas = res.isOk() ? res.getContent() : new ArrayList<>();
        return filtrarVentasPorFecha(todasVentas, fecha);
    }

    @Override
    public List<Venta> obtenerVentasPorRango(String fechaInicio, String fechaFin) {
        Response<List<Venta>> res = ventaRepo.findAll();
        List<Venta> todasVentas = res.isOk() ? res.getContent() : new ArrayList<>();
        return filtrarVentasPorRango(todasVentas, fechaInicio, fechaFin);
    }

    private Venta obtenerVentaReciente() {
        Response<List<Venta>> res = ventaRepo.findAll();
        if (!res.isOk() || res.getContent().isEmpty()) {
            return null;
        }
        List<Venta> ventas = res.getContent();
        return ventas.get(ventas.size() - 1);
    }

    private String generarNumeroBoleta(int ventaId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fecha = sdf.format(new Date());
        return "B" + fecha + "-" + String.format("%04d", ventaId);
    }

    private List<Venta> filtrarVentasPorFecha(List<Venta> ventas, String fecha) {
        List<Venta> resultado = new java.util.ArrayList<>();
        for (Venta venta : ventas) {
            if (venta.getFecha() != null && venta.getFecha().toString().startsWith(fecha)) {
                resultado.add(venta);
            }
        }
        return resultado;
    }

    private List<Venta> filtrarVentasPorRango(List<Venta> ventas, String fechaInicio, String fechaFin) {
        List<Venta> resultado = new java.util.ArrayList<>();
        for (Venta venta : ventas) {
            if (venta.getFecha() != null) {
                String fechaVenta = venta.getFecha().toString().split(" ")[0];
                if (fechaVenta.compareTo(fechaInicio) >= 0 && fechaVenta.compareTo(fechaFin) <= 0) {
                    resultado.add(venta);
                }
            }
        }
        return resultado;
    }

    private boolean validarPrecioUnitario(double precioUnitario, int productoId) {
        if (precioUnitario < PRECIO_MINIMO) {
            System.out.println("Precio demasiado bajo: " + precioUnitario);
            return false;
        }

        if (precioUnitario > PRECIO_MAXIMO) {
            System.out.println("Precio excesivamente alto: " + precioUnitario);
            return false;
        }

        Response<Producto> resProducto = productoRepo.findById(productoId);
        if (resProducto.isOk()) {
            Producto producto = resProducto.getContent();
            double precioOriginal = producto.getPrecioVenta();

            double porcentajeDesviacion = Math.abs((precioUnitario - precioOriginal) / precioOriginal) * 100;

            if (porcentajeDesviacion > PORCENTAJE_DESVIACION_MAXIMA) {
                System.out.println("Desviación de precio excesiva: " + porcentajeDesviacion + "%");
                System.out.println("Precio original: " + precioOriginal + ", Precio ingresado: " + precioUnitario);
                return false;
            }

            if (porcentajeDesviacion > 50.0) {
                System.out.println("ADVERTENCIA: Desviación de precio significativa: " + porcentajeDesviacion + "%");
                System.out.println("Producto: " + producto.getNombre() + " (ID: " + productoId + ")");
                System.out.println("Precio original: " + precioOriginal + ", Precio ingresado: " + precioUnitario);
                registrarAdvertenciaPrecio(productoId, precioOriginal, precioUnitario, porcentajeDesviacion);
            }
        }
        return true;
    }

    private void registrarAdvertenciaPrecio(int productoId, double precioOriginal, double precioIngresado, double porcentajeDesviacion) {
        System.out.println("=== ADVERTENCIA DE PRECIO ===");
        System.out.println("Producto ID: " + productoId);
        System.out.println("Precio original: " + precioOriginal);
        System.out.println("Precio ingresado: " + precioIngresado);
        System.out.println("Desviación: " + String.format("%.2f", porcentajeDesviacion) + "%");
        System.out.println("Fecha: " + new Date());
        System.out.println("=============================");
    }

    public boolean validarPrecioUnitarioConAutorizacion(double precioUnitario, int productoId, boolean autorizado) {
        if (precioUnitario < PRECIO_MINIMO || precioUnitario > PRECIO_MAXIMO) {
            return false;
        }

        if (autorizado) {
            System.out.println("Precio override autorizado para producto ID: " + productoId);
            return true;
        }

        return validarPrecioUnitario(precioUnitario, productoId);
    }

    @Override
    public BoletaVentaDTO generarBoleta(int ventaId, List<DetalleVenta> detalles) {
        try {
            Response<Venta> ventaRes = ventaRepo.findById(ventaId);
            if (!ventaRes.isOk()) {
                throw new RuntimeException("Venta no encontrada con ID: " + ventaId);
            }
            Venta venta = ventaRes.getContent();

            Response<Cliente> clienteRes = clienteRepo.findByDni(venta.getClienteDni());
            Cliente cliente = clienteRes.isOk() ? clienteRes.getContent() : null;

            // Se utiliza el método findById para obtener la respuesta del repositorio
            Response<Usuario> vendedorResponse = usuarioRepo.findById(venta.getVendedorId());
            if(!vendedorResponse.isOk()){
                throw new RuntimeException("SVI.generarBoleta: Error al obtener el vendedor.");
            }

            MontosCalculados montos = calcularMontosVentaCompleta(detalles);

            if (venta.getNumeroBoleta() == null || venta.getNumeroBoleta().isEmpty()) {
                String numeroBoleta = generarNumeroBoleta(ventaId);
                venta.setNumeroBoleta(numeroBoleta);
            }

            venta.setIgv(montos.getIgvSolo());
            venta.setIgvAplicado(IGV_PORCENTAJE * 100);
            venta.setTotalFinal(montos.getTotalConIGV());

            ventaRepo.update(venta);

            return new BoletaVentaDTO(
                    venta,
                    cliente,
                    vendedorResponse.getContent(),
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
        Response<Double> res = productoRepo.getGananciaTotal();
        return res.isOk() ? res.getContent() : 0.0;
    }

    @Override
    public boolean validarProductoExiste(int productoId) {
        Response<Producto> prodReq = productoRepo.findById(productoId);
        boolean existe = prodReq.isOk() && prodReq.getContent() != null;

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
            Response<Producto> pReq = servicioProducto.obtenerPorId(id);

            if (pReq.isOk() && pReq.getContent() != null && pReq.getContent().getCantidad() > 0) {
                resultado.add(pReq.getContent());
            } else {
                ventaRepo.registrarProductoNoEncontrado(id, criterio, null);
            }
        } catch (NumberFormatException e) {
            Response<List<Producto>> listReq = servicioProducto.buscarProductos(criterio, "nombre");

            if (listReq.isOk() && listReq.getContent() != null) {
                for (Producto p : listReq.getContent()) {
                    if (p.getCantidad() > 0) {
                        resultado.add(p);
                    }
                }
            }

            if (resultado.isEmpty()) {
                ventaRepo.registrarProductoNoEncontrado(null, criterio, null);
            }
        }
        return resultado;
    }

    @Override
    public List<Venta> listarVentas() {
        try {
            Response<List<Venta>> res = ventaRepo.findAll();
            List<Venta> ventas = res.isOk() ? res.getContent() : new ArrayList<>();
            System.out.println("Se obtuvieron " + ventas.size() + " ventas");
            return ventas;

        } catch (Exception e) {
            System.err.println("Error en listarVentas: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean eliminarVenta(int ventaId) {
        try {
            Response<Venta> vRes = ventaRepo.findById(ventaId);
            if (!vRes.isOk()) {
                System.out.println("Venta no encontrada con ID: " + ventaId);
                return false;
            }
            Venta venta = vRes.getContent();

            System.out.println("Eliminando venta ID: " + ventaId + " - Boleta: " + venta.getNumeroBoleta());

            Response<List<DetalleVenta>> detRes = detalleVentaRepo.findByVenta(ventaId);
            List<DetalleVenta> detalles = detRes.isOk() ? detRes.getContent() : new ArrayList<>();
            System.out.println("Encontrados " + detalles.size() + " detalles para la venta ID: " + ventaId);

            for (DetalleVenta detalle : detalles) {
                Response<Producto> prodRes = productoRepo.findById(detalle.getProductoId());
                if (prodRes.isOk()) {
                    Producto producto = prodRes.getContent();
                    int stockActual = producto.getCantidad();
                    int nuevoStock = stockActual + detalle.getCantidad();

                    System.out.println("Revirtiendo stock producto ID: " + producto.getId() +
                            " - Stock actual: " + stockActual +
                            " + Cantidad revertida: " + detalle.getCantidad() +
                            " = Nuevo stock: " + nuevoStock);

                    producto.setCantidad(nuevoStock);
                    boolean stockActualizado = productoRepo.update(producto).isOk();

                    if (!stockActualizado) {
                        System.err.println("Error al revertir stock del producto ID: " + producto.getId());
                        return false;
                    }
                } else {
                    System.err.println("Producto no encontrado ID: " + detalle.getProductoId() + " - continuando...");
                }
            }

            for (DetalleVenta detalle : detalles) {
                boolean detalleEliminado = detalleVentaRepo.delete(detalle.getId()).isOk();
                if (!detalleEliminado) {
                    System.err.println("Error al eliminar detalle ID: " + detalle.getId());
                }
            }

            boolean ventaEliminada = ventaRepo.delete(ventaId).isOk();

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