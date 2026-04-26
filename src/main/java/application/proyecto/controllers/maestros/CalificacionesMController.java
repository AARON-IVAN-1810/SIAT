package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class CalificacionesMController extends BaseController {

    // --- BARRA SUPERIOR ---
    @FXML private TextField txtBuscar;
    @FXML private Button btnCerrarSesion;

    // --- FILTROS DE SELECCIÓN ---
    @FXML private ComboBox<String> cbGrupo;
    @FXML private TextField txtTurno;
    @FXML private ComboBox<String> cbMateria;
    @FXML private ComboBox<String> cbActividad;

    // --- BOTONES DE ACCIÓN ---
    @FXML private Button btnCargar;
    @FXML private Button btnNuevaActividad;
    @FXML private Button btnGuardarCalificaciones;

    // --- ETIQUETAS DE RESUMEN (Labels) ---
    @FXML private Label lblActividadActual;
    @FXML private Label lblGrupoSeleccionado;
    @FXML private Label lblTurnoSeleccionado;
    @FXML private Label lblMateriaSeleccionada;

    // --- TABLA DE CALIFICACIONES ---
    @FXML private TableView<?> tablaCalificaciones;
    @FXML private TableColumn<?, ?> colNoControl;
    @FXML private TableColumn<?, ?> colNombreAlumno;
    @FXML private TableColumn<?, ?> colGrupo;
    @FXML private TableColumn<?, ?> colMateria;
    @FXML private TableColumn<?, ?> colTurno;
    @FXML private TableColumn<?, ?> colCalificacion;

    @FXML
    public void initialize() {
        System.out.println("Controlador de Calificaciones (Maestros) listo.");
        // Aquí podrías inicializar los ComboBox con datos de prueba o de la DB
    }

    // --- MÉTODOS DE LÓGICA ---

    @FXML
    private void handleCargarCalificaciones() {
        System.out.println("Cargando lista de alumnos y sus notas...");
    }

    @FXML
    private void handleGuardarCalificaciones() {
        System.out.println("Enviando calificaciones a la base de datos...");
    }

    @FXML
    private void handleNuevaActividad() {
        System.out.println("Abriendo ventana para crear nueva actividad...");
    }
}