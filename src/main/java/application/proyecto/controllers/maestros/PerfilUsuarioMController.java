package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PerfilUsuarioMController extends BaseController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidoPaterno;
    @FXML private TextField txtApellidoMaterno;
    @FXML private TextField txtNumEmpleado;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;

    @FXML private PasswordField txtActual;
    @FXML private PasswordField txtNueva;
    @FXML private PasswordField txtConfirmar;

    @FXML private ComboBox<String> cbPregunta1;
    @FXML private ComboBox<String> cbPregunta2;
    @FXML private TextField txtRespuesta1;
    @FXML private TextField txtRespuesta2;

    private int idMaestroActual = 0;
    private int idUsuarioActual = 0;
    private String passwordActual = "";

    @FXML
    public void initialize() {
        configurarPreguntas();
        cargarPerfil();
    }

    private void configurarPreguntas() {
        cbPregunta1.setItems(FXCollections.observableArrayList(
                "color favorito",
                "pelicula favorita",
                "primera mascota",
                "comida favorita",
                "cual es tu pelicula favorita",
                "cual es tu color favorito",
                "cual fue tu primera mascota",
                "cual es tu comida favorita"
        ));

        cbPregunta2.setItems(FXCollections.observableArrayList(
                "color favorito",
                "pelicula favorita",
                "primera mascota",
                "comida favorita",
                "cual es tu pelicula favorita",
                "cual es tu color favorito",
                "cual fue tu primera mascota",
                "cual es tu comida favorita"
        ));

        txtNumEmpleado.setEditable(false);
    }

    private void cargarPerfil() {
        idUsuarioActual = SesionUsuario.getIdUsuario();
        idMaestroActual = SesionUsuario.getIdMaestro();

        if (idUsuarioActual == 0 || idMaestroActual == 0) {
            mostrarError("no hay usuario en sesion");
            return;
        }

        String sql = """
                select
                m.nombre,
                m.apellido_paterno,
                m.apellido_materno,
                m.num_empleado,
                m.correo,
                m.telefono,
                u.password_hash,
                u.pregunta_1,
                u.respuesta_1,
                u.pregunta_2,
                u.respuesta_2
                from maestro m
                inner join usuario u on m.id_usuario=u.id_usuario
                where m.id_maestro=?
                and u.id_usuario=?
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestroActual);
            ps.setInt(2, idUsuarioActual);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtNombre.setText(texto(rs.getString("nombre")));
                    txtApellidoPaterno.setText(texto(rs.getString("apellido_paterno")));
                    txtApellidoMaterno.setText(texto(rs.getString("apellido_materno")));
                    txtNumEmpleado.setText(texto(rs.getString("num_empleado")));
                    txtCorreo.setText(texto(rs.getString("correo")));
                    txtTelefono.setText(texto(rs.getString("telefono")));

                    passwordActual = texto(rs.getString("password_hash"));

                    String pregunta1 = texto(rs.getString("pregunta_1"));
                    String pregunta2 = texto(rs.getString("pregunta_2"));

                    cbPregunta1.setValue(pregunta1.isEmpty() ? null : pregunta1);
                    cbPregunta2.setValue(pregunta2.isEmpty() ? null : pregunta2);

                    txtRespuesta1.setText(texto(rs.getString("respuesta_1")));
                    txtRespuesta2.setText(texto(rs.getString("respuesta_2")));
                } else {
                    mostrarError("no se encontro informacion del usuario actual");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar perfil");
        }
    }

    @FXML
    private void handleGuardarDatos() {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String apellidoPaterno = txtApellidoPaterno.getText() == null ? "" : txtApellidoPaterno.getText().trim();
        String apellidoMaterno = txtApellidoMaterno.getText() == null ? "" : txtApellidoMaterno.getText().trim();
        String correo = txtCorreo.getText() == null ? "" : txtCorreo.getText().trim().toLowerCase();
        String telefono = txtTelefono.getText() == null ? "" : txtTelefono.getText().trim();

        if (idMaestroActual == 0) {
            mostrarError("no hay maestro cargado");
            return;
        }

        if (nombre.isEmpty() || apellidoPaterno.isEmpty() || apellidoMaterno.isEmpty()) {
            mostrarError("captura nombre y apellidos");
            return;
        }

        if (!correo.isEmpty() && !correo.contains("@")) {
            mostrarError("captura un correo valido");
            return;
        }

        String sql = """
                update maestro
                set nombre=?,
                apellido_paterno=?,
                apellido_materno=?,
                correo=?,
                telefono=?
                where id_maestro=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, apellidoPaterno);
            ps.setString(3, apellidoMaterno);
            ps.setString(4, correo);
            ps.setString(5, telefono);
            ps.setInt(6, idMaestroActual);

            ps.executeUpdate();

            mostrarInfo("datos personales actualizados correctamente");
            cargarPerfil();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar datos personales");
        }
    }

    @FXML
    private void handleCambiarContrasena() {
        String actual = txtActual.getText() == null ? "" : txtActual.getText().trim();
        String nueva = txtNueva.getText() == null ? "" : txtNueva.getText().trim();
        String confirmar = txtConfirmar.getText() == null ? "" : txtConfirmar.getText().trim();

        if (idUsuarioActual == 0) {
            mostrarError("no hay usuario cargado");
            return;
        }

        if (!passwordActual.isEmpty() && !actual.equals(passwordActual)) {
            mostrarError("la contrasena actual no coincide");
            return;
        }

        if (nueva.isEmpty() || confirmar.isEmpty()) {
            mostrarError("captura la nueva contrasena y su confirmacion");
            return;
        }

        if (!nueva.equals(confirmar)) {
            mostrarError("la nueva contrasena y la confirmacion no coinciden");
            return;
        }

        if (nueva.length() > 8) {
            mostrarError("la contrasena no puede tener mas de 8 caracteres");
            return;
        }

        String sql = """
                update usuario
                set password_hash=?
                where id_usuario=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nueva);
            ps.setInt(2, idUsuarioActual);

            ps.executeUpdate();

            passwordActual = nueva;

            txtActual.clear();
            txtNueva.clear();
            txtConfirmar.clear();

            mostrarInfo("contrasena actualizada correctamente");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cambiar contrasena");
        }
    }

    @FXML
    private void handleGuardarPreguntas() {
        String pregunta1 = cbPregunta1.getValue();
        String pregunta2 = cbPregunta2.getValue();
        String respuesta1 = txtRespuesta1.getText() == null ? "" : txtRespuesta1.getText().trim().toLowerCase();
        String respuesta2 = txtRespuesta2.getText() == null ? "" : txtRespuesta2.getText().trim().toLowerCase();

        if (idUsuarioActual == 0) {
            mostrarError("no hay usuario cargado");
            return;
        }

        if (pregunta1 == null || pregunta2 == null || respuesta1.isEmpty() || respuesta2.isEmpty()) {
            mostrarError("selecciona dos preguntas y captura sus respuestas");
            return;
        }

        if (pregunta1.equals(pregunta2)) {
            mostrarError("las preguntas deben ser diferentes");
            return;
        }

        String sql = """
                update usuario
                set pregunta_1=?,
                respuesta_1=?,
                pregunta_2=?,
                respuesta_2=?
                where id_usuario=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, pregunta1);
            ps.setString(2, respuesta1);
            ps.setString(3, pregunta2);
            ps.setString(4, respuesta2);
            ps.setInt(5, idUsuarioActual);

            ps.executeUpdate();

            mostrarInfo("preguntas de recuperacion guardadas correctamente");
            cargarPerfil();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar preguntas");
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("informacion");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}