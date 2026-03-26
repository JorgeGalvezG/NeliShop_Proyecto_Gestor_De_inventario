package io.carpets.repositories;

import io.carpets.DTOs.CompraCompletaDTO;
import io.carpets.entidades.Compra;
import io.carpets.util.Response;

import java.sql.Date;
import java.util.List;

/*
    Interfaz del repositorio para la entidad Compra
    define los métodos CRUD para interactuar con la base de datos.
*/
public interface CompraRepository {
    Response save(Compra compra);
    Response update(Compra compra);
    Response delete(int consulta);
    Response<Compra> findById(int id);
    Response<List<Compra>> findAll();
    Response<List<CompraCompletaDTO>> listarComprasConDetalles();
    Response<List<Compra>> findByDate(Date desde, Date hasta);
}