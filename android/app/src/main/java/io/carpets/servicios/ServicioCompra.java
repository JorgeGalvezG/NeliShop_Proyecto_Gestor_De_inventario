package io.carpets.servicios;

import io.carpets.entidades.Compra;
import io.carpets.entidades.DetalleCompra;
import io.carpets.util.Response;

import java.util.List;
import java.util.Map;

public interface ServicioCompra {
    Response registrarCompra(Compra compra, List<DetalleCompra> detalles);
    Response<List<Map<String, Object>>> listarCompras();

    Response actualizarDescripcionCompra(Compra compra);
    Response actualizarStockPorCompra(List<DetalleCompra> detalles);

    Response eliminarDetalleCompra(int detalleId);

    Response editarDetalleCompra(DetalleCompra detalle_local);


    Response eliminarCompra(int compraId);
}