package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class AsistenciaMController extends BaseController {

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<GrupoItem> cbGrupo;
    @FXML private TextField txtTurno;
    @FXML private ComboBox<MateriaItem> cbMateria;
    @FXML private DatePicker dpFecha;

    @FXML private Button btnCargar;
    @FXML private Button btnTodosAsistieron;
    @FXML private Button btnLimpiar;
    @FXML private Button btnGuardarAsistencia;

    @FXML private Label lblFechaSeleccionada;
    @FXML private Label lblGrupoSeleccionado;
    @FXML private Label lblTurnoSeleccionado;
    @FXML private Label lblMateriaSeleccionada;
    @FXML private Label lblTotalAlumnos;

    @FXML private TableView<AlumnoAsistencia> tablaAsistencia;
    @FXML private TableColumn<AlumnoAsistencia, String> colNoControl;
    @FXML private TableColumn<AlumnoAsistencia, String> colNombreAlumno;
    @FXML private TableColumn<AlumnoAsistencia, String> colGrupo;
    @FXML private TableColumn<AlumnoAsistencia, String> colTurno;
    @FXML private TableColumn<AlumnoAsistencia, String> colEstado;

    private final ObservableList<AlumnoAsistencia> listaAlumnos = FXCollections.observableArrayList();
    private FilteredList<AlumnoAsistencia> listaFiltrada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarEventos();
        cargarGrupos();
        dpFecha.setValue(LocalDate.now());
        actualizarResumen();
    }

    private int getIdMaestroActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTabla() {
        colNoControl.setCellValueFactory(new PropertyValueFactory<>("numControl"));
        colNombreAlumno.setCellValueFactory(new PropertyValueFactory<>("nombreAlumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colTurno.setCellValueFactory(new PropertyValueFactory<>("turno"));

        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());
        colEstado.setCellFactory(ComboBoxTableCell.forTableColumn("asistio", "falta"));
        colEstado.setOnEditCommit(event -> event.getRowValue().setEstado(event.getNewValue()));

        tablaAsistencia.setEditable(true);

        listaFiltrada = new FilteredList<>(listaAlumnos, p -> true);
        tablaAsistencia.setItems(listaFiltrada);
    }

    private void configurarEventos() {
        btnCargar.setOnAction(event -> cargarAlumnos());
        btnTodosAsistieron.setOnAction(event -> marcarTodosAsistieron());
        btnLimpiar.setOnAction(event -> limpiar());
        btnGuardarAsistencia.setOnAction(event -> guardarAsistencia());

        cbGrupo.setOnAction(event -> {
            GrupoItem grupo = cbGrupo.getValue();

            cbMateria.getItems().clear();
            cbMateria.setValue(null);
            listaAlumnos.clear();
            lblTotalAlumnos.setText("0");

            if (grupo != null) {
                txtTurno.setText(grupo.getTurno());
                cargarMaterias(grupo.getIdGrupoCiclo());
            } else {
                txtTurno.clear();
            }

            actualizarResumen();
        });

        cbMateria.setOnAction(event -> {
            listaAlumnos.clear();
            lblTotalAlumnos.setText("0");
            actualizarResumen();
        });

        dpFecha.setOnAction(event -> {
            listaAlumnos.clear();
            lblTotalAlumnos.setText("0");
            actualizarResumen();
        });

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
                ct.nombre as turno,
                ce.nombre as ciclo
                from carga c
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                where c.id_maestro=?
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                order by ce.nombre desc,g.semestre,g.nombre
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
                            rs.getString("turno"),
                            rs.getString("ciclo")
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
                m.nombre as materia,
                m.clave as clave,
                g.nombre as grupo,
                ct.nombre as turno,
                ce.nombre as ciclo
                from carga c
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
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
                            rs.getString("materia"),
                            rs.getString("clave"),
                            rs.getString("grupo"),
                            rs.getString("turno"),
                            rs.getString("ciclo")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar materias");
        }
    }

    private void cargarAlumnos() {
        MateriaItem materia = cbMateria.getValue();

        if (materia == null || dpFecha.getValue() == null) {
            mostrarError("selecciona grupo, materia y fecha");
            return;
        }

        listaAlumnos.clear();

        String sql = """
                select
                a.id_alumno,
                a.num_control,
                concat(a.nombre,' ',a.apellido_paterno,' ',a.apellido_materno) as nombre_alumno,
                concat(ga.nombre,' - ',cta.nombre) as grupo_admin,
                cta.nombre as turno_admin,
                ifnull(cea.nombre,'asistio') as estado
                from alumno_carga ac
                inner join alumno a on ac.id_alumno=a.id_alumno
                inner join grupo_ciclo gca on a.id_grupo_ciclo=gca.id_grupo_ciclo
                inner join grupo ga on gca.id_grupo=ga.id_grupo
                inner join cat_turno cta on ga.id_turno=cta.id_turno
                left join asistencia_sesion s on s.id_carga=? and s.fecha=?
                left join asistencia_detalle ad on ad.id_asistencia_sesion=s.id_asistencia_sesion
                and ad.id_alumno=a.id_alumno
                left join cat_estado_asistencia cea on ad.id_estado_asistencia=cea.id_estado_asistencia
                where ac.id_carga=?
                and ac.id_estatus_general=1
                and a.id_estatus_general=1
                order by a.apellido_paterno,a.apellido_materno,a.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, materia.getIdCarga());
            ps.setDate(2, Date.valueOf(dpFecha.getValue()));
            ps.setInt(3, materia.getIdCarga());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaAlumnos.add(new AlumnoAsistencia(
                            rs.getInt("id_alumno"),
                            rs.getString("num_control"),
                            rs.getString("nombre_alumno"),
                            rs.getString("grupo_admin"),
                            rs.getString("turno_admin"),
                            rs.getString("estado")
                    ));
                }
            }

            lblTotalAlumnos.setText(String.valueOf(listaAlumnos.size()));
            actualizarResumen();
            filtrarTabla();

            if (listaAlumnos.isEmpty()) {
                mostrarError("no hay alumnos inscritos en esta clase");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alumnos");
        }
    }

    private void guardarAsistencia() {
        MateriaItem materia = cbMateria.getValue();

        if (materia == null || dpFecha.getValue() == null || listaAlumnos.isEmpty()) {
            mostrarError("carga alumnos antes de guardar asistencia");
            return;
        }

        String sqlSesion = """
                insert into asistencia_sesion(id_carga,fecha)
                values(?,?)
                on duplicate key update fecha=values(fecha)
                """;

        String sqlBuscarSesion = """
                select id_asistencia_sesion
                from asistencia_sesion
                where id_carga=?
                and fecha=?
                limit 1
                """;

        String sqlDetalle = """
                insert into asistencia_detalle(
                id_asistencia_sesion,
                id_alumno,
                id_estado_asistencia
                )
                values(?,?,?)
                on duplicate key update
                id_estado_asistencia=values(id_estado_asistencia)
                """;

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try (PreparedStatement psSesion = con.prepareStatement(sqlSesion)) {
                psSesion.setInt(1, materia.getIdCarga());
                psSesion.setDate(2, Date.valueOf(dpFecha.getValue()));
                psSesion.executeUpdate();
            }

            int idSesion = 0;

            try (PreparedStatement psBuscar = con.prepareStatement(sqlBuscarSesion)) {
                psBuscar.setInt(1, materia.getIdCarga());
                psBuscar.setDate(2, Date.valueOf(dpFecha.getValue()));

                try (ResultSet rs = psBuscar.executeQuery()) {
                    if (rs.next()) {
                        idSesion = rs.getInt("id_asistencia_sesion");
                    }
                }
            }

            if (idSesion == 0) {
                throw new Exception("no se pudo obtener la sesion de asistencia");
            }

            try (PreparedStatement psDetalle = con.prepareStatement(sqlDetalle)) {
                for (AlumnoAsistencia alumno : listaAlumnos) {
                    psDetalle.setInt(1, idSesion);
                    psDetalle.setInt(2, alumno.getIdAlumno());
                    psDetalle.setInt(3, alumno.getEstado().equalsIgnoreCase("asistio") ? 1 : 0);
                    psDetalle.addBatch();
                }

                psDetalle.executeBatch();
            }

            con.commit();
            mostrarInfo("asistencia guardada correctamente");
            cargarAlumnos();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar asistencia");
        }
    }

    private void marcarTodosAsistieron() {
        for (AlumnoAsistencia alumno : listaAlumnos) {
            alumno.setEstado("asistio");
        }

        tablaAsistencia.refresh();
    }

    private void limpiar() {
        cbGrupo.setValue(null);
        cbMateria.getItems().clear();
        cbMateria.setValue(null);
        txtTurno.clear();
        dpFecha.setValue(LocalDate.now());
        txtBuscar.clear();
        listaAlumnos.clear();
        lblTotalAlumnos.setText("0");
        actualizarResumen();
    }

    private void filtrarTabla() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase();

        listaFiltrada.setPredicate(alumno ->
                alumno.getNumControl().toLowerCase().contains(texto) ||
                        alumno.getNombreAlumno().toLowerCase().contains(texto) ||
                        alumno.getGrupo().toLowerCase().contains(texto) ||
                        alumno.getTurno().toLowerCase().contains(texto) ||
                        alumno.getEstado().toLowerCase().contains(texto)
        );
    }

    private void actualizarResumen() {
        GrupoItem grupo = cbGrupo.getValue();
        MateriaItem materia = cbMateria.getValue();

        lblFechaSeleccionada.setText(dpFecha.getValue() == null ? "sin fecha seleccionada" : dpFecha.getValue().toString());
        lblGrupoSeleccionado.setText(grupo == null ? "sin grupo" : grupo.getNombre());
        lblTurnoSeleccionado.setText(grupo == null ? "sin turno" : grupo.getTurno());
        lblMateriaSeleccionada.setText(materia == null ? "sin materia" : materia.getNombre());
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
        private final String ciclo;

        public GrupoItem(int idGrupoCiclo, String nombre, int semestre, String turno, String ciclo) {
            this.idGrupoCiclo = idGrupoCiclo;
            this.nombre = nombre;
            this.semestre = semestre;
            this.turno = turno;
            this.ciclo = ciclo;
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

        public String getCiclo() {
            return ciclo;
        }

        @Override
        public String toString() {
            return nombre + " - " + turno + " - " + ciclo;
        }
    }

    public static class MateriaItem {
        private final int idCarga;
        private final String nombre;
        private final String clave;
        private final String grupo;
        private final String turno;
        private final String ciclo;

        public MateriaItem(int idCarga, String nombre, String clave, String grupo, String turno, String ciclo) {
            this.idCarga = idCarga;
            this.nombre = nombre;
            this.clave = clave;
            this.grupo = grupo;
            this.turno = turno;
            this.ciclo = ciclo;
        }

        public int getIdCarga() {
            return idCarga;
        }

        public String getNombre() {
            return nombre;
        }

        public String getClave() {
            return clave;
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

        @Override
        public String toString() {
            return clave + " - " + nombre;
        }
    }

    public static class AlumnoAsistencia {
        private final int idAlumno;
        private final String numControl;
        private final String nombreAlumno;
        private final String grupo;
        private final String turno;
        private final SimpleStringProperty estado;

        public AlumnoAsistencia(int idAlumno, String numControl, String nombreAlumno, String grupo, String turno, String estado) {
            this.idAlumno = idAlumno;
            this.numControl = numControl;
            this.nombreAlumno = nombreAlumno;
            this.grupo = grupo;
            this.turno = turno;
            this.estado = new SimpleStringProperty(estado == null ? "asistio" : estado);
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

        public String getTurno() {
            return turno;
        }

        public String getEstado() {
            return estado.get();
        }

        public void setEstado(String estado) {
            this.estado.set(estado);
        }

        public SimpleStringProperty estadoProperty() {
            return estado;
        }
    }
}