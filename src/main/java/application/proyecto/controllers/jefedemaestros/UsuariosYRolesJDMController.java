package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class UsuariosYRolesJDMController extends BaseController {

    // --- FORMULARIO IZQUIERDO ---
    @FXML private ComboBox<String> cbMaestroUsuario;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private CheckBox chkRolMaestro;
    @FXML private CheckBox chkRolTutor;
    @FXML private CheckBox chkRolAdministrador;

    // --- BÚSQUEDA Y FILTRO ---
    @FXML private TextField txtBuscarUsuarioInterno;
    @FXML private ComboBox<String> cbFiltroUsuarios;

    // --- TABLA DE USUARIOS ---
    @FXML private TableView<?> tablaUsuarios;
    @FXML private TableColumn<?, ?> colIdUsuario;
    @FXML private TableColumn<?, ?> colUsuario;
    @FXML private TableColumn<?, ?> colMaestroRelacionado;
    @FXML private TableColumn<?, ?> colRolesUsuario;
    @FXML private TableColumn<?, ?> colEstatusUsuario;
    @FXML private TableColumn<?, ?> colAccionUsuario;

    @FXML
    public void initialize() {
        System.out.println("Vista de Usuarios y Roles cargada correctamente.");
        // El Logout ya funciona por el BaseController
    }

    @FXML
    private void handleGuardarUsuario() {
        // Aquí irá tu lógica para insertar en la DB
        String user = txtUsuario.getText();
        boolean isMaestro = chkRolMaestro.isSelected();
        System.out.println("Guardando usuario: " + user + " con rol maestro: " + isMaestro);
    }
}