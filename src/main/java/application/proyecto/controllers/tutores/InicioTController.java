package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class InicioTController extends BaseController {

    @FXML private Label lblAlertasActivas;
    @FXML private Label lblCasosSeguimiento;
    @FXML private Label lblCasosCerrados;
    @FXML private Label lblIntervencionesRecientes;

    @FXML private ComboBox<String> cmbGrupoFiltro;
    @FXML private ComboBox<String> cmbTipoFiltro;
    @FXML private TextField txtBuscarTabla;

    @FXML private TableView<AlertaTutor> tablaAlertasTutor;
    @FXML private TableColumn<AlertaTutor, String> colAlumno;
    @FXML private TableColumn<AlertaTutor, String> colGrupo;
    @FXML private TableColumn<AlertaTutor, String> colTipoAlerta;
    @FXML private TableColumn<AlertaTutor, String> colPrioridad;
    @FXML private TableColumn<AlertaTutor, String> colEstatus;
    @FXML private TableColumn<AlertaTutor, String> colFecha;

    private final ObservableList<AlertaTutor> listaAlertas = FXCollections.observableArrayList();
    private FilteredList<AlertaTutor> listaFiltrada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        configurarEventos();
        mostrarCargando();
        cargarDatosInicialesAsync();
    }

    private int getIdTutorActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTabla() {
        colAlumno.setCellValueFactory(new PropertyValueFactory<>("alumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colTipoAlerta.setCellValueFactory(new PropertyValueFactory<>("tipoAlerta"));
        colPrioridad.setCellValueFactory(new PropertyValueFactory<>("prioridad"));
        colEstatus.setCellValueFactory(new PropertyValueFactory<>("estatus"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        configurarColumnaPrioridad();

        listaFiltrada = new FilteredList<>(listaAlertas, p -> true);
        tablaAlertasTutor.setItems(listaFiltrada);
    }

    private void configurarColumnaPrioridad() {
        colPrioridad.setCellFactory(tc -> new TableCell<AlertaTutor, String>() {
            @Override
            protected void updateItem(String prioridad, boolean empty) {
                super.updateItem(prioridad, empty);

                if (empty || prioridad == null || prioridad.trim().isEmpty()) {
                    setText(null);
                    setStyle("");
                    return;
                }

                String valor = prioridad.toLowerCase().trim();
                setText(prioridad);

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
                "calificacion",
                "conducta"
        ));
        cmbTipoFiltro.setValue("todos");
    }

    private void configurarEventos() {
        txtBuscarTabla.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbGrupoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbTipoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void mostrarCargando() {
        lblAlertasActivas.setText("...");
        lblCasosSeguimiento.setText("...");
        lblCasosCerrados.setText("...");
        lblIntervencionesRecientes.setText("...");
    }

    private void cargarDatosInicialesAsync() {
        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        Task<DatosInicioTutor> task = new Task<>() {
            @Override
            protected DatosInicioTutor call() throws Exception {
                DatosInicioTutor datos = new DatosInicioTutor();

                try (Connection con = ConexionBD.conectar()) {
                    cargarMetricasDesdeSP(con, idTutor, datos);
                    cargarGruposDesdeSP(con, idTutor, datos);
                    cargarAlertasDesdeSP(con, idTutor, datos);
                }

                return datos;
            }
        };

        task.setOnSucceeded(event -> {
            DatosInicioTutor datos = task.getValue();

            lblAlertasActivas.setText(String.valueOf(datos.alertasActivas));
            lblCasosSeguimiento.setText(String.valueOf(datos.casosSeguimiento));
            lblCasosCerrados.setText(String.valueOf(datos.casosCerrados));
            lblIntervencionesRecientes.setText(String.valueOf(datos.intervencionesRecientes));

            cmbGrupoFiltro.getItems().setAll(datos.grupos);
            cmbGrupoFiltro.setValue("todos");

            listaAlertas.setAll(datos.alertas);
            aplicarFiltro();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            mostrarError("error al cargar datos del inicio");
            lblAlertasActivas.setText("0");
            lblCasosSeguimiento.setText("0");
            lblCasosCerrados.setText("0");
            lblIntervencionesRecientes.setText("0");
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarMetricasDesdeSP(Connection con, int idTutor, DatosInicioTutor datos) throws Exception {
        String sql = "{call sp_tutor_metricas_inicio(?)}";

        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idTutor);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    datos.alertasActivas = rs.getInt("alertas_activas");
                    datos.casosSeguimiento = rs.getInt("casos_seguimiento");
                    datos.casosCerrados = rs.getInt("casos_cerrados");
                    datos.intervencionesRecientes = rs.getInt("intervenciones_recientes");
                }
            }
        }
    }

    private void cargarGruposDesdeSP(Connection con, int idTutor, DatosInicioTutor datos) throws Exception {
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

    private void cargarAlertasDesdeSP(Connection con, int idTutor, DatosInicioTutor datos) throws Exception {
        datos.alertas.clear();

        String sql = "{call sp_tutor_alertas_inicio(?)}";

        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idTutor);

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    datos.alertas.add(new AlertaTutor(
                            rs.getString("alumno"),
                            rs.getString("grupo"),
                            rs.getString("tipo_alerta"),
                            rs.getString("prioridad"),
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

        String texto = txtBuscarTabla.getText() == null ? "" : txtBuscarTabla.getText().toLowerCase().trim();
        String grupo = cmbGrupoFiltro.getValue() == null ? "todos" : cmbGrupoFiltro.getValue().toLowerCase().trim();
        String tipo = cmbTipoFiltro.getValue() == null ? "todos" : cmbTipoFiltro.getValue().toLowerCase().trim();

        listaFiltrada.setPredicate(alerta -> {
            boolean coincideTexto =
                    texto.isEmpty() ||
                            alerta.getAlumno().toLowerCase().contains(texto) ||
                            alerta.getGrupo().toLowerCase().contains(texto) ||
                            alerta.getTipoAlerta().toLowerCase().contains(texto) ||
                            alerta.getPrioridad().toLowerCase().contains(texto) ||
                            alerta.getEstatus().toLowerCase().contains(texto) ||
                            alerta.getFecha().toLowerCase().contains(texto);

            String grupoAlerta = alerta.getGrupo().toLowerCase();
            String tipoAlerta = alerta.getTipoAlerta().toLowerCase();

            boolean coincideGrupo =
                    grupo.equals("todos") ||
                            grupoAlerta.contains(grupo);

            boolean coincideTipo =
                    tipo.equals("todos") ||
                            tipoAlerta.equals(tipo) ||
                            (tipo.equals("actividad") && tipoAlerta.equals("calificacion")) ||
                            (tipo.equals("calificacion") && tipoAlerta.equals("actividad"));

            return coincideTexto && coincideGrupo && coincideTipo;
        });
    }

    @FXML
    private void handleActualizar() {
        mostrarCargando();
        cargarDatosInicialesAsync();
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

    private static class DatosInicioTutor {
        private int alertasActivas = 0;
        private int casosSeguimiento = 0;
        private int casosCerrados = 0;
        private int intervencionesRecientes = 0;
        private final List<String> grupos = new ArrayList<>();
        private final List<AlertaTutor> alertas = new ArrayList<>();
    }

    public static class AlertaTutor {
        private final String alumno;
        private final String grupo;
        private final String tipoAlerta;
        private final String prioridad;
        private final String estatus;
        private final String fecha;

        public AlertaTutor(String alumno, String grupo, String tipoAlerta, String prioridad, String estatus, String fecha) {
            this.alumno = textoSeguro(alumno);
            this.grupo = textoSeguro(grupo);
            this.tipoAlerta = textoSeguro(tipoAlerta);
            this.prioridad = textoSeguro(prioridad);
            this.estatus = textoSeguro(estatus);
            this.fecha = textoSeguro(fecha);
        }

        private static String textoSeguro(String valor) {
            return valor == null ? "" : valor;
        }

        public String getAlumno() {
            return alumno;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getTipoAlerta() {
            return tipoAlerta;
        }

        public String getPrioridad() {
            return prioridad;
        }

        public String getEstatus() {
            return estatus;
        }

        public String getFecha() {
            return fecha;
        }
    }
}