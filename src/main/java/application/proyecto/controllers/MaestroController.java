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

public class MaestroController {

    @FXML private StackPane contentArea;

    @FXML private ToggleButton btnInicio;
    @FXML private ToggleButton btnlinks;
    @FXML private ToggleButton btnAsistencia;
    @FXML private ToggleButton btnCalificacion;
    @FXML private ToggleButton btnConducta;
    @FXML private ToggleButton btnReportes;
    @FXML private ToggleButton btnHistorial;
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
                btnlinks,
                btnAsistencia,
                btnCalificacion,
                btnConducta,
                btnReportes,
                btnHistorial,
                btnperfil
        );

        configurarBotonesMenu();

        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getRoot().getProperties().put("controller", this);
            }
        });
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
    private void clickInicio() {
        marcarBotonActivo(btnInicio);
        cargarVista("/application/proyecto/views/maestro/InicioM.fxml");
    }

    @FXML
    private void selectLinks() {
        marcarBotonActivo(btnlinks);
        cargarVista("/application/proyecto/views/maestro/LinksInscripcionM.fxml");
    }

    @FXML
    private void clickAsistencia() {
        marcarBotonActivo(btnAsistencia);
        cargarVista("/application/proyecto/views/maestro/AsistenciaM.fxml");
    }

    @FXML
    private void clickCalificacion() {
        marcarBotonActivo(btnCalificacion);
        cargarVista("/application/proyecto/views/maestro/CalificacionesM.fxml");
    }

    @FXML
    private void clickConducta() {
        marcarBotonActivo(btnConducta);
        cargarVista("/application/proyecto/views/maestro/AlertasM.fxml");
    }

    @FXML
    private void clickReportes() {
        marcarBotonActivo(btnReportes);
        cargarVista("/application/proyecto/views/maestro/ReportesM.fxml");
    }

    @FXML
    private void clickHistorial() {
        marcarBotonActivo(btnHistorial);
        cargarVista("/application/proyecto/views/maestro/HistorialAlumnoM.fxml");
    }

    @FXML
    private void selectPerfil() {
        marcarBotonActivo(btnperfil);
        cargarVista("/application/proyecto/views/maestro/PerfilUsuarioM.fxml");
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