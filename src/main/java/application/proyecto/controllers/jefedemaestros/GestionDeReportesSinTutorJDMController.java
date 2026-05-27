package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionOrientacion;
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
import java.util.TreeSet;

public class GestionDeReportesSinTutorJDMController extends BaseController {

    @FXML private ComboBox<String> cmbGrupoFiltro;
    @FXML private ComboBox<String> cmbTipoFiltro;
    @FXML private ComboBox<String> cmbEstatusFiltro;
    @FXML private TextField txtBuscarTabla;

    @FXML private TableView<ReporteSinTutor> tablaGestionReportesTutor;
    @FXML private TableColumn<ReporteSinTutor, String> colAlumno;
    @FXML private TableColumn<ReporteSinTutor, String> colGrupo;
    @FXML private TableColumn<ReporteSinTutor, String> colMateria;
    @FXML private TableColumn<ReporteSinTutor, String> colTipoReporte;
    @FXML private TableColumn<ReporteSinTutor, String> colMaestro;
    @FXML private TableColumn<ReporteSinTutor, String> colEstatus;

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

    private final ObservableList<ReporteSinTutor> listaReportes = FXCollections.observableArrayList();
    private FilteredList<ReporteSinTutor> listaFiltrada;
    private ReporteSinTutor reporteSeleccionado;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        configurarEventos();
        limpiarDetalle();
        cargarReportesSinTutorAsync();
    }

    private void configurarTabla() {
        colAlumno.setCellValueFactory(new PropertyValueFactory<>("alumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colTipoReporte.setCellValueFactory(new PropertyValueFactory<>("tipoReporte"));
        colMaestro.setCellValueFactory(new PropertyValueFactory<>("maestro"));
        colEstatus.setCellValueFactory(new PropertyValueFactory<>("estatus"));

        listaFiltrada = new FilteredList<>(listaReportes, reporte -> true);
        tablaGestionReportesTutor.setItems(listaFiltrada);

        tablaGestionReportesTutor.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            reporteSeleccionado = actual;
            mostrarDetalle(actual);
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
        cmbGrupoFiltro.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltro());
        cmbTipoFiltro.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltro());
        cmbEstatusFiltro.valueProperty().addListener((obs, anterior, actual) -> cargarReportesSinTutorAsync());
        txtBuscarTabla.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro());
    }

    private void cargarReportesSinTutorAsync() {
        String estatusSeleccionado = valorCombo(cmbEstatusFiltro);
        String grupoSeleccionado = cmbGrupoFiltro.getValue();

        Task<ObservableList<ReporteSinTutor>> task = new Task<>() {
            @Override
            protected ObservableList<ReporteSinTutor> call() throws Exception {
                ObservableList<ReporteSinTutor> reportes = FXCollections.observableArrayList();

                String sql = "{call sp_jdm_reportes_sin_tutor(?)}";

                try (Connection con = ConexionBD.conectar();
                     CallableStatement cs = con.prepareCall(sql)) {

                    cs.setString(1, estatusSeleccionado);

                    try (ResultSet rs = cs.executeQuery()) {
                        while (rs.next()) {
                            reportes.add(new ReporteSinTutor(
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

                return reportes;
            }
        };

        task.setOnSucceeded(event -> {
            listaReportes.setAll(task.getValue());
            cargarGruposDesdeReportes(grupoSeleccionado);
            aplicarFiltro();

            reporteSeleccionado = null;
            tablaGestionReportesTutor.getSelectionModel().clearSelection();
            limpiarDetalle();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            mostrarError("error al cargar reportes sin tutor");
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarGruposDesdeReportes(String grupoAnterior) {
        TreeSet<String> grupos = new TreeSet<>();

        for (ReporteSinTutor reporte : listaReportes) {
            if (reporte.getGrupo() != null && !reporte.getGrupo().trim().isEmpty()) {
                grupos.add(reporte.getGrupo());
            }
        }

        cmbGrupoFiltro.getItems().clear();
        cmbGrupoFiltro.getItems().add("todos");
        cmbGrupoFiltro.getItems().addAll(grupos);

        if (grupoAnterior != null && cmbGrupoFiltro.getItems().contains(grupoAnterior)) {
            cmbGrupoFiltro.setValue(grupoAnterior);
        } else {
            cmbGrupoFiltro.setValue("todos");
        }
    }

    private void aplicarFiltro() {
        if (listaFiltrada == null) {
            return;
        }

        String grupo = valorCombo(cmbGrupoFiltro);
        String tipo = valorCombo(cmbTipoFiltro);
        String estatusFiltro = valorCombo(cmbEstatusFiltro);
        String texto = txtBuscarTabla.getText() == null ? "" : txtBuscarTabla.getText().trim().toLowerCase();

        listaFiltrada.setPredicate(reporte -> {
            String grupoReporte = textoSeguro(reporte.getGrupo());
            String tipoReporte = textoSeguro(reporte.getTipoReporte());
            String estatusReporte = textoSeguro(reporte.getEstatus());

            boolean coincideGrupo =
                    grupo.equals("todos") ||
                            grupoReporte.equals(grupo);

            boolean coincideTipo =
                    tipo.equals("todos") ||
                            tipoReporte.equals(tipo) ||
                            (tipo.equals("actividad") && tipoReporte.equals("calificacion")) ||
                            (tipo.equals("calificacion") && tipoReporte.equals("actividad"));

            boolean coincideEstatus = switch (estatusFiltro) {
                case "pendiente" -> estatusReporte.equals("pendiente");
                case "cerrada" -> estatusReporte.equals("cerrada") || estatusReporte.equals("cerrado");
                default -> true;
            };

            boolean coincideTexto =
                    texto.isEmpty() ||
                            textoSeguro(reporte.getAlumno()).contains(texto) ||
                            textoSeguro(reporte.getNumControl()).contains(texto) ||
                            textoSeguro(reporte.getGrupo()).contains(texto) ||
                            textoSeguro(reporte.getMateria()).contains(texto) ||
                            textoSeguro(reporte.getMaestro()).contains(texto) ||
                            textoSeguro(reporte.getTipoReporte()).contains(texto) ||
                            textoSeguro(reporte.getDescripcion()).contains(texto) ||
                            textoSeguro(reporte.getEstatus()).contains(texto) ||
                            textoSeguro(reporte.getFecha()).contains(texto);

            return coincideGrupo && coincideTipo && coincideEstatus && coincideTexto;
        });
    }

    private String valorCombo(ComboBox<String> combo) {
        return combo.getValue() == null ? "todos" : combo.getValue().trim().toLowerCase();
    }

    private String textoSeguro(String texto) {
        return texto == null ? "" : texto.toLowerCase();
    }

    private void mostrarDetalle(ReporteSinTutor reporte) {
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
        reporteSeleccionado = null;
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

        cargarVistaOrientacionSinTutor(event);
    }

    private void cargarVistaOrientacionSinTutor(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/jefedemaestros/OrientacionSinTutorJDM.fxml"));
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
            mostrarError("no se pudo abrir la vista de orientacion sin tutor");
        }
    }

    @FXML
    private void handleActualizar() {
        cargarReportesSinTutorAsync();
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

    public static class ReporteSinTutor {
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

        public ReporteSinTutor(int idReporte, int idAlumno, String numControl, String alumno, String grupo,
                               String semestre, String turno, String materia, String maestro, String tipoReporte,
                               String descripcion, String estatus, String fecha) {
            this.idReporte = idReporte;
            this.idAlumno = idAlumno;
            this.numControl = textoSeguroNormal(numControl);
            this.alumno = textoSeguroNormal(alumno);
            this.grupo = textoSeguroNormal(grupo);
            this.semestre = textoSeguroNormal(semestre);
            this.turno = textoSeguroNormal(turno);
            this.materia = textoSeguroNormal(materia);
            this.maestro = textoSeguroNormal(maestro).isEmpty() ? "sin maestro" : textoSeguroNormal(maestro);
            this.tipoReporte = textoSeguroNormal(tipoReporte);
            this.descripcion = textoSeguroNormal(descripcion);
            this.estatus = textoSeguroNormal(estatus);
            this.fecha = textoSeguroNormal(fecha);
        }

        private static String textoSeguroNormal(String valor) {
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