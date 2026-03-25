package io.carpets.servicios.implementacion;

import io.carpets.entidades.Producto;
import io.carpets.repositories.ProductoRepository;
import io.carpets.repositories.implementacion.ProductoRepositoryImplementacion;
import io.carpets.servicios.ServicioProducto;
import io.carpets.util.Response;


import java.util.Date;
import java.util.List;

public class    ServicioProductoImplementacion implements ServicioProducto {

    private final ProductoRepository repo = new ProductoRepositoryImplementacion();

    /**
     * Validación de stock(??) no se para q se usa.
     * @param productoId
     * @param cantidad
     * @return
     */
    @Override
    public Response<Producto> validarStock(int productoId, int cantidad) {
        Response<Producto> response = repo.findById(productoId);
        if(response.isOk()){
            Producto p = response.getContent();


            if(p != null && p.getCantidad() >= cantidad) response.exito(p);
            else response.internal_error("SPI.validarStock: Stock no válido");

        }else{
            // Mensaje log pasado para el MethodChannelHandler.
            response.internal_error("SPI.validarStock: Error de flujo.");
        }
        return response;
    }

    /**
     * Actualización del inventario.
     * @param producto Contiene el nuevo Stock. Esto reemplazará toda la información del producto.
     * @return Response, que indica si la función llegó a ejecutarse correctamente.
     */
    @Override
    public Response actualizarInventario(Producto producto) {
        return repo.update(producto);
    }

    /**
     * @return Ganancia acumulada en la base de datos.
     */
    public Response<Double> getGananciaTotal(){
        return repo.getGananciaTotal();
    }

    /**
     * Lista todos los productos
     * @return Lista de productos
     */
    @Override
    public Response<List<Producto>> obtenerTodos() {
        return repo.findAll();
    }

    /**
     * Encuentra un producto utilizando su id.
     * @param id Identificador del producto.
     * @return Respuesta de la función, si hubo o no hubo error.
     */
    @Override
    public Response<Producto> obtenerPorId(int id) {
        return repo.findById(id);
    }

    /**
     * Filtra los productos por nombre y categoría.
     * @param criterio Nombre del producto (Supongo)
     * @param tipo Me imagino que la categoría...
     * @return Lista de productos filtrados.
     */
    @Override
    public Response<List<Producto>> buscarProductos(String criterio, String tipo) {

        if (criterio == null || criterio.trim().isEmpty()) {
            return repo.findAll();
        }

        Response<List<Producto>> porNombre = repo.findByNombre(criterio);
        Response<List<Producto>> porCategoria = repo.findByCategoria(criterio);

        if(!porNombre.isOk()){
            porNombre.internal_error("SPI.buscarProductos: Error al filtrar productos por nombre.");
            return porNombre;
        }

        if(!porCategoria.isOk()){
            porNombre.internal_error("SPI.buscarProductos: Error al filtrar productos por categoría.");
            return porNombre;
        }

        if (tipo == null || "all".equalsIgnoreCase(tipo)) {
            // Búsqueda en todos los campos disponibles
            List<Producto> resultados = new java.util.ArrayList<>();
            resultados.addAll(porNombre.getContent());
            resultados.addAll(porCategoria.getContent());

            Response<List<Producto>> response = new Response<List<Producto>>();
            response.exito(resultados.stream().distinct().collect(java.util.stream.Collectors.toList()));

            // Eliminar duplicados
            return response;
        }

        return switch (tipo.toLowerCase()) {
            case "nombre" -> porNombre;
            case "categoria" -> porCategoria;
            default -> repo.findAll();
        };
    }

    /**
     * Agrega un producto a la base de datos.
     * @param producto Información del producto.
     * @return Response, que contiene si el flujo no tuvo errores.
     */
    @Override
    public Response agregarProducto(Producto producto) {
        Response response = new Response();
            // 1. Validaciones básicas
            if (producto == null) {
                response.internal_error("SPI.agregarProducto: Producto nulo.");
                return response;
            }

            // 2. Validar nombre
            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                response.internal_error("SPI.agregarProducto: Nombre de producto vacío.");
                return response;
            }

            if (producto.getNombre().trim().length() > 100) {
                response.internal_error("SPI.agregarProducto: Nombre de producto mayor a 100 carácteres.");
                return response;
            }

            // 3. Validar precios
            if (producto.getPrecioCompra() <= 0) {
                response.internal_error("SPI.agregarProducto: Precio de compra en cero o negativo.");
                return response;
            }

            if (producto.getPrecioVenta() <= 0) {
                response.internal_error("SPI.agregarProducto: Precio de venta en cero o negativo.");
                return response;
            }

            if (producto.getPrecioVenta() < producto.getPrecioCompra()) {
                response.internal_error("SPI.agregarProducto: Precio de venta menor al precio de compra.");
                return response;
            }

            // 4. Validar cantidad
            if (producto.getCantidad() < 0) {
                response.internal_error("SPI.agregarProducto: La cantidad de producto no puede ser negativo.");
                return response;
            }

            // 6. Validar que no exista un producto con el mismo nombre
            List<Producto> productosMismoNombre = repo.findByNombre(producto.getNombre().trim()).getContent();
            if (productosMismoNombre != null && !productosMismoNombre.isEmpty()) {
                response.internal_error("SPI.agregarProducto: No es posible agregar otro producto con el mismo nombre.");
                return response;
            }

            // 7. Establecer fecha de ingreso si no está establecida
            if (producto.getFechaIngreso() == null) {
                producto.setFechaIngreso(new Date());
            }

            // 8. Guardar en la base de datos
            response = repo.save(producto);

            return response;

    }

    @Override
    public Response eliminarProducto(int idProducto) {
        Response<Producto> response = new Response<Producto>();

            // 1. Validar que el ID sea válido
            if (idProducto <= 0) {
                response.internal_error("SPI.eliminarProducto: El id de producto '" + idProducto + "' es inválido.");
                return response;
            }

            // 2. Validar que el producto existe
            response = repo.findById(idProducto);
            if(!response.isOk()){
                response.internal_error("SPI.eliminarProducto: Producto no encontrado con ID= " + idProducto);
                return response;
            }

            Producto producto = response.getContent();

            // Hacemos un nuevo 'response' para almacenar el de delete.
            Response DeleteResponse = repo.delete(idProducto);

            if (!DeleteResponse.isOk()) {
                DeleteResponse.internal_error("SPI.eliminarProducto: Error al eliminar el producto de la base de datos.");
            }

            return DeleteResponse;

    }
}