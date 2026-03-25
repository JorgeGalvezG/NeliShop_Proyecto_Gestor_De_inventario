package io.carpets.repositories.implementacion;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Compra;
import io.carpets.repositories.CompraRepository;
import io.carpets.util.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
    Implementación del repositorio para la entidad Compra
    se encarga de las operaciones CRUD en la base de datos.
*/


public class CompraRepositoryImplementacion implements CompraRepository {

    /**
     * Registra una compra dentro de la base de datos.
     * @param compra Contiene todos los datos de la compra.
     * @return Retorna 'true' si todo salió bien, 'false' si hubo algún problema.
     */
     @Override
     public Response save(Compra compra) {
         Response response = new Response();
        String sql = "INSERT INTO compra (descripcion) VALUES (?)";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, compra.getDescripcion());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    compra.setId(rs.getInt(1)); //El valor (no la variable) de compra ahora contiene el id añadido en la bd.
                }
                response.exito();
            }

        } catch (SQLException e) {
            response.internal_error("CRI.save: " + e.getMessage());
        }
        return response;
    }

    /**
     * Actualiza los datos de la compra.
     * @param compra Contiene los datos de la compra a actualizar.
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public Response update(Compra compra) {
        Response response = new Response();

        String sql = "UPDATE compra SET descripcion=? WHERE id_compra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, compra.getDescripcion());
            stmt.setInt(2, compra.getId());

            if(stmt.executeUpdate() > 0){
               response.exito();
               return response;
            }

            response.internal_error("CRI.update: No se actualizó ningún registro, el id del id a actualizar = " + compra.getId());

        } catch (SQLException e) {
            response.internal_error("CRI.update: " + e.getMessage());

        }
        return response;
    }

    /**
     * Elimina una compra usando su id.
     * @param consulta Consulta de eliminación para una consulta.
     * @return true si es que todo salió bien, false si es que hubo algún error
     */
    @Override
    public Response delete(String consulta) {
        Response response = new Response();
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(consulta)) {

            if(stmt.executeUpdate() > 0){
                response.exito();
                return response;
            }
            response.message_error("CRI.delete: Error en el eliminado de la compra, revisar id de detalle de compra y compra. Consulta= " + consulta);
        } catch (SQLException e) {
            response.message_error("CRI.delete: " + e.getMessage());
        }
        return response;
    }

    /**
     * Encuentra una compra usando su id.
     * @param id Identificador de la compra.
     * @return true si es que todo salió bien, false si es que hubo algún error.
     */
    @Override
    public Response<Compra> findById(int id) {
        Response<Compra> response = new Response<Compra>();
        String sql = "SELECT * FROM v_compras; WHERE id_compra=?";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("id_compra"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setMonto(rs.getDouble("monto"));
                c.setFecha(rs.getDate("fecha"));
                response.exito(c);
                return response;
            }
            response.internal_error("CRI.findById: No se encontró compra alguna con el id= " + id);

        } catch (SQLException e) {
            response.internal_error("CRI.findById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Encuentra todas las compras registradas.
     * @return Una lista con todas las compras y, si hubo algún error o no hay compras, una lista vacía.
     */
    @Override
    public Response<List<Compra>> findAll() {
        Response<List<Compra>> response = new Response<>();
        List<Compra> lista = new ArrayList<>();
        String sql = "SELECT * FROM v_compras;";
        try (Connection conn = ConfiguracionBaseDatos.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("id_compra"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setMonto(rs.getDouble("monto"));
                c.setFecha(rs.getDate("fecha"));
                lista.add(c);
            }

            if(lista.isEmpty()){
                response.internal_error("CRI.findAll: Lista retornada vacía.");
                return  response;
            }

            response.exito(lista);
        } catch (SQLException e) {
            response.internal_error("CRI.findAll: " + e.getMessage());
        }

        return response;
    }

    /**
     * Encuentra las compras hechas entre dos fechas
     * @param Desde Fecha desde se donde se quiere filtrar, si no se desea usar, ponga null.
     * @param Hasta Fecha limite de filtrado, si no lo usará, coloque null.
     * @return Listado de compras hechas entre las dos fechas.
     */
    public Response<List<Compra>> findByDate(Date Desde, Date Hasta){

        Response<List<Compra>> response = new Response<List<Compra>>();
        List<Compra> lista = new ArrayList<>();

        String sql = "SELECT * FROM v_compras WHERE ";
        String and = "";
        if(Desde != null){
            sql += Desde + " <= fecha ";
            and = "and";
        }
        if(Hasta != null){
            sql += and + Hasta + " >= fecha";
        }

        try(Connection conn = ConfiguracionBaseDatos.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            while (rs.next()) {
                Compra c = new Compra();
                c.setId(rs.getInt("id_compra"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setMonto(rs.getDouble("monto"));
                c.setFecha(rs.getDate("fecha"));
                lista.add(c);
            }
            response.exito(lista);

        }catch(SQLException e){
            response.internal_error("CRI.findByDate: " + e.getMessage());
        }

        return response;
    }
}