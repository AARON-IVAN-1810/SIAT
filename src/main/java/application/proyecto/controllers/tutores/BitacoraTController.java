package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
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
        cbAlumnoBitacora.getItems().clear();
        listBitacoraAlumno.getItems().clear();

        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        String sql = """
                select distinct
                gc.id_grupo_ciclo,
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo
                from tutoria_asignacion ta
                inner join grupo_ciclo gc on ta.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                order by grupo
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idTutor);

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
            lblMensajeBitacora.setText("Selecciona un grupo tutorado");
            return;
        }

        String sql = """
                select distinct
                al.id_alumno,
                concat(al.num_control,' - ',al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as alumno
                from alumno al
                left join alumno_carga ac on al.id_alumno=ac.id_alumno
                and ac.id_estatus_general=1
                left join carga c on ac.id_carga=c.id_carga
                and c.id_estatus_general=1
                where al.id_estatus_general=1
                and (
                    al.id_grupo_ciclo=?
                    or c.id_grupo_ciclo=?
                )
                order by alumno
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, grupo.getId());
            ps.setInt(2, grupo.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbAlumnoBitacora.getItems().add(new ItemCombo(
                            rs.getInt("id_alumno"),
                            rs.getString("alumno")
                    ));
                }
            }

            if (cbAlumnoBitacora.getItems().isEmpty()) {
                lblMensajeBitacora.setText("No hay alumnos relacionados con este grupo o sus clases");
            } else {
                lblMensajeBitacora.setText("Selecciona un alumno y presiona Buscar");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alumnos");
        }
    }

    @FXML
    private void handleBuscarAlumno() {
        ItemCombo grupo = cbGrupoBitacora.getValue();
        ItemCombo alumno = cbAlumnoBitacora.getValue();

        if (grupo == null) {
            mostrarError("selecciona un grupo");
            return;
        }

        if (alumno == null) {
            mostrarError("selecciona un alumno");
            return;
        }

        cargarBitacoraAlumno(alumno.getId(), grupo.getId());
    }

    private void cargarBitacoraAlumno(int idAlumno, int idGrupoCiclo) {
        listBitacoraAlumno.getItems().clear();

        String sql = """
                select evento,fecha_evento
                from(
                    select
                    concat(
                    '[ALERTA] ',
                    cta.nombre,
                    ' | ',
                    concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre),
                    ' | ',
                    ifnull(m.nombre,'sin materia'),
                    ' | ',
                    ifnull(a.motivo,'sin motivo')
                    ) as evento,
                    a.creada_en as fecha_evento
                    from alerta a
                    inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                    inner join carga c on a.id_carga=c.id_carga
                    inner join materia m on c.id_materia=m.id_materia
                    inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                    inner join grupo g on gc.id_grupo=g.id_grupo
                    inner join cat_turno ct on g.id_turno=ct.id_turno
                    inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                    where a.id_alumno=?
                    and c.id_grupo_ciclo=?

                    union all

                    select
                    concat(
                    '[REPORTE] ',
                    cta.nombre,
                    ' | ',
                    concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre),
                    ' | ',
                    ifnull(m.nombre,'sin materia'),
                    ' | ',
                    ifnull(rd.descripcion,'sin descripcion')
                    ) as evento,
                    rd.creado_en as fecha_evento
                    from reporte_docente rd
                    inner join cat_tipo_alerta cta on rd.id_tipo_alerta=cta.id_tipo_alerta
                    inner join carga c on rd.id_carga=c.id_carga
                    inner join materia m on c.id_materia=m.id_materia
                    inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                    inner join grupo g on gc.id_grupo=g.id_grupo
                    inner join cat_turno ct on g.id_turno=ct.id_turno
                    inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                    where rd.id_alumno=?
                    and c.id_grupo_ciclo=?

                    union all

                    select
                    concat(
                    '[ORIENTACION] ',
                    cti.nombre,
                    ' | acuerdos: ',
                    ifnull(it.acuerdos,'sin acuerdos'),
                    ' | ',
                    ifnull(it.observaciones,'sin observaciones')
                    ) as evento,
                    it.fecha_intervencion as fecha_evento
                    from intervencion_tutor it
                    inner join cat_tipo_intervencion cti on it.id_tipo_intervencion=cti.id_tipo_intervencion
                    where it.id_alumno=?
                    and it.id_maestro_tutor=?
                ) eventos
                order by fecha_evento desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAlumno);
            ps.setInt(2, idGrupoCiclo);
            ps.setInt(3, idAlumno);
            ps.setInt(4, idGrupoCiclo);
            ps.setInt(5, idAlumno);
            ps.setInt(6, getIdTutorActual());

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
            this.nombre = nombre == null ? "" : nombre;
        }

        public int getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
}