package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BitacoraTController extends BaseController {

    @FXML private ComboBox<ItemCombo> cbGrupoBitacora;
    @FXML private ComboBox<ItemCombo> cbAlumnoBitacora;
    @FXML private Button btnBuscarAlumnoBitacora;
    @FXML private ListView<String> listBitacoraAlumno;
    @FXML private Label lblMensajeBitacora;

    @FXML
    public void initialize() {
        cargarGruposTutor();

        cbGrupoBitacora.setOnAction(event -> cargarAlumnosPorGrupo());
        btnBuscarAlumnoBitacora.setOnAction(event -> handleBuscarAlumno());

        lblMensajeBitacora.setText("Selecciona un alumno para visualizar su seguimiento");
    }

    private int getIdTutorActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void cargarGruposTutor() {
        cbGrupoBitacora.getItems().clear();

        String sql = """
                select distinct
                gc.id_grupo_ciclo,
                g.nombre as grupo
                from tutoria_asignacion ta
                inner join grupo_ciclo gc on ta.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                order by g.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, getIdTutorActual());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbGrupoBitacora.getItems().add(new ItemCombo(
                            rs.getInt("id_grupo_ciclo"),
                            rs.getString("grupo")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos tutorados");
        }
    }

    private void cargarAlumnosPorGrupo() {
        cbAlumnoBitacora.getItems().clear();
        listBitacoraAlumno.getItems().clear();

        ItemCombo grupo = cbGrupoBitacora.getValue();

        if (grupo == null) {
            return;
        }

        String sql = """
                select
                id_alumno,
                concat(num_control,' - ',nombre,' ',apellido_paterno,' ',apellido_materno) as alumno
                from alumno
                where id_grupo_ciclo=?
                and id_estatus_general=1
                order by apellido_paterno,apellido_materno,nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, grupo.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbAlumnoBitacora.getItems().add(new ItemCombo(
                            rs.getInt("id_alumno"),
                            rs.getString("alumno")
                    ));
                }
            }

            lblMensajeBitacora.setText("Selecciona un alumno y presiona Buscar");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alumnos");
        }
    }

    @FXML
    private void handleBuscarAlumno() {
        ItemCombo alumno = cbAlumnoBitacora.getValue();

        if (alumno == null) {
            mostrarError("selecciona un alumno");
            return;
        }

        cargarBitacoraAlumno(alumno.getId());
    }

    private void cargarBitacoraAlumno(int idAlumno) {
        listBitacoraAlumno.getItems().clear();

        String sql = """
                select evento,fecha_evento
                from(
                    select
                    concat('[ALERTA] ',cta.nombre,' | ',ifnull(m.nombre,'sin materia'),' | ',ifnull(a.motivo,'sin motivo')) as evento,
                    a.creada_en as fecha_evento
                    from alerta a
                    inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                    left join carga c on a.id_carga=c.id_carga
                    left join materia m on c.id_materia=m.id_materia
                    where a.id_alumno=?

                    union all

                    select
                    concat('[REPORTE] ',cta.nombre,' | ',ifnull(m.nombre,'sin materia'),' | ',ifnull(rd.descripcion,'sin descripcion')) as evento,
                    rd.creado_en as fecha_evento
                    from reporte_docente rd
                    inner join cat_tipo_alerta cta on rd.id_tipo_alerta=cta.id_tipo_alerta
                    left join carga c on rd.id_carga=c.id_carga
                    left join materia m on c.id_materia=m.id_materia
                    where rd.id_alumno=?

                    union all

                    select
                    concat('[ORIENTACION] ',it.tipo_intervencion,' | acuerdos: ',ifnull(it.acuerdos,'sin acuerdos'),' | ',ifnull(it.observaciones,'sin observaciones')) as evento,
                    it.fecha_intervencion as fecha_evento
                    from intervencion_tutor it
                    where it.id_alumno=?
                ) eventos
                order by fecha_evento desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAlumno);
            ps.setInt(2, idAlumno);
            ps.setInt(3, idAlumno);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listBitacoraAlumno.getItems().add(
                            rs.getString("fecha_evento") + "  -  " + rs.getString("evento")
                    );
                }
            }

            if (listBitacoraAlumno.getItems().isEmpty()) {
                lblMensajeBitacora.setText("No hay eventos registrados para este alumno");
            } else {
                lblMensajeBitacora.setText("Eventos encontrados: " + listBitacoraAlumno.getItems().size());
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar bitacora del alumno");
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class ItemCombo {
        private final int id;
        private final String nombre;

        public ItemCombo(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
}