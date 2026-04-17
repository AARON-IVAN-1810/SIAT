package application.proyecto.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

public class LoginController {

    @FXML
    private ComboBox<String> cbRol;

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    public void initialize() {
        cbRol.setItems(FXCollections.observableArrayList(
                "Jefe de Maestros", "Maestro", "Tutor"
        ));
    }

    @FXML
    private void clikIngresar(ActionEvent event) {
        String rol = cbRol.getValue();

        if (rol == null) {
            mostrarAlerta("Selecciona un rol.");
            return;
        }

        switch (rol) {
            case "Jefe de Maestros":
                abrirVista("/application/proyecto/views/JefeDeMaestros.fxml", "Panel Jefe de Maestros", event);
                break;

            case "Maestro":
                abrirVista("/application/proyecto/views/Maestro.fxml", "Panel Maestro", event);
                break;

            case "Tutor":
                abrirVista("/application/proyecto/views/Tutor.fxml", "Panel Tutor", event);
                break;

            default:
                mostrarAlerta("Rol no valido.");
                break;
        }
    }

    private void abrirVista(String ruta, String titulo, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle(titulo);

            stage.setMaximized(true);
            stage.setFullScreen(false);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("No se pudo abrir la vista: " + ruta);
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}