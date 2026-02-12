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

import java.util.List;
import java.util.UUID;

public class ServicioCompraImplementacion implements ServicioCompra {

    private CompraRepository compraRepo = new CompraRepositoryImplementacion();
    private DetalleCompraRepository detalleCompraRepo = new DetalleCompraRepositoryImplementacion();
    private ProductoRepository productoRepo = new ProductoRepositoryImplementacion();
    private ServicioProducto servicioProducto = new ServicioProductoImplementacion();

    // Límites para validación de datos del producto
    private static final double PRECIO_COMPRA_MINIMO = 0.01;
    private static final double PRECIO_COMPRA_MAXIMO = 100000.0;
    private static final int CANTIDAD_MINIMA = 1;
    private static final int CANTIDAD_MAXIMA = 10000;

    @Override
    public boolean registrarCompra(Compra compra, List<DetalleCompra> detalles) {
        try {

            // Guardar compra
            boolean compraGuardada = compraRepo.save(compra);
            if (!compraGuardada) {
                return false;
            }

            // Obtener el ID de la compra recién guardada
            if (compra.getId() <= 0) {
                return false;
            }

            // Guardar detalles
            for (DetalleCompra detalle : detalles) {
                detalle.setCompraId(compra.getId());

                // Guardar detalle
                boolean detalleOk = detalleCompraRepo.save(detalle);
                if (!detalleOk) return false;
            }

            // Actualizar stock de productos - NUEVA LÓGICA EXTRACTADA
            boolean stockActualizado = actualizarStockPorCompra(detalles);
            if (!stockActualizado) {
                System.err.println("Error al actualizar stock de productos");
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Método para actualizar el stock de productos basado en los detalles de compra
     * Itera los detalles, suma la cantidad al stock de cada producto y persiste los cambios
     * @param detalles Lista de detalles de compra
     * @return true si se actualizó correctamente, false si hubo error
     */
    public boolean actualizarStockPorCompra(List<DetalleCompra> detalles) {
        try {
            System.out.println("Iniciando actualización de stock para " + detalles.size() + " productos...");

            for (DetalleCompra detalle : detalles) {
                // Obtener el producto desde la base de datos
                Producto producto = productoRepo.findById(detalle.getProductoId());
                if (producto == null) {
                    System.err.println("Producto no encontrado con ID: " + detalle.getProductoId());
                    return false;
                }

                // Verificar que el producto no sea temporal (ID negativo)
                if (producto.getId() < 0) {
                    System.out.println("Producto con ID temporal " + producto.getId() + " - omitiendo actualización de stock");
                    continue;
                }

                // Calcular nuevo stock: stock actual + cantidad comprada
                int stockActual = producto.getCantidad();
                int nuevoStock = stockActual + detalle.getUnidades();

                System.out.println("Actualizando stock producto ID: " + producto.getId() +
                        " - Stock anterior: " + stockActual +
                        " + Unidades compradas: " + detalle.getUnidades() +
                        " = Nuevo stock: " + nuevoStock);

                // Actualizar el stock del producto
                producto.setCantidad(nuevoStock);

                // Persistir cambios en la base de datos
                boolean actualizado = productoRepo.update(producto);
                if (!actualizado) {
                    System.err.println("Error al actualizar stock del producto ID: " + producto.getId());
                    return false;
                }

                System.out.println("Stock actualizado correctamente para producto: " + producto.getNombre());
            }

            System.out.println("Actualización de stock completada exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en actualizarStockPorCompra: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public DetalleCompra agregarProductoExistenteACompra(int productoId, int cantidad) {
        try {
            // 1. Validar que el producto existe
            Producto producto = servicioProducto.obtenerPorId(productoId);
            if (producto == null) {
                throw new RuntimeException("Producto no encontrado con ID: " + productoId);
            }

            // 2. Validar cantidad
            if (cantidad < CANTIDAD_MINIMA) {
                throw new RuntimeException("La cantidad debe ser al menos " + CANTIDAD_MINIMA);
            }

            if (cantidad > CANTIDAD_MAXIMA) {
                throw new RuntimeException("La cantidad no puede exceder " + CANTIDAD_MAXIMA);
            }

            // 3. Obtener el precio de compra del producto
            double precioCompra = producto.getPrecioCompra();

            // 4. Validar que el precio de compra sea válido
            if (precioCompra < PRECIO_COMPRA_MINIMO) {
                throw new RuntimeException("El precio de compra del producto no es válido: " + precioCompra);
            }

            // 5. Crear el detalle de compra con los datos automáticos
            DetalleCompra detalle = new DetalleCompra();
            detalle.setProductoId(productoId);
            detalle.setUnidades(cantidad);
            detalle.setPrecioUnitario(precioCompra);

            System.out.println("Producto agregado a compra: " + producto.getNombre());
            System.out.println("Precio de compra automático: " + precioCompra);
            System.out.println("Cantidad: " + cantidad);

            return detalle;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al agregar producto existente a compra: " + e.getMessage());
        }
    }

    @Override
    public List<Compra> listarCompras() {
        return compraRepo.findAll();
    }

    @Override
    public DetalleCompra agregarProductoNuevoACompra(DetalleCompra detalle) {
        try {
            // 1. Validar datos básicos del detalle
            if (!validarDetalleCompra(detalle)) {
                throw new RuntimeException("Datos del producto inválidos");
            }

            // 2. Si el producto no tiene ID (es nuevo), generar uno temporal
            if (detalle.getProductoId() <= 0) {
                // Para productos nuevos, usar un ID temporal negativo
                // Esto indica que es un producto que aún no existe en la base de datos
                detalle.setProductoId(generarIdTemporal());
            }

            // 3. Retornar el detalle validado y listo para agregar a la compra
            return detalle;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al agregar producto a compra: " + e.getMessage());
        }
    }

    @Override
    public boolean validarDatosProductoNuevo(String codigo, int cantidad, double precioCompra) {
        // 1. Validar código (no vacío y longitud razonable)
        if (codigo == null || codigo.trim().isEmpty()) {
            System.out.println("El código del producto no puede estar vacío");
            return false;
        }

        if (codigo.trim().length() > 50) {
            System.out.println("El código del producto es demasiado largo");
            return false;
        }

        // 2. Validar cantidad
        if (cantidad < CANTIDAD_MINIMA) {
            System.out.println("La cantidad debe ser al menos " + CANTIDAD_MINIMA);
            return false;
        }

        if (cantidad > CANTIDAD_MAXIMA) {
            System.out.println("La cantidad no puede exceder " + CANTIDAD_MAXIMA);
            return false;
        }

        // 3. Validar precio de compra
        if (precioCompra < PRECIO_COMPRA_MINIMO) {
            System.out.println("El precio de compra debe ser mayor a " + PRECIO_COMPRA_MINIMO);
            return false;
        }

        if (precioCompra > PRECIO_COMPRA_MAXIMO) {
            System.out.println("El precio de compra no puede exceder " + PRECIO_COMPRA_MAXIMO);
            return false;
        }

        return true;
    }

    /**
     * Valida los datos básicos de un DetalleCompra
     */
    private boolean validarDetalleCompra(DetalleCompra detalle) {
        if (detalle == null) {
            System.out.println("El detalle de compra no puede ser nulo");
            return false;
        }

        // Validar unidades
        if (detalle.getUnidades() < CANTIDAD_MINIMA) {
            System.out.println("Las unidades deben ser al menos " + CANTIDAD_MINIMA);
            return false;
        }

        if (detalle.getUnidades() > CANTIDAD_MAXIMA) {
            System.out.println("Las unidades no pueden exceder " + CANTIDAD_MAXIMA);
            return false;
        }

        // Validar que tenga al menos un identificador (productoId)
        if (detalle.getProductoId() == 0) {
            System.out.println("El producto debe tener un identificador válido");
            return false;
        }

        return true;
    }

    /**
     * Genera un ID temporal para productos nuevos
     * Los IDs temporales son negativos para diferenciarlos de los reales
     */
    private int generarIdTemporal() {
        return -Math.abs(UUID.randomUUID().hashCode() % 1000000);
    }

    @Override
    public boolean eliminarDetalleCompra(int detalleId) {
        try {
            // 1. Validar que el detalle existe
            DetalleCompra detalle = detalleCompraRepo.findById(detalleId);
            if (detalle == null) {
                System.out.println("Detalle de compra no encontrado con ID: " + detalleId);
                return false;
            }

            System.out.println("Eliminando detalle de compra ID: " + detalleId +
                    " de la compra ID: " + detalle.getCompraId());

            // 2. Eliminar el detalle usando el repository
            boolean eliminado = detalleCompraRepo.delete(detalleId);

            if (eliminado) {
                System.out.println("Detalle de compra eliminado exitosamente");
            } else {
                System.out.println("Error al eliminar detalle de compra");
            }

            return eliminado;

        } catch (Exception e) {
            System.err.println("Error en eliminarDetalleCompra: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean editarDetalleCompra(int detalleId, int cantidad, double precio) {
        try {
            // 1. Validar que el detalle existe
            DetalleCompra detalle = detalleCompraRepo.findById(detalleId);
            if (detalle == null) {
                System.out.println("Detalle de compra no encontrado con ID: " + detalleId);
                return false;
            }

            // 2. Validar cantidad
            if (cantidad < CANTIDAD_MINIMA) {
                System.out.println("La cantidad debe ser al menos " + CANTIDAD_MINIMA);
                return false;
            }

            if (cantidad > CANTIDAD_MAXIMA) {
                System.out.println("La cantidad no puede exceder " + CANTIDAD_MAXIMA);
                return false;
            }

            // 3. Validar precio
            if (precio < PRECIO_COMPRA_MINIMO) {
                System.out.println("El precio debe ser mayor a " + PRECIO_COMPRA_MINIMO);
                return false;
            }

            if (precio > PRECIO_COMPRA_MAXIMO) {
                System.out.println("El precio no puede exceder " + PRECIO_COMPRA_MAXIMO);
                return false;
            }

            // 4. Verificar si hubo cambios reales
            boolean cambios = false;
            if (detalle.getUnidades() != cantidad) {
                System.out.println("Actualizando cantidad: " + detalle.getUnidades() + " → " + cantidad);
                detalle.setUnidades(cantidad);
                cambios = true;
            }

            if (detalle.getPrecioUnitario() != precio) {
                System.out.println("Actualizando precio: " + detalle.getPrecioUnitario() + " → " + precio);
                detalle.setPrecioUnitario(precio);
                cambios = true;
            }

            if (!cambios) {
                System.out.println("No hay cambios para aplicar al detalle ID: " + detalleId);
                return true; // No hay cambios, pero no es un error
            }

            // 5. Actualizar el detalle en la base de datos
            boolean actualizado = detalleCompraRepo.update(detalle);

            if (actualizado) {
                System.out.println("Detalle de compra actualizado exitosamente");
                System.out.println("   - ID: " + detalleId);
                System.out.println("   - Cantidad: " + cantidad);
                System.out.println("   - Precio unitario: " + precio);
            } else {
                System.out.println("Error al actualizar detalle de compra");
            }

            return actualizado;

        } catch (Exception e) {
            System.err.println("Error en editarDetalleCompra: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean eliminarCompra(int compraId) {
        try {
            // 1. Validar que la compra existe
            Compra compra = compraRepo.findById(compraId);
            if (compra == null) {
                System.out.println("Compra no encontrada con ID: " + compraId);
                return false;
            }

            System.out.println("Eliminando compra ID: " + compraId + " - Descripción: " + compra.getDescripcion());

            // 2. Obtener todos los detalles de la compra
            List<DetalleCompra> detalles = detalleCompraRepo.findByCompraId(compraId);
            System.out.println("Encontrados " + detalles.size() + " detalles para la compra ID: " + compraId);

            // 3. Revertir el stock de los productos (restar lo que se había sumado en la compra)
            for (DetalleCompra detalle : detalles) {
                Producto producto = productoRepo.findById(detalle.getProductoId());
                if (producto != null) {
                    // Validar que el producto no sea temporal (ID negativo)
                    if (producto.getId() < 0) {
                        System.out.println("Producto temporal ID: " + producto.getId() + " - omitiendo reversión de stock");
                        continue;
                    }

                    int stockActual = producto.getCantidad();
                    int nuevoStock = stockActual - detalle.getUnidades(); // RESTAMOS lo que se había sumado

                    // Validar que no quede stock negativo
                    if (nuevoStock < 0) {
                        System.err.println("Advertencia: Stock negativo para producto ID: " + producto.getId() +
                                " - Stock resultante: " + nuevoStock);
                        // Podemos continuar o detener según tu política de negocio
                    }

                    System.out.println("Revirtiendo stock producto ID: " + producto.getId() +
                            " - Stock actual: " + stockActual +
                            " - Unidades compradas: " + detalle.getUnidades() +
                            " = Nuevo stock: " + nuevoStock);

                    producto.setCantidad(nuevoStock);
                    boolean stockActualizado = productoRepo.update(producto);

                    if (!stockActualizado) {
                        System.err.println("Error al revertir stock del producto ID: " + producto.getId());
                        return false;
                    }
                } else {
                    System.err.println("Producto no encontrado ID: " + detalle.getProductoId() + " - continuando...");
                }
            }

            // 4. Eliminar los detalles de compra primero (por integridad referencial)
            for (DetalleCompra detalle : detalles) {
                boolean detalleEliminado = detalleCompraRepo.delete(detalle.getId());
                if (!detalleEliminado) {
                    System.err.println("Error al eliminar detalle ID: " + detalle.getId());
                    // Continuamos aunque falle uno, para intentar limpiar lo máximo posible
                }
            }

            // 5. Finalmente eliminar la compra
            boolean compraEliminada = compraRepo.delete(compraId);

            if (compraEliminada) {
                System.out.println(" Compra eliminada exitosamente:");
                System.out.println("   - ID: " + compraId);
                System.out.println("   - Descripción: " + compra.getDescripcion());
                System.out.println("   - Monto: " + compra.getMonto());
                System.out.println("   - Stock revertido para " + detalles.size() + " productos");
            } else {
                System.err.println(" Error al eliminar la compra de la base de datos");
            }

            return compraEliminada;

        } catch (Exception e) {
            System.err.println("Error en eliminarCompra: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }




}