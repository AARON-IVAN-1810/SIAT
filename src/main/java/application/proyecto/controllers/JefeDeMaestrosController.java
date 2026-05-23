package application.proyecto.controllers;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class JefeDeMaestrosController {

    @FXML
    private StackPane contentArea;


    @FXML
    private void SelectInicio() {
        cargarVista("/application/proyecto/views/jefedemaestros/InicioJDM.fxml");
    }

    @FXML
    private void selectmaterias() {
        cargarVista("/application/proyecto/views/jefedemaestros/MateriasJDM.fxml");
    }

    @FXML
    private void selectgrupos() {
        cargarVista("/application/proyecto/views/jefedemaestros/GruposJDM.fxml");
    }

    @FXML
    private void selectmaestros() {
        cargarVista("/application/proyecto/views/jefedemaestros/MaestrosJDM.fxml");
    }

    @FXML
    private void selectalumnos() {
        cargarVista("/application/proyecto/views/jefedemaestros/AlumnosJDM.fxml");
    }

    @FXML
    private void selectHistorialAlumnos() {
        cargarVista("/application/proyecto/views/jefedemaestros/HistorialAlumnoJDM.fxml");
    }

    @FXML
    private void selectestadisticas() {
        cargarVista("/application/proyecto/views/jefedemaestros/EstadisticasJDM.fxml");
    }

    @FXML
    private void selectusuarios() {
        cargarVista("/application/proyecto/views/jefedemaestros/UsuariosYRolesJDM.fxml");
    }

    public void selectPerfil() {
        cargarVista("/application/proyecto/views/jefedemaestros/PerfilUsuarioJDM.fxml");
    }

    private void cargarVista(String ruta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Node vista = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(vista);

        } catch (IOException e) {
            System.out.println("No se pudo cargar la vista: " + ruta);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(javafx.scene.input.MouseEvent event) { // Cambiado a MouseEvent
        try {
            System.out.println("Cerrando sesión desde el encabezado...");

            // 1. Cargamos el Login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/Login.fxml"));
            Parent root = loader.load();

            // 2. Obtenemos la ventana (Stage) desde el texto que clickeamos
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Cambiamos la escena
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setMaximized(false); // El login suele ser pequeño
            stage.show();

        } catch (IOException e) {
            System.err.println("No se encontró Login.fxml. Revisa la carpeta 'views'.");
            e.printStackTrace();
        }
    }
}