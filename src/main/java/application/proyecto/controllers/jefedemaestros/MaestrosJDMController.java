package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class MaestrosJDMController extends BaseController {

    @FXML private TextField txtNombreMaestro;
    @FXML private TextField txtApellidoPaternoMaestro;
    @FXML private TextField txtApellidoMaternoMaestro;
    @FXML private TextField txtNumeroEmpleado;
    @FXML private TextField txtCorreoInstitucional;
    @FXML private TextField txtTelefonoInstitucional;

    @FXML private TextField txtBuscarMaestroInterno;
    @FXML private ComboBox<String> cbFiltroMaestros;

    @FXML private TableView<MaestroJDM> tablaMaestros;
    @FXML private TableColumn<MaestroJDM, String> colNombreMaestro;
    @FXML private TableColumn<MaestroJDM, String> colNumeroEmpleadoMaestro;
    @FXML private TableColumn<MaestroJDM, String> colCorreoMaestro;
    @FXML private TableColumn<MaestroJDM, String> colTelefonoMaestro;
    @FXML private TableColumn<MaestroJDM, String> colEstatusMaestro;

    private final ObservableList<MaestroJDM> listaMaestros = FXCollections.observableArrayList();

    private MaestroJDM maestroSeleccionado;
    private boolean modoEdicion = false;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        cargarMaestros();
        configurarBusqueda();
    }

    private void configurarTabla() {
        colNombreMaestro.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colNumeroEmpleadoMaestro.setCellValueFactory(new PropertyValueFactory<>("numeroEmpleado"));
        colCorreoMaestro.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTelefonoMaestro.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEstatusMaestro.setCellValueFactory(new PropertyValueFactory<>("estatus"));
    }

    private void configurarCombos() {
        cbFiltroMaestros.setItems(FXCollections.observableArrayList("todos", "activo", "inactivo"));
        cbFiltroMaestros.setValue("todos");
    }

    private void configurarEventos() {
        tablaMaestros.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            maestroSeleccionado = newValue;
        });
    }

    private void cargarMaestros() {
        listaMaestros.clear();

        String sql = """
                select
                m.id_maestro,
                m.id_usuario,
                m.nombre,
                m.apellido_paterno,
                m.apellido_materno,
                m.num_empleado,
                m.correo,
                m.telefono,
                ceg.nombre as estatus
                from maestro m
                inner join cat_estatus_general ceg on m.id_estatus_general=ceg.id_estatus_general
                order by m.apellido_paterno,m.apellido_materno,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaMaestros.add(new MaestroJDM(
                        rs.getInt("id_maestro"),
                        rs.getInt("id_usuario"),
                        rs.getString("nombre"),
                        rs.getString("apellido_paterno"),
                        rs.getString("apellido_materno"),
                        rs.getString("num_empleado"),
                        rs.getString("correo"),
                        rs.getString("telefono"),
                        rs.getString("estatus")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar maestros");
        }
    }

    @FXML
    private void handleGuardarMaestro() {
        String nombre = txtNombreMaestro.getText() == null ? "" : txtNombreMaestro.getText().trim();
        String apellidoPaterno = txtApellidoPaternoMaestro.getText() == null ? "" : txtApellidoPaternoMaestro.getText().trim();
        String apellidoMaterno = txtApellidoMaternoMaestro.getText() == null ? "" : txtApellidoMaternoMaestro.getText().trim();
        String numeroEmpleado = txtNumeroEmpleado.getText() == null ? "" : txtNumeroEmpleado.getText().trim();
        String correo = txtCorreoInstitucional.getText() == null ? "" : txtCorreoInstitucional.getText().trim().toLowerCase();
        String telefono = txtTelefonoInstitucional.getText() == null ? "" : txtTelefonoInstitucional.getText().trim();

        if (nombre.isEmpty() || apellidoPaterno.isEmpty() || apellidoMaterno.isEmpty() || numeroEmpleado.isEmpty()) {
            mostrarError("captura nombre, apellidos y numero de empleado");
            return;
        }

        if (modoEdicion && maestroSeleccionado != null) {
            actualizarMaestro(nombre, apellidoPaterno, apellidoMaterno, numeroEmpleado, correo, telefono);
        } else {
            insertarMaestro(nombre, apellidoPaterno, apellidoMaterno, numeroEmpleado, correo, telefono);
        }
    }

    private void insertarMaestro(String nombre, String apellidoPaterno, String apellidoMaterno, String numeroEmpleado, String correo, String telefono) {
        String sqlUsuario = """
                insert into usuario(
                usuario,
                password_hash,
                pregunta_1,
                respuesta_1,
                pregunta_2,
                respuesta_2,
                id_estatus_general
                )
                values(?,'000','color favorito','azul','pelicula favorita','avatar',1)
                """;

        String sqlMaestro = """
                insert into maestro(
                id_usuario,
                num_empleado,
                nombre,
                apellido_paterno,
                apellido_materno,
                correo,
                telefono,
                id_estatus_general
                )
                values(?,?,?,?,?,?,?,1)
                """;

        String sqlUsuarioRol = """
                insert into usuario_rol(id_usuario,id_rol)
                values(?,?)
                on duplicate key update id_rol=id_rol
                """;

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            int idUsuario = 0;
            int idRolMaestro = obtenerIdRol(con, "maestro");

            try (PreparedStatement ps = con.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, numeroEmpleado);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        idUsuario = rs.getInt(1);
                    }
                }
            }

            if (idUsuario == 0) {
                throw new Exception("no se pudo crear el usuario");
            }

            try (PreparedStatement ps = con.prepareStatement(sqlUsuarioRol)) {
                ps.setInt(1, idUsuario);
                ps.setInt(2, idRolMaestro);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlMaestro)) {
                ps.setInt(1, idUsuario);
                ps.setString(2, numeroEmpleado);
                ps.setString(3, nombre);
                ps.setString(4, apellidoPaterno);
                ps.setString(5, apellidoMaterno);
                ps.setString(6, correo);
                ps.setString(7, telefono);
                ps.executeUpdate();
            }

            con.commit();

            mostrarInfo("maestro guardado correctamente. usuario creado con contrasena 000");
            limpiarFormulario();
            cargarMaestros();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar maestro. verifica que el numero de empleado no exista");
        }
    }

    private int obtenerIdRol(Connection con, String nombreRol) throws Exception {
        String sql = """
                select id_rol
                from rol
                where nombre=?
                limit 1
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreRol);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_rol");
                }
            }
        }

        throw new Exception("no existe el rol " + nombreRol);
    }

    private void actualizarMaestro(String nombre, String apellidoPaterno, String apellidoMaterno, String numeroEmpleado, String correo, String telefono) {
        String sqlMaestro = """
                update maestro
                set num_empleado=?,
                nombre=?,
                apellido_paterno=?,
                apellido_materno=?,
                correo=?,
                telefono=?
                where id_maestro=?
                """;

        String sqlUsuario = """
                update usuario
                set usuario=?
                where id_usuario=?
                """;

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlMaestro)) {
                ps.setString(1, numeroEmpleado);
                ps.setString(2, nombre);
                ps.setString(3, apellidoPaterno);
                ps.setString(4, apellidoMaterno);
                ps.setString(5, correo);
                ps.setString(6, telefono);
                ps.setInt(7, maestroSeleccionado.getIdMaestro());
                ps.executeUpdate();
            }

            if (maestroSeleccionado.getIdUsuario() > 0) {
                try (PreparedStatement ps = con.prepareStatement(sqlUsuario)) {
                    ps.setString(1, numeroEmpleado);
                    ps.setInt(2, maestroSeleccionado.getIdUsuario());
                    ps.executeUpdate();
                }
            }

            con.commit();

            mostrarInfo("maestro actualizado correctamente");
            limpiarFormulario();
            cargarMaestros();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al actualizar maestro");
        }
    }

    @FXML
    private void handleEditarMaestro() {
        if (maestroSeleccionado == null) {
            mostrarError("selecciona un maestro de la tabla");
            return;
        }

        modoEdicion = true;

        txtNombreMaestro.setText(maestroSeleccionado.getNombre());
        txtApellidoPaternoMaestro.setText(maestroSeleccionado.getApellidoPaterno());
        txtApellidoMaternoMaestro.setText(maestroSeleccionado.getApellidoMaterno());
        txtNumeroEmpleado.setText(maestroSeleccionado.getNumeroEmpleado());
        txtCorreoInstitucional.setText(maestroSeleccionado.getCorreo());
        txtTelefonoInstitucional.setText(maestroSeleccionado.getTelefono());
    }

    @FXML
    private void handleCambiarEstatusMaestro() {
        if (maestroSeleccionado == null) {
            mostrarError("selecciona un maestro de la tabla");
            return;
        }

        int nuevoEstatus = maestroSeleccionado.getEstatus().equalsIgnoreCase("activo") ? 0 : 1;

        String sqlMaestro = """
                update maestro
                set id_estatus_general=?
                where id_maestro=?
                """;

        String sqlUsuario = """
                update usuario
                set id_estatus_general=?
                where id_usuario=?
                """;

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlMaestro)) {
                ps.setInt(1, nuevoEstatus);
                ps.setInt(2, maestroSeleccionado.getIdMaestro());
                ps.executeUpdate();
            }

            if (maestroSeleccionado.getIdUsuario() > 0) {
                try (PreparedStatement ps = con.prepareStatement(sqlUsuario)) {
                    ps.setInt(1, nuevoEstatus);
                    ps.setInt(2, maestroSeleccionado.getIdUsuario());
                    ps.executeUpdate();
                }
            }

            con.commit();

            mostrarInfo("estatus actualizado correctamente");
            limpiarFormulario();
            cargarMaestros();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cambiar estatus");
        }
    }

    private void configurarBusqueda() {
        FilteredList<MaestroJDM> filtro = new FilteredList<>(listaMaestros, p -> true);

        txtBuscarMaestroInterno.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));
        cbFiltroMaestros.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));

        tablaMaestros.setItems(filtro);
    }

    private void aplicarFiltro(FilteredList<MaestroJDM> filtro) {
        String texto = txtBuscarMaestroInterno.getText() == null ? "" : txtBuscarMaestroInterno.getText().toLowerCase();
        String estatus = cbFiltroMaestros.getValue() == null ? "todos" : cbFiltroMaestros.getValue().toLowerCase();

        filtro.setPredicate(maestro -> {
            boolean coincideTexto =
                    maestro.getNombreCompleto().toLowerCase().contains(texto) ||
                            maestro.getNumeroEmpleado().toLowerCase().contains(texto) ||
                            maestro.getCorreo().toLowerCase().contains(texto) ||
                            maestro.getTelefono().toLowerCase().contains(texto);

            boolean coincideEstatus =
                    estatus.equals("todos") ||
                            maestro.getEstatus().toLowerCase().equals(estatus);

            return coincideTexto && coincideEstatus;
        });
    }

    private void limpiarFormulario() {
        txtNombreMaestro.clear();
        txtApellidoPaternoMaestro.clear();
        txtApellidoMaternoMaestro.clear();
        txtNumeroEmpleado.clear();
        txtCorreoInstitucional.clear();
        txtTelefonoInstitucional.clear();

        maestroSeleccionado = null;
        modoEdicion = false;
        tablaMaestros.getSelectionModel().clearSelection();
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

    public static class MaestroJDM {
        private final int idMaestro;
        private final int idUsuario;
        private final String nombre;
        private final String apellidoPaterno;
        private final String apellidoMaterno;
        private final String numeroEmpleado;
        private final String correo;
        private final String telefono;
        private final String estatus;

        public MaestroJDM(int idMaestro, int idUsuario, String nombre, String apellidoPaterno, String apellidoMaterno, String numeroEmpleado, String correo, String telefono, String estatus) {
            this.idMaestro = idMaestro;
            this.idUsuario = idUsuario;
            this.nombre = nombre;
            this.apellidoPaterno = apellidoPaterno;
            this.apellidoMaterno = apellidoMaterno;
            this.numeroEmpleado = numeroEmpleado;
            this.correo = correo == null ? "" : correo;
            this.telefono = telefono == null ? "" : telefono;
            this.estatus = estatus;
        }

        public int getIdMaestro() {
            return idMaestro;
        }

        public int getIdUsuario() {
            return idUsuario;
        }

        public String getNombre() {
            return nombre;
        }

        public String getApellidoPaterno() {
            return apellidoPaterno;
        }

        public String getApellidoMaterno() {
            return apellidoMaterno;
        }

        public String getNombreCompleto() {
            return nombre + " " + apellidoPaterno + " " + apellidoMaterno;
        }

        public String getNumeroEmpleado() {
            return numeroEmpleado;
        }

        public String getCorreo() {
            return correo;
        }

        public String getTelefono() {
            return telefono;
        }

        public String getEstatus() {
            return estatus;
        }
    }
}