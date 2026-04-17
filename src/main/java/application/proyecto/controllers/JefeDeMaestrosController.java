package application.proyecto.controllers;

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
}