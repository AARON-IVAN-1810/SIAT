package application.proyecto.controllers.jefedemaestros;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class AlumnosJDMController {

    // Estas líneas son las que faltaban para que la vista NO TRUENE al abrir
    @FXML private TextField txtNombreAlumno, txtNumeroControlAlumno, txtBuscarAlumnoInterno;
    @FXML private ComboBox<String> cbGrupoAlumno, cbFiltroAlumnos;
    @FXML private TableView<?> tablaAlumnos; // El ? es porque aún no definimos el modelo
    @FXML private TableColumn<?, ?> colIdAlumno, colNombreAlumno, colNumeroControlAlumno, colGrupoAlumno, colEstatusAlumno, colAccionAlumno;

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            System.out.println("Saliendo al login desde Alumnos...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        System.out.println("Vista de Alumnos cargada correctamente.");
        // Aquí es donde después programarás que se llene la tabla
    }
}