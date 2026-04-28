package application.proyecto.daos;

import application.proyecto.db.ConexionDB;
import application.proyecto.models.Alerta;
import application.proyecto.models.Materia;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;

public class MaestroDAO {

    // Método para obtener los números de las tarjetas (Total alumnos, clases, etc.)
    public int obtenerContador(String funcion, int idMaestro) {
        String sql = "{? = CALL " + funcion + "(?)}";
        try (Connection con = ConexionDB.getConexion();
             CallableStatement cs = con.prepareCall(sql)) {
            
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, idMaestro);
            cs.execute();
            return cs.getInt(1);
            
        } catch (SQLException e) {
            System.err.println("Error al obtener contador: " + e.getMessage());
            return 0;
        }
    }

    // Método para llenar la tabla de "Mis Grupos"
    public ObservableList<Materia> obtenerMisGrupos(int idMaestro) {
        ObservableList<Materia> lista = FXCollections.observableArrayList();
        String sql = "SELECT clave_materia, nombre_materia, total_alumnos FROM vw_materias_maestro WHERE id_maestro = ?";

        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idMaestro);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(new Materia(
                    0, // id_carga no se usa aquí pero el modelo lo pide
                    rs.getString("clave_materia"),
                    rs.getString("nombre_materia"),
                    rs.getInt("total_alumnos")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener grupos: " + e.getMessage());
        }
        return lista;
    }

    public ObservableList<Alerta> obtenerAlertasMaestro(int idMaestro) {
    ObservableList<Alerta> lista = FXCollections.observableArrayList();
    String sql = "SELECT a.num_control, a.nombre, m.nombre as materia, ta.nombre as tipo " +
                 "FROM alerta al " +
                 "JOIN alumno a ON al.id_alumno = a.id_alumno " +
                 "JOIN carga c ON al.id_carga = c.id_carga " +
                 "JOIN materia m ON c.id_materia = m.id_materia " +
                 "JOIN cat_tipo_alerta ta ON al.id_tipo_alerta = ta.id_tipo_alerta " +
                 "WHERE c.id_maestro = ? AND al.id_estatus_alerta = 1";

    try (Connection con = ConexionDB.getConexion();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, idMaestro);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            lista.add(new Alerta(
                rs.getString("num_control"),
                rs.getString("nombre"),
                rs.getString("materia"),
                rs.getString("tipo")
            ));
        }
    } catch (SQLException e) { e.printStackTrace(); }
    return lista;
}

// Método para obtener las materias de un maestro específico
public ObservableList<String> obtenerNombresMaterias(int idMaestro) {
    ObservableList<String> materias = FXCollections.observableArrayList();
    String sql = "SELECT DISTINCT m.nombre FROM carga c " +
                 "JOIN materia m ON c.id_materia = m.id_materia " +
                 "WHERE c.id_maestro = ? AND c.id_estatus_general = 1";

    try (Connection con = ConexionDB.getConexion();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, idMaestro);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            materias.add(rs.getString("nombre"));
        }
    } catch (SQLException e) { e.printStackTrace(); }
    return materias;
}

// Método para obtener los grupos de un maestro específico
public ObservableList<String> obtenerNombresGrupos(int idMaestro) {
    ObservableList<String> grupos = FXCollections.observableArrayList();
    String sql = "SELECT DISTINCT g.nombre FROM carga c " +
                 "JOIN grupo_ciclo gc ON c.id_grupo_ciclo = gc.id_grupo_ciclo " +
                 "JOIN grupo g ON gc.id_grupo = g.id_grupo " +
                 "WHERE c.id_maestro = ? AND c.id_estatus_general = 1";

    try (Connection con = ConexionDB.getConexion();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, idMaestro);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            grupos.add(rs.getString("nombre"));
        }
    } catch (SQLException e) { e.printStackTrace(); }
    return grupos;
}
}