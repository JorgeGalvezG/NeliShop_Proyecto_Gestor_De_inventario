package io.carpets.repositories;

import io.carpets.entidades.Producto;
import io.carpets.util.Response;

import java.util.List;

/*
    Interfaz del repositorio para la entidad Producto.
    Define los métodos CRUD y consultas específicas con manejo de respuestas seguro.
*/
public interface ProductoRepository {

    Response save(Producto producto);

    Response update(Producto producto);

    Response delete(int id);

    Response<Producto> findById(int id);

    Response<List<Producto>> findAll();

    Response<List<Producto>> findByCategoria(String categoriaNombre);

    Response<List<Producto>> findByNombre(String nombre);

    Response<Double> getGananciaTotal();

    // Renombrado para mayor claridad y tipado a Boolean
    Response<Boolean> existeIdById(int id);
}