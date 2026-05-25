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

public class UsuariosYRolesJDMController extends BaseController {

    @FXML private ComboBox<ItemCombo> cbMaestroUsuario;
    @FXML private TextField txtUsuario;
    @FXML private TextField txtNombreMaestro;
    @FXML private TextField txtNumeroEmpleado;

    @FXML private CheckBox chkRolMaestro;
    @FXML private CheckBox chkRolTutor;
    @FXML private CheckBox chkRolJefeMaestros;

    @FXML private Label lblUsuariosActivos;
    @FXML private Label lblPendientesContrasena;
    @FXML private Label lblMaestrosTutores;

    @FXML private TextField txtBuscarUsuarioInterno;
    @FXML private ComboBox<String> cbFiltroUsuarios;

    @FXML private TableView<UsuarioRolJDM> tablaUsuarios;
    @FXML private TableColumn<UsuarioRolJDM, String> colMaestroRelacionado;
    @FXML private TableColumn<UsuarioRolJDM, String> colNumeroEmpleadoUsuario;
    @FXML private TableColumn<UsuarioRolJDM, String> colRolesUsuario;
    @FXML private TableColumn<UsuarioRolJDM, String> colAccesoUsuario;
    @FXML private TableColumn<UsuarioRolJDM, String> colEstatusUsuario;

    private final ObservableList<UsuarioRolJDM> listaUsuarios = FXCollections.observableArrayList();

    private UsuarioRolJDM usuarioSeleccionado;
    private ItemCombo maestroSeleccionado;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        cargarMaestros();
        cargarUsuarios();
        cargarResumen();
        configurarBusqueda();
    }

    private void configurarTabla() {
        colMaestroRelacionado.setCellValueFactory(new PropertyValueFactory<>("maestro"));
        colNumeroEmpleadoUsuario.setCellValueFactory(new PropertyValueFactory<>("numeroEmpleado"));
        colRolesUsuario.setCellValueFactory(new PropertyValueFactory<>("roles"));
        colAccesoUsuario.setCellValueFactory(new PropertyValueFactory<>("acceso"));
        colEstatusUsuario.setCellValueFactory(new PropertyValueFactory<>("estatus"));
    }

    private void configurarCombos() {
        cbFiltroUsuarios.setItems(FXCollections.observableArrayList("todos", "activo", "inactivo"));
        cbFiltroUsuarios.setValue("todos");

        txtUsuario.setEditable(false);
        txtNombreMaestro.setEditable(false);
        txtNumeroEmpleado.setEditable(false);
    }

    private void configurarEventos() {
        cbMaestroUsuario.valueProperty().addListener((obs, oldValue, newValue) -> {
            maestroSeleccionado = newValue;

            if (newValue != null) {
                cargarDatosMaestro(newValue.getId());
            } else {
                limpiarDatosMaestro();
            }
        });

        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            usuarioSeleccionado = newValue;

            if (newValue != null) {
                seleccionarMaestroPorId(newValue.getIdMaestro());
            }
        });
    }

    private void cargarMaestros() {
        cbMaestroUsuario.getItems().clear();

        String sql = """
                select
                m.id_maestro,
                concat(m.nombre,' ',m.apellido_paterno,' ',m.apellido_materno) as maestro
                from maestro m
                where m.id_estatus_general=1
                order by m.apellido_paterno,m.apellido_materno,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbMaestroUsuario.getItems().add(new ItemCombo(
                        rs.getInt("id_maestro"),
                        rs.getString("maestro")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar maestros");
        }
    }

    private void cargarDatosMaestro(int idMaestro) {
        limpiarRoles();

        String sql = """
                select
                m.id_maestro,
                m.id_usuario,
                m.num_empleado,
                concat(m.nombre,' ',m.apellido_paterno,' ',m.apellido_materno) as maestro,
                u.usuario
                from maestro m
                left join usuario u on m.id_usuario=u.id_usuario
                where m.id_maestro=?
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtNombreMaestro.setText(texto(rs.getString("maestro")));
                    txtNumeroEmpleado.setText(texto(rs.getString("num_empleado")));

                    String usuario = rs.getString("usuario");
                    txtUsuario.setText(usuario == null || usuario.isBlank() ? rs.getString("num_empleado") : usuario);

                    int idUsuario = rs.getInt("id_usuario");

                    if (!rs.wasNull() && idUsuario > 0) {
                        cargarRolesUsuario(idUsuario);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar datos del maestro");
        }
    }

    private void cargarRolesUsuario(int idUsuario) {
        limpiarRoles();

        String sql = """
                select r.nombre
                from usuario_rol ur
                inner join rol r on ur.id_rol=r.id_rol
                where ur.id_usuario=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rol = rs.getString("nombre");

                    if (rol.equalsIgnoreCase("maestro")) {
                        chkRolMaestro.setSelected(true);
                    }

                    if (rol.equalsIgnoreCase("tutor")) {
                        chkRolTutor.setSelected(true);
                    }

                    if (rol.equalsIgnoreCase("jefe de maestros")) {
                        chkRolJefeMaestros.setSelected(true);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar roles");
        }
    }

    private void cargarUsuarios() {
        listaUsuarios.clear();

        String sql = """
                select
                m.id_maestro,
                ifnull(m.id_usuario,0) as id_usuario,
                concat(m.nombre,' ',m.apellido_paterno,' ',m.apellido_materno) as maestro,
                m.num_empleado,
                ifnull(u.usuario,m.num_empleado) as usuario,
                ifnull(group_concat(r.nombre order by r.nombre separator ', '),'sin roles') as roles,
                case
                when m.id_usuario is null then 'sin usuario'
                when u.password_hash is null or u.password_hash='' then 'pendiente de contrasena'
                else 'activo'
                end as acceso,
                ceg.nombre as estatus
                from maestro m
                left join usuario u on m.id_usuario=u.id_usuario
                left join usuario_rol ur on u.id_usuario=ur.id_usuario
                left join rol r on ur.id_rol=r.id_rol
                inner join cat_estatus_general ceg on m.id_estatus_general=ceg.id_estatus_general
                group by
                m.id_maestro,
                m.id_usuario,
                m.nombre,
                m.apellido_paterno,
                m.apellido_materno,
                m.num_empleado,
                u.usuario,
                u.password_hash,
                ceg.nombre
                order by m.apellido_paterno,m.apellido_materno,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaUsuarios.add(new UsuarioRolJDM(
                        rs.getInt("id_maestro"),
                        rs.getInt("id_usuario"),
                        rs.getString("maestro"),
                        rs.getString("num_empleado"),
                        rs.getString("usuario"),
                        rs.getString("roles"),
                        rs.getString("acceso"),
                        rs.getString("estatus")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar usuarios");
        }
    }

    private void cargarResumen() {
        String sql = """
                select
                count(distinct case when m.id_usuario is not null and u.id_estatus_general=1 then u.id_usuario end) as usuarios_activos,
                count(distinct case when m.id_usuario is not null and (u.password_hash is null or u.password_hash='') then u.id_usuario end) as pendientes,
                count(distinct case when r.nombre='tutor' and m.id_estatus_general=1 then m.id_maestro end) as tutores
                from maestro m
                left join usuario u on m.id_usuario=u.id_usuario
                left join usuario_rol ur on u.id_usuario=ur.id_usuario
                left join rol r on ur.id_rol=r.id_rol
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                lblUsuariosActivos.setText(String.valueOf(rs.getInt("usuarios_activos")));
                lblPendientesContrasena.setText(String.valueOf(rs.getInt("pendientes")));
                lblMaestrosTutores.setText(String.valueOf(rs.getInt("tutores")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar resumen");
        }
    }

    @FXML
    private void handleGuardarUsuario() {
        if (maestroSeleccionado == null) {
            mostrarError("selecciona un maestro");
            return;
        }

        if (!chkRolMaestro.isSelected() && !chkRolTutor.isSelected() && !chkRolJefeMaestros.isSelected()) {
            mostrarError("selecciona al menos un rol");
            return;
        }

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            int idUsuario = obtenerOCrearIdUsuarioMaestro(con, maestroSeleccionado.getId());

            actualizarRoles(con, idUsuario);

            con.commit();

            mostrarInfo("usuario y roles actualizados correctamente");
            limpiarFormulario();
            cargarMaestros();
            cargarUsuarios();
            cargarResumen();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar usuario y roles");
        }
    }

    private int obtenerOCrearIdUsuarioMaestro(Connection con, int idMaestro) throws Exception {
        String sqlBuscar = """
                select
                m.id_usuario,
                m.num_empleado
                from maestro m
                where m.id_maestro=?
                limit 1
                """;

        int idUsuario = 0;
        String numeroEmpleado = "";

        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setInt(1, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idUsuario = rs.getInt("id_usuario");

                    if (rs.wasNull()) {
                        idUsuario = 0;
                    }

                    numeroEmpleado = rs.getString("num_empleado");
                }
            }
        }

        if (idUsuario > 0) {
            activarUsuario(con, idUsuario);
            return idUsuario;
        }

        if (numeroEmpleado == null || numeroEmpleado.isBlank()) {
            throw new Exception("el maestro no tiene numero de empleado");
        }

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

        String sqlActualizarMaestro = """
                update maestro
                set id_usuario=?
                where id_maestro=?
                """;

        try (PreparedStatement ps = con.prepareStatement(sqlActualizarMaestro)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idMaestro);
            ps.executeUpdate();
        }

        return idUsuario;
    }

    private void activarUsuario(Connection con, int idUsuario) throws Exception {
        String sql = """
                update usuario
                set id_estatus_general=1
                where id_usuario=?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    private void actualizarRoles(Connection con, int idUsuario) throws Exception {
        String sqlDelete = "delete from usuario_rol where id_usuario=?";

        try (PreparedStatement ps = con.prepareStatement(sqlDelete)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }

        if (chkRolMaestro.isSelected()) {
            insertarRol(con, idUsuario, "maestro");
        }

        if (chkRolTutor.isSelected()) {
            insertarRol(con, idUsuario, "tutor");
        }

        if (chkRolJefeMaestros.isSelected()) {
            insertarRol(con, idUsuario, "jefe de maestros");
        }
    }

    private void insertarRol(Connection con, int idUsuario, String nombreRol) throws Exception {
        String sql = """
                insert into usuario_rol(id_usuario,id_rol)
                select ?,id_rol
                from rol
                where nombre=?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, nombreRol);
            ps.executeUpdate();
        }
    }

    @FXML
    private void handleCambiarEstatusUsuario() {
        if (usuarioSeleccionado == null) {
            mostrarError("selecciona un usuario de la tabla");
            return;
        }

        int nuevoEstatus = usuarioSeleccionado.getEstatus().equalsIgnoreCase("activo") ? 0 : 1;

        String sqlMaestro = "update maestro set id_estatus_general=? where id_maestro=?";
        String sqlUsuario = "update usuario set id_estatus_general=? where id_usuario=?";

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlMaestro)) {
                ps.setInt(1, nuevoEstatus);
                ps.setInt(2, usuarioSeleccionado.getIdMaestro());
                ps.executeUpdate();
            }

            if (usuarioSeleccionado.getIdUsuario() > 0) {
                try (PreparedStatement ps = con.prepareStatement(sqlUsuario)) {
                    ps.setInt(1, nuevoEstatus);
                    ps.setInt(2, usuarioSeleccionado.getIdUsuario());
                    ps.executeUpdate();
                }
            }

            con.commit();

            mostrarInfo("estatus actualizado correctamente");
            limpiarFormulario();
            cargarMaestros();
            cargarUsuarios();
            cargarResumen();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cambiar estatus");
        }
    }

    private void configurarBusqueda() {
        FilteredList<UsuarioRolJDM> filtro = new FilteredList<>(listaUsuarios, p -> true);

        txtBuscarUsuarioInterno.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));
        cbFiltroUsuarios.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));

        tablaUsuarios.setItems(filtro);
    }

    private void aplicarFiltro(FilteredList<UsuarioRolJDM> filtro) {
        String texto = txtBuscarUsuarioInterno.getText() == null ? "" : txtBuscarUsuarioInterno.getText().toLowerCase();
        String estatus = cbFiltroUsuarios.getValue() == null ? "todos" : cbFiltroUsuarios.getValue().toLowerCase();

        filtro.setPredicate(usuario -> {
            boolean coincideTexto =
                    usuario.getMaestro().toLowerCase().contains(texto) ||
                            usuario.getNumeroEmpleado().toLowerCase().contains(texto) ||
                            usuario.getUsuario().toLowerCase().contains(texto) ||
                            usuario.getRoles().toLowerCase().contains(texto) ||
                            usuario.getAcceso().toLowerCase().contains(texto);

            boolean coincideEstatus =
                    estatus.equals("todos") ||
                            usuario.getEstatus().toLowerCase().equals(estatus);

            return coincideTexto && coincideEstatus;
        });
    }

    private void seleccionarMaestroPorId(int idMaestro) {
        for (ItemCombo item : cbMaestroUsuario.getItems()) {
            if (item.getId() == idMaestro) {
                cbMaestroUsuario.setValue(item);
                return;
            }
        }
    }

    private void limpiarFormulario() {
        cbMaestroUsuario.setValue(null);
        limpiarDatosMaestro();
        limpiarRoles();

        usuarioSeleccionado = null;
        maestroSeleccionado = null;
        tablaUsuarios.getSelectionModel().clearSelection();
    }

    private void limpiarDatosMaestro() {
        txtUsuario.clear();
        txtNombreMaestro.clear();
        txtNumeroEmpleado.clear();
    }

    private void limpiarRoles() {
        chkRolMaestro.setSelected(false);
        chkRolTutor.setSelected(false);
        chkRolJefeMaestros.setSelected(false);
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

    public static class ItemCombo {
        private final int id;
        private final String nombre;

        public ItemCombo(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public int getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    public static class UsuarioRolJDM {
        private final int idMaestro;
        private final int idUsuario;
        private final String maestro;
        private final String numeroEmpleado;
        private final String usuario;
        private final String roles;
        private final String acceso;
        private final String estatus;

        public UsuarioRolJDM(int idMaestro, int idUsuario, String maestro, String numeroEmpleado, String usuario, String roles, String acceso, String estatus) {
            this.idMaestro = idMaestro;
            this.idUsuario = idUsuario;
            this.maestro = textoSeguro(maestro);
            this.numeroEmpleado = textoSeguro(numeroEmpleado);
            this.usuario = textoSeguro(usuario);
            this.roles = textoSeguro(roles);
            this.acceso = textoSeguro(acceso);
            this.estatus = textoSeguro(estatus);
        }

        private static String textoSeguro(String valor) {
            return valor == null ? "" : valor;
        }

        public int getIdMaestro() {
            return idMaestro;
        }

        public int getIdUsuario() {
            return idUsuario;
        }

        public String getMaestro() {
            return maestro;
        }

        public String getNumeroEmpleado() {
            return numeroEmpleado;
        }

        public String getUsuario() {
            return usuario;
        }

        public String getRoles() {
            return roles;
        }

        public String getAcceso() {
            return acceso;
        }

        public String getEstatus() {
            return estatus;
        }
    }
}