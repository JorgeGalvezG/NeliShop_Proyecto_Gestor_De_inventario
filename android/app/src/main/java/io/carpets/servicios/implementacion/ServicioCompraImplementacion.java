package io.carpets.servicios.implementacion;

import io.carpets.DTOs.CompraCompletaDTO;
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

import java.util.Collections;
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

        // Guardar la cabecera de la compra
        if (!compraRepo.save(compra).isOk()) {
            response.internal_error("SCI.registrarCompra: Error guardando compra.");
            return response;
        }

        // Guardar cada detalle asociándolo al ID de la compra generada
        for (DetalleCompra detalle : detalles) {
            detalle.setCompraId(compra.getId());
            if (!detalleCompraRepo.save(detalle).isOk()) {
                response.internal_error("SCI.registrarCompra: Error al guardar detalle de compra.");
                return response;
            }
        }

        // Actualizar el stock en el inventario
        Response stockUpdate = actualizarStockPorCompra(detalles);
        if (!stockUpdate.isOk()) {
            response.internal_error("SCI.registrarCompra: Error al actualizar Stock -> " + stockUpdate.getMap().get("mensaje"));
            return response;
        }

        response.exito();
        return response;
    }

    public Response actualizarDescripcionCompra(Compra compra) {
        Response response = compraRepo.update(compra);
        if (!response.isOk()) {
            response.internal_error("SCI.actualizarDescripcionCompra: Error al actualizar descripción.");
        }
        return response;
    }

    /**
     * Suma la cantidad comprada al stock actual de los productos en la base de datos.
     */
    public Response actualizarStockPorCompra(List<DetalleCompra> detalles) {
        Response response = new Response();
        try {
            for (DetalleCompra detalle : detalles) {
                Response<Producto> request = productoRepo.findById(detalle.getProductoId());

                if (!request.isOk()) {
                    response.internal_error("SCI.actualizarStockPorCompra: Error al obtener el producto, id= " + detalle.getProductoId());
                    return response;
                }

                Producto producto = request.getContent();

                if (producto.getId() < 0) {
                    continue; // Ignorar productos temporales
                }

                int nuevoStock = producto.getCantidad() + detalle.getUnidades();
                producto.setCantidad(nuevoStock);

                // Persistir el nuevo stock
                if (!productoRepo.update(producto).isOk()) {
                    response.internal_error("SCI.actualizarStockPorCompra: Error al actualizar stock del producto ID: " + producto.getId());
                    return response;
                }
            }
            response.exito();
            return response;
        } catch (Exception e) {
            response.internal_error("Error en actualizarStockPorCompra: " + e.getMessage());
            return response;
        }
    }
    @Override
    public DetalleCompra agregarProductoExistenteACompra(int productoId, int cantidad) {
        try {
            // Extraemos el Response primero
            Response<Producto> prodResponse = servicioProducto.obtenerPorId(productoId);

            // Validamos que sea exitoso y traiga contenido
            if (!prodResponse.isOk() || prodResponse.getContent() == null) {
                throw new RuntimeException("Producto no encontrado con ID: " + productoId);
            }

            // Recién aquí sacamos el Producto
            Producto producto = prodResponse.getContent();

            if (cantidad < CANTIDAD_MINIMA || cantidad > CANTIDAD_MAXIMA) throw new RuntimeException("Cantidad fuera de rango válido");

            double precioCompra = producto.getPrecioCompra();
            if (precioCompra < PRECIO_COMPRA_MINIMO) throw new RuntimeException("Precio de compra inválido: " + precioCompra);

            DetalleCompra detalle = new DetalleCompra();
            detalle.setProductoId(productoId);
            detalle.setUnidades(cantidad);
            detalle.setPrecioUnitario(precioCompra);
            return detalle;

        } catch (Exception e) {
            throw new RuntimeException("Error al agregar producto: " + e.getMessage());
        }
    }

    @Override
    public Response agregarProductoNuevoACompra(DetalleCompra detalle) {
        Response response = new Response();
        try {
            if (detalle == null || detalle.getUnidades() <= 0) {
                response.internal_error("Datos del producto inválidos");
                return response;
            }

            if (detalle.getProductoId() <= 0) {
                // Asignar ID temporal negativo simulado para consistencia con frontend
                detalle.setProductoId((int) (System.currentTimeMillis() % 100000) * -1);
            }

            response.exito(detalle);
            return response;
        } catch (Exception e) {
            response.internal_error("Error al registrar producto nuevo: " + e.getMessage());
            return response;
        }
    }

    /**
     * Construye un resumen en forma de mapas (diccionarios) de las compras y extrae
     * una imagen de muestra del primer producto adquirido para la interfaz.
     */
    @Override
    public Response<List<Map<String, Object>>> listarCompras() {
        Response<List<Map<String, Object>>> response = new Response<>();

        Response<List<Compra>> request = compraRepo.findAll();
        if (!request.isOk()) {
            response.internal_error("SCI.listarCompras: Error al listar las compras.");
            return response;
        }

        List<Compra> compras = request.getContent();
        List<Map<String, Object>> listaMapas = new ArrayList<>();

        for (Compra c : compras) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("descripcion", c.getDescripcion());
            map.put("monto", c.getMonto());

            Response<List<DetalleCompra>> det_request = detalleCompraRepo.findByCompraId(c.getId());
            if(!det_request.isOk()) {
                response.internal_error("SCI.listaCompras: Error al obtener detalles id=" + c.getId());
                return response;
            }

            List<DetalleCompra> detalles = det_request.getContent();
            if (!detalles.isEmpty()) {
                Response<Producto> prod_request = productoRepo.findById(detalles.get(0).getProductoId());
                if(prod_request.isOk()) {
                    Producto p = prod_request.getContent();
                    map.put("imagePath", p.getImagePath());
                }
            }
            listaMapas.add(map);
        }
        response.exito(listaMapas);
        return response;
    }

    @Override
    public List<CompraCompletaDTO> listarComprasConDetalles() {
        return Collections.emptyList();
    }

    @Override
    public Response eliminarDetalleCompra(int detalleId) {
        Response response = new Response();

        if (!detalleCompraRepo.findById(detalleId).isOk()) {
            response.internal_error("SCI.eliminarDetalleCompra: Detalle no encontrado, id= " + detalleId);
            return response;
        }

        if (!detalleCompraRepo.delete(detalleId).isOk()) {
            response.internal_error("SCI.eliminarDetalleCompra: Error al eliminar el detalle.");
            return response;
        }

        response.exito();
        return response;
    }

    public Response editarDetalleCompra(DetalleCompra detalle_local) {
        Response response = new Response();

        Response<DetalleCompra> request = detalleCompraRepo.findById(detalle_local.getId());
        if (!request.isOk()) {
            response.internal_error("SCI.editarDetalleCompra: Error al ubicar el detalle original.");
            return response;
        }
        DetalleCompra detalle_remoto = request.getContent();

        if (detalle_local.getUnidades() < CANTIDAD_MINIMA || detalle_local.getUnidades() > CANTIDAD_MAXIMA) {
            response.internal_error("Cantidad de compra fuera de rango.");
            return response;
        }

        if (detalle_local.getPrecioUnitario() < PRECIO_COMPRA_MINIMO || detalle_local.getPrecioUnitario() > PRECIO_COMPRA_MAXIMO) {
            response.internal_error("Precio de compra fuera de rango.");
            return response;
        }

        if (detalle_local.getProductoId() == detalle_remoto.getProductoId()) {
            int diferencia = detalle_local.getUnidades() - detalle_remoto.getUnidades();

            if (diferencia == 0 && detalle_local.getPrecioUnitario() == detalle_remoto.getPrecioUnitario()) {
                response.exito();
                return response;
            }

            Response<Producto> prod_request = productoRepo.findById(detalle_local.getProductoId());
            if (!prod_request.isOk()) {
                response.internal_error("SCI.editarDetalleCompra: Error obteniendo producto original.");
                return response;
            }

            Producto producto = prod_request.getContent();
            producto.setCantidad(producto.getCantidad() + diferencia);

            if (!productoRepo.update(producto).isOk()) {
                response.internal_error("SCI.editarDetalleCompra: Error actualizando stock.");
                return response;
            }

        } else {
            // Producto cambió: Restaurar stock antiguo y asignar al nuevo
            Response<Producto> prod_down_request = productoRepo.findById(detalle_remoto.getProductoId());
            if (prod_down_request.isOk()) {
                Producto producto_down = prod_down_request.getContent();
                producto_down.setCantidad(producto_down.getCantidad() - detalle_remoto.getUnidades());
                productoRepo.update(producto_down);
            }

            Response<Producto> prod_up_request = productoRepo.findById(detalle_local.getProductoId());
            if (prod_up_request.isOk()) {
                Producto producto_up = prod_up_request.getContent();
                producto_up.setCantidad(producto_up.getCantidad() + detalle_local.getUnidades());
                productoRepo.update(producto_up);
            }
        }

        if (!detalleCompraRepo.update(detalle_local).isOk()) {
            response.internal_error("SCI.editarDetalleCompra: Error actualizando registro.");
            return response;
        }

        response.exito();
        return response;
    }

    /**
     * Elimina una compra y todos sus detalles.
     * En lugar de construir un String SQL masivo, utiliza el patrón Repository
     * procesando elemento por elemento para garantizar la seguridad (Prevención de SQL Injection).
     */
    @Override
    public Response eliminarCompra(int compraId) {
        Response response = new Response();

        try {
            Response<Compra> compra_request = compraRepo.findById(compraId);
            if (!compra_request.isOk()) {
                response.internal_error("SCI.eliminarCompra: La compra no existe. Id=" + compraId);
                return response;
            }

            Response<List<DetalleCompra>> detcompra_request = detalleCompraRepo.findByCompraId(compraId);
            if (!detcompra_request.isOk()) {
                response.internal_error("SCI.eliminarCompra: Detalles no encontrados.");
                return response;
            }

            List<DetalleCompra> detalles = detcompra_request.getContent();

            // 1. Revertir el stock y eliminar los detalles individuales
            for (DetalleCompra detalle : detalles) {
                Response<Producto> prod_request = productoRepo.findById(detalle.getProductoId());

                if (prod_request.isOk()) {
                    Producto producto = prod_request.getContent();

                    if (producto.getId() >= 0) {
                        int nuevoStock = producto.getCantidad() - detalle.getUnidades();
                        if (nuevoStock < 0) {
                            response.internal_error("Error: Revertir esta compra causaría stock negativo en ID: " + producto.getId());
                            return response;
                        }
                        producto.setCantidad(nuevoStock);
                        productoRepo.update(producto);
                    }
                }
                // Eliminar el detalle después de ajustar stock
                detalleCompraRepo.delete(detalle.getId());
            }

            // 2. Eliminar la cabecera (Compra) final
            if (!compraRepo.delete(compraId).isOk()) {
                response.internal_error("SCI.eliminarCompra: Error al eliminar la cabecera de la compra.");
                return response;
            }

            response.exito();
            return response;

        } catch (Exception e) {
            response.internal_error("Error grave en eliminación: " + e.getMessage());
            return response;
        }
    }
}