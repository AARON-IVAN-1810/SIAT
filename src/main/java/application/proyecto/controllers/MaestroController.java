package application.proyecto.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.io.IOException;


public class MaestroController {

    @FXML
    private StackPane contentArea;

    @FXML
    private void clickInicio() {cargarVista("/application/proyecto/views/maestro/InicioM.fxml");}
    @FXML
    private void clickAsistencia() {
        cargarVista("/application/proyecto/views/maestro/AsistenciaM.fxml");
    }
    @FXML
    private void clickCalificacion() {
        cargarVista("/application/proyecto/views/maestro/CalificacionesM.fxml");
    }
    @FXML
    private void clickConducta() {
        cargarVista("/application/proyecto/views/maestro/AlertasM.fxml");
    }
    @FXML
    private void clickReportes() {
        cargarVista("/application/proyecto/views/maestro/ReportesM.fxml");
    }
    @FXML
    private void selectPerfil() {
        cargarVista("/application/proyecto/views/maestro/PerfilUsuarioM.fxml");
    }
    @FXML
    private void clickHistorial() {
        cargarVista("/application/proyecto/views/maestro/HistorialAlumnoM.fxml");
    }

    @FXML
    private void selectLinks() {
        cargarVista("/application/proyecto/views/maestro/LinksInscripcionM.fxml");
    }



    @FXML
    public void initialize() {
        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getRoot().getProperties().put("controller", this);
            }
        });
    }


    public void cargarVista(String ruta) {
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