package application.proyecto.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class JefeDeMaestrosController {

    @FXML private StackPane contentArea;

    @FXML private ToggleButton btninicio;
    @FXML private ToggleButton btncatmaterias;
    @FXML private ToggleButton btnmaterias;
    @FXML private ToggleButton btnlinks;
    @FXML private ToggleButton btngrupos;
    @FXML private ToggleButton btnmaestros;
    @FXML private ToggleButton btnalumnos;
    @FXML private ToggleButton btnHistorialAlumnos;
    @FXML private ToggleButton btnestadisticas;
    @FXML private ToggleButton btnusuarios;
    @FXML private ToggleButton btnperfil;
    @FXML private ToggleButton btncargamaterias;

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
                btninicio,
                btncatmaterias,
                btnmaterias,
                btnlinks,
                btngrupos,
                btnmaestros,
                btnalumnos,
                btnHistorialAlumnos,
                btnestadisticas,
                btnusuarios,
                btncargamaterias,
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
    private void SelectInicio() {
        marcarBotonActivo(btninicio);
        cargarVista("/application/proyecto/views/jefedemaestros/InicioJDM.fxml");
    }

    @FXML
    private void selectCatalogoMateriasmaterias() {
        marcarBotonActivo(btncatmaterias);
        cargarVista("/application/proyecto/views/jefedemaestros/CatalogoMateriasJDM.fxml");
    }

    @FXML
    private void selectCargaMateriasmaterias() {
        marcarBotonActivo(btncargamaterias);
        cargarVista("/application/proyecto/views/jefedemaestros/ImportacionClasesJDM.fxml");
    }

    @FXML
    private void selectmaterias() {
        marcarBotonActivo(btnmaterias);
        cargarVista("/application/proyecto/views/jefedemaestros/MateriasJDM.fxml");
    }

    @FXML
    private void selectlinks() {
        marcarBotonActivo(btnlinks);
        cargarVista("/application/proyecto/views/jefedemaestros/LinksInscripcionJDM.fxml");
    }

    @FXML
    private void selectgrupos() {
        marcarBotonActivo(btngrupos);
        cargarVista("/application/proyecto/views/jefedemaestros/GruposJDM.fxml");
    }

    @FXML
    private void selectmaestros() {
        marcarBotonActivo(btnmaestros);
        cargarVista("/application/proyecto/views/jefedemaestros/MaestrosJDM.fxml");
    }

    @FXML
    private void selectalumnos() {
        marcarBotonActivo(btnalumnos);
        cargarVista("/application/proyecto/views/jefedemaestros/AlumnosJDM.fxml");
    }

    @FXML
    private void selectHistorialAlumnos() {
        marcarBotonActivo(btnHistorialAlumnos);
        cargarVista("/application/proyecto/views/jefedemaestros/HistorialAlumnoJDM.fxml");
    }

    @FXML
    private void selectestadisticas() {
        marcarBotonActivo(btnestadisticas);
        cargarVista("/application/proyecto/views/jefedemaestros/EstadisticasJDM.fxml");
    }

    @FXML
    private void selectusuarios() {
        marcarBotonActivo(btnusuarios);
        cargarVista("/application/proyecto/views/jefedemaestros/UsuariosYRolesJDM.fxml");
    }

    @FXML
    private void selectPerfil() {
        marcarBotonActivo(btnperfil);
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
    private void handleLogout(javafx.scene.input.MouseEvent event) {
        try {
            System.out.println("Cerrando sesion desde el encabezado...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setMaximized(false);
            stage.show();

        } catch (IOException e) {
            System.err.println("No se encontro Login.fxml. Revisa la carpeta views.");
            e.printStackTrace();
        }
    }
}