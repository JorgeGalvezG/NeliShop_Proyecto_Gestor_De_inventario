package io.carpets.repositories;

import io.carpets.entidades.Venta;
import java.util.List;
import io.carpets.DTOs.VentaCompletaDTO;
import io.carpets.util.Response;


public interface VentaRepository {
    boolean save(Venta venta);
    Response update(Venta venta);
    Response delete(int id);
    Venta findById(int id);
    Response<List<Venta>> findAll();
    Response<List<Venta>> findByNumeroBoleta(String numeroBoleta);
    Response<List<VentaCompletaDTO>> listarVentasConDetalles();



    //  registrar productos no encontrados
    Response registrarProductoNoEncontrado(Integer idProductoSolicitado, String nombreProductoSolicitado, Integer vendedorId);
}