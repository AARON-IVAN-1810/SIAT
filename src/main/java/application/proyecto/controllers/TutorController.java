package application.proyecto.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class TutorController {

    @FXML
    private StackPane contentArea;

    @FXML
    private void ClickInicio() {
        cargarVista("/application/proyecto/views/tutor/InicioT.fxml");
    }

    @FXML
    private void ClickGestionAlertas() {
        cargarVista("/application/proyecto/views/tutor/GestionDeAlertasT.fxml");
    }
    @FXML
    private void ClickOrientacion() {
        cargarVista("/application/proyecto/views/tutor/OrientacionT.fxml");
    }
    @FXML
    private void ClickReporte() {
        cargarVista("/application/proyecto/views/tutor/HistorialAlumnoT.fxml");
    }
    @FXML
    private void selectPerfil() {
        cargarVista("/application/proyecto/views/tutor/PerfilUsuarioT.fxml");
    }
    @FXML
    private void ClickGestionReportes() {cargarVista("/application/proyecto/views/tutor/GestionDeReportesT.fxml");}



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
