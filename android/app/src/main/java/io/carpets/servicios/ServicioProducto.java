package io.carpets.servicios;

import io.carpets.entidades.Producto;
import io.carpets.util.Response;

import java.util.List;

public interface ServicioProducto {


    Response<Producto> validarStock(int productoId, int cantidad);
    Response actualizarInventario(Producto producto);
    public Response<List<Producto>> obtenerTodos();
    Response<Producto> obtenerPorId(int id);
    Response<List<Producto>> buscarProductos(String criterio, String tipo);

    Response<Double> getGananciaTotal();

    Response agregarProducto(Producto producto);

    Response eliminarProducto(int idProducto);
}