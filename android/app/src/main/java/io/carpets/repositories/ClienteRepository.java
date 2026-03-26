package io.carpets.repositories;

import io.carpets.entidades.Cliente;
import io.carpets.util.Response;

import java.util.List;

public interface ClienteRepository {

    Response save(Cliente cliente);

    Response update(Cliente cliente);

    Response delete(String dni);

    Response<Cliente> findByDni(String dni);

    Response<List<Cliente>> findAll();

    Response<List<Cliente>> findByNombre(String nombre);

    Response<Boolean> existePorDni(String dni);
}