package io.carpets.repositories;

import io.carpets.entidades.DetalleCompra;
import java.util.List;

/*
    Interfaz del repositorio para la entidad DetalleCompra
    define los métodos CRUD para interactuar con la base de datos.
*/

public interface DetalleCompraRepository {
    boolean save(DetalleCompra detalle);
    boolean update(DetalleCompra detalle);
    boolean delete(int id);
    DetalleCompra findById(int id);
    List<DetalleCompra> findByCompraId(int compraId);
    List<DetalleCompra> findAll();
}