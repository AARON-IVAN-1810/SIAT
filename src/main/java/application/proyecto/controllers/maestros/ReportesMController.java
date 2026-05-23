package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionAlerta;
import application.proyecto.utils.SesionUsuario;
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

public class ReportesMController extends BaseController {

    @FXML private TextField txtBuscarSuperior;

    @FXML private ComboBox<MateriaItem> cbMateria;
    @FXML private ComboBox<GrupoItem> cbGrupo;
    @FXML private TextField txtTurno;
    @FXML private ComboBox<AlumnoItem> cbAlumno;
    @FXML private ComboBox<TipoAlertaItem> cbMotivo;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnGenerarReporte;

    @FXML private ComboBox<String> cbFiltroMotivo;
    @FXML private ComboBox<String> cbFiltroEstado;
    @FXML private TextField txtBuscarTabla;

    @FXML private TableView<ReporteItem> tablaReportes;
    @FXML private TableColumn<ReporteItem, String> colNoControl;
    @FXML private TableColumn<ReporteItem, String> colNombre;
    @FXML private TableColumn<ReporteItem, String> colGrupo;
    @FXML private TableColumn<ReporteItem, String> colTurno;
    @FXML private TableColumn<ReporteItem, String> colMateria;
    @FXML private TableColumn<ReporteItem, String> colMotivo;
    @FXML private TableColumn<ReporteItem, String> colDescripcion;
    @FXML private TableColumn<ReporteItem, String> colEstado;

    private final ObservableList<ReporteItem> listaReportes = FXCollections.observableArrayList();
    private FilteredList<ReporteItem> listaFiltrada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        cargarGrupos();
        cargarTiposAlerta();
        cargarReportes();
        cargarAlertaSeleccionada();
    }

    private int getIdMaestroActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTabla() {
        colNoControl.setCellValueFactory(new PropertyValueFactory<>("numControl"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreAlumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colTurno.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        listaFiltrada = new FilteredList<>(listaReportes, p -> true);
        tablaReportes.setItems(listaFiltrada);
    }

    private void configurarCombos() {
        cbFiltroMotivo.setItems(FXCollections.observableArrayList(
                "todos",
                "asistencia",
                "actividad",
                "conducta"
        ));

        cbFiltroEstado.setItems(FXCollections.observableArrayList(
                "todos",
                "pendiente",
                "seguimiento",
                "cerrado"
        ));

        cbFiltroMotivo.setValue("todos");
        cbFiltroEstado.setValue("todos");
    }

    private void configurarEventos() {
        cbGrupo.setOnAction(event -> {
            GrupoItem grupo = cbGrupo.getValue();

            cbMateria.getItems().clear();
            cbMateria.setValue(null);
            cbAlumno.getItems().clear();
            cbAlumno.setValue(null);

            if (grupo != null) {
                txtTurno.setText(grupo.getTurno());
                cargarMaterias(grupo.getIdGrupoCiclo());
                cargarAlumnos(grupo.getIdGrupoCiclo());
            } else {
                txtTurno.clear();
            }
        });

        btnGenerarReporte.setOnAction(event -> handleGenerarReporte());

        txtBuscarTabla.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cbFiltroMotivo.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cbFiltroEstado.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void cargarGrupos() {
        cbGrupo.getItems().clear();

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

            ps.setInt(1, getIdMaestroActual());

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

            ps.setInt(1, getIdMaestroActual());
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

    private void cargarAlumnos(int idGrupoCiclo) {
        cbAlumno.getItems().clear();

        String sql = """
                select
                id_alumno,
                num_control,
                concat(nombre,' ',apellido_paterno,' ',apellido_materno) as alumno
                from alumno
                where id_grupo_ciclo=?
                and id_estatus_general=1
                order by apellido_paterno,apellido_materno,nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idGrupoCiclo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbAlumno.getItems().add(new AlumnoItem(
                            rs.getInt("id_alumno"),
                            rs.getString("num_control"),
                            rs.getString("alumno")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alumnos");
        }
    }

    private void cargarTiposAlerta() {
        cbMotivo.getItems().clear();

        String sql = """
                select id_tipo_alerta,nombre
                from cat_tipo_alerta
                where nombre in ('asistencia','actividad','conducta')
                order by id_tipo_alerta
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbMotivo.getItems().add(new TipoAlertaItem(
                        rs.getInt("id_tipo_alerta"),
                        rs.getString("nombre")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar motivos");
        }
    }

    private void cargarAlertaSeleccionada() {
        if (SesionAlerta.getIdAlerta() == 0) {
            return;
        }

        GrupoItem grupoEncontrado = null;

        for (GrupoItem grupo : cbGrupo.getItems()) {
            if (grupo.getNombre().equalsIgnoreCase(SesionAlerta.getGrupo())) {
                grupoEncontrado = grupo;
                break;
            }
        }

        if (grupoEncontrado == null) {
            mostrarError("no se encontro el grupo de la alerta seleccionada");
            return;
        }

        cbGrupo.setValue(grupoEncontrado);
        txtTurno.setText(grupoEncontrado.getTurno());

        cargarMaterias(grupoEncontrado.getIdGrupoCiclo());
        cargarAlumnos(grupoEncontrado.getIdGrupoCiclo());

        seleccionarMateriaPorIdCarga(SesionAlerta.getIdCarga());
        seleccionarAlumnoPorId(SesionAlerta.getIdAlumno());
        seleccionarMotivoPorNombre(SesionAlerta.getMotivo());
    }

    private void seleccionarGrupoPorNombre(String grupoNombre) {
        for (GrupoItem grupo : cbGrupo.getItems()) {
            if (grupo.getNombre().equalsIgnoreCase(grupoNombre)) {
                cbGrupo.setValue(grupo);
                return;
            }
        }
    }

    private void seleccionarMateriaPorIdCarga(int idCarga) {
        for (MateriaItem materia : cbMateria.getItems()) {
            if (materia.getIdCarga() == idCarga) {
                cbMateria.setValue(materia);
                return;
            }
        }
    }

    private void seleccionarAlumnoPorId(int idAlumno) {
        for (AlumnoItem alumno : cbAlumno.getItems()) {
            if (alumno.getIdAlumno() == idAlumno) {
                cbAlumno.setValue(alumno);
                return;
            }
        }
    }

    private void seleccionarMotivoPorNombre(String motivoNombre) {
        for (TipoAlertaItem motivo : cbMotivo.getItems()) {
            if (motivo.getNombre().equalsIgnoreCase(motivoNombre)) {
                cbMotivo.setValue(motivo);
                return;
            }
        }
    }

    @FXML
    private void handleGenerarReporte() {
        MateriaItem materia = cbMateria.getValue();
        GrupoItem grupo = cbGrupo.getValue();
        AlumnoItem alumno = cbAlumno.getValue();
        TipoAlertaItem motivo = cbMotivo.getValue();
        String descripcion = txtDescripcion.getText() == null ? "" : txtDescripcion.getText().trim();

        if (materia == null || grupo == null || alumno == null || motivo == null || descripcion.isEmpty()) {
            mostrarError("captura materia, grupo, alumno, motivo y descripcion");
            return;
        }

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            int idAlertaParaReporte = 0;

            if (SesionAlerta.getIdAlerta() > 0) {
                idAlertaParaReporte = SesionAlerta.getIdAlerta();
            } else if (motivo.getNombre().equalsIgnoreCase("conducta")) {
                idAlertaParaReporte = insertarAlertaConducta(con, alumno, materia, motivo);
            }

            insertarReporte(con, idAlertaParaReporte, alumno, materia, motivo, descripcion);

            if (SesionAlerta.getIdAlerta() > 0) {
                actualizarAlertaSeguimiento(con, SesionAlerta.getIdAlerta());
            }

            con.commit();

            mostrarInfo("reporte generado correctamente");
            limpiarFormulario();
            SesionAlerta.limpiar();
            cargarReportes();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al generar reporte");
        }
    }

    private int insertarAlertaConducta(Connection con, AlumnoItem alumno, MateriaItem materia, TipoAlertaItem motivo) throws Exception {
        String sql = """
                insert into alerta(
                id_alumno,
                id_carga,
                id_tipo_alerta,
                id_prioridad_alerta,
                motivo,
                id_estatus_alerta
                )
                values(?,?,?,?,?,1)
                """;

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, alumno.getIdAlumno());
            ps.setInt(2, materia.getIdCarga());
            ps.setInt(3, motivo.getIdTipoAlerta());
            ps.setInt(4, 2);
            ps.setString(5, "un maestro ha detectado una conducta inusual en el alumno");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new Exception("no se pudo generar la alerta de conducta");
    }

    private void insertarReporte(Connection con, int idAlerta, AlumnoItem alumno, MateriaItem materia, TipoAlertaItem motivo, String descripcion) throws Exception {
        String sql = """
                insert into reporte_docente(
                id_alerta,
                id_alumno,
                id_carga,
                id_tipo_alerta,
                descripcion,
                id_estatus_reporte_docente
                )
                values(?,?,?,?,?,1)
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (idAlerta > 0) {
                ps.setInt(1, idAlerta);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }

            ps.setInt(2, alumno.getIdAlumno());
            ps.setInt(3, materia.getIdCarga());
            ps.setInt(4, motivo.getIdTipoAlerta());
            ps.setString(5, descripcion);
            ps.executeUpdate();
        }
    }

    private void actualizarAlertaSeguimiento(Connection con, int idAlerta) throws Exception {
        String sql = """
                update alerta
                set id_estatus_alerta=1
                where id_alerta=?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAlerta);
            ps.executeUpdate();
        }
    }

    private void cargarReportes() {
        listaReportes.clear();

        String sql = """
                select
                al.num_control,
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as nombre_alumno,
                g.nombre as grupo,
                ct.nombre as turno,
                m.nombre as materia,
                cta.nombre as motivo,
                rd.descripcion,
                cer.nombre as estado
                from reporte_docente rd
                inner join alumno al on rd.id_alumno=al.id_alumno
                inner join carga c on rd.id_carga=c.id_carga
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join cat_tipo_alerta cta on rd.id_tipo_alerta=cta.id_tipo_alerta
                inner join cat_estatus_reporte_docente cer on rd.id_estatus_reporte_docente=cer.id_estatus_reporte_docente
                where c.id_maestro=?
                order by rd.id_reporte_docente desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, getIdMaestroActual());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaReportes.add(new ReporteItem(
                            rs.getString("num_control"),
                            rs.getString("nombre_alumno"),
                            rs.getString("grupo"),
                            rs.getString("turno"),
                            rs.getString("materia"),
                            rs.getString("motivo"),
                            rs.getString("descripcion"),
                            rs.getString("estado")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar reportes");
        }
    }

    private void aplicarFiltro() {
        String texto = txtBuscarTabla.getText() == null
                ? ""
                : txtBuscarTabla.getText().toLowerCase().trim();

        String motivo = cbFiltroMotivo.getValue() == null
                ? "todos"
                : cbFiltroMotivo.getValue().toLowerCase().trim();

        String estado = cbFiltroEstado.getValue() == null
                ? "todos"
                : cbFiltroEstado.getValue().toLowerCase().trim();

        listaFiltrada.setPredicate(reporte -> {
            boolean coincideTexto =
                    reporte.getNumControl().toLowerCase().contains(texto) ||
                            reporte.getNombreAlumno().toLowerCase().contains(texto) ||
                            reporte.getGrupo().toLowerCase().contains(texto) ||
                            reporte.getTurno().toLowerCase().contains(texto) ||
                            reporte.getMateria().toLowerCase().contains(texto) ||
                            reporte.getMotivo().toLowerCase().contains(texto) ||
                            reporte.getDescripcion().toLowerCase().contains(texto) ||
                            reporte.getEstado().toLowerCase().contains(texto);

            boolean coincideMotivo =
                    motivo.equals("todos") ||
                            reporte.getMotivo().toLowerCase().equals(motivo);

            boolean coincideEstado =
                    estado.equals("todos") ||
                            reporte.getEstado().toLowerCase().equals(estado);

            return coincideTexto && coincideMotivo && coincideEstado;
        });
    }

    private void limpiarFormulario() {
        cbMateria.setValue(null);
        cbGrupo.setValue(null);
        cbAlumno.setValue(null);
        cbMotivo.setValue(null);
        txtTurno.clear();
        txtDescripcion.clear();
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

        public int getIdGrupoCiclo() { return idGrupoCiclo; }
        public String getNombre() { return nombre; }
        public String getTurno() { return turno; }

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

        public int getIdCarga() { return idCarga; }

        @Override
        public String toString() {
            return nombre;
        }
    }

    public static class AlumnoItem {
        private final int idAlumno;
        private final String numControl;
        private final String nombre;

        public AlumnoItem(int idAlumno, String numControl, String nombre) {
            this.idAlumno = idAlumno;
            this.numControl = numControl;
            this.nombre = nombre;
        }

        public int getIdAlumno() { return idAlumno; }

        @Override
        public String toString() {
            return numControl + " - " + nombre;
        }
    }

    public static class TipoAlertaItem {
        private final int idTipoAlerta;
        private final String nombre;

        public TipoAlertaItem(int idTipoAlerta, String nombre) {
            this.idTipoAlerta = idTipoAlerta;
            this.nombre = nombre;
        }

        public int getIdTipoAlerta() { return idTipoAlerta; }
        public String getNombre() { return nombre; }

        @Override
        public String toString() {
            return nombre;
        }
    }

    public static class ReporteItem {
        private final String numControl;
        private final String nombreAlumno;
        private final String grupo;
        private final String turno;
        private final String materia;
        private final String motivo;
        private final String descripcion;
        private final String estado;

        public ReporteItem(String numControl, String nombreAlumno, String grupo, String turno, String materia, String motivo, String descripcion, String estado) {
            this.numControl = numControl;
            this.nombreAlumno = nombreAlumno;
            this.grupo = grupo;
            this.turno = turno;
            this.materia = materia;
            this.motivo = motivo;
            this.descripcion = descripcion;
            this.estado = estado;
        }

        public String getNumControl() { return numControl; }
        public String getNombreAlumno() { return nombreAlumno; }
        public String getGrupo() { return grupo; }
        public String getTurno() { return turno; }
        public String getMateria() { return materia; }
        public String getMotivo() { return motivo; }
        public String getDescripcion() { return descripcion; }
        public String getEstado() { return estado; }
    }
}