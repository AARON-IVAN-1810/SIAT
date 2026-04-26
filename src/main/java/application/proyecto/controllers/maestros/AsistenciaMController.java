package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AsistenciaMController extends BaseController {

    // --- BARRA SUPERIOR ---
    @FXML private TextField txtBuscar;
    @FXML private Button btnCerrarSesion;

    // --- FORMULARIO DE SELECCIÓN ---
    @FXML private ComboBox<String> cbGrupo;
    @FXML private TextField txtTurno;
    @FXML private ComboBox<String> cbMateria;
    @FXML private DatePicker dpFecha;
    
    // --- BOTONES DE ACCIÓN ---
    @FXML private Button btnCargar;
    @FXML private Button btnTodosAsistieron;
    @FXML private Button btnLimpiar;
    @FXML private Button btnGuardarAsistencia;

    // --- ETIQUETAS DE RESUMEN (Labels) ---
    @FXML private Label lblFechaSeleccionada;
    @FXML private Label lblGrupoSeleccionado;
    @FXML private Label lblTurnoSeleccionado;
    @FXML private Label lblMateriaSeleccionada;
    @FXML private Label lblTotalAlumnos;

    // --- TABLA DE ASISTENCIA ---
    @FXML private TableView<?> tablaAsistencia;
    @FXML private TableColumn<?, ?> colNoControl;
    @FXML private TableColumn<?, ?> colNombreAlumno;
    @FXML private TableColumn<?, ?> colGrupo;
    @FXML private TableColumn<?, ?> colTurno;
    @FXML private TableColumn<?, ?> colEstado;

    @FXML
    public void initialize() {
        System.out.println("Controlador de Asistencia (Maestros) inicializado.");
        // Aquí puedes configurar valores iniciales para los ComboBox
    }

    // --- MÉTODOS DE ACCIÓN ---

    @FXML
    private void handleCargarAlumnos() {
        System.out.println("Cargando lista de alumnos para el grupo seleccionado...");
    }

    @FXML
    private void handleGuardarAsistencia() {
        System.out.println("Guardando registro de asistencia en la base de datos...");
    }

    @FXML
    private void handleLimpiarCampos() {
        cbGrupo.setValue(null);
        cbMateria.setValue(null);
        dpFecha.setValue(null);
        txtTurno.clear();
        System.out.println("Formulario de asistencia limpio.");
    }
}