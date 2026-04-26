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

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.setMaximized(false); 
            stage.setWidth(500);
            stage.setHeight(450);
            stage.centerOnScreen();
            
            stage.show();
            System.out.println("Sesión cerrada exitosamente.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}