package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class MateriasJDMController extends BaseController {

    // --- CABLES PARA EL PANEL DE AGREGAR ---
    @FXML private TextField txtNombreMateria;
    @FXML private TextField txtClaveMateria;
    @FXML private ComboBox<String> cbTurnoMateria;
    @FXML private ComboBox<String> cbMaestroMateria;

    // --- CABLES PARA EL BUSCADOR Y FILTROS ---
    @FXML private TextField txtBuscarMateriaInterna;
    @FXML private ComboBox<String> cbFiltroMaterias;

    // --- CABLES PARA LA TABLA ---
    @FXML private TableView<?> tablaMaterias;
    @FXML private TableColumn<?, ?> colIdMateria;
    @FXML private TableColumn<?, ?> colNombreMateria;
    @FXML private TableColumn<?, ?> colClaveMateria;
    @FXML private TableColumn<?, ?> colTurnoMateria;
    @FXML private TableColumn<?, ?> colMaestroMateria;
    @FXML private TableColumn<?, ?> colAccionMateria;

    @FXML
    public void initialize() {
        // Este método se ejecuta al cargar la vista
        System.out.println("Vista de Materias JDM lista y Logout heredado.");
        
        // Aquí es donde después harás el SELECT de tus maestros 
        // para llenar el cbMaestroMateria
    }

    @FXML
    private void handleGuardarMateria() {
        // Aquí irá tu lógica para el INSERT en la base de datos
        System.out.println("Intentando guardar materia: " + txtNombreMateria.getText());
    }
}