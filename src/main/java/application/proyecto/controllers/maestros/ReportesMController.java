package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ReportesMController extends BaseController {

    // --- BÚSQUEDA SUPERIOR ---
    @FXML private TextField txtBuscarSuperior;

    // --- FORMULARIO DE GENERACIÓN ---
    @FXML private ComboBox<String> cbMateria;
    @FXML private ComboBox<String> cbGrupo;
    @FXML private TextField txtTurno;
    @FXML private ComboBox<String> cbAlumno;
    @FXML private ComboBox<String> cbMotivo;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnGenerarReporte;

    // --- FILTROS DE TABLA ---
    @FXML private ComboBox<String> cbFiltroMateria;
    @FXML private ComboBox<String> cbFiltroMotivo;
    @FXML private ComboBox<String> cbFiltroEstado;
    @FXML private TextField txtBuscarTabla;

    // --- TABLA DE REPORTES ---
    @FXML private TableView<?> tablaReportes;
    @FXML private TableColumn<?, ?> colNoControl;
    @FXML private TableColumn<?, ?> colNombre;
    @FXML private TableColumn<?, ?> colGrupo;
    @FXML private TableColumn<?, ?> colTurno;
    @FXML private TableColumn<?, ?> colMateria;
    @FXML private TableColumn<?, ?> colMotivo;
    @FXML private TableColumn<?, ?> colDescripcion;
    @FXML private TableColumn<?, ?> colEstado;

    @FXML
    public void initialize() {
        // Configuración inicial al cargar la vista
        System.out.println("Módulo de Reportes (Maestros) inicializado.");
        
        // Aquí se conectará la lógica para llenar los ComboBox desde la BD
    }

    @FXML
    private void handleGenerarReporte() {
        // Lógica para validar y guardar el reporte en la base de datos
        String alumno = cbAlumno.getValue();
        String motivo = cbMotivo.getValue();
        String descripcion = txtDescripcion.getText();

        if (alumno != null && motivo != null && !descripcion.isEmpty()) {
            System.out.println("Generando reporte para: " + alumno + " por " + motivo);
            // Ejecutar INSERT en la base de datos
        } else {
            System.out.println("Error: Campos incompletos.");
        }
    }
}