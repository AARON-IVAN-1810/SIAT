package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AlertasMController extends BaseController {

    // --- BUSQUEDA SUPERIOR ---
    @FXML private TextField txtBuscarSuperior;

    // --- FILTROS Y BUSQUEDA DE TABLA ---
    @FXML private ComboBox<String> cbFiltroEstado;
    @FXML private ComboBox<String> cbFiltroMotivo;
    @FXML private TextField txtBuscarTabla;

    // --- TABLA DE ALERTAS ---
    @FXML private TableView<?> tablaAlertas;
    @FXML private TableColumn<?, ?> colNoControl;
    @FXML private TableColumn<?, ?> colNombre;
    @FXML private TableColumn<?, ?> colGrupo;
    @FXML private TableColumn<?, ?> colTurno;
    @FXML private TableColumn<?, ?> colMateria;
    @FXML private TableColumn<?, ?> colMotivo;
    @FXML private TableColumn<?, ?> colEstado;

    // --- BOTONES ---
    @FXML private Button btnDarSeguimiento;

    @FXML
    public void initialize() {
        System.out.println("Vista de Alertas de Maestro cargada.");
        // Aquí cargarás las alertas específicas del maestro logueado
    }

    @FXML
    private void handleDarSeguimiento() {
        // Lógica para abrir el detalle de la alerta seleccionada
        System.out.println("Abriendo seguimiento de alerta...");
    }
}