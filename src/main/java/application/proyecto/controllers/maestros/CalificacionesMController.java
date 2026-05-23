package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CalificacionesMController extends BaseController {

    @FXML private TextField txtBuscar;

    @FXML private ComboBox<GrupoItem> cbGrupo;
    @FXML private TextField txtTurno;
    @FXML private ComboBox<MateriaItem> cbMateria;
    @FXML private ComboBox<ActividadItem> cbActividad;

    @FXML private Button btnCargar;
    @FXML private Button btnNuevaActividad;
    @FXML private Button btnTodosEntregaron;
    @FXML private Button btnGuardarEntregas;

    @FXML private Label lblActividadActual;
    @FXML private Label lblGrupoSeleccionado;
    @FXML private Label lblTurnoSeleccionado;
    @FXML private Label lblMateriaSeleccionada;

    @FXML private TableView<AlumnoEntrega> tablaCalificaciones;
    @FXML private TableColumn<AlumnoEntrega, String> colNoControl;
    @FXML private TableColumn<AlumnoEntrega, String> colNombreAlumno;
    @FXML private TableColumn<AlumnoEntrega, String> colGrupo;
    @FXML private TableColumn<AlumnoEntrega, String> colMateria;
    @FXML private TableColumn<AlumnoEntrega, String> colTurno;
    @FXML private TableColumn<AlumnoEntrega, String> colCalificacion;

    private final ObservableList<AlumnoEntrega> listaAlumnos = FXCollections.observableArrayList();
    private FilteredList<AlumnoEntrega> listaFiltrada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarEventos();
        cargarGrupos();
        actualizarResumen();
    }

    private int getIdMaestroActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTabla() {
        colNoControl.setCellValueFactory(new PropertyValueFactory<>("numControl"));
        colNombreAlumno.setCellValueFactory(new PropertyValueFactory<>("nombreAlumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colTurno.setCellValueFactory(new PropertyValueFactory<>("turno"));

        colCalificacion.setCellValueFactory(data -> data.getValue().entregaProperty());
        colCalificacion.setCellFactory(ComboBoxTableCell.forTableColumn("entrego", "no entrego"));
        colCalificacion.setOnEditCommit(event -> event.getRowValue().setEntrega(event.getNewValue()));

        tablaCalificaciones.setEditable(true);

        listaFiltrada = new FilteredList<>(listaAlumnos, p -> true);
        tablaCalificaciones.setItems(listaFiltrada);
    }

    private void configurarEventos() {
        btnCargar.setOnAction(event -> cargarAlumnos());
        btnTodosEntregaron.setOnAction(event -> marcarTodosEntregaron());
        btnGuardarEntregas.setOnAction(event -> guardarEntregas());
        btnNuevaActividad.setOnAction(event -> abrirNuevaActividad());

        cbGrupo.setOnAction(event -> {
            GrupoItem grupo = cbGrupo.getValue();

            cbMateria.getItems().clear();
            cbMateria.setValue(null);
            cbActividad.getItems().clear();
            cbActividad.setValue(null);
            listaAlumnos.clear();

            if (grupo != null) {
                txtTurno.setText(grupo.getTurno());
                cargarMaterias(grupo.getIdGrupoCiclo());
            } else {
                txtTurno.clear();
            }

            actualizarResumen();
        });

        cbMateria.setOnAction(event -> {
            MateriaItem materia = cbMateria.getValue();

            cbActividad.getItems().clear();
            cbActividad.setValue(null);
            listaAlumnos.clear();

            if (materia != null) {
                cargarActividades(materia.getIdCarga());
            }

            actualizarResumen();
        });

        cbActividad.setOnAction(event -> actualizarResumen());

        txtBuscar.textProperty().addListener((obs, oldValue, newValue) -> filtrarTabla());
    }

    private void cargarGrupos() {
        cbGrupo.getItems().clear();

        int idMaestro = getIdMaestroActual();

        if (idMaestro == 0) {
            mostrarError("no hay maestro en sesion");
            return;
        }

        String sql = """
                select distinct
                gc.id_grupo_ciclo,
                g.nombre as grupo,
                g.semestre,
                ct.nombre as turno
                from carga c
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                where c.id_maestro=?
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                order by g.semestre,g.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbGrupo.getItems().add(new GrupoItem(
                            rs.getInt("id_grupo_ciclo"),
                            rs.getString("grupo"),
                            rs.getInt("semestre"),
                            rs.getString("turno")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos");
        }
    }

    private void cargarMaterias(int idGrupoCiclo) {
        cbMateria.getItems().clear();

        int idMaestro = getIdMaestroActual();

        String sql = """
                select
                c.id_carga,
                m.nombre as materia
                from carga c
                inner join materia m on c.id_materia=m.id_materia
                where c.id_maestro=?
                and c.id_grupo_ciclo=?
                and c.id_estatus_general=1
                order by m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);
            ps.setInt(2, idGrupoCiclo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbMateria.getItems().add(new MateriaItem(
                            rs.getInt("id_carga"),
                            rs.getString("materia")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar materias");
        }
    }

    private void cargarActividades(int idCarga) {
        cbActividad.getItems().clear();

        String sql = """
                select
                id_actividad,
                titulo,
                fecha
                from actividad
                where id_carga=?
                and id_estatus_general=1
                order by fecha desc,titulo
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCarga);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbActividad.getItems().add(new ActividadItem(
                            rs.getInt("id_actividad"),
                            rs.getString("titulo"),
                            rs.getString("fecha")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar actividades");
        }
    }

    private void cargarAlumnos() {
        GrupoItem grupo = cbGrupo.getValue();
        MateriaItem materia = cbMateria.getValue();
        ActividadItem actividad = cbActividad.getValue();

        if (grupo == null || materia == null || actividad == null) {
            mostrarError("selecciona grupo, materia y actividad");
            return;
        }

        listaAlumnos.clear();

        String sql = """
                select
                a.id_alumno,
                a.num_control,
                concat(a.nombre,' ',a.apellido_paterno,' ',a.apellido_materno) as nombre_alumno,
                g.nombre as grupo,
                m.nombre as materia,
                ct.nombre as turno,
                ifnull(ae.entrego,1) as entrego
                from alumno a
                inner join grupo_ciclo gc on a.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join carga c on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join materia m on c.id_materia=m.id_materia
                left join actividad_entrega ae on ae.id_actividad=?
                and ae.id_alumno=a.id_alumno
                where a.id_grupo_ciclo=?
                and c.id_carga=?
                and a.id_estatus_general=1
                order by a.apellido_paterno,a.apellido_materno,a.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, actividad.getIdActividad());
            ps.setInt(2, grupo.getIdGrupoCiclo());
            ps.setInt(3, materia.getIdCarga());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaAlumnos.add(new AlumnoEntrega(
                            rs.getInt("id_alumno"),
                            rs.getString("num_control"),
                            rs.getString("nombre_alumno"),
                            rs.getString("grupo"),
                            rs.getString("materia"),
                            rs.getString("turno"),
                            rs.getInt("entrego") == 1 ? "entrego" : "no entrego"
                    ));
                }
            }

            actualizarResumen();
            filtrarTabla();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alumnos");
        }
    }

    private void guardarEntregas() {
        ActividadItem actividad = cbActividad.getValue();

        if (actividad == null || listaAlumnos.isEmpty()) {
            mostrarError("carga alumnos antes de guardar entregas");
            return;
        }

        String sql = """
                insert into actividad_entrega(
                id_actividad,
                id_alumno,
                entrego
                )
                values(?,?,?)
                on duplicate key update
                entrego=values(entrego)
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (AlumnoEntrega alumno : listaAlumnos) {
                ps.setInt(1, actividad.getIdActividad());
                ps.setInt(2, alumno.getIdAlumno());
                ps.setInt(3, alumno.getEntrega().equalsIgnoreCase("entrego") ? 1 : 0);
                ps.addBatch();
            }

            ps.executeBatch();

            mostrarInfo("entregas guardadas correctamente");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar entregas");
        }
    }

    private void marcarTodosEntregaron() {
        for (AlumnoEntrega alumno : listaAlumnos) {
            alumno.setEntrega("entrego");
        }

        tablaCalificaciones.refresh();
    }

    private void abrirNuevaActividad() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/maestro/NuevaActividadM.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Nueva actividad");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("no se pudo abrir la vista de nueva actividad");
        }
    }

    private void filtrarTabla() {
        String texto = txtBuscar.getText() == null
                ? ""
                : txtBuscar.getText().toLowerCase().trim();

        listaFiltrada.setPredicate(alumno ->
                alumno.getNumControl().toLowerCase().contains(texto) ||
                        alumno.getNombreAlumno().toLowerCase().contains(texto) ||
                        alumno.getGrupo().toLowerCase().contains(texto) ||
                        alumno.getMateria().toLowerCase().contains(texto) ||
                        alumno.getTurno().toLowerCase().contains(texto) ||
                        alumno.getEntrega().toLowerCase().contains(texto)
        );
    }

    private void actualizarResumen() {
        GrupoItem grupo = cbGrupo.getValue();
        MateriaItem materia = cbMateria.getValue();
        ActividadItem actividad = cbActividad.getValue();

        lblActividadActual.setText(actividad == null ? "Sin actividad seleccionada" : actividad.getTitulo());
        lblGrupoSeleccionado.setText(grupo == null ? "Sin grupo" : grupo.getNombre());
        lblTurnoSeleccionado.setText(grupo == null ? "Sin turno" : grupo.getTurno());
        lblMateriaSeleccionada.setText(materia == null ? "Sin materia" : materia.getNombre());
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

    public static class GrupoItem {
        private final int idGrupoCiclo;
        private final String nombre;
        private final int semestre;
        private final String turno;

        public GrupoItem(int idGrupoCiclo, String nombre, int semestre, String turno) {
            this.idGrupoCiclo = idGrupoCiclo;
            this.nombre = nombre;
            this.semestre = semestre;
            this.turno = turno;
        }

        public int getIdGrupoCiclo() {
            return idGrupoCiclo;
        }

        public String getNombre() {
            return nombre;
        }

        public String getTurno() {
            return turno;
        }

        @Override
        public String toString() {
            return nombre + " - " + semestre + " semestre";
        }
    }

    public static class MateriaItem {
        private final int idCarga;
        private final String nombre;

        public MateriaItem(int idCarga, String nombre) {
            this.idCarga = idCarga;
            this.nombre = nombre;
        }

        public int getIdCarga() {
            return idCarga;
        }

        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    public static class ActividadItem {
        private final int idActividad;
        private final String titulo;
        private final String fecha;

        public ActividadItem(int idActividad, String titulo, String fecha) {
            this.idActividad = idActividad;
            this.titulo = titulo;
            this.fecha = fecha;
        }

        public int getIdActividad() {
            return idActividad;
        }

        public String getTitulo() {
            return titulo;
        }

        @Override
        public String toString() {
            return titulo + " - " + fecha;
        }
    }

    public static class AlumnoEntrega {
        private final int idAlumno;
        private final String numControl;
        private final String nombreAlumno;
        private final String grupo;
        private final String materia;
        private final String turno;
        private final SimpleStringProperty entrega;

        public AlumnoEntrega(int idAlumno, String numControl, String nombreAlumno, String grupo, String materia, String turno, String entrega) {
            this.idAlumno = idAlumno;
            this.numControl = numControl;
            this.nombreAlumno = nombreAlumno;
            this.grupo = grupo;
            this.materia = materia;
            this.turno = turno;
            this.entrega = new SimpleStringProperty(entrega);
        }

        public int getIdAlumno() {
            return idAlumno;
        }

        public String getNumControl() {
            return numControl;
        }

        public String getNombreAlumno() {
            return nombreAlumno;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getMateria() {
            return materia;
        }

        public String getTurno() {
            return turno;
        }

        public String getEntrega() {
            return entrega.get();
        }

        public void setEntrega(String entrega) {
            this.entrega.set(entrega);
        }

        public SimpleStringProperty entregaProperty() {
            return entrega;
        }
    }
}