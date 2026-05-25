package application.proyecto.controllers;

import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private ComboBox<String> cbRol;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private TextField txtContrasenaVisible;
    @FXML private Button btnMostrarContrasena;

    private boolean contrasenaVisible = false;

    @FXML
    public void initialize() {
        cbRol.setItems(FXCollections.observableArrayList(
                "Jefe de Maestros",
                "Maestro",
                "Tutor"
        ));

        configurarCampoContrasena();
    }

    private void configurarCampoContrasena() {
        if (txtContrasenaVisible == null || txtContrasena == null) {
            return;
        }

        txtContrasenaVisible.textProperty().bindBidirectional(txtContrasena.textProperty());

        txtContrasenaVisible.setVisible(false);
        txtContrasenaVisible.setManaged(false);

        txtContrasena.setVisible(true);
        txtContrasena.setManaged(true);

        if (btnMostrarContrasena != null) {
            btnMostrarContrasena.setText("\uD83D\uDD12");
        }
    }

    @FXML
    private void handleMostrarContrasena() {
        contrasenaVisible = !contrasenaVisible;

        txtContrasenaVisible.setVisible(contrasenaVisible);
        txtContrasenaVisible.setManaged(contrasenaVisible);

        txtContrasena.setVisible(!contrasenaVisible);
        txtContrasena.setManaged(!contrasenaVisible);

        if (contrasenaVisible) {
            btnMostrarContrasena.setText("\uD83D\uDD13");
            txtContrasenaVisible.requestFocus();
            txtContrasenaVisible.positionCaret(txtContrasenaVisible.getText().length());
        } else {
            btnMostrarContrasena.setText("\uD83D\uDD12");
            txtContrasena.requestFocus();
            txtContrasena.positionCaret(txtContrasena.getText().length());
        }
    }

    @FXML
    private void clikIngresar(ActionEvent event) {
        String usuario = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText() == null ? "" : txtContrasena.getText().trim();
        String rolSeleccionado = cbRol.getValue();

        if (usuario.isEmpty() || contrasena.isEmpty() || rolSeleccionado == null) {
            mostrarAlerta("captura usuario, contrasena y rol");
            return;
        }

        String rolBD = convertirRolParaBD(rolSeleccionado);

        String sql = """
                select
                u.id_usuario,
                u.usuario,
                m.id_maestro,
                concat(m.nombre,' ',m.apellido_paterno,' ',m.apellido_materno) as maestro,
                r.nombre as rol
                from usuario u
                inner join maestro m on u.id_usuario=m.id_usuario
                inner join usuario_rol ur on u.id_usuario=ur.id_usuario
                inner join rol r on ur.id_rol=r.id_rol
                where u.usuario=?
                and u.password_hash=?
                and u.id_estatus_general=1
                and m.id_estatus_general=1
                and r.nombre=?
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario);
            ps.setString(2, contrasena);
            ps.setString(3, rolBD);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SesionUsuario.iniciarSesion(
                            rs.getInt("id_usuario"),
                            rs.getInt("id_maestro"),
                            rs.getString("usuario"),
                            rs.getString("maestro"),
                            rs.getString("rol")
                    );

                    abrirVistaPorRol(rolSeleccionado, event);
                } else {
                    mostrarAlerta("usuario, contrasena o rol incorrecto");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("error al iniciar sesion");
        }
    }

    private String convertirRolParaBD(String rolVista) {
        return switch (rolVista) {
            case "Jefe de Maestros" -> "jefe de maestros";
            case "Maestro" -> "maestro";
            case "Tutor" -> "tutor";
            default -> "";
        };
    }

    private void abrirVistaPorRol(String rol, ActionEvent event) {
        switch (rol) {
            case "Jefe de Maestros":
                abrirVista("/application/proyecto/views/JefeDeMaestros.fxml", "Panel Jefe de Maestros", event);
                break;

            case "Maestro":
                abrirVista("/application/proyecto/views/Maestro.fxml", "Panel Maestro", event);
                break;

            case "Tutor":
                abrirVista("/application/proyecto/views/Tutor.fxml", "Panel Tutor", event);
                break;

            default:
                mostrarAlerta("rol no valido");
                break;
        }
    }

    private void abrirVista(String ruta, String titulo, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(titulo);
            stage.setFullScreen(false);
            stage.show();
            stage.setMaximized(true);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("no se pudo abrir la vista");
        }
    }

    @FXML
    private void handleMostrarRecuperacion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/RecuperarContrasena.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Recuperar contrasena");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("no se pudo abrir la ventana de recuperacion");
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}