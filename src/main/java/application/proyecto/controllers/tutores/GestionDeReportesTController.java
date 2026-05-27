package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionOrientacion;
import application.proyecto.utils.SesionUsuario;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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
        limpiarDetalle();
        cargarDatosAsync();
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
                "conducta"
        ));
        cmbTipoFiltro.setValue("todos");

        cmbEstatusFiltro.setItems(FXCollections.observableArrayList(
                "todos",
                "pendiente",
                "cerrada"
        ));
        cmbEstatusFiltro.setValue("todos");
    }

    private void configurarEventos() {
        cmbGrupoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbTipoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbEstatusFiltro.valueProperty().addListener((obs, oldValue, newValue) -> cargarDatosAsync());
        txtBuscarTabla.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void cargarDatosAsync() {
        int idTutor = getIdTutorActual();
        String estatusSeleccionado = valorCombo(cmbEstatusFiltro);
        String grupoSeleccionado = cmbGrupoFiltro.getValue();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        Task<DatosGestionReportes> task = new Task<>() {
            @Override
            protected DatosGestionReportes call() throws Exception {
                DatosGestionReportes datos = new DatosGestionReportes();

                try (Connection con = ConexionBD.conectar()) {
                    cargarGruposDesdeSP(con, idTutor, datos);
                    cargarReportesDesdeSP(con, idTutor, estatusSeleccionado, datos);
                }

                return datos;
            }
        };

        task.setOnSucceeded(event -> {
            DatosGestionReportes datos = task.getValue();

            cmbGrupoFiltro.getItems().setAll(datos.grupos);

            if (grupoSeleccionado != null && datos.grupos.contains(grupoSeleccionado)) {
                cmbGrupoFiltro.setValue(grupoSeleccionado);
            } else {
                cmbGrupoFiltro.setValue("todos");
            }

            listaReportes.setAll(datos.reportes);
            aplicarFiltro();

            reporteSeleccionado = null;
            tablaGestionReportesTutor.getSelectionModel().clearSelection();
            limpiarDetalle();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            mostrarError("error al cargar reportes");
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarGruposDesdeSP(Connection con, int idTutor, DatosGestionReportes datos) throws Exception {
        datos.grupos.clear();
        datos.grupos.add("todos");

        String sql = "{call sp_tutor_grupos(?)}";

        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idTutor);

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    datos.grupos.add(rs.getString("grupo"));
                }
            }
        }
    }

    private void cargarReportesDesdeSP(Connection con, int idTutor, String estatusSeleccionado, DatosGestionReportes datos) throws Exception {
        datos.reportes.clear();

        String sql = "{call sp_tutor_reportes_gestion(?,?)}";

        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idTutor);
            cs.setString(2, estatusSeleccionado);

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    datos.reportes.add(new ReporteTutor(
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
                case "cerrada" -> reporte.getEstatus().equalsIgnoreCase("cerrada") ||
                        reporte.getEstatus().equalsIgnoreCase("cerrado");
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

    private String valorCombo(ComboBox<String> combo) {
        return combo.getValue() == null ? "todos" : combo.getValue().trim().toLowerCase();
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

        marcarReporteEnSeguimientoAsync(event);
    }

    private void marcarReporteEnSeguimientoAsync(ActionEvent actionEvent) {
        if (reporteSeleccionado == null) {
            return;
        }

        int idTutor = getIdTutorActual();
        int idReporte = reporteSeleccionado.getIdReporte();

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                String sql = "{call sp_tutor_actualizar_estatus_reporte(?,?,?)}";

                try (Connection con = ConexionBD.conectar();
                     CallableStatement cs = con.prepareCall(sql)) {

                    cs.setInt(1, idTutor);
                    cs.setInt(2, idReporte);
                    cs.setInt(3, 2);

                    try (ResultSet rs = cs.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt("filas_afectadas");
                        }
                    }
                }

                return 0;
            }
        };

        task.setOnSucceeded(event -> {
            int filas = task.getValue();

            if (filas == 0) {
                mostrarError("no tienes permiso para modificar este reporte");
                return;
            }

            prepararSesionOrientacion();
            cargarVistaOrientacion(actionEvent);
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            mostrarError("error al cambiar reporte a seguimiento");
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void prepararSesionOrientacion() {
        if (reporteSeleccionado == null) {
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

    @FXML
    private void handleActualizar() {
        cargarDatosAsync();
    }

    private void mostrarError(String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("error");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    private static class DatosGestionReportes {
        private final List<String> grupos = new ArrayList<>();
        private final List<ReporteTutor> reportes = new ArrayList<>();
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
            this.maestro = textoSeguro(maestro).isEmpty() ? "sin maestro" : textoSeguro(maestro);
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