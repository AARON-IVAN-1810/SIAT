package application.proyecto.controllers;

import application.proyecto.utils.ConexionBD;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RecuperarContrasenaController {

    @FXML private TextField txtUsuarioRecuperacion;
    @FXML private ComboBox<String> cbPregunta1Recuperacion;
    @FXML private ComboBox<String> cbPregunta2Recuperacion;
    @FXML private TextField txtRespuesta1Recuperacion;
    @FXML private TextField txtRespuesta2Recuperacion;
    @FXML private PasswordField txtNuevaContrasenaRecuperacion;
    @FXML private PasswordField txtConfirmarContrasenaRecuperacion;
    @FXML private Button btnBuscarPreguntas;
    @FXML private Button btnActualizarContrasena;
    @FXML private Button btnCancelar;

    private int idUsuarioRecuperacion = 0;

    @FXML
    public void initialize() {
        cbPregunta1Recuperacion.setDisable(true);
        cbPregunta2Recuperacion.setDisable(true);
        btnActualizarContrasena.setDisable(true);
    }

    @FXML
    private void handleBuscarPreguntas() {
        String usuario = txtUsuarioRecuperacion.getText() == null ? "" : txtUsuarioRecuperacion.getText().trim();

        if (usuario.isEmpty()) {
            mostrarAlerta("captura tu usuario");
            return;
        }

        String sql = """
                select
                id_usuario,
                pregunta_1,
                pregunta_2
                from usuario
                where usuario=?
                and id_estatus_general=1
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idUsuarioRecuperacion = rs.getInt("id_usuario");

                    String pregunta1 = rs.getString("pregunta_1");
                    String pregunta2 = rs.getString("pregunta_2");

                    if (pregunta1 == null || pregunta2 == null || pregunta1.isBlank() || pregunta2.isBlank()) {
                        mostrarAlerta("este usuario no tiene preguntas de recuperacion configuradas");
                        return;
                    }

                    cbPregunta1Recuperacion.setItems(FXCollections.observableArrayList(pregunta1));
                    cbPregunta2Recuperacion.setItems(FXCollections.observableArrayList(pregunta2));
                    cbPregunta1Recuperacion.setValue(pregunta1);
                    cbPregunta2Recuperacion.setValue(pregunta2);

                    cbPregunta1Recuperacion.setDisable(false);
                    cbPregunta2Recuperacion.setDisable(false);
                    btnActualizarContrasena.setDisable(false);

                    mostrarAlerta("preguntas cargadas correctamente");
                } else {
                    idUsuarioRecuperacion = 0;
                    mostrarAlerta("no se encontro el usuario");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("error al buscar preguntas");
        }
    }

    @FXML
    private void handleRecuperarContrasena() {
        String respuesta1 = txtRespuesta1Recuperacion.getText() == null ? "" : txtRespuesta1Recuperacion.getText().trim().toLowerCase();
        String respuesta2 = txtRespuesta2Recuperacion.getText() == null ? "" : txtRespuesta2Recuperacion.getText().trim().toLowerCase();
        String nueva = txtNuevaContrasenaRecuperacion.getText() == null ? "" : txtNuevaContrasenaRecuperacion.getText().trim();
        String confirmar = txtConfirmarContrasenaRecuperacion.getText() == null ? "" : txtConfirmarContrasenaRecuperacion.getText().trim();

        if (idUsuarioRecuperacion == 0) {
            mostrarAlerta("primero busca tus preguntas");
            return;
        }

        if (respuesta1.isEmpty() || respuesta2.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            mostrarAlerta("captura respuestas y nueva contrasena");
            return;
        }

        if (!nueva.equals(confirmar)) {
            mostrarAlerta("la nueva contrasena y la confirmacion no coinciden");
            return;
        }

        if (nueva.length() > 8) {
            mostrarAlerta("la contrasena no puede tener mas de 8 caracteres");
            return;
        }

        String sqlValidar = """
                select id_usuario
                from usuario
                where id_usuario=?
                and lower(respuesta_1)=?
                and lower(respuesta_2)=?
                limit 1
                """;

        String sqlActualizar = """
                update usuario
                set password_hash=?
                where id_usuario=?
                """;

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            boolean respuestasCorrectas = false;

            try (PreparedStatement ps = con.prepareStatement(sqlValidar)) {
                ps.setInt(1, idUsuarioRecuperacion);
                ps.setString(2, respuesta1);
                ps.setString(3, respuesta2);

                try (ResultSet rs = ps.executeQuery()) {
                    respuestasCorrectas = rs.next();
                }
            }

            if (!respuestasCorrectas) {
                con.rollback();
                mostrarAlerta("las respuestas no coinciden");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sqlActualizar)) {
                ps.setString(1, nueva);
                ps.setInt(2, idUsuarioRecuperacion);
                ps.executeUpdate();
            }

            con.commit();

            mostrarAlerta("contrasena actualizada correctamente");
            cerrarVentana();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("error al recuperar contrasena");
        }
    }

    @FXML
    private void handleCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}