package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class InicioTController extends BaseController {

    // --- TARJETAS DE RESUMEN (Métricas) ---
    @FXML private Label lblAlertasActivas;
    @FXML private Label lblCasosSeguimiento;
    @FXML private Label lblCasosCerrados;
    @FXML private Label lblIntervencionesRecientes;

    // --- FILTROS ---
    @FXML private ComboBox<String> cmbGrupoFiltro;
    @FXML private ComboBox<String> cmbPrioridadFiltro;
    @FXML private Button btnBuscar;
    @FXML private TextField txtBuscarTabla;

    // --- TABLA DE ALERTAS PENDIENTES ---
    @FXML private TableView<?> tablaAlertasTutor;
    @FXML private TableColumn<?, ?> colAlumno;
    @FXML private TableColumn<?, ?> colGrupo;
    @FXML private TableColumn<?, ?> colTipoAlerta;
    @FXML private TableColumn<?, ?> colPrioridad;
    @FXML private TableColumn<?, ?> colEstatus;
    @FXML private TableColumn<?, ?> colAccion;

    @FXML
    public void initialize() {
        System.out.println("Panel de Inicio (Tutor) cargado.");
        
        // Inicializamos los contadores en 0 para evitar que se vea el "--"
        lblAlertasActivas.setText("0");
        lblCasosSeguimiento.setText("0");
        lblCasosCerrados.setText("0");
        lblIntervencionesRecientes.setText("0");
    }

    @FXML
    private void handleBuscar() {
        // Aquí programarás la consulta a la base de datos filtrada
        String grupo = cmbGrupoFiltro.getValue();
        String prioridad = cmbPrioridadFiltro.getValue();
        System.out.println("Filtrando alertas de tutoría por: " + grupo + " | " + prioridad);
    }
}