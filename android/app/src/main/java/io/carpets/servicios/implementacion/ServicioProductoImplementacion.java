package io.carpets.servicios.implementacion;

import io.carpets.entidades.Producto;
import io.carpets.repositories.ProductoRepository;
import io.carpets.repositories.implementacion.ProductoRepositoryImplementacion;
import io.carpets.servicios.ServicioProducto;

import java.util.Date;
import java.util.List;

public class ServicioProductoImplementacion implements ServicioProducto {

    private ProductoRepository repo = new ProductoRepositoryImplementacion();

    @Override
    public boolean validarStock(int productoId, int cantidad) {
        Producto p = repo.findById(productoId);
        return p != null && p.getCantidad() >= cantidad;
    }

    @Override
    public void actualizarInventario(Producto producto) {
        repo.update(producto);
    }

    public double getGananciaTotal(){
        return repo.getGananciaTotal();
    }

    @Override
    public List<Producto> obtenerTodos() {
        return repo.findAll();
    }

    @Override
    public Producto obtenerPorId(int id) {
        return repo.findById(id);
    }

    @Override
    public List<Producto> buscarProductos(String criterio, String tipo) {
        if (criterio == null || criterio.trim().isEmpty()) {
            return repo.findAll();
        }

        if (tipo == null || "all".equalsIgnoreCase(tipo)) {
            // Búsqueda en todos los campos disponibles
            List<Producto> resultados = new java.util.ArrayList<>();
            resultados.addAll(repo.findByNombre(criterio));
            resultados.addAll(repo.findByCategoria(criterio));
            // Eliminar duplicados
            return resultados.stream().distinct().collect(java.util.stream.Collectors.toList());
        }

        switch (tipo.toLowerCase()) {
            case "nombre":
                return repo.findByNombre(criterio);
            case "categoria":
                return repo.findByCategoria(criterio);
            default:
                return repo.findAll();
        }
    }

    // AGREGA ESTE MÉTODO MEJORADO a tu clase existente
    @Override
    public boolean agregarProducto(Producto producto) {
        try {
            // 1. Validaciones básicas
            if (producto == null) {
                System.out.println("El producto no puede ser nulo");
                return false;
            }

            // 2. Validar nombre
            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                System.out.println("El nombre del producto es requerido");
                return false;
            }

            if (producto.getNombre().trim().length() > 100) {
                System.out.println("El nombre del producto es demasiado largo");
                return false;
            }

            // 3. Validar precios
            if (producto.getPrecioCompra() <= 0) {
                System.out.println("El precio de compra debe ser mayor a 0");
                return false;
            }

            if (producto.getPrecioVenta() <= 0) {
                System.out.println("El precio de venta debe ser mayor a 0");
                return false;
            }

            if (producto.getPrecioVenta() < producto.getPrecioCompra()) {
                System.out.println("El precio de venta no puede ser menor al precio de compra");
                return false;
            }

            // 4. Validar cantidad
            if (producto.getCantidad() < 0) {
                System.out.println("La cantidad no puede ser negativa");
                return false;
            }

            // 5. Validar categoría
            if (producto.getCategoriaNombre() == null || producto.getCategoriaNombre().trim().isEmpty()) {
                System.out.println("La categoría del producto es requerida");
                return false;
            }

            // 6. Validar que no exista un producto con el mismo nombre (opcional)
            // Esto es opcional - depende si quieres nombres únicos
            List<Producto> productosMismoNombre = repo.findByNombre(producto.getNombre().trim());
            if (productosMismoNombre != null && !productosMismoNombre.isEmpty()) {
                System.out.println("Ya existe un producto con el nombre: " + producto.getNombre());
                return false;
            }

            // 7. Establecer fecha de ingreso si no está establecida
            if (producto.getFechaIngreso() == null) {
                producto.setFechaIngreso(new Date());
            }

            // 8. Guardar en la base de datos
            boolean guardado = repo.save(producto);

            if (guardado) {
                System.out.println("Producto agregado exitosamente:");
                System.out.println("   - Nombre: " + producto.getNombre());
                System.out.println("   - Precio Compra: " + producto.getPrecioCompra());
                System.out.println("   - Precio Venta: " + producto.getPrecioVenta());
                System.out.println("   - Stock: " + producto.getCantidad());
                System.out.println("   - Categoría: " + producto.getCategoriaNombre());
            } else {
                System.out.println("Error al guardar el producto en la base de datos");
            }

            return guardado;

        } catch (Exception e) {
            System.err.println("Error en agregarProducto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean eliminarProducto(int idProducto) {
        try {
            // 1. Validar que el ID sea válido
            if (idProducto <= 0) {
                System.out.println("ID de producto inválido: " + idProducto);
                return false;
            }

            // 2. Validar que el producto existe
            Producto producto = repo.findById(idProducto);
            if (producto == null) {
                System.out.println("Producto no encontrado con ID: " + idProducto);
                return false;
            }

            // 3. Validar que el producto no tenga stock
            if (producto.getCantidad() > 0) {
                System.out.println("No se puede eliminar producto con stock existente: " + producto.getCantidad() + " unidades");
                return false;
            }

            // 4. Eliminar el producto
            boolean eliminado = repo.delete(idProducto);

            if (eliminado) {
                System.out.println(" Producto eliminado exitosamente:");
                System.out.println("   - ID: " + idProducto);
                System.out.println("   - Nombre: " + producto.getNombre());
            } else {
                System.out.println(" Error al eliminar el producto de la base de datos");
            }

            return eliminado;

        } catch (Exception e) {
            System.err.println("Error en eliminarProducto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}