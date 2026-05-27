package application.proyecto.controllers.jefedemaestros;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class GruposSinTutorJDMController {

    @FXML private StackPane contentArea;

    @FXML private ToggleButton btnGestionAlertas;
    @FXML private ToggleButton btnGestionReportes;
    @FXML private ToggleButton btnOrientacion;
    @FXML private ToggleButton btnRegresar;

    private List<ToggleButton> botonesMenu;

    private static final String ESTILO_NORMAL = """
            -fx-background-color: transparent;
            -fx-background-radius: 6;
            -fx-border-color: transparent;
            -fx-border-radius: 6;
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-font-weight: normal;
            -fx-cursor: hand;
            """;

    private static final String ESTILO_HOVER = """
            -fx-background-color: #34476f;
            -fx-background-radius: 6;
            -fx-border-color: transparent;
            -fx-border-radius: 6;
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-font-weight: normal;
            -fx-cursor: hand;
            """;

    private static final String ESTILO_ACTIVO = """
            -fx-background-color: #4568ad;
            -fx-background-radius: 6;
            -fx-border-color: transparent;
            -fx-border-radius: 6;
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-font-weight: normal;
            -fx-cursor: hand;
            """;

    @FXML
    private void initialize() {
        botonesMenu = Arrays.asList(
                btnGestionAlertas,
                btnGestionReportes,
                btnOrientacion,
                btnRegresar
        );

        configurarBotonesMenu();
        mostrarInicio();
    }

    private void configurarBotonesMenu() {
        for (ToggleButton boton : botonesMenu) {
            if (boton == null) {
                continue;
            }

            boton.setFocusTraversable(false);
            boton.setCursor(Cursor.HAND);
            boton.setStyle(ESTILO_NORMAL);

            if (!boton.getStyleClass().contains("menu-button")) {
                boton.getStyleClass().add("menu-button");
            }

            boton.setOnMouseEntered(event -> {
                if (!boton.isSelected()) {
                    boton.setStyle(ESTILO_HOVER);
                }
            });

            boton.setOnMouseExited(event -> {
                if (boton.isSelected()) {
                    boton.setStyle(ESTILO_ACTIVO);
                } else {
                    boton.setStyle(ESTILO_NORMAL);
                }
            });
        }
    }

    private void mostrarInicio() {
        seleccionarBoton(null);
    }

    @FXML
    private void ClickGestionAlertas() {
        seleccionarBoton(btnGestionAlertas);
        cargarVista("/application/proyecto/views/jefedemaestros/GestionDeAlertasSinTutorJDM.fxml");
    }

    @FXML
    private void ClickGestionReportes() {
        seleccionarBoton(btnGestionReportes);
        cargarVista("/application/proyecto/views/jefedemaestros/GestionDeReportesSinTutorJDM.fxml");
    }

    @FXML
    private void ClickOrientacion() {
        seleccionarBoton(btnOrientacion);
        cargarVista("/application/proyecto/views/jefedemaestros/OrientacionSinTutorJDM.fxml");
    }

    @FXML
    private void ClickRegresar(ActionEvent event) {
        seleccionarBoton(btnRegresar);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/JefeDeMaestros.fxml"));
            Parent root = loader.load();

            StackPane contentAreaJefe = (StackPane) root.lookup("#contentArea");

            if (contentAreaJefe != null) {
                Parent inicio = FXMLLoader.load(getClass().getResource("/application/proyecto/views/jefedemaestros/InicioJDM.fxml"));
                contentAreaJefe.getChildren().setAll(inicio);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al regresar al panel de jefe de maestros");
        }
    }

    private void cargarVista(String ruta) {
        try {
            Parent vista = FXMLLoader.load(getClass().getResource(ruta));
            contentArea.getChildren().setAll(vista);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar la vista");
        }
    }

    private void seleccionarBoton(ToggleButton botonSeleccionado) {
        for (ToggleButton boton : botonesMenu) {
            if (boton == null) {
                continue;
            }

            boton.setSelected(false);
            boton.getStyleClass().remove("menu-button-active");
            boton.setStyle(ESTILO_NORMAL);

            if (!boton.getStyleClass().contains("menu-button")) {
                boton.getStyleClass().add("menu-button");
            }
        }

        if (botonSeleccionado != null) {
            botonSeleccionado.setSelected(true);
            botonSeleccionado.setStyle(ESTILO_ACTIVO);

            if (!botonSeleccionado.getStyleClass().contains("menu-button-active")) {
                botonSeleccionado.getStyleClass().add("menu-button-active");
            }
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}