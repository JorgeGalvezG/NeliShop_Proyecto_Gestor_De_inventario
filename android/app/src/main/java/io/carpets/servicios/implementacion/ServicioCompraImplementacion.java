package io.carpets.servicios.implementacion;

import io.carpets.entidades.Compra;
import io.carpets.entidades.DetalleCompra;
import io.carpets.entidades.Producto;
import io.carpets.repositories.CompraRepository;
import io.carpets.repositories.DetalleCompraRepository;
import io.carpets.repositories.ProductoRepository;
import io.carpets.repositories.implementacion.CompraRepositoryImplementacion;
import io.carpets.repositories.implementacion.DetalleCompraRepositoryImplementacion;
import io.carpets.repositories.implementacion.ProductoRepositoryImplementacion;
import io.carpets.servicios.ServicioCompra;
import io.carpets.servicios.ServicioProducto;
import io.carpets.util.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicioCompraImplementacion implements ServicioCompra {

    final private CompraRepository compraRepo = new CompraRepositoryImplementacion();
    final private DetalleCompraRepository detalleCompraRepo = new DetalleCompraRepositoryImplementacion();
    final private ProductoRepository productoRepo = new ProductoRepositoryImplementacion();
    final private ServicioProducto servicioProducto = new ServicioProductoImplementacion();

    // Límites para validación de datos del producto
    private static final double PRECIO_COMPRA_MINIMO = 0.01;
    private static final double PRECIO_COMPRA_MAXIMO = 100000.0;
    private static final int CANTIDAD_MINIMA = 1;
    private static final int CANTIDAD_MAXIMA = 10000;

    @Override
    public Response registrarCompra(Compra compra, List<DetalleCompra> detalles) {

        Response response = new Response();
        //Si revisas compraRepo.Save verás que 'compra' almacena el id de la nueva fila.
        if (!compraRepo.save(compra).isOk()) {
            response.internal_error("SCI.registrarCompra: Error guardando compra.");
            return response;
        }

        // Guardar detalles
        for (DetalleCompra detalle : detalles) {
            detalle.setCompraId(compra.getId());

            // Guardar detalle
            if (!detalleCompraRepo.save(detalle).isOk()) {
                response.internal_error("SCI.registrarCompra: Error al guardar detalle de compra.");
                return response;
            }
        }

        // Actualizar stock de productos - NUEVA LÓGICA EXTRACTADA
        if (!actualizarStockPorCompra(detalles).isOk()) {
            response.internal_error("SCI.registrarCompra: Error al actualizar Stock.");
            return response;
        }
        response.exito();
        return response;
    }


    /**
     * Actualiza la descripción de una compra.
     *
     * @param compra Contenedor de información.
     * @return response, que contiene la respuesta de la función.
     */
    public Response actualizarDescripcionCompra(Compra compra) {
        Response response = compraRepo.update(compra);

        if (!response.isOk()) {
            response.internal_error("SCI.actualizarDescripcionCompra: Error al actualizar descripción.");
        }

        return response;
    }


    /**
     * Método para actualizar el stock de productos basado en los detalles de compra
     * Itera los detalles, suma la cantidad al stock de cada producto y persiste los cambios
     *
     * @param detalles Lista de detalles de compra
     * @return true si se actualizó correctamente, false si hubo error
     */
    public Response actualizarStockPorCompra(List<DetalleCompra> detalles) {
        Response response = new Response();
        for (DetalleCompra detalle : detalles) {
            // Obtener el producto desde la base de datos
            Response<Producto> request = productoRepo.findById(detalle.getProductoId());

            if (!request.isOk()) {
                response.internal_error("SCI.actualizarStockPorCompra: Error al obtener el producto, id= " + detalle.getId());
                return response;
            }

            Producto producto = request.getContent();

            // Verificar que el producto no sea temporal (O sea tabla log_producto_no_encontrado)
            if (producto.getId() < 0) {
                System.out.println("Producto con ID temporal " + producto.getId() + " - omitiendo actualización de stock");
                continue;
            }

            // Calcular nuevo stock: stock actual + cantidad comprada
            int stockActual = producto.getCantidad();
            int nuevoStock = stockActual + detalle.getUnidades();

            // Actualizar el stock del producto
            producto.setCantidad(nuevoStock);

            // Persistir cambios en la base de datos
            if (!productoRepo.update(producto).isOk()) {
                response.internal_error("SCI.actualizarStockPorCompra: Error al actualizar stock del producto ID: " + producto.getId());
                return request;
            }
        }
        response.exito();
        return response;
    }


    /**
     * Hace una lista de todas las compras.
     * @return Response, te dice si hubo errores en el trayecto.
     */
    @Override
    public Response<List<Map<String, Object>>> listarCompras() {
        Response<List<Map<String, Object>>> response = new Response<>();

        Response<List<Compra>> request = compraRepo.findAll();
        if (!request.isOk()) {
            response.internal_error("SCI.listarCompras: Error al listar las compras.");
            return response;
        }

        //Hacemos una lista con las compras. La lista de mapas tendrá el contenido.
        List<Compra> compras = request.getContent();
        List<Map<String, Object>> listaMapas = new ArrayList<>();

        //Por cada compra.
        for (Compra c : compras) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("descripcion", c.getDescripcion());
            map.put("monto", c.getMonto());

            // Buscamos los detalles de esta compra para obtener una imagen de muestra.
            Response<List<DetalleCompra>> det_request = detalleCompraRepo.findByCompraId(c.getId());
            if(!det_request.isOk()){
                response.internal_error("SCI.listaCompras: Error al obtener los detalles de compra de la compra con Id=" + c.getId());
                return response;
            }

            List<DetalleCompra> detalles = det_request.getContent();
            if (!detalles.isEmpty()) {
                // Tomamos el primer producto de la compra para mostrar su foto
                Response<Producto> prod_request = productoRepo.findById(detalles.get(0).getProductoId());
                if(!prod_request.isOk()){
                    response.internal_error("SCI.listaCompras: Error al obtener los detalles de compra de la compra con Id=" + c.getId());
                    return response;
                }

                //Recuperamos el producto.
                Producto p = prod_request.getContent();
                map.put("imagePath", p.getImagePath());
            }

            listaMapas.add(map);
        }
        response.exito(listaMapas);
        return response;
    }

    /**
     * Elimina un detalle de compra específico.
     * @param detalleId Identificador del detalle de compra.
     * @return Response, te dice si hubo problemas en el trayecto.
     */
    @Override
    public Response eliminarDetalleCompra(int detalleId) {
        Response response = new Response();

        //Verifica si el detalle existe.
        if (!detalleCompraRepo.findById(detalleId).isOk()) {
            response.internal_error("SCI.eliminarDetalleCompra: Detalle indicado no encontrado, id= " + detalleId);
            return response;
        }

        //Elimina el detalle :v
        if (!detalleCompraRepo.delete(detalleId).isOk()) {
            response.internal_error("SCI.eliminarDetalleCompra: Error al eliminar el detalle de Compra.");
            return response;
        }

        response.isOk();
        return response;
    }

    /**
     * Cambia los datos del detalle de venta
     * @param detalle_local Objeto con los datos nuevos.
     * @return Response, te indica si hubo errores o no.
     */
    public Response editarDetalleCompra(DetalleCompra detalle_local) {
        Response response = new Response();

        //Validar que el detalle existe
        Response<DetalleCompra> request = detalleCompraRepo.findById(detalle_local.getId());
        if (!request.isOk()) {
            response.internal_error("SCI.editarDetalleCompra: Error al editar el detalle de compra.");
            return response;
        }
        DetalleCompra detalle_remoto = request.getContent();

        //Que sea una cantidad válida
        if (detalle_local.getUnidades() < CANTIDAD_MINIMA || detalle_local.getUnidades() > CANTIDAD_MAXIMA) {
            response.internal_error("SCI.editarDetalleCompra: cantidad de compra fuera de los rangos. ctd= " + detalle_local.getUnidades());
            return response;
        }

        //Que tenga un precio válido
        if (detalle_local.getPrecioUnitario() < PRECIO_COMPRA_MINIMO || detalle_local.getPrecioUnitario() > PRECIO_COMPRA_MAXIMO) {
            response.internal_error("SCI.editarDetalleCompra: precio de compra fuera de los rangos. ctd= " + detalle_local.getPrecioUnitario());
            return response;
        }

        //Si en el detalle de compra no cambió de producto.
        if(detalle_local.getProductoId() == detalle_remoto.getProductoId()){

            //Si el stock del detalle local sube o baja, acá se mostrará cuánto.
            int diferencia = detalle_local.getUnidades() - detalle_remoto.getUnidades();

            //si esto es true, no hay ningún cambio q hacer porque es igual xd.
            if(diferencia == 0 && detalle_local.getPrecioUnitario() == detalle_remoto.getPrecioUnitario()){
                response.isOk();
                return response;
            }

            //Recogemos el producto para actualizarle su stock.
            Response<Producto> prod_request = productoRepo.findById(detalle_local.getProductoId());
            if(!prod_request.isOk()){
                response.internal_error("SCI.editarDetalleCompra: Error al obtener el producto con id= " + detalle_local.getProductoId());
                return response;
            }

            //Actualizamos el stock.
            Producto producto = prod_request.getContent();
            producto.setCantidad(producto.getCantidad() + diferencia);

            if(!productoRepo.update(producto).isOk()){
                response.internal_error("SCI.editarDetalleCompra: Error al actualizar el stock del producto con id= " + detalle_local.getProductoId());
                return response;
            }

        //En el caso de que haya cambiado de producto.
        }else{

            //Obtenemos el producto antiguo
            Response<Producto> prod_down_request = productoRepo.findById(detalle_remoto.getProductoId());
            if(!prod_down_request.isOk()){
                response.internal_error("SCI.editarDetalleCompra: Error al obtener el producto con id= " + detalle_remoto.getProductoId());
                return response;
            }

            //Le quitamos el stock otorgado (down)
            Producto producto_down = prod_down_request.getContent();
            producto_down.setCantidad(producto_down.getCantidad() - detalle_remoto.getUnidades());

            //obtenemos el producto nuevo.
            Response<Producto> prod_up_request = productoRepo.findById(detalle_local.getProductoId());
            if(!prod_up_request.isOk()){
                response.internal_error("SCI.editarDetalleCompra: Error al obtener el producto con id= " + detalle_local.getProductoId());
                return response;
            }
            //Le ponemos más stock... Se le suma porque antes del cambio no se le destinó stock, ahora sí.
            Producto producto_up = prod_up_request.getContent();
            producto_up.setCantidad(producto_up.getCantidad() + detalle_local.getUnidades());

        }


        //Actualizamos ahora si el detalle de compra.
        if (!detalleCompraRepo.update(detalle_local).isOk()) {
            response.internal_error("SCI.editarDetalleCompra: Error al actualizar el detalle de compra.");
            return response;
        }

        response.isOk();
        return response;
    }

    /**
     * Elimina una compra y todos sus detalles de compra. Solo se ejecutará después de revisar errores e internet. Antes no.
     *
     * @param compraId Identificador de la compra.
     * @return Response, indica si la función salió bien o no.
     */
    @Override
    public Response eliminarCompra(int compraId) {
        String Sql = " ";
        Response response = new Response();

        Response<Compra> compra_request = compraRepo.findById(compraId);
        if (!compra_request.isOk()) {
            response.internal_error("SCI.eliminarCompra: La compra seleccionada no existe. Id=" + compraId);
            return response;
        }

        Compra compra = compra_request.getContent();
        Response<List<DetalleCompra>> detcompra_request = detalleCompraRepo.findByCompraId(compraId);

        //Si es que la lista está vacía o hubo errores.
        if (!detcompra_request.isOk()) {
            response.internal_error("SCI.eliminarCompra: Los detalles de compra no fueron encontrados o la compra no tiene detalles. Id=" + compraId);
            return response;
        }

        List<DetalleCompra> detalles = detcompra_request.getContent();

        // Revertir el stock de los productos. Solo se revierten si no saca un stock negativo.
        for (DetalleCompra detalle : detalles) {

            Response<Producto> prod_request = productoRepo.findById(detalle.getProductoId());
            if (!prod_request.isOk()) {
                response.internal_error("SCI.eliminarCompra: El producto referenciado en el detalle de compra no existe. Id = " + detalle.getProductoId());
                return response;
            }

            Producto producto = prod_request.getContent();
            // Validar que el producto no sea temporal (ID negativo)
            if (producto.getId() < 0) {
                System.out.println("Producto temporal ID: " + producto.getId() + " - omitiendo reversión de stock");
                continue;
            }

            int stockActual = producto.getCantidad();
            int nuevoStock = stockActual - detalle.getUnidades();

            // Validar que no quede stock negativo
            if (nuevoStock < 0) {
                response.internal_error("SCI.eliminarCompra: Error.Si se elimina el detalle de compra (id=" +  detalle.getId() + ") el producto (id=" + producto.getId() + ") tendrá stock negativo.");
                return response;
            }

            producto.setCantidad(nuevoStock);

            Sql += "UPDATE producto SET cantidad = " + producto.getCantidad() + " where id_producto = " + producto.getId() + "; \n";
        }

        // La tabla detalleCOmpra no permite el borrado de registros sin antes borrar sus referencias.
        for (DetalleCompra detalle : detalles) {
            Sql += "DELETE from detalle_compra where id_detalle_compra = " + detalle.getId() + "; \n";
        }

        // Finalmente eliminamos la compra
        Sql += "DELETE from compra where id_compra = " + compra.getId() + "; \n";

        //Solo se manda la consulta al final para que, si es que de pronto se va el internet en medio de los for, no haya problemas con el stock de los productos.
        if(!compraRepo.delete(Sql).isOk()){
            response.internal_error("SCI.eliminarCompra: Error al eliminar la Compra.");
        }

        response.exito();
        return response;

    }



}