package io.carpets.Configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import io.carpets.Credenciales;


public class ConfiguracionBaseDatos {

    // Datos de conexión
    private static Connection connection = null;

    // Obtener conexión
    public static Connection getConnection() throws SQLException {

        if (connection == null || connection.isClosed()) {
            try {
                // Cargar driver de MySQL
                Class.forName("com.mysql.jdbc.Driver");

                String url = "jdbc:mysql://" + Credenciales.HOST + ":" + Credenciales.PORT + "/" + Credenciales.DATABASE
                        + "?useSSL=true&requireSSL=true&verifyServerCertificate=false&serverTimezone=UTC&enabledTLSProtocols=TLSv1.2";
                connection = DriverManager.getConnection(url, Credenciales.USER, Credenciales.PASSWORD);
                System.out.println("Conexión a la base de datos establecida.");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new SQLException("Driver JDBC no encontrado.");
            }
        }
        return connection;
    }

    // Cerrar conexión
    public static void cerrarConexion() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Conexión cerrada correctamente.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}