package io.carpets.servicios;

import io.carpets.entidades.Venta;
import io.carpets.entidades.DetalleVenta;
import io.carpets.entidades.Producto;
import io.carpets.DTOs.MontosCalculados;
import io.carpets.DTOs.BoletaVentaDTO;
import java.util.List;

public interface ServicioVenta {
    int registrarVenta(Venta venta, List<DetalleVenta> detalles);
    List<Venta> obtenerVentasPorDia(String fecha);
    List<Venta> obtenerVentasPorRango(String fechaInicio, String fechaFin);
    List<Producto> buscarProductoEnVentaPorIdONombre(String criterio);
    boolean validarProductoExiste(int productoId);

    // Métodos para cálculos
    MontosCalculados calcularMontos(double precioUnitario, int cantidad);
    MontosCalculados calcularMontosVentaCompleta(List<DetalleVenta> detalles);
    double calcularTotalVenta(List<DetalleVenta> detalles);

    // ⭐️ NUEVO: Calcular ganancia total (Venta - Compra)
    double calcularGananciaTotal();

    BoletaVentaDTO generarBoleta(int ventaId, List<DetalleVenta> detalles);

    List<Venta> listarVentas();

    boolean eliminarVenta(int ventaId);
}