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

public class AlumnosJDMController extends BaseController {

    @FXML private TextField txtNombreAlumno;
    @FXML private TextField txtApellidoPaternoAlumno;
    @FXML private TextField txtApellidoMaternoAlumno;
    @FXML private TextField txtNumeroControlAlumno;
    @FXML private TextField txtBuscarAlumnoInterno;

    @FXML private ComboBox<ItemCombo> cbGrupoAlumno;
    @FXML private ComboBox<ItemCombo> cbSexoAlumno;
    @FXML private ComboBox<String> cbFiltroAlumnos;

    @FXML private TableView<AlumnoJDM> tablaAlumnos;
    @FXML private TableColumn<AlumnoJDM, String> colNombreAlumno;
    @FXML private TableColumn<AlumnoJDM, String> colNumeroControlAlumno;
    @FXML private TableColumn<AlumnoJDM, String> colGrupoAlumno;
    @FXML private TableColumn<AlumnoJDM, String> colSexoAlumno;
    @FXML private TableColumn<AlumnoJDM, String> colEstatusAlumno;

    @FXML private ComboBox<ItemCombo> cbClaseInscripcion;
    @FXML private TableView<ClaseAlumnoJDM> tablaClasesAlumno;
    @FXML private TableColumn<ClaseAlumnoJDM, String> colClaseMateria;
    @FXML private TableColumn<ClaseAlumnoJDM, String> colClaseGrupo;
    @FXML private TableColumn<ClaseAlumnoJDM, String> colClaseTurno;
    @FXML private TableColumn<ClaseAlumnoJDM, String> colClaseCiclo;
    @FXML private TableColumn<ClaseAlumnoJDM, String> colClaseMaestro;
    @FXML private TableColumn<ClaseAlumnoJDM, String> colClaseEstatus;

    private final ObservableList<AlumnoJDM> listaAlumnos = FXCollections.observableArrayList();
    private final ObservableList<ClaseAlumnoJDM> listaClasesAlumno = FXCollections.observableArrayList();

    private AlumnoJDM alumnoSeleccionado;
    private boolean modoEdicion = false;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        cargarGrupos();
        cargarSexos();
        cargarClasesDisponibles();
        cargarAlumnos();
        configurarBusqueda();
    }

    private void configurarTabla() {
        colNombreAlumno.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colNumeroControlAlumno.setCellValueFactory(new PropertyValueFactory<>("numeroControl"));
        colGrupoAlumno.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colSexoAlumno.setCellValueFactory(new PropertyValueFactory<>("sexo"));
        colEstatusAlumno.setCellValueFactory(new PropertyValueFactory<>("estatus"));

        colClaseMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colClaseGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colClaseTurno.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colClaseCiclo.setCellValueFactory(new PropertyValueFactory<>("ciclo"));
        colClaseMaestro.setCellValueFactory(new PropertyValueFactory<>("maestro"));
        colClaseEstatus.setCellValueFactory(new PropertyValueFactory<>("estatus"));

        tablaClasesAlumno.setItems(listaClasesAlumno);
    }

    private void configurarCombos() {
        cbFiltroAlumnos.setItems(FXCollections.observableArrayList("todos", "activo", "inactivo"));
        cbFiltroAlumnos.setValue("todos");
    }

    private void configurarEventos() {
        tablaAlumnos.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            alumnoSeleccionado = newValue;

            if (alumnoSeleccionado != null) {
                cargarClasesAlumno(alumnoSeleccionado.getIdAlumno());
            } else {
                listaClasesAlumno.clear();
            }
        });
    }

    private void cargarGrupos() {
        cbGrupoAlumno.getItems().clear();

        String sql = """
                select
                gc.id_grupo_ciclo,
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo
                from grupo_ciclo gc
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                where gc.id_estatus_general=1
                and g.id_estatus_general=1
                order by ce.nombre desc,g.semestre,g.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbGrupoAlumno.getItems().add(new ItemCombo(
                        rs.getInt("id_grupo_ciclo"),
                        rs.getString("grupo")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos");
        }
    }

    private void cargarSexos() {
        cbSexoAlumno.getItems().clear();

        String sql = "select id_sexo,nombre from cat_sexo order by id_sexo";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbSexoAlumno.getItems().add(new ItemCombo(
                        rs.getInt("id_sexo"),
                        rs.getString("nombre")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar sexo");
        }
    }

    private void cargarClasesDisponibles() {
        cbClaseInscripcion.getItems().clear();

        String sql = """
                select
                c.id_carga,
                concat(m.clave,' - ',m.nombre,' | ',g.nombre,' - ',ct.nombre,' - ',ce.nombre,' | ',ma.nombre,' ',ma.apellido_paterno) as clase
                from carga c
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join maestro ma on c.id_maestro=ma.id_maestro
                where c.id_estatus_general=1
                order by ce.nombre desc,g.semestre,g.nombre,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbClaseInscripcion.getItems().add(new ItemCombo(
                        rs.getInt("id_carga"),
                        rs.getString("clase")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar clases");
        }
    }

    private void cargarAlumnos() {
        listaAlumnos.clear();

        String sql = """
                select
                a.id_alumno,
                a.nombre,
                a.apellido_paterno,
                a.apellido_materno,
                a.num_control,
                a.id_grupo_ciclo,
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo,
                a.id_sexo,
                cs.nombre as sexo,
                ceg.nombre as estatus
                from alumno a
                inner join grupo_ciclo gc on a.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_sexo cs on a.id_sexo=cs.id_sexo
                inner join cat_estatus_general ceg on a.id_estatus_general=ceg.id_estatus_general
                order by g.nombre,a.apellido_paterno,a.apellido_materno,a.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaAlumnos.add(new AlumnoJDM(
                        rs.getInt("id_alumno"),
                        rs.getString("nombre"),
                        rs.getString("apellido_paterno"),
                        rs.getString("apellido_materno"),
                        rs.getString("num_control"),
                        rs.getInt("id_grupo_ciclo"),
                        rs.getString("grupo"),
                        rs.getInt("id_sexo"),
                        rs.getString("sexo"),
                        rs.getString("estatus")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alumnos");
        }
    }

    private void cargarClasesAlumno(int idAlumno) {
        listaClasesAlumno.clear();

        String sql = """
                select
                ac.id_alumno_carga,
                c.id_carga,
                m.nombre as materia,
                g.nombre as grupo,
                ct.nombre as turno,
                ce.nombre as ciclo,
                concat(ma.nombre,' ',ma.apellido_paterno,' ',ma.apellido_materno) as maestro,
                ceg.nombre as estatus
                from alumno_carga ac
                inner join carga c on ac.id_carga=c.id_carga
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join maestro ma on c.id_maestro=ma.id_maestro
                inner join cat_estatus_general ceg on ac.id_estatus_general=ceg.id_estatus_general
                where ac.id_alumno=?
                order by ce.nombre,g.semestre,g.nombre,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAlumno);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaClasesAlumno.add(new ClaseAlumnoJDM(
                            rs.getInt("id_alumno_carga"),
                            rs.getInt("id_carga"),
                            rs.getString("materia"),
                            rs.getString("grupo"),
                            rs.getString("turno"),
                            rs.getString("ciclo"),
                            rs.getString("maestro"),
                            rs.getString("estatus")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar clases del alumno");
        }
    }

    @FXML
    private void handleGuardarAlumno() {
        String nombre = txtNombreAlumno.getText() == null ? "" : txtNombreAlumno.getText().trim();
        String apellidoPaterno = txtApellidoPaternoAlumno.getText() == null ? "" : txtApellidoPaternoAlumno.getText().trim();
        String apellidoMaterno = txtApellidoMaternoAlumno.getText() == null ? "" : txtApellidoMaternoAlumno.getText().trim();
        String numeroControl = txtNumeroControlAlumno.getText() == null ? "" : txtNumeroControlAlumno.getText().trim();
        ItemCombo grupo = cbGrupoAlumno.getValue();
        ItemCombo sexo = cbSexoAlumno.getValue();

        if (nombre.isEmpty() || apellidoPaterno.isEmpty() || apellidoMaterno.isEmpty() || numeroControl.isEmpty() || grupo == null || sexo == null) {
            mostrarError("captura nombre, apellidos, numero de control, grupo y sexo");
            return;
        }

        if (modoEdicion && alumnoSeleccionado != null) {
            actualizarAlumno(nombre, apellidoPaterno, apellidoMaterno, numeroControl, grupo.getId(), sexo.getId());
        } else {
            insertarAlumno(nombre, apellidoPaterno, apellidoMaterno, numeroControl, grupo.getId(), sexo.getId());
        }
    }

    private void insertarAlumno(String nombre, String apellidoPaterno, String apellidoMaterno, String numeroControl, int idGrupoCiclo, int idSexo) {
        String sql = """
                insert into alumno(
                id_grupo_ciclo,
                num_control,
                nombre,
                apellido_paterno,
                apellido_materno,
                id_sexo,
                id_estatus_general
                )
                values(?,?,?,?,?,?,1)
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idGrupoCiclo);
            ps.setString(2, numeroControl);
            ps.setString(3, nombre);
            ps.setString(4, apellidoPaterno);
            ps.setString(5, apellidoMaterno);
            ps.setInt(6, idSexo);
            ps.executeUpdate();

            mostrarInfo("alumno guardado correctamente");
            limpiarFormulario();
            cargarAlumnos();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar alumno. verifica que el numero de control no exista");
        }
    }

    private void actualizarAlumno(String nombre, String apellidoPaterno, String apellidoMaterno, String numeroControl, int idGrupoCiclo, int idSexo) {
        String sql = """
                update alumno
                set id_grupo_ciclo=?,
                num_control=?,
                nombre=?,
                apellido_paterno=?,
                apellido_materno=?,
                id_sexo=?
                where id_alumno=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idGrupoCiclo);
            ps.setString(2, numeroControl);
            ps.setString(3, nombre);
            ps.setString(4, apellidoPaterno);
            ps.setString(5, apellidoMaterno);
            ps.setInt(6, idSexo);
            ps.setInt(7, alumnoSeleccionado.getIdAlumno());
            ps.executeUpdate();

            mostrarInfo("alumno actualizado correctamente");
            limpiarFormulario();
            cargarAlumnos();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al actualizar alumno");
        }
    }

    @FXML
    private void handleEditarAlumno() {
        if (alumnoSeleccionado == null) {
            mostrarError("selecciona un alumno de la tabla");
            return;
        }

        modoEdicion = true;

        txtNombreAlumno.setText(alumnoSeleccionado.getNombre());
        txtApellidoPaternoAlumno.setText(alumnoSeleccionado.getApellidoPaterno());
        txtApellidoMaternoAlumno.setText(alumnoSeleccionado.getApellidoMaterno());
        txtNumeroControlAlumno.setText(alumnoSeleccionado.getNumeroControl());

        seleccionarComboPorId(cbGrupoAlumno, alumnoSeleccionado.getIdGrupoCiclo());
        seleccionarComboPorId(cbSexoAlumno, alumnoSeleccionado.getIdSexo());
    }

    @FXML
    private void handleCambiarEstatusAlumno() {
        if (alumnoSeleccionado == null) {
            mostrarError("selecciona un alumno de la tabla");
            return;
        }

        int nuevoEstatus = alumnoSeleccionado.getEstatus().equalsIgnoreCase("activo") ? 0 : 1;

        String sql = "update alumno set id_estatus_general=? where id_alumno=?";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, nuevoEstatus);
            ps.setInt(2, alumnoSeleccionado.getIdAlumno());
            ps.executeUpdate();

            mostrarInfo("estatus actualizado correctamente");
            limpiarFormulario();
            cargarAlumnos();
            listaClasesAlumno.clear();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cambiar estatus");
        }
    }

    @FXML
    private void handleInscribirAlumnoClase() {
        if (alumnoSeleccionado == null) {
            mostrarError("selecciona un alumno");
            return;
        }

        ItemCombo clase = cbClaseInscripcion.getValue();

        if (clase == null) {
            mostrarError("selecciona una clase");
            return;
        }

        String sql = """
                insert into alumno_carga(id_alumno,id_carga,id_estatus_general)
                values(?,?,1)
                on duplicate key update id_estatus_general=1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, alumnoSeleccionado.getIdAlumno());
            ps.setInt(2, clase.getId());
            ps.executeUpdate();

            mostrarInfo("alumno inscrito correctamente");
            cbClaseInscripcion.setValue(null);
            cargarClasesAlumno(alumnoSeleccionado.getIdAlumno());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al inscribir alumno a clase");
        }
    }

    @FXML
    private void handleQuitarInscripcionAlumnoClase() {
        if (alumnoSeleccionado == null) {
            mostrarError("selecciona un alumno");
            return;
        }

        ClaseAlumnoJDM claseSeleccionada = tablaClasesAlumno.getSelectionModel().getSelectedItem();

        if (claseSeleccionada == null) {
            mostrarError("selecciona una clase inscrita");
            return;
        }

        String sql = "update alumno_carga set id_estatus_general=0 where id_alumno_carga=?";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, claseSeleccionada.getIdAlumnoCarga());
            ps.executeUpdate();

            mostrarInfo("inscripcion desactivada correctamente");
            cargarClasesAlumno(alumnoSeleccionado.getIdAlumno());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al quitar inscripcion");
        }
    }

    private void configurarBusqueda() {
        FilteredList<AlumnoJDM> filtro = new FilteredList<>(listaAlumnos, p -> true);

        txtBuscarAlumnoInterno.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));
        cbFiltroAlumnos.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));

        tablaAlumnos.setItems(filtro);
    }

    private void aplicarFiltro(FilteredList<AlumnoJDM> filtro) {
        String texto = txtBuscarAlumnoInterno.getText() == null ? "" : txtBuscarAlumnoInterno.getText().toLowerCase();
        String estatus = cbFiltroAlumnos.getValue() == null ? "todos" : cbFiltroAlumnos.getValue().toLowerCase();

        filtro.setPredicate(alumno -> {
            boolean coincideTexto =
                    alumno.getNombreCompleto().toLowerCase().contains(texto) ||
                            alumno.getNumeroControl().toLowerCase().contains(texto) ||
                            alumno.getGrupo().toLowerCase().contains(texto) ||
                            alumno.getSexo().toLowerCase().contains(texto);

            boolean coincideEstatus =
                    estatus.equals("todos") ||
                            alumno.getEstatus().toLowerCase().equals(estatus);

            return coincideTexto && coincideEstatus;
        });
    }

    private void seleccionarComboPorId(ComboBox<ItemCombo> combo, int id) {
        for (ItemCombo item : combo.getItems()) {
            if (item.getId() == id) {
                combo.setValue(item);
                return;
            }
        }
    }

    private void limpiarFormulario() {
        txtNombreAlumno.clear();
        txtApellidoPaternoAlumno.clear();
        txtApellidoMaternoAlumno.clear();
        txtNumeroControlAlumno.clear();
        cbGrupoAlumno.setValue(null);
        cbSexoAlumno.setValue(null);

        alumnoSeleccionado = null;
        modoEdicion = false;
        tablaAlumnos.getSelectionModel().clearSelection();
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

    public static class AlumnoJDM {
        private final int idAlumno;
        private final String nombre;
        private final String apellidoPaterno;
        private final String apellidoMaterno;
        private final String numeroControl;
        private final int idGrupoCiclo;
        private final String grupo;
        private final int idSexo;
        private final String sexo;
        private final String estatus;

        public AlumnoJDM(int idAlumno, String nombre, String apellidoPaterno, String apellidoMaterno, String numeroControl, int idGrupoCiclo, String grupo, int idSexo, String sexo, String estatus) {
            this.idAlumno = idAlumno;
            this.nombre = nombre;
            this.apellidoPaterno = apellidoPaterno;
            this.apellidoMaterno = apellidoMaterno;
            this.numeroControl = numeroControl;
            this.idGrupoCiclo = idGrupoCiclo;
            this.grupo = grupo;
            this.idSexo = idSexo;
            this.sexo = sexo;
            this.estatus = estatus;
        }

        public int getIdAlumno() {
            return idAlumno;
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

        public String getNumeroControl() {
            return numeroControl;
        }

        public int getIdGrupoCiclo() {
            return idGrupoCiclo;
        }

        public String getGrupo() {
            return grupo;
        }

        public int getIdSexo() {
            return idSexo;
        }

        public String getSexo() {
            return sexo;
        }

        public String getEstatus() {
            return estatus;
        }
    }

    public static class ClaseAlumnoJDM {
        private final int idAlumnoCarga;
        private final int idCarga;
        private final String materia;
        private final String grupo;
        private final String turno;
        private final String ciclo;
        private final String maestro;
        private final String estatus;

        public ClaseAlumnoJDM(int idAlumnoCarga, int idCarga, String materia, String grupo, String turno, String ciclo, String maestro, String estatus) {
            this.idAlumnoCarga = idAlumnoCarga;
            this.idCarga = idCarga;
            this.materia = materia;
            this.grupo = grupo;
            this.turno = turno;
            this.ciclo = ciclo;
            this.maestro = maestro;
            this.estatus = estatus;
        }

        public int getIdAlumnoCarga() {
            return idAlumnoCarga;
        }

        public int getIdCarga() {
            return idCarga;
        }

        public String getMateria() {
            return materia;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getTurno() {
            return turno;
        }

        public String getCiclo() {
            return ciclo;
        }

        public String getMaestro() {
            return maestro;
        }

        public String getEstatus() {
            return estatus;
        }
    }
}