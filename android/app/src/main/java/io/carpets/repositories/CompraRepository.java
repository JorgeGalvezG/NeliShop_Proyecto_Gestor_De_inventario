package io.carpets.repositories;

import io.carpets.entidades.Compra;
import java.util.List;

/*
    Interfaz del repositorio para la entidad Compra
    define los métodos CRUD para interactuar con la base de datos.
*/
public interface CompraRepository {
    boolean save(Compra compra);
    boolean update(Compra compra);
    boolean delete(int id);
    Compra findById(int id);
    List<Compra> findAll();
}