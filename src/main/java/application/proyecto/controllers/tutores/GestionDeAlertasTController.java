package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class GestionDeAlertasTController extends BaseController {

    // --- FILTROS (Center) ---
    @FXML private ComboBox<String> cmbAlumnoFiltro;
    @FXML private ComboBox<String> cmbGrupoFiltro;
    @FXML private ComboBox<String> cmbTipoFiltro;
    @FXML private ComboBox<String> cmbPrioridadFiltro;
    @FXML private ComboBox<String> cmbEstatusFiltro;
    @FXML private Button btnBuscarFiltros;

    // --- TABLA (Center) ---
    @FXML private TableView<?> tablaGestionAlertasTutor;
    @FXML private TableColumn<?, ?> colAlumno;
    @FXML private TableColumn<?, ?> colGrupo;
    @FXML private TableColumn<?, ?> colMotivo;
    @FXML private TableColumn<?, ?> colPrioridad;
    @FXML private TableColumn<?, ?> colTipo;
    @FXML private TableColumn<?, ?> colEstatus;
    @FXML private TableColumn<?, ?> colAccion;

    // --- DETALLES (Right Panel) ---
    @FXML private Label lblNombreAlumno;
    @FXML private Label lblNumeroControl;
    @FXML private Label lblGrupo;
    @FXML private Label lblSemestre;
    @FXML private Label lblTipoAlerta;
    @FXML private Label lblPrioridadDetalle;
    @FXML private Label lblMotivoDetalle;
    @FXML private Label lblFechaCreacion;
    @FXML private Label lblOrigen;
    @FXML private Label lblEstatusDetalle;

    @FXML
    public void initialize() {
        System.out.println("Módulo de Gestión de Alertas (Tutores) cargado.");
        
        // Aquí podrías agregar un listener a la tabla para que, al seleccionar una fila,
        // se actualicen los Labels del panel derecho automáticamente.
    }

    @FXML
    private void handleBuscarConFiltros() {
        System.out.println("Filtrando alertas según los criterios seleccionados...");
        // Lógica para consultar la base de datos con los filtros de los ComboBox
    }

    // Métodos para los botones de acción del panel derecho
    @FXML
    private void handleCambiarEstatus() {
        System.out.println("Abriendo diálogo para cambiar estatus de la alerta...");
    }

    @FXML
    private void handleAgregarObservacion() {
        System.out.println("Abriendo editor de observaciones...");
    }

    @FXML
    private void handleRegistrarOrientacion() {
        System.out.println("Iniciando registro de orientación para el alumno...");
    }

    @FXML
    private void handleCerrarAlerta() {
        System.out.println("Confirmando cierre definitivo de la alerta...");
    }
}