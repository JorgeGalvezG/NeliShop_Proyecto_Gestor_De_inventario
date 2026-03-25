package io.carpets.repositories;

import io.carpets.entidades.DetalleCompra;
import io.carpets.util.Response;

import java.util.List;

/*
    Interfaz del repositorio para la entidad DetalleCompra
    define los métodos CRUD para interactuar con la base de datos.
*/

public interface DetalleCompraRepository {
    Response save(DetalleCompra detalle);
    Response update(DetalleCompra detalle);
    Response delete(int id);
    Response<DetalleCompra> findById(int id);
    Response<List<DetalleCompra>> findByCompraId(int compraId);
    Response<List<DetalleCompra>> findAll();
}