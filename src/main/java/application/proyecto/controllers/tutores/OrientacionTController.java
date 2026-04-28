package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class OrientacionTController extends BaseController {

    // --- BOTONES DE ACCIÓN RÁPIDA ---
    @FXML private Button btnRegistrarIntervencion;
    @FXML private Button btnVerAlertas;

    // --- FILTROS DE HISTORIAL ---
    @FXML private ComboBox<String> cmbFiltroAlumno;
    @FXML private ComboBox<String> cmbSeleccionAlumno;
    @FXML private TextField txtFechaDesde;
    @FXML private TextField txtFechaHasta;

    // --- TABLA DE INTERVENCIONES ---
    @FXML private TableView<?> tablaIntervencionesTutor;
    @FXML private TableColumn<?, ?> colAlumnoIntervencion;
    @FXML private TableColumn<?, ?> colFechaIntervencion;
    @FXML private TableColumn<?, ?> colTipoIntervencion;
    @FXML private TableColumn<?, ?> colAcuerdosIntervencion;
    @FXML private TableColumn<?, ?> colResultadoIntervencion;
    @FXML private TableColumn<?, ?> colAccionIntervencion;

    // --- BITÁCORA ---
    @FXML private ListView<String> listBitacoraTutor;

    @FXML
    public void initialize() {
        System.out.println("Módulo de Orientación cargado. Listo para registrar intervenciones.");
        
        // Ejemplo: Mensaje inicial en la bitácora
        listBitacoraTutor.getItems().add("Esperando selección de alumno para mostrar eventos...");
    }

    // --- MÉTODOS DE LÓGICA ---

    @FXML
    private void handleRegistrarIntervencion() {
        // Aquí abrirías el modal o ventana para la nueva intervención
        System.out.println("Abriendo formulario de nueva intervención...");
    }

    @FXML
    private void handleVerAlertas() {
        // Lógica para saltar a la vista de Gestión de Alertas
        System.out.println("Cambiando a la vista de todas las alertas...");
    }

    @FXML
    private void handleFiltrarHistorial() {
        System.out.println("Filtrando historial de intervenciones por fecha y alumno...");
    }
}