package io.carpets.servicios.implementacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import io.carpets.Configuracion.ConfiguracionBaseDatos;
import io.carpets.entidades.Usuario;
import io.carpets.repositories.UsuarioRepository;
import io.carpets.repositories.implementacion.UsuarioRepositoryImplementacion;
import io.carpets.servicios.ServicioUsuario;
import io.carpets.util.Response;

/*
    Implementación del servicio para la entidad Usuario
    se encarga de la lógica de negocio relacionada con los usuarios.
*/
public class ServicioUsuarioImplementacion implements ServicioUsuario {

    private UsuarioRepository repo = new UsuarioRepositoryImplementacion();

    @Override
    public Response<Map<String, Object>> login(String username, String password) {
        Response <Map<String, Object>> response = repo.login(username, password);
        if(!response.isOk()){
            response.internal_error("SUI.login: Error al obtener usuario.");
        }
        return response;
    }
}