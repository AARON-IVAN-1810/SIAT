package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class InicioJDMController extends BaseController {

    // --- CABLES PARA LAS TARJETAS (Métricas) ---
    @FXML private Label lblTotalAlumnos;
    @FXML private Label lblAlertasActivas;
    @FXML private Label lblRiesgoBajo;
    @FXML private Label lblTotalGrupos;
    @FXML private Label lblTotalMaestros;

    // --- CABLES PARA LA TABLA DE ALERTAS ---
    @FXML private TableView<?> tablaAlertasRecientes;
    @FXML private ComboBox<String> cmbFiltroAlertas;
    @FXML private TextField txtBuscarAlertasRecientes;

    @FXML
    public void initialize() {
        lblTotalAlumnos.setText("0");
        lblAlertasActivas.setText("0");
        lblRiesgoBajo.setText("0");
        lblTotalGrupos.setText("0");
        lblTotalMaestros.setText("0");
        
        System.out.println("Panel de Inicio cargado. Logout heredado correctamente.");
    }
}