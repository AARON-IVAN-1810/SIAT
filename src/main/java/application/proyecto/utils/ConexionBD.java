package application.proyecto.utils;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConexionBD {

    private static final String URL = "jdbc:mysql://localhost:3306/bd_siat";
    private static final String USER = "root";
    private static final String PASSWORD = "Aaron1810";

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("error en la conexion a la base de datos");
            e.printStackTrace();
            return null;
        }
    }
}