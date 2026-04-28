package application.proyecto.daos;

import application.proyecto.db.ConexionDB;
import application.proyecto.models.Usuario;
import application.proyecto.models.Alerta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.*;

public class UsuarioDAO {

    public Usuario login(String user, String pass) {
        String sql = "SELECT u.id_usuario, u.usuario, r.nombre as rol " +
                     "FROM usuario u " +
                     "JOIN usuario_rol ur ON u.id_usuario = ur.id_usuario " +
                     "JOIN rol r ON ur.id_rol = r.id_rol " +
                     "WHERE u.usuario = ? AND u.password_hash = ? AND u.id_estatus_general = 1";

        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, user);
            ps.setString(2, pass);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                        rs.getInt("id_usuario"),
                        rs.getString("usuario"),
                        rs.getString("rol")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en el login: " + e.getMessage());
        }
        return null;
    }
}