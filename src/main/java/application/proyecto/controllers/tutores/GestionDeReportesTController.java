package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionOrientacion;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GestionDeReportesTController extends BaseController {

    @FXML private ComboBox<String> cmbGrupoFiltro;
    @FXML private ComboBox<String> cmbTipoFiltro;
    @FXML private ComboBox<String> cmbEstatusFiltro;
    @FXML private TextField txtBuscarTabla;

    @FXML private TableView<ReporteTutor> tablaGestionReportesTutor;
    @FXML private TableColumn<ReporteTutor, String> colAlumno;
    @FXML private TableColumn<ReporteTutor, String> colGrupo;
    @FXML private TableColumn<ReporteTutor, String> colMateria;
    @FXML private TableColumn<ReporteTutor, String> colTipoReporte;
    @FXML private TableColumn<ReporteTutor, String> colMaestro;
    @FXML private TableColumn<ReporteTutor, String> colEstatus;

    @FXML private Label lblNombreAlumno;
    @FXML private Label lblNumeroControl;
    @FXML private Label lblGrupo;
    @FXML private Label lblSemestre;
    @FXML private Label lblTurno;
    @FXML private Label lblMateria;
    @FXML private Label lblMaestro;
    @FXML private Label lblTipoReporte;
    @FXML private Label lblDescripcion;
    @FXML private Label lblFechaCreacion;
    @FXML private Label lblEstatusDetalle;

    private final ObservableList<ReporteTutor> listaReportes = FXCollections.observableArrayList();
    private FilteredList<ReporteTutor> listaFiltrada;
    private ReporteTutor reporteSeleccionado;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        configurarEventos();
        cargarGrupos();
        cargarReportes();
        limpiarDetalle();
    }

    private int getIdTutorActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTabla() {
        colAlumno.setCellValueFactory(new PropertyValueFactory<>("alumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colTipoReporte.setCellValueFactory(new PropertyValueFactory<>("tipoReporte"));
        colMaestro.setCellValueFactory(new PropertyValueFactory<>("maestro"));
        colEstatus.setCellValueFactory(new PropertyValueFactory<>("estatus"));

        listaFiltrada = new FilteredList<>(listaReportes, p -> true);
        tablaGestionReportesTutor.setItems(listaFiltrada);

        tablaGestionReportesTutor.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            reporteSeleccionado = newValue;
            mostrarDetalle(newValue);
        });
    }

    private void configurarFiltros() {
        cmbGrupoFiltro.setItems(FXCollections.observableArrayList("todos"));
        cmbGrupoFiltro.setValue("todos");

        cmbTipoFiltro.setItems(FXCollections.observableArrayList(
                "todos",
                "asistencia",
                "actividad",
                "calificacion",
                "conducta"
        ));
        cmbTipoFiltro.setValue("todos");

        cmbEstatusFiltro.setItems(FXCollections.observableArrayList(
                "todos",
                "pendiente",
                "seguimiento",
                "cerrado",
                "cerrada",
                "activa",
                "inactiva"
        ));
        cmbEstatusFiltro.setValue("todos");
    }

    private void configurarEventos() {
        cmbGrupoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbTipoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbEstatusFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        txtBuscarTabla.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void cargarGrupos() {
        cmbGrupoFiltro.getItems().clear();
        cmbGrupoFiltro.getItems().add("todos");

        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        String sql = """
                select distinct
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
                    cmbGrupoFiltro.getItems().add(rs.getString("grupo"));
                }
            }

            cmbGrupoFiltro.setValue("todos");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos");
        }
    }

    private void cargarReportes() {
        listaReportes.clear();

        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        String sql = """
                select distinct
                rd.id_reporte_docente,
                al.id_alumno,
                al.num_control,
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as alumno,
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo,
                g.semestre,
                ct.nombre as turno,
                concat(m.clave,' - ',m.nombre) as materia,
                concat(ma.nombre,' ',ma.apellido_paterno,' ',ma.apellido_materno) as maestro,
                cta.nombre as tipo_reporte,
                rd.descripcion,
                cer.nombre as estatus,
                rd.creado_en as fecha_orden,
                date_format(rd.creado_en,'%Y-%m-%d') as fecha
                from tutoria_asignacion ta
                inner join carga c on ta.id_grupo_ciclo=c.id_grupo_ciclo
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join reporte_docente rd on rd.id_carga=c.id_carga
                inner join alumno al on rd.id_alumno=al.id_alumno
                inner join alumno_carga ac on ac.id_carga=c.id_carga
                and ac.id_alumno=al.id_alumno
                inner join materia m on c.id_materia=m.id_materia
                inner join maestro ma on c.id_maestro=ma.id_maestro
                inner join cat_tipo_alerta cta on rd.id_tipo_alerta=cta.id_tipo_alerta
                inner join cat_estatus_reporte_docente cer on rd.id_estatus_reporte_docente=cer.id_estatus_reporte_docente
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                and al.id_estatus_general=1
                and ac.id_estatus_general=1
                order by fecha_orden desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaReportes.add(new ReporteTutor(
                            rs.getInt("id_reporte_docente"),
                            rs.getInt("id_alumno"),
                            rs.getString("num_control"),
                            rs.getString("alumno"),
                            rs.getString("grupo"),
                            rs.getString("semestre"),
                            rs.getString("turno"),
                            rs.getString("materia"),
                            rs.getString("maestro"),
                            rs.getString("tipo_reporte"),
                            rs.getString("descripcion"),
                            rs.getString("estatus"),
                            rs.getString("fecha")
                    ));
                }
            }

            aplicarFiltro();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar reportes");
        }
    }

    private void aplicarFiltro() {
        if (listaFiltrada == null) {
            return;
        }

        String grupo = cmbGrupoFiltro.getValue() == null ? "todos" : cmbGrupoFiltro.getValue().toLowerCase().trim();
        String tipo = cmbTipoFiltro.getValue() == null ? "todos" : cmbTipoFiltro.getValue().toLowerCase().trim();
        String estatusFiltro = cmbEstatusFiltro.getValue() == null ? "todos" : cmbEstatusFiltro.getValue().toLowerCase().trim();
        String texto = txtBuscarTabla.getText() == null ? "" : txtBuscarTabla.getText().toLowerCase().trim();

        listaFiltrada.setPredicate(reporte -> {
            boolean coincideGrupo =
                    grupo.equals("todos") ||
                            reporte.getGrupo().toLowerCase().contains(grupo);

            String tipoReporte = reporte.getTipoReporte().toLowerCase();

            boolean coincideTipo =
                    tipo.equals("todos") ||
                            tipoReporte.equals(tipo) ||
                            (tipo.equals("actividad") && tipoReporte.equals("calificacion")) ||
                            (tipo.equals("calificacion") && tipoReporte.equals("actividad"));

            boolean coincideEstatus = switch (estatusFiltro) {
                case "pendiente" -> reporte.getEstatus().equalsIgnoreCase("pendiente");
                case "seguimiento" -> reporte.getEstatus().equalsIgnoreCase("seguimiento");
                case "cerrado" -> reporte.getEstatus().equalsIgnoreCase("cerrado");
                case "cerrada" -> reporte.getEstatus().equalsIgnoreCase("cerrada");
                case "activa" -> reporte.getEstatus().equalsIgnoreCase("pendiente") ||
                        reporte.getEstatus().equalsIgnoreCase("seguimiento");
                case "inactiva" -> reporte.getEstatus().equalsIgnoreCase("cerrado") ||
                        reporte.getEstatus().equalsIgnoreCase("cerrada");
                default -> true;
            };

            boolean coincideTexto =
                    texto.isEmpty() ||
                            reporte.getAlumno().toLowerCase().contains(texto) ||
                            reporte.getNumControl().toLowerCase().contains(texto) ||
                            reporte.getGrupo().toLowerCase().contains(texto) ||
                            reporte.getMateria().toLowerCase().contains(texto) ||
                            reporte.getMaestro().toLowerCase().contains(texto) ||
                            reporte.getTipoReporte().toLowerCase().contains(texto) ||
                            reporte.getDescripcion().toLowerCase().contains(texto) ||
                            reporte.getEstatus().toLowerCase().contains(texto) ||
                            reporte.getFecha().toLowerCase().contains(texto);

            return coincideGrupo && coincideTipo && coincideEstatus && coincideTexto;
        });
    }

    private void mostrarDetalle(ReporteTutor reporte) {
        if (reporte == null) {
            limpiarDetalle();
            return;
        }

        lblNombreAlumno.setText(reporte.getAlumno());
        lblNumeroControl.setText(reporte.getNumControl());
        lblGrupo.setText(reporte.getGrupo());
        lblSemestre.setText(reporte.getSemestre());
        lblTurno.setText(reporte.getTurno());
        lblMateria.setText(reporte.getMateria());
        lblMaestro.setText(reporte.getMaestro());
        lblTipoReporte.setText(reporte.getTipoReporte());
        lblDescripcion.setText(reporte.getDescripcion());
        lblFechaCreacion.setText(reporte.getFecha());
        lblEstatusDetalle.setText(reporte.getEstatus());
    }

    private void limpiarDetalle() {
        lblNombreAlumno.setText("Selecciona un reporte");
        lblNumeroControl.setText("---");
        lblGrupo.setText("---");
        lblSemestre.setText("---");
        lblTurno.setText("---");
        lblMateria.setText("---");
        lblMaestro.setText("---");
        lblTipoReporte.setText("Sin seleccionar");
        lblDescripcion.setText("Selecciona un reporte para mostrar la descripcion.");
        lblFechaCreacion.setText("---");
        lblEstatusDetalle.setText("---");
    }

    @FXML
    private void handleDarSeguimiento(ActionEvent event) {
        if (reporteSeleccionado == null) {
            mostrarError("selecciona un reporte");
            return;
        }

        SesionOrientacion.limpiar();

        SesionOrientacion.setIdAlerta(0);
        SesionOrientacion.setIdReporteDocente(reporteSeleccionado.getIdReporte());
        SesionOrientacion.setIdAlumno(reporteSeleccionado.getIdAlumno());
        SesionOrientacion.setNumControl(reporteSeleccionado.getNumControl());
        SesionOrientacion.setNombreAlumno(reporteSeleccionado.getAlumno());
        SesionOrientacion.setGrupo(reporteSeleccionado.getGrupo());
        SesionOrientacion.setSemestre(reporteSeleccionado.getSemestre());
        SesionOrientacion.setTurno(reporteSeleccionado.getTurno());
        SesionOrientacion.setMateria(reporteSeleccionado.getMateria());
        SesionOrientacion.setTipoAlerta(reporteSeleccionado.getTipoReporte());
        SesionOrientacion.setPrioridad("sin prioridad");
        SesionOrientacion.setMotivoDetalle(reporteSeleccionado.getDescripcion());
        SesionOrientacion.setFechaAlerta(reporteSeleccionado.getFecha());

        cargarVistaOrientacion(event);
    }

    private void cargarVistaOrientacion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/tutor/OrientacionT.fxml"));
            Parent vista = loader.load();

            Node source = (Node) event.getSource();
            StackPane contentArea = (StackPane) source.getScene().lookup("#contentArea");

            if (contentArea == null) {
                mostrarError("no se encontro el contenedor principal");
                return;
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(vista);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("no se pudo abrir la vista de orientacion");
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class ReporteTutor {
        private final int idReporte;
        private final int idAlumno;
        private final String numControl;
        private final String alumno;
        private final String grupo;
        private final String semestre;
        private final String turno;
        private final String materia;
        private final String maestro;
        private final String tipoReporte;
        private final String descripcion;
        private final String estatus;
        private final String fecha;

        public ReporteTutor(int idReporte, int idAlumno, String numControl, String alumno, String grupo, String semestre,
                            String turno, String materia, String maestro, String tipoReporte, String descripcion,
                            String estatus, String fecha) {
            this.idReporte = idReporte;
            this.idAlumno = idAlumno;
            this.numControl = textoSeguro(numControl);
            this.alumno = textoSeguro(alumno);
            this.grupo = textoSeguro(grupo);
            this.semestre = textoSeguro(semestre);
            this.turno = textoSeguro(turno);
            this.materia = textoSeguro(materia);
            this.maestro = textoSeguro(maestro).isEmpty() ? "sin maestro" : maestro;
            this.tipoReporte = textoSeguro(tipoReporte);
            this.descripcion = textoSeguro(descripcion);
            this.estatus = textoSeguro(estatus);
            this.fecha = textoSeguro(fecha);
        }

        private static String textoSeguro(String valor) {
            return valor == null ? "" : valor;
        }

        public int getIdReporte() {
            return idReporte;
        }

        public int getIdAlumno() {
            return idAlumno;
        }

        public String getNumControl() {
            return numControl;
        }

        public String getAlumno() {
            return alumno;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getSemestre() {
            return semestre;
        }

        public String getTurno() {
            return turno;
        }

        public String getMateria() {
            return materia;
        }

        public String getMaestro() {
            return maestro;
        }

        public String getTipoReporte() {
            return tipoReporte;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getEstatus() {
            return estatus;
        }

        public String getFecha() {
            return fecha;
        }
    }
}