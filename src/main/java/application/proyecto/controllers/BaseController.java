package application.proyecto.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class BaseController {

    @FXML
    protected void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/application/proyecto/views/login.fxml")
            );

            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setMaximized(false);
            stage.setFullScreen(false);

            Scene scene = new Scene(root, 927, 713);
            stage.setScene(scene);
            stage.setTitle("LOGIN");

            stage.setWidth(927);
            stage.setHeight(713);
            stage.centerOnScreen();
            stage.show();

            System.out.println("sesion cerrada correctamente");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}