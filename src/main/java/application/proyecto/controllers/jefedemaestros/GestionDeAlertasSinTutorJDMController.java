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
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.TreeSet;

public class GestionDeAlertasSinTutorJDMController extends BaseController {

    @FXML private ComboBox<String> cmbGrupoFiltro;
    @FXML private ComboBox<String> cmbTipoFiltro;
    @FXML private ComboBox<String> cmbEstatusFiltro;
    @FXML private TextField txtBuscarTabla;

    @FXML private TableView<AlertaSinTutor> tablaGestionAlertasTutor;
    @FXML private TableColumn<AlertaSinTutor, String> colAlumno;
    @FXML private TableColumn<AlertaSinTutor, String> colGrupo;
    @FXML private TableColumn<AlertaSinTutor, String> colMateria;
    @FXML private TableColumn<AlertaSinTutor, String> colTipoAlerta;
    @FXML private TableColumn<AlertaSinTutor, String> colPrioridad;
    @FXML private TableColumn<AlertaSinTutor, String> colEstatus;

    @FXML private Label lblNombreAlumno;
    @FXML private Label lblNumeroControl;
    @FXML private Label lblGrupo;
    @FXML private Label lblSemestre;
    @FXML private Label lblTurno;
    @FXML private Label lblMateria;
    @FXML private Label lblTipoAlerta;
    @FXML private Label lblPrioridadDetalle;
    @FXML private Label lblMotivoDetalle;
    @FXML private Label lblFechaCreacion;
    @FXML private Label lblEstatusDetalle;

    private final ObservableList<AlertaSinTutor> listaAlertas = FXCollections.observableArrayList();
    private FilteredList<AlertaSinTutor> listaFiltrada;
    private AlertaSinTutor alertaSeleccionada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        configurarEventos();
        limpiarDetalle();
        cargarAlertasSinTutorAsync();
    }

    private void configurarTabla() {
        colAlumno.setCellValueFactory(new PropertyValueFactory<>("alumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colTipoAlerta.setCellValueFactory(new PropertyValueFactory<>("tipoAlerta"));
        colPrioridad.setCellValueFactory(new PropertyValueFactory<>("prioridad"));
        colEstatus.setCellValueFactory(new PropertyValueFactory<>("estatus"));

        configurarColumnaPrioridad();

        listaFiltrada = new FilteredList<>(listaAlertas, alerta -> true);
        tablaGestionAlertasTutor.setItems(listaFiltrada);

        tablaGestionAlertasTutor.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            alertaSeleccionada = actual;
            mostrarDetalle(actual);
        });
    }

    private void configurarColumnaPrioridad() {
        colPrioridad.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(String prioridad, boolean empty) {
                super.updateItem(prioridad, empty);

                if (empty || prioridad == null || prioridad.trim().isEmpty()) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(prioridad);

                String valor = prioridad.toLowerCase().trim();

                if (valor.contains("alta")) {
                    setStyle("""
                            -fx-background-color: #fee2e2;
                            -fx-text-fill: #991b1b;
                            -fx-font-weight: bold;
                            -fx-alignment: center;
                            """);
                } else if (valor.contains("media")) {
                    setStyle("""
                            -fx-background-color: #fef3c7;
                            -fx-text-fill: #92400e;
                            -fx-font-weight: bold;
                            -fx-alignment: center;
                            """);
                } else if (valor.contains("baja")) {
                    setStyle("""
                            -fx-background-color: #dcfce7;
                            -fx-text-fill: #166534;
                            -fx-font-weight: bold;
                            -fx-alignment: center;
                            """);
                } else {
                    setStyle("""
                            -fx-background-color: transparent;
                            -fx-text-fill: #111827;
                            -fx-alignment: center;
                            """);
                }
            }
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
                "activa",
                "cerrada",
                "inactiva"
        ));
        cmbEstatusFiltro.setValue("todos");
    }

    private void configurarEventos() {
        cmbGrupoFiltro.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltro());
        cmbTipoFiltro.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltro());
        cmbEstatusFiltro.valueProperty().addListener((obs, anterior, actual) -> cargarAlertasSinTutorAsync());
        txtBuscarTabla.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro());
    }

    private void cargarAlertasSinTutorAsync() {
        String estatusSeleccionado = valorCombo(cmbEstatusFiltro);
        String grupoSeleccionado = cmbGrupoFiltro.getValue();

        Task<ObservableList<AlertaSinTutor>> task = new Task<>() {
            @Override
            protected ObservableList<AlertaSinTutor> call() throws Exception {
                ObservableList<AlertaSinTutor> alertas = FXCollections.observableArrayList();

                String sql = "{call sp_jdm_alertas_sin_tutor(?)}";

                try (Connection con = ConexionBD.conectar();
                     CallableStatement cs = con.prepareCall(sql)) {

                    cs.setString(1, estatusSeleccionado);

                    try (ResultSet rs = cs.executeQuery()) {
                        while (rs.next()) {
                            alertas.add(new AlertaSinTutor(
                                    rs.getInt("id_alerta"),
                                    rs.getInt("id_alumno"),
                                    rs.getString("num_control"),
                                    rs.getString("alumno"),
                                    rs.getString("grupo"),
                                    rs.getString("semestre"),
                                    rs.getString("turno"),
                                    rs.getString("materia"),
                                    rs.getString("tipo_alerta"),
                                    rs.getString("prioridad"),
                                    rs.getString("motivo_detalle"),
                                    rs.getString("estatus"),
                                    rs.getString("fecha")
                            ));
                        }
                    }
                }

                return alertas;
            }
        };

        task.setOnSucceeded(event -> {
            listaAlertas.setAll(task.getValue());
            cargarGruposDesdeAlertas(grupoSeleccionado);
            aplicarFiltro();

            alertaSeleccionada = null;
            tablaGestionAlertasTutor.getSelectionModel().clearSelection();
            limpiarDetalle();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            mostrarError("error al cargar alertas de grupos sin tutor");
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarGruposDesdeAlertas(String grupoAnterior) {
        TreeSet<String> grupos = new TreeSet<>();

        for (AlertaSinTutor alerta : listaAlertas) {
            if (alerta.getGrupo() != null && !alerta.getGrupo().trim().isEmpty()) {
                grupos.add(alerta.getGrupo());
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

        listaFiltrada.setPredicate(alerta -> {
            String grupoAlerta = textoSeguro(alerta.getGrupo());
            String tipoAlerta = textoSeguro(alerta.getTipoAlerta());
            String estatusAlerta = textoSeguro(alerta.getEstatus());

            boolean coincideGrupo =
                    grupo.equals("todos") ||
                            grupoAlerta.equals(grupo);

            boolean coincideTipo =
                    tipo.equals("todos") ||
                            tipoAlerta.equals(tipo) ||
                            (tipo.equals("actividad") && tipoAlerta.equals("calificacion")) ||
                            (tipo.equals("calificacion") && tipoAlerta.equals("actividad"));

            boolean coincideEstatus = switch (estatusFiltro) {
                case "activa" -> estatusAlerta.equals("pendiente") || estatusAlerta.equals("seguimiento");
                case "cerrada" -> estatusAlerta.equals("cerrada");
                case "inactiva" -> estatusAlerta.equals("cerrada");
                default -> true;
            };

            boolean coincideTexto =
                    texto.isEmpty() ||
                            textoSeguro(alerta.getAlumno()).contains(texto) ||
                            textoSeguro(alerta.getNumControl()).contains(texto) ||
                            textoSeguro(alerta.getGrupo()).contains(texto) ||
                            textoSeguro(alerta.getMateria()).contains(texto) ||
                            textoSeguro(alerta.getTipoAlerta()).contains(texto) ||
                            textoSeguro(alerta.getPrioridad()).contains(texto) ||
                            textoSeguro(alerta.getEstatus()).contains(texto) ||
                            textoSeguro(alerta.getFecha()).contains(texto);

            return coincideGrupo && coincideTipo && coincideEstatus && coincideTexto;
        });
    }

    private String valorCombo(ComboBox<String> combo) {
        return combo.getValue() == null ? "todos" : combo.getValue().trim().toLowerCase();
    }

    private String textoSeguro(String texto) {
        return texto == null ? "" : texto.toLowerCase();
    }

    private void mostrarDetalle(AlertaSinTutor alerta) {
        if (alerta == null) {
            limpiarDetalle();
            return;
        }

        lblNombreAlumno.setText(alerta.getAlumno());
        lblNumeroControl.setText(alerta.getNumControl());
        lblGrupo.setText(alerta.getGrupo());
        lblSemestre.setText(alerta.getSemestre());
        lblTurno.setText(alerta.getTurno());
        lblMateria.setText(alerta.getMateria());
        lblTipoAlerta.setText(alerta.getTipoAlerta());
        lblPrioridadDetalle.setText(alerta.getPrioridad());
        lblMotivoDetalle.setText(alerta.getMotivoDetalle());
        lblFechaCreacion.setText(alerta.getFecha());
        lblEstatusDetalle.setText(alerta.getEstatus());
    }

    private void limpiarDetalle() {
        alertaSeleccionada = null;
        lblNombreAlumno.setText("Selecciona una alerta");
        lblNumeroControl.setText("---");
        lblGrupo.setText("---");
        lblSemestre.setText("---");
        lblTurno.setText("---");
        lblMateria.setText("---");
        lblTipoAlerta.setText("Sin seleccionar");
        lblPrioridadDetalle.setText("Sin seleccionar");
        lblMotivoDetalle.setText("Selecciona una alerta para mostrar el motivo detallado.");
        lblFechaCreacion.setText("---");
        lblEstatusDetalle.setText("---");
    }

    @FXML
    private void handleCambiarEstatus() {
        if (alertaSeleccionada == null) {
            mostrarError("selecciona una alerta");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>("seguimiento", "pendiente", "seguimiento", "cerrada");
        dialog.setTitle("cambiar estatus");
        dialog.setHeaderText("selecciona el nuevo estatus");
        dialog.setContentText("estatus:");

        dialog.showAndWait().ifPresent(this::actualizarEstatus);
    }

    @FXML
    private void handleRegistrarOrientacion(ActionEvent event) {
        if (alertaSeleccionada == null) {
            mostrarError("selecciona una alerta");
            return;
        }

        SesionOrientacion.limpiar();

        SesionOrientacion.setIdAlerta(alertaSeleccionada.getIdAlerta());
        SesionOrientacion.setIdReporteDocente(0);
        SesionOrientacion.setIdAlumno(alertaSeleccionada.getIdAlumno());
        SesionOrientacion.setNumControl(alertaSeleccionada.getNumControl());
        SesionOrientacion.setNombreAlumno(alertaSeleccionada.getAlumno());
        SesionOrientacion.setGrupo(alertaSeleccionada.getGrupo());
        SesionOrientacion.setSemestre(alertaSeleccionada.getSemestre());
        SesionOrientacion.setTurno(alertaSeleccionada.getTurno());
        SesionOrientacion.setMateria(alertaSeleccionada.getMateria());
        SesionOrientacion.setTipoAlerta(alertaSeleccionada.getTipoAlerta());
        SesionOrientacion.setPrioridad(alertaSeleccionada.getPrioridad());
        SesionOrientacion.setMotivoDetalle(alertaSeleccionada.getMotivoDetalle());
        SesionOrientacion.setFechaAlerta(alertaSeleccionada.getFecha());

        cargarVistaOrientacion(event);
    }

    private void cargarVistaOrientacion(ActionEvent event) {
        String[] rutas = {
                "/application/proyecto/views/jefedemaestros/OrientacionGruposSinTutorJDM.fxml",
                "/application/proyecto/views/jefedemaestros/OrientacionSinTutorJDM.fxml",
                "/application/proyecto/views/jefedemaestros/OrientacionGruposSinTutor.fxml"
        };

        try {
            Parent vista = cargarPrimerFXMLDisponible(rutas);

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
            mostrarError("no se pudo abrir la vista de orientacion de grupos sin tutor");
        }
    }

    private Parent cargarPrimerFXMLDisponible(String[] rutas) throws IOException {
        for (String ruta : rutas) {
            URL recurso = getClass().getResource(ruta);

            if (recurso != null) {
                FXMLLoader loader = new FXMLLoader(recurso);
                return loader.load();
            }
        }

        throw new IOException("no se encontro ningun fxml valido");
    }

    @FXML
    private void handleCerrarAlerta() {
        if (alertaSeleccionada == null) {
            mostrarError("selecciona una alerta");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("confirmar");
        confirmacion.setHeaderText("cerrar alerta");
        confirmacion.setContentText("seguro que deseas cerrar esta alerta?");

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.OK) {
                actualizarEstatus("cerrada");
            }
        });
    }

    private void actualizarEstatus(String estatus) {
        if (alertaSeleccionada == null) {
            mostrarError("selecciona una alerta");
            return;
        }

        int idEstatus = switch (estatus.toLowerCase()) {
            case "cerrada" -> 0;
            case "pendiente" -> 1;
            case "seguimiento" -> 2;
            default -> 1;
        };

        String sql = """
                update alerta a
                inner join alumno al on a.id_alumno=al.id_alumno
                inner join grupo_ciclo gc on al.id_grupo_ciclo=gc.id_grupo_ciclo
                left join tutoria_asignacion ta on gc.id_grupo_ciclo=ta.id_grupo_ciclo
                and ta.id_estatus_tutoria=1
                set
                a.id_estatus_alerta=?,
                a.cerrada_en=case when ?=0 then now() else null end
                where a.id_alerta=?
                and ta.id_tutoria is null
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idEstatus);
            ps.setInt(2, idEstatus);
            ps.setInt(3, alertaSeleccionada.getIdAlerta());

            int filas = ps.executeUpdate();

            if (filas == 0) {
                mostrarError("no se pudo actualizar la alerta o el grupo ya tiene tutor");
                return;
            }

            mostrarInfo("estatus actualizado correctamente");
            cargarAlertasSinTutorAsync();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al actualizar estatus");
        }
    }

    @FXML
    private void handleActualizar() {
        cargarAlertasSinTutorAsync();
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

    private void mostrarInfo(String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("informacion");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    public static class AlertaSinTutor {
        private final int idAlerta;
        private final int idAlumno;
        private final String numControl;
        private final String alumno;
        private final String grupo;
        private final String semestre;
        private final String turno;
        private final String materia;
        private final String tipoAlerta;
        private final String prioridad;
        private final String motivoDetalle;
        private final String estatus;
        private final String fecha;

        public AlertaSinTutor(int idAlerta, int idAlumno, String numControl, String alumno, String grupo,
                              String semestre, String turno, String materia, String tipoAlerta,
                              String prioridad, String motivoDetalle, String estatus, String fecha) {
            this.idAlerta = idAlerta;
            this.idAlumno = idAlumno;
            this.numControl = textoSeguroNormal(numControl);
            this.alumno = textoSeguroNormal(alumno);
            this.grupo = textoSeguroNormal(grupo);
            this.semestre = textoSeguroNormal(semestre);
            this.turno = textoSeguroNormal(turno);
            this.materia = textoSeguroNormal(materia);
            this.tipoAlerta = textoSeguroNormal(tipoAlerta);
            this.prioridad = textoSeguroNormal(prioridad);
            this.motivoDetalle = textoSeguroNormal(motivoDetalle);
            this.estatus = textoSeguroNormal(estatus);
            this.fecha = textoSeguroNormal(fecha);
        }

        private static String textoSeguroNormal(String valor) {
            return valor == null ? "" : valor;
        }

        public int getIdAlerta() {
            return idAlerta;
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

        public String getTipoAlerta() {
            return tipoAlerta;
        }

        public String getPrioridad() {
            return prioridad;
        }

        public String getMotivoDetalle() {
            return motivoDetalle;
        }

        public String getEstatus() {
            return estatus;
        }

        public String getFecha() {
            return fecha;
        }
    }
}