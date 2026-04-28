package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.models.AlumnoAsistencia;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.CheckBox;

public class AsistenciaMController extends BaseController {

    // --- FILTROS DE ARRIBA ---
    @FXML private ComboBox<String> cmbGrupo;
    @FXML private ComboBox<String> cmbMateria;
    @FXML private TextField txtTurno;
    @FXML private DatePicker dpFecha;

    // --- ETIQUETAS DE RESUMEN (La barra azul clarito) ---
    @FXML private Label lblResumenFecha;
    @FXML private Label lblResumenGrupo;
    @FXML private Label lblResumenTurno;
    @FXML private Label lblResumenMateria;
    @FXML private Label lblResumenAlumnos;

    // --- TABLA PRINCIPAL ---
    @FXML private TableView<AlumnoAsistencia> tablaAsistencia;
    @FXML private TableColumn<AlumnoAsistencia, String> colNoControl;
    @FXML private TableColumn<AlumnoAsistencia, String> colNombreAlumno;
    @FXML private TableColumn<AlumnoAsistencia, String> colGrupo;
    @FXML private TableColumn<AlumnoAsistencia, String> colTurno;
    @FXML private TableColumn<AlumnoAsistencia, CheckBox> colEstado;

    @FXML
    public void initialize() {
        System.out.println("✅ Pantalla de Asistencia cargada.");
        
        // 1. Configurar las columnas de la tabla con los nombres exactos de los Getters
        colNoControl.setCellValueFactory(new PropertyValueFactory<>("numControl"));
        colNombreAlumno.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colTurno.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        application.proyecto.daos.MaestroDAO maestroDAO = new application.proyecto.daos.MaestroDAO();
        int idMaestroLogueado = 1; // El ID de Juan Pérez

        // 3. Llenar los ComboBox con la info real
        cmbMateria.setItems(maestroDAO.obtenerNombresMaterias(idMaestroLogueado));
        cmbGrupo.setItems(maestroDAO.obtenerNombresGrupos(idMaestroLogueado));

        // 4. Seleccionar el primer elemento automáticamente para que no se vea vacío
        if (!cmbMateria.getItems().isEmpty()) cmbMateria.getSelectionModel().selectFirst();
        if (!cmbGrupo.getItems().isEmpty()) cmbGrupo.getSelectionModel().selectFirst();
    }

    // --- ACCIONES DE LOS BOTONES ---
    @FXML
    private void clicCargar() {
        System.out.println("Botón Cargar presionado. Aquí buscaremos en la BD.");
        // Aquí meteremos la lógica del DAO en el siguiente paso
    }

    @FXML
    private void clicGuardar() {
        System.out.println("Botón Guardar presionado. Recorriendo tabla...");
    }
}