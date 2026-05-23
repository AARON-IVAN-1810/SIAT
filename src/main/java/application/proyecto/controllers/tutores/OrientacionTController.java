package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionOrientacion;
import application.proyecto.utils.SesionUsuario;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;

public class OrientacionTController extends BaseController {

    // ===== FORMULARIO =====

    @FXML private ComboBox<String> cmbGrupo;
    @FXML private ComboBox<String> cmbAlumno;

    @FXML private TextField txtSemestre;
    @FXML private TextField txtTurno;
    @FXML private TextField txtMateria;
    @FXML private TextField txtTipoAlerta;
    @FXML private TextField txtPrioridad;
    @FXML private TextField txtFechaAlerta;
    @FXML private TextField txtFechaOrientacion;

    @FXML private ComboBox<String> cmbTipoIntervencion;

    @FXML private TextArea txtAcuerdos;
    @FXML private TextArea txtDescripcionOrientacion;

    // ===== BOTONES =====

    @FXML private Button btnRegistrarIntervencion;
    @FXML private Button btnVerAlertas;

    // ===== TABLA =====

    @FXML private TableView<Intervencion> tablaIntervencionesTutor;

    @FXML private TableColumn<Intervencion, String> colAlumnoIntervencion;
    @FXML private TableColumn<Intervencion, String> colGrupoIntervencion;
    @FXML private TableColumn<Intervencion, String> colFechaIntervencion;
    @FXML private TableColumn<Intervencion, String> colTipoIntervencion;
    @FXML private TableColumn<Intervencion, String> colAcuerdosIntervencion;
    @FXML private TableColumn<Intervencion, String> colDescripcionIntervencion;

    // ===== BITACORA =====

    @FXML private ListView<String> listBitacoraTutor;

    private final ObservableList<Intervencion> listaIntervenciones = FXCollections.observableArrayList();

    private int idAlumnoActual = 0;

    @FXML
    public void initialize() {

        configurarTabla();
        cargarTiposIntervencion();
        cargarGruposTutor();

        txtFechaOrientacion.setText(LocalDate.now().toString());

        cargarDatosSesionOrientacion();

        cmbGrupo.setOnAction(e -> cargarAlumnosPorGrupo());
        cmbAlumno.setOnAction(e -> cargarDatosAlumnoSeleccionado());
    }

    // ===== CONFIGURAR TABLA =====

    private void configurarTabla() {

        colAlumnoIntervencion.setCellValueFactory(new PropertyValueFactory<>("alumno"));
        colGrupoIntervencion.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colFechaIntervencion.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colTipoIntervencion.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colAcuerdosIntervencion.setCellValueFactory(new PropertyValueFactory<>("acuerdos"));
        colDescripcionIntervencion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        tablaIntervencionesTutor.setItems(listaIntervenciones);
    }

    // ===== CARGAR TIPOS =====

    private void cargarTiposIntervencion() {

        cmbTipoIntervencion.setItems(FXCollections.observableArrayList(
                "seguimiento academico",
                "orientacion personal",
                "canalizacion",
                "entrevista",
                "llamada",
                "acuerdo academico"
        ));
    }

    // ===== CARGAR GRUPOS DEL TUTOR =====

    private void cargarGruposTutor() {

        cmbGrupo.getItems().clear();

        String sql = """
                select distinct g.nombre
                from tutoria_asignacion ta
                inner join grupo_ciclo gc on ta.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                order by g.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, SesionUsuario.getIdMaestro());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                cmbGrupo.getItems().add(rs.getString("nombre"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== CARGAR ALUMNOS POR GRUPO =====

    private void cargarAlumnosPorGrupo() {

        cmbAlumno.getItems().clear();

        String grupo = cmbGrupo.getValue();

        if (grupo == null) return;

        String sql = """
                select concat(a.nombre,' ',a.apellido_paterno,' ',a.apellido_materno) as alumno
                from alumno a
                inner join grupo_ciclo gc on a.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                where g.nombre=?
                order by alumno
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, grupo);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                cmbAlumno.getItems().add(rs.getString("alumno"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== CARGAR DATOS DESDE SESION =====

    private void cargarDatosSesionOrientacion() {

        if (SesionOrientacion.getIdAlumno() == 0) {
            return;
        }

        idAlumnoActual = SesionOrientacion.getIdAlumno();

        cmbGrupo.setValue(SesionOrientacion.getGrupo());

        cargarAlumnosPorGrupo();

        cmbAlumno.setValue(SesionOrientacion.getNombreAlumno());

        txtSemestre.setText(SesionOrientacion.getSemestre());
        txtTurno.setText(SesionOrientacion.getTurno());
        txtMateria.setText(SesionOrientacion.getMateria());

        txtTipoAlerta.setText(SesionOrientacion.getTipoAlerta());
        txtPrioridad.setText(SesionOrientacion.getPrioridad());

        txtFechaAlerta.setText(SesionOrientacion.getFechaAlerta());

        cargarIntervencionesAlumno();
        cargarBitacoraAlumno();
    }

    // ===== CARGAR DATOS MANUALES =====

    private void cargarDatosAlumnoSeleccionado() {

        String alumno = cmbAlumno.getValue();

        if (alumno == null) return;

        String sql = """
                select
                a.id_alumno,
                g.semestre,
                ct.nombre as turno
                from alumno a
                inner join grupo_ciclo gc on a.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                where concat(a.nombre,' ',a.apellido_paterno,' ',a.apellido_materno)=?
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, alumno);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                idAlumnoActual = rs.getInt("id_alumno");

                txtSemestre.setText(rs.getString("semestre"));
                txtTurno.setText(rs.getString("turno"));

                cargarIntervencionesAlumno();
                cargarBitacoraAlumno();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== REGISTRAR INTERVENCION =====

    @FXML
    private void handleRegistrarIntervencion() {

        if (idAlumnoActual == 0) {
            mostrarError("Selecciona un alumno.");
            return;
        }

        if (cmbTipoIntervencion.getValue() == null) {
            mostrarError("Selecciona el tipo de intervencion.");
            return;
        }

        String acuerdos = txtAcuerdos.getText() == null ? "" : txtAcuerdos.getText().trim();
        String descripcion = txtDescripcionOrientacion.getText() == null ? "" : txtDescripcionOrientacion.getText().trim();
        String motivoSituacion = txtTipoAlerta.getText() == null ? "seguimiento del alumno" : txtTipoAlerta.getText().trim();

        if (acuerdos.isEmpty() || descripcion.isEmpty()) {
            mostrarError("Completa los campos de acuerdos y descripcion.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar");
        confirmacion.setHeaderText("Registrar intervencion");
        confirmacion.setContentText("Deseas registrar esta intervencion?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String sql = """
            insert into intervencion_tutor(
            id_alerta,
            id_reporte_docente,
            id_maestro_tutor,
            id_alumno,
            id_tipo_intervencion,
            fecha_intervencion,
            motivo_situacion,
            acuerdos,
            id_resultado_intervencion,
            observaciones
            )
            values(?,?,?,?,?,?,?,?,?,?)
            """;

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                if (SesionOrientacion.getIdAlerta() > 0) {
                    ps.setInt(1, SesionOrientacion.getIdAlerta());
                } else {
                    ps.setNull(1, java.sql.Types.INTEGER);
                }

                if (SesionOrientacion.getIdReporteDocente() > 0) {
                    ps.setInt(2, SesionOrientacion.getIdReporteDocente());
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }

                ps.setInt(3, SesionUsuario.getIdMaestro());
                ps.setInt(4, idAlumnoActual);
                ps.setInt(5, obtenerIdTipoIntervencion(cmbTipoIntervencion.getValue()));
                ps.setDate(6, java.sql.Date.valueOf(LocalDate.now()));
                ps.setString(7, motivoSituacion);
                ps.setString(8, acuerdos);
                ps.setInt(9, 1);
                ps.setString(10, descripcion);

                ps.executeUpdate();
            }

            cerrarAlertaSiExiste(con);
            cerrarReporteSiExiste(con);

            con.commit();

            mostrarInfo("Intervencion registrada correctamente.");

            txtAcuerdos.clear();
            txtDescripcionOrientacion.clear();

            cargarIntervencionesAlumno();
            cargarBitacoraAlumno();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al registrar intervencion.");
        }
    }

    private int obtenerIdTipoIntervencion(String tipo) {
        if (tipo == null) {
            return 4;
        }

        return switch (tipo.toLowerCase().trim()) {
            case "entrevista" -> 1;
            case "llamada" -> 2;
            case "asesoria" -> 3;
            case "seguimiento", "seguimiento academico" -> 4;
            case "orientacion", "orientacion personal" -> 5;
            case "canalizacion", "acuerdo academico" -> 4;
            default -> 4;
        };
    }








    // ===== VER ALERTAS =====

    @FXML
    private void handleVerAlertas() {

        mostrarInfo("Regresa al modulo de gestion de alertas.");
    }

    // ===== CARGAR INTERVENCIONES =====

    private void cargarIntervencionesAlumno() {

        listaIntervenciones.clear();

        if (idAlumnoActual == 0) return;

        String sql = """
            select
            concat(a.nombre,' ',a.apellido_paterno,' ',a.apellido_materno) as alumno,
            g.nombre as grupo,
            date(it.fecha_intervencion) as fecha,
            cti.nombre as tipo_intervencion,
            it.acuerdos,
            it.observaciones
            from intervencion_tutor it
            inner join alumno a on it.id_alumno=a.id_alumno
            inner join grupo_ciclo gc on a.id_grupo_ciclo=gc.id_grupo_ciclo
            inner join grupo g on gc.id_grupo=g.id_grupo
            inner join cat_tipo_intervencion cti on it.id_tipo_intervencion=cti.id_tipo_intervencion
            where it.id_alumno=?
            and it.id_maestro_tutor=?
            order by it.fecha_intervencion desc,it.id_intervencion desc
            """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAlumnoActual);
            ps.setInt(2, SesionUsuario.getIdMaestro());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaIntervenciones.add(new Intervencion(
                            rs.getString("alumno"),
                            rs.getString("grupo"),
                            rs.getString("fecha"),
                            rs.getString("tipo_intervencion"),
                            rs.getString("acuerdos"),
                            rs.getString("observaciones")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al cargar intervenciones.");
        }
    }

    // ===== CARGAR BITACORA =====

    private void cargarBitacoraAlumno() {

        listBitacoraTutor.getItems().clear();

        if (idAlumnoActual == 0) return;

        String sql = """
                select concat(
                '[ALERTA] ',
                cta.nombre,
                ' - ',
                date(a.creada_en)
                ) as evento
                from alerta a
                inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                where a.id_alumno=?

                union all

                select concat(
                '[REPORTE] ',
                cta.nombre,
                ' - ',
                date(rd.creado_en)
                ) as evento
                from reporte_docente rd
                inner join cat_tipo_alerta cta on rd.id_tipo_alerta=cta.id_tipo_alerta
                where rd.id_alumno=?

                order by evento desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAlumnoActual);
            ps.setInt(2, idAlumnoActual);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                listBitacoraTutor.getItems().add(rs.getString("evento"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void cerrarAlertaSiExiste(Connection con) throws Exception {
        if (SesionOrientacion.getIdAlerta() <= 0) {
            return;
        }

        String sql = """
            update alerta
            set id_estatus_alerta=0,
            cerrada_en=now()
            where id_alerta=?
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, SesionOrientacion.getIdAlerta());
            ps.executeUpdate();
        }
    }

    private void cerrarReporteSiExiste(Connection con) throws Exception {
        if (SesionOrientacion.getIdReporteDocente() <= 0) {
            return;
        }

        String sql = """
            update reporte_docente
            set id_estatus_reporte_docente=0
            where id_reporte_docente=?
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, SesionOrientacion.getIdReporteDocente());
            ps.executeUpdate();
        }
    }



    // ===== ALERTAS =====

    private void mostrarError(String mensaje) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacion");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ===== MODELO =====

    public static class Intervencion {

        private final String alumno;
        private final String grupo;
        private final String fecha;
        private final String tipo;
        private final String acuerdos;
        private final String descripcion;

        public Intervencion(String alumno, String grupo, String fecha,
                            String tipo, String acuerdos, String descripcion) {

            this.alumno = alumno;
            this.grupo = grupo;
            this.fecha = fecha;
            this.tipo = tipo;
            this.acuerdos = acuerdos;
            this.descripcion = descripcion;
        }

        public String getAlumno() {
            return alumno;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getFecha() {
            return fecha;
        }

        public String getTipo() {
            return tipo;
        }

        public String getAcuerdos() {
            return acuerdos;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }
}