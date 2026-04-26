package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class InicioMController extends BaseController {

    // --- TARJETAS DE RESUMEN ---
    @FXML private Label lblTotalClases;
    @FXML private Label lblTotalAlumnos;
    @FXML private Label lblTotalAlertas;

    // --- TABLA: MIS GRUPOS ---
    @FXML private TableView<?> tablaGrupos;
    @FXML private TableColumn<?, ?> colCodigoGrupo;
    @FXML private TableColumn<?, ?> colNombreMateriaGrupo;
    @FXML private TableColumn<?, ?> colTotalAlumnosGrupo;

    // --- TABLA: ALERTAS ACTIVAS ---
    @FXML private TableView<?> tablaAlertas;
    @FXML private TableColumn<?, ?> colNoControlAlerta;
    @FXML private TableColumn<?, ?> colNombreAlerta;
    @FXML private TableColumn<?, ?> colMateriaAlerta;
    @FXML private TableColumn<?, ?> colTipoAlerta;

    @FXML
    public void initialize() {
        System.out.println("Panel de Inicio (Maestro) cargado.");
        
        // Inicializamos las etiquetas con valores por defecto
        lblTotalClases.setText("0");
        lblTotalAlumnos.setText("0");
        lblTotalAlertas.setText("0");
    }
}