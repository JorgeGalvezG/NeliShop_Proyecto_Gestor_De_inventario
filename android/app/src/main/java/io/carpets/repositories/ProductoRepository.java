package io.carpets.repositories;

import io.carpets.entidades.Producto;
import io.carpets.util.Response;

import java.util.List;

public interface ProductoRepository {
    Response save(Producto producto);
    Response update(Producto producto);
    Response delete(int id);
    Response<Producto> findById(int id);
    Response<List<Producto>> findAll();
    Response<List<Producto>> findByCategoria(String categoriaNombre);
    Response<List<Producto>> findByNombre(String nombre);
    Response<Double> getGananciaTotal();

    Response existeIdById(int id);

}








