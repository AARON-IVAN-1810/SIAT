package application.proyecto.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TutorController {

    @FXML private StackPane contentArea;

    @FXML private ToggleButton btnInicio;
    @FXML private ToggleButton btnGestionAlertas;
    @FXML private ToggleButton btnGestionReportes;
    @FXML private ToggleButton btnOrientacion;
    @FXML private ToggleButton btnbitacora;
    @FXML private ToggleButton btnReporte;
    @FXML private ToggleButton btnperfil;

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
    public void initialize() {
        botonesMenu = Arrays.asList(
                btnInicio,
                btnGestionAlertas,
                btnGestionReportes,
                btnOrientacion,
                btnbitacora,
                btnReporte,
                btnperfil
        );

        configurarBotonesMenu();
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

    private void marcarBotonActivo(ToggleButton botonActivo) {
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

        if (botonActivo != null) {
            botonActivo.setSelected(true);
            botonActivo.setStyle(ESTILO_ACTIVO);

            if (!botonActivo.getStyleClass().contains("menu-button-active")) {
                botonActivo.getStyleClass().add("menu-button-active");
            }
        }
    }

    @FXML
    private void ClickInicio() {
        marcarBotonActivo(btnInicio);
        cargarVista("/application/proyecto/views/tutor/InicioT.fxml");
    }

    @FXML
    private void ClickGestionAlertas() {
        marcarBotonActivo(btnGestionAlertas);
        cargarVista("/application/proyecto/views/tutor/GestionDeAlertasT.fxml");
    }

    @FXML
    private void ClickGestionReportes() {
        marcarBotonActivo(btnGestionReportes);
        cargarVista("/application/proyecto/views/tutor/GestionDeReportesT.fxml");
    }

    @FXML
    private void ClickOrientacion() {
        marcarBotonActivo(btnOrientacion);
        cargarVista("/application/proyecto/views/tutor/OrientacionT.fxml");
    }

    @FXML
    private void ClickBitacora() {
        marcarBotonActivo(btnbitacora);
        cargarVista("/application/proyecto/views/tutor/BitacoraT.fxml");
    }

    @FXML
    private void ClickReporte() {
        marcarBotonActivo(btnReporte);
        cargarVista("/application/proyecto/views/tutor/HistorialAlumnoT.fxml");
    }

    @FXML
    private void selectPerfil() {
        marcarBotonActivo(btnperfil);
        cargarVista("/application/proyecto/views/tutor/PerfilUsuarioT.fxml");
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