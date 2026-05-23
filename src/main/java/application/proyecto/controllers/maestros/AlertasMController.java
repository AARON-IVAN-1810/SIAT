package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionAlerta;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import application.proyecto.controllers.MaestroController;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AlertasMController extends BaseController {

    @FXML private TextField txtBuscarSuperior;
    @FXML private ComboBox<String> cbFiltroEstado;
    @FXML private ComboBox<String> cbFiltroMotivo;
    @FXML private TextField txtBuscarTabla;

    @FXML private TableView<AlertaMaestro> tablaAlertas;
    @FXML private TableColumn<AlertaMaestro, String> colNoControl;
    @FXML private TableColumn<AlertaMaestro, String> colNombre;
    @FXML private TableColumn<AlertaMaestro, String> colGrupo;
    @FXML private TableColumn<AlertaMaestro, String> colTurno;
    @FXML private TableColumn<AlertaMaestro, String> colMateria;
    @FXML private TableColumn<AlertaMaestro, String> colMotivo;
    @FXML private TableColumn<AlertaMaestro, String> colEstado;

    @FXML private Button btnDarSeguimiento;

    private final ObservableList<AlertaMaestro> listaAlertas = FXCollections.observableArrayList();
    private FilteredList<AlertaMaestro> listaFiltrada;
    private AlertaMaestro alertaSeleccionada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        configurarEventos();
        cargarAlertas();
    }

    private int getIdMaestroActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTabla() {
        colNoControl.setCellValueFactory(new PropertyValueFactory<>("numControl"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreAlumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colTurno.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        listaFiltrada = new FilteredList<>(listaAlertas, p -> true);
        tablaAlertas.setItems(listaFiltrada);
    }

    private void configurarFiltros() {
        cbFiltroEstado.setItems(FXCollections.observableArrayList(
                "todos",
                "pendiente",
                "cerrada"

        ));

        cbFiltroMotivo.setItems(FXCollections.observableArrayList(
                "todos",
                "asistencia",
                "actividad"

        ));

        cbFiltroEstado.setValue("todos");
        cbFiltroMotivo.setValue("todos");
    }

    private void configurarEventos() {
        tablaAlertas.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            alertaSeleccionada = newValue;
        });

        txtBuscarTabla.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cbFiltroEstado.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cbFiltroMotivo.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void cargarAlertas() {
        listaAlertas.clear();

        int idMaestro = getIdMaestroActual();

        if (idMaestro == 0) {
            mostrarError("no hay maestro en sesion");
            return;
        }

        String sql = """
                select
                a.id_alerta,
                al.id_alumno,
                c.id_carga,
                al.num_control,
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as nombre_alumno,
                g.nombre as grupo,
                ct.nombre as turno,
                m.nombre as materia,
                cta.nombre as motivo,
                cea.nombre as estado
                from alerta a
                inner join alumno al on a.id_alumno=al.id_alumno
                inner join carga c on a.id_carga=c.id_carga
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join materia m on c.id_materia=m.id_materia
                inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                inner join cat_estatus_alerta cea on a.id_estatus_alerta=cea.id_estatus_alerta
                where c.id_maestro=?
                order by a.creada_en desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaAlertas.add(new AlertaMaestro(
                            rs.getInt("id_alerta"),
                            rs.getInt("id_alumno"),
                            rs.getInt("id_carga"),
                            rs.getString("num_control"),
                            rs.getString("nombre_alumno"),
                            rs.getString("grupo"),
                            rs.getString("turno"),
                            rs.getString("materia"),
                            rs.getString("motivo"),
                            rs.getString("estado")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alertas");
        }
    }

    private void aplicarFiltro() {
        String texto = txtBuscarTabla.getText() == null
                ? ""
                : txtBuscarTabla.getText().toLowerCase().trim();

        String estado = cbFiltroEstado.getValue() == null
                ? "todos"
                : cbFiltroEstado.getValue().toLowerCase().trim();

        String motivo = cbFiltroMotivo.getValue() == null
                ? "todos"
                : cbFiltroMotivo.getValue().toLowerCase().trim();

        listaFiltrada.setPredicate(alerta -> {
            boolean coincideTexto =
                    alerta.getNumControl().toLowerCase().contains(texto) ||
                            alerta.getNombreAlumno().toLowerCase().contains(texto) ||
                            alerta.getGrupo().toLowerCase().contains(texto) ||
                            alerta.getTurno().toLowerCase().contains(texto) ||
                            alerta.getMateria().toLowerCase().contains(texto) ||
                            alerta.getMotivo().toLowerCase().contains(texto) ||
                            alerta.getEstado().toLowerCase().contains(texto);

            boolean coincideEstado =
                    estado.equals("todos") ||
                            alerta.getEstado().toLowerCase().equals(estado);

            boolean coincideMotivo =
                    motivo.equals("todos") ||
                            alerta.getMotivo().toLowerCase().equals(motivo);

            return coincideTexto && coincideEstado && coincideMotivo;
        });
    }

    @FXML
    private void handleDarSeguimiento() {
        if (alertaSeleccionada == null) {
            mostrarError("selecciona una alerta de la tabla");
            return;
        }

        SesionAlerta.seleccionarAlerta(
                alertaSeleccionada.getIdAlerta(),
                alertaSeleccionada.getIdAlumno(),
                alertaSeleccionada.getIdCarga(),
                alertaSeleccionada.getGrupo(),
                alertaSeleccionada.getTurno(),
                alertaSeleccionada.getMateria(),
                alertaSeleccionada.getNombreAlumno(),
                alertaSeleccionada.getNumControl(),
                alertaSeleccionada.getMotivo()
        );

        Stage stage = (Stage) btnDarSeguimiento.getScene().getWindow();
        Scene scene = stage.getScene();

        MaestroController maestroController = (MaestroController) scene.getRoot().getProperties().get("controller");

        if (maestroController != null) {
            maestroController.cargarVista("/application/proyecto/views/maestro/ReportesM.fxml");
        } else {
            mostrarError("no se pudo abrir la vista de reportes automaticamente");
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("informacion");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class AlertaMaestro {
        private final int idAlerta;
        private final int idAlumno;
        private final int idCarga;
        private final String numControl;
        private final String nombreAlumno;
        private final String grupo;
        private final String turno;
        private final String materia;
        private final String motivo;
        private final String estado;

        public AlertaMaestro(int idAlerta, int idAlumno, int idCarga, String numControl, String nombreAlumno, String grupo, String turno, String materia, String motivo, String estado) {
            this.idAlerta = idAlerta;
            this.idAlumno = idAlumno;
            this.idCarga = idCarga;
            this.numControl = numControl;
            this.nombreAlumno = nombreAlumno;
            this.grupo = grupo;
            this.turno = turno;
            this.materia = materia;
            this.motivo = motivo;
            this.estado = estado;
        }

        public int getIdAlerta() { return idAlerta; }
        public int getIdAlumno() { return idAlumno; }
        public int getIdCarga() { return idCarga; }
        public String getNumControl() { return numControl; }
        public String getNombreAlumno() { return nombreAlumno; }
        public String getGrupo() { return grupo; }
        public String getTurno() { return turno; }
        public String getMateria() { return materia; }
        public String getMotivo() { return motivo; }
        public String getEstado() { return estado; }
    }
}