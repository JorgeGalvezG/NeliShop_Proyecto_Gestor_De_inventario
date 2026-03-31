package io.carpets.servicios;

import io.carpets.entidades.Venta;
import io.carpets.entidades.DetalleVenta;
import io.carpets.entidades.Producto;
import io.carpets.DTOs.MontosCalculados;
import io.carpets.DTOs.BoletaVentaDTO;
import io.carpets.DTOs.VentaCompletaDTO;
import io.carpets.DTOs.DetalleVentaDTO;
import io.carpets.util.Response;

import java.util.List;

public interface ServicioVenta {
    int registrarVenta(Venta venta, List<DetalleVenta> detalles);
    List<Venta> obtenerVentasPorDia(String fecha);
    List<Venta> obtenerVentasPorRango(String fechaInicio, String fechaFin);
    List<Producto> buscarProductoEnVentaPorIdONombre(String criterio);
    List<VentaCompletaDTO> listarVentasConDetalles();
    Response eliminarDetalleVenta(int detalleId);
    boolean validarProductoExiste(int productoId);

    // Métodos para cálculos
    MontosCalculados calcularMontos(double precioUnitario, int cantidad);
    MontosCalculados calcularMontosVentaCompleta(List<DetalleVenta> detalles);
    double calcularTotalVenta(List<DetalleVenta> detalles);

    // Calcular ganancia total (Venta - Compra)
    double calcularGananciaTotal();

    BoletaVentaDTO generarBoleta(int ventaId, List<DetalleVenta> detalles);

    List<Venta> listarVentas();

    Response eliminarVenta(int ventaId);
}