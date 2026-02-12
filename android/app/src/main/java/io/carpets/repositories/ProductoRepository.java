package io.carpets.repositories;

import io.carpets.entidades.Producto;
import java.util.List;

public interface ProductoRepository {
    boolean save(Producto producto);
    boolean update(Producto producto);
    boolean delete(int id);
    Producto findById(int id);
    List<Producto> findAll();
    List<Producto> findByCategoria(String categoriaNombre);
    List<Producto> findByNombre(String nombre);
    double getGananciaTotal();

    boolean existeIdById(int id);

}








