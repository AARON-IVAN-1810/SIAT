package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class MaestrosJDMController extends BaseController {

    // Campos de texto para agregar/editar
    @FXML private TextField txtNombreMaestro;
    @FXML private TextField txtNumeroEmpleado;
    @FXML private TextField txtCorreoInstitucional;
    @FXML private TextField txtTelefonoInstitucional;
    
    // Buscador interno
    @FXML private TextField txtBuscarMaestroInterno;
    @FXML private ComboBox<String> cbFiltroMaestros;

    // Tabla de maestros
    @FXML private TableView<?> tablaMaestros;
    @FXML private TableColumn<?, ?> colIdMaestro;
    @FXML private TableColumn<?, ?> colNombreMaestro;
    @FXML private TableColumn<?, ?> colNumeroEmpleadoMaestro;
    @FXML private TableColumn<?, ?> colCorreoMaestro;
    @FXML private TableColumn<?, ?> colEstatusMaestro;
    @FXML private TableColumn<?, ?> colAccionMaestro;

    @FXML
    public void initialize() {
        System.out.println("Controlador de Maestros JDM inicializado.");
        // Aquí puedes cargar los datos de la base de datos después
    }

    @FXML
    private void handleGuardarMaestro() {
        // Lógica para guardar en la base de datos
        String nombre = txtNombreMaestro.getText();
        String empleado = txtNumeroEmpleado.getText();
        System.out.println("Guardando a: " + nombre + " con ID: " + empleado);
    }
}