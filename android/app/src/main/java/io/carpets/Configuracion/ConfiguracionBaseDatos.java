package io.carpets.Configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import io.carpets.Credenciales;

public class ConfiguracionBaseDatos {

    /**
     * Fabrica y retorna una nueva conexion a la base de datos.
     * Al devolver una instancia nueva en cada llamada, evitamos colisiones de hilos (Thread-Safety).
     * El cierre de esta conexion se delega a los bloques try-with-resources de los Repositorios.
     * * @return Connection Conexion activa a MySQL
     * @throws SQLException Si falla la conexion o no se encuentra el driver
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            String url = "jdbc:mysql://" + Credenciales.HOST + ":" + Credenciales.PORT + "/" + Credenciales.DATABASE
                    + "?useSSL=true&requireSSL=true&verifyServerCertificate=false"
                    + "&serverTimezone=UTC&enabledTLSProtocols=TLSv1.2";

            // Se crea y retorna la conexion directamente.
            // No se imprime en consola para no saturar el Logcat en consultas masivas.
            return DriverManager.getConnection(url, Credenciales.USER, Credenciales.PASSWORD);

        } catch (ClassNotFoundException e) {
            System.err.println("ConfiguracionBaseDatos.getConnection: " + e.getMessage());
            throw new SQLException("ConfiguracionBaseDatos: Driver JDBC no encontrado.");
        }
    }
}