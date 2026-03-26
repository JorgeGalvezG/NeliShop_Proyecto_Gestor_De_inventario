package io.carpets.repositories;

import io.carpets.entidades.DetalleVenta;
import io.carpets.util.Response;

import java.util.List;

/*
    Interfaz del repositorio para la entidad DetalleVenta
    define los métodos CRUD para interactuar con la base de datos.
*/
public interface DetalleVentaRepository {
    boolean save(DetalleVenta detalle);
    boolean update(DetalleVenta detalle);
    boolean delete(int id);
    DetalleVenta findById(int id);
    Response<List<DetalleVenta>> findByVenta(int ventaId);
}