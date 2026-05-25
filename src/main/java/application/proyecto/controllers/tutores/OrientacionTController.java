package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionOrientacion;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class OrientacionTController extends BaseController {

    @FXML private ComboBox<GrupoItem> cmbGrupo;
    @FXML private ComboBox<AlumnoItem> cmbAlumno;

    @FXML private TextField txtSemestre;
    @FXML private TextField txtTurno;
    @FXML private TextField txtMateria;
    @FXML private TextField txtTipoAlerta;
    @FXML private TextField txtPrioridad;
    @FXML private TextField txtFechaAlerta;
    @FXML private TextField txtFechaOrientacion;

    @FXML private ComboBox<TipoIntervencionItem> cmbTipoIntervencion;

    @FXML private TextArea txtAcuerdos;
    @FXML private TextArea txtDescripcionOrientacion;

    @FXML private Button btnRegistrarIntervencion;
    @FXML private Button btnVerAlertas;

    @FXML private TableView<Intervencion> tablaIntervencionesTutor;

    @FXML private TableColumn<Intervencion, String> colAlumnoIntervencion;
    @FXML private TableColumn<Intervencion, String> colGrupoIntervencion;
    @FXML private TableColumn<Intervencion, String> colFechaIntervencion;
    @FXML private TableColumn<Intervencion, String> colTipoIntervencion;
    @FXML private TableColumn<Intervencion, String> colAcuerdosIntervencion;
    @FXML private TableColumn<Intervencion, String> colDescripcionIntervencion;

    @FXML private ListView<String> listBitacoraTutor;

    private final ObservableList<Intervencion> listaIntervenciones = FXCollections.observableArrayList();

    private int idAlumnoActual = 0;
    private int idGrupoCicloActual = 0;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarEventos();
        cargarTiposIntervencion();
        cargarGruposTutor();

        txtFechaOrientacion.setText(LocalDate.now().toString());

        cargarDatosSesionOrientacion();
    }

    private int getIdTutorActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTabla() {
        colAlumnoIntervencion.setCellValueFactory(new PropertyValueFactory<>("alumno"));
        colGrupoIntervencion.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colFechaIntervencion.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colTipoIntervencion.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colAcuerdosIntervencion.setCellValueFactory(new PropertyValueFactory<>("acuerdos"));
        colDescripcionIntervencion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        tablaIntervencionesTutor.setItems(listaIntervenciones);
    }

    private void configurarEventos() {
        cmbGrupo.setOnAction(event -> {
            cargarAlumnosPorGrupo();
            limpiarDatosAlumnoManual();
        });

        cmbAlumno.setOnAction(event -> cargarDatosAlumnoSeleccionado());

        btnRegistrarIntervencion.setOnAction(event -> handleRegistrarIntervencion());
        btnVerAlertas.setOnAction(event -> handleVerAlertas());
    }

    private void cargarTiposIntervencion() {
        cmbTipoIntervencion.getItems().clear();

        String sql = """
                select id_tipo_intervencion,nombre
                from cat_tipo_intervencion
                order by id_tipo_intervencion
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cmbTipoIntervencion.getItems().add(new TipoIntervencionItem(
                        rs.getInt("id_tipo_intervencion"),
                        rs.getString("nombre")
                ));
            }

            if (!cmbTipoIntervencion.getItems().isEmpty()) {
                cmbTipoIntervencion.setValue(cmbTipoIntervencion.getItems().get(0));
            }

        } catch (Exception e) {
            e.printStackTrace();
            cargarTiposIntervencionRespaldo();
        }
    }

    private void cargarTiposIntervencionRespaldo() {
        cmbTipoIntervencion.setItems(FXCollections.observableArrayList(
                new TipoIntervencionItem(1, "entrevista"),
                new TipoIntervencionItem(2, "llamada"),
                new TipoIntervencionItem(3, "asesoria"),
                new TipoIntervencionItem(4, "seguimiento academico"),
                new TipoIntervencionItem(5, "orientacion personal")
        ));

        cmbTipoIntervencion.setValue(cmbTipoIntervencion.getItems().get(0));
    }

    private void cargarGruposTutor() {
        cmbGrupo.getItems().clear();
        cmbAlumno.getItems().clear();

        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        String sql = """
                select distinct
                gc.id_grupo_ciclo,
                g.nombre as grupo,
                g.semestre,
                ct.nombre as turno,
                ce.nombre as ciclo
                from tutoria_asignacion ta
                inner join grupo_ciclo gc on ta.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                order by ciclo desc,semestre,grupo
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cmbGrupo.getItems().add(new GrupoItem(
                            rs.getInt("id_grupo_ciclo"),
                            rs.getString("grupo"),
                            rs.getString("semestre"),
                            rs.getString("turno"),
                            rs.getString("ciclo")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos tutorados");
        }
    }

    private void cargarAlumnosPorGrupo() {
        cmbAlumno.getItems().clear();

        GrupoItem grupo = cmbGrupo.getValue();

        if (grupo == null) {
            idGrupoCicloActual = 0;
            return;
        }

        idGrupoCicloActual = grupo.getIdGrupoCiclo();

        String sql = """
                select distinct
                al.id_alumno,
                al.num_control,
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as alumno
                from alumno al
                left join alumno_carga ac on al.id_alumno=ac.id_alumno
                and ac.id_estatus_general=1
                left join carga c on ac.id_carga=c.id_carga
                and c.id_estatus_general=1
                where al.id_estatus_general=1
                and (
                    al.id_grupo_ciclo=?
                    or c.id_grupo_ciclo=?
                )
                order by alumno
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, grupo.getIdGrupoCiclo());
            ps.setInt(2, grupo.getIdGrupoCiclo());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cmbAlumno.getItems().add(new AlumnoItem(
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

    private void cargarDatosSesionOrientacion() {
        if (SesionOrientacion.getIdAlumno() == 0) {
            return;
        }

        idAlumnoActual = SesionOrientacion.getIdAlumno();

        GrupoItem grupoSesion = buscarGrupoPorNombre(SesionOrientacion.getGrupo());

        if (grupoSesion != null) {
            cmbGrupo.setValue(grupoSesion);
            idGrupoCicloActual = grupoSesion.getIdGrupoCiclo();
            cargarAlumnosPorGrupo();
        }

        seleccionarAlumnoPorId(idAlumnoActual);

        if (cmbAlumno.getValue() == null) {
            cmbAlumno.setValue(new AlumnoItem(
                    idAlumnoActual,
                    SesionOrientacion.getNumControl(),
                    SesionOrientacion.getNombreAlumno()
            ));
        }

        txtSemestre.setText(textoSeguro(SesionOrientacion.getSemestre()));
        txtTurno.setText(textoSeguro(SesionOrientacion.getTurno()));
        txtMateria.setText(textoSeguro(SesionOrientacion.getMateria()));
        txtTipoAlerta.setText(textoSeguro(SesionOrientacion.getTipoAlerta()));
        txtPrioridad.setText(textoSeguro(SesionOrientacion.getPrioridad()));
        txtFechaAlerta.setText(textoSeguro(SesionOrientacion.getFechaAlerta()));

        cargarIntervencionesAlumno();
        cargarBitacoraAlumno();
    }

    private GrupoItem buscarGrupoPorNombre(String nombreGrupo) {
        if (nombreGrupo == null) {
            return null;
        }

        String buscado = nombreGrupo.toLowerCase().trim();

        for (GrupoItem grupo : cmbGrupo.getItems()) {
            String textoGrupo = grupo.toString().toLowerCase().trim();

            if (textoGrupo.equals(buscado) || textoGrupo.contains(buscado) || buscado.contains(textoGrupo)) {
                return grupo;
            }
        }

        return null;
    }

    private void seleccionarAlumnoPorId(int idAlumno) {
        for (AlumnoItem alumno : cmbAlumno.getItems()) {
            if (alumno.getIdAlumno() == idAlumno) {
                cmbAlumno.setValue(alumno);
                return;
            }
        }
    }

    private void cargarDatosAlumnoSeleccionado() {
        AlumnoItem alumno = cmbAlumno.getValue();
        GrupoItem grupo = cmbGrupo.getValue();

        if (alumno == null) {
            return;
        }

        idAlumnoActual = alumno.getIdAlumno();

        if (grupo != null) {
            idGrupoCicloActual = grupo.getIdGrupoCiclo();
            txtSemestre.setText(grupo.getSemestre());
            txtTurno.setText(grupo.getTurno());
        }

        if (SesionOrientacion.getIdAlumno() == 0) {
            txtMateria.setText("");
            txtTipoAlerta.setText("");
            txtPrioridad.setText("");
            txtFechaAlerta.setText("");
        }

        cargarIntervencionesAlumno();
        cargarBitacoraAlumno();
    }

    private void limpiarDatosAlumnoManual() {
        idAlumnoActual = 0;
        cmbAlumno.setValue(null);

        if (SesionOrientacion.getIdAlumno() == 0) {
            txtSemestre.clear();
            txtTurno.clear();
            txtMateria.clear();
            txtTipoAlerta.clear();
            txtPrioridad.clear();
            txtFechaAlerta.clear();
            listaIntervenciones.clear();
            listBitacoraTutor.getItems().clear();
        }
    }

    @FXML
    private void handleRegistrarIntervencion() {
        if (idAlumnoActual == 0) {
            mostrarError("selecciona un alumno");
            return;
        }

        TipoIntervencionItem tipo = cmbTipoIntervencion.getValue();

        if (tipo == null) {
            mostrarError("selecciona el tipo de intervencion");
            return;
        }

        String acuerdos = txtAcuerdos.getText() == null ? "" : txtAcuerdos.getText().trim();
        String descripcion = txtDescripcionOrientacion.getText() == null ? "" : txtDescripcionOrientacion.getText().trim();
        String motivoSituacion = txtTipoAlerta.getText() == null || txtTipoAlerta.getText().trim().isEmpty()
                ? "seguimiento del alumno"
                : txtTipoAlerta.getText().trim();

        if (acuerdos.isEmpty() || descripcion.isEmpty()) {
            mostrarError("completa los campos de acuerdos y descripcion");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("confirmar");
        confirmacion.setHeaderText("registrar intervencion");
        confirmacion.setContentText("deseas registrar esta intervencion?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String sql = """
                insert into intervencion_tutor(
                id_alerta,
                id_reporte_docente,
                id_maestro_tutor,
                id_alumno,
                id_tipo_intervencion,
                fecha_intervencion,
                motivo_situacion,
                acuerdos,
                id_resultado_intervencion,
                observaciones
                )
                values(?,?,?,?,?,?,?,?,?,?)
                """;

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (SesionOrientacion.getIdAlerta() > 0) {
                    ps.setInt(1, SesionOrientacion.getIdAlerta());
                } else {
                    ps.setNull(1, java.sql.Types.INTEGER);
                }

                if (SesionOrientacion.getIdReporteDocente() > 0) {
                    ps.setInt(2, SesionOrientacion.getIdReporteDocente());
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }

                ps.setInt(3, getIdTutorActual());
                ps.setInt(4, idAlumnoActual);
                ps.setInt(5, tipo.getIdTipoIntervencion());
                ps.setDate(6, java.sql.Date.valueOf(obtenerFechaOrientacion()));
                ps.setString(7, motivoSituacion);
                ps.setString(8, acuerdos);
                ps.setInt(9, 1);
                ps.setString(10, descripcion);

                ps.executeUpdate();
            }

            cerrarAlertaSiExiste(con);
            cerrarReporteSiExiste(con);

            con.commit();

            mostrarInfo("intervencion registrada correctamente");

            txtAcuerdos.clear();
            txtDescripcionOrientacion.clear();

            cargarIntervencionesAlumno();
            cargarBitacoraAlumno();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al registrar intervencion");
        }
    }

    private LocalDate obtenerFechaOrientacion() {
        try {
            if (txtFechaOrientacion.getText() != null && !txtFechaOrientacion.getText().trim().isEmpty()) {
                return LocalDate.parse(txtFechaOrientacion.getText().trim());
            }
        } catch (Exception ignored) {
        }

        return LocalDate.now();
    }

    @FXML
    private void handleVerAlertas() {
        mostrarInfo("regresa al modulo de gestion de alertas");
    }

    private void cargarIntervencionesAlumno() {
        listaIntervenciones.clear();

        if (idAlumnoActual == 0) {
            return;
        }

        String sql = """
                select
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as alumno,
                coalesce(
                    concat(gcl.nombre,' - ',ctcl.nombre,' - ',cecl.nombre,' | ',m.nombre),
                    concat(gad.nombre,' - ',ctad.nombre,' - ',cead.nombre),
                    'sin grupo'
                ) as grupo,
                date(it.fecha_intervencion) as fecha,
                cti.nombre as tipo_intervencion,
                it.acuerdos,
                it.observaciones
                from intervencion_tutor it
                inner join alumno al on it.id_alumno=al.id_alumno
                inner join cat_tipo_intervencion cti on it.id_tipo_intervencion=cti.id_tipo_intervencion

                left join alerta aa on it.id_alerta=aa.id_alerta
                left join reporte_docente rd on it.id_reporte_docente=rd.id_reporte_docente
                left join carga c on c.id_carga=coalesce(aa.id_carga,rd.id_carga)
                left join materia m on c.id_materia=m.id_materia
                left join grupo_ciclo gccl on c.id_grupo_ciclo=gccl.id_grupo_ciclo
                left join grupo gcl on gccl.id_grupo=gcl.id_grupo
                left join cat_turno ctcl on gcl.id_turno=ctcl.id_turno
                left join ciclo_escolar cecl on gccl.id_ciclo_escolar=cecl.id_ciclo_escolar

                left join grupo_ciclo gcad on al.id_grupo_ciclo=gcad.id_grupo_ciclo
                left join grupo gad on gcad.id_grupo=gad.id_grupo
                left join cat_turno ctad on gad.id_turno=ctad.id_turno
                left join ciclo_escolar cead on gcad.id_ciclo_escolar=cead.id_ciclo_escolar

                where it.id_alumno=?
                and it.id_maestro_tutor=?
                order by it.fecha_intervencion desc,it.id_intervencion desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAlumnoActual);
            ps.setInt(2, getIdTutorActual());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaIntervenciones.add(new Intervencion(
                            rs.getString("alumno"),
                            rs.getString("grupo"),
                            rs.getString("fecha"),
                            rs.getString("tipo_intervencion"),
                            rs.getString("acuerdos"),
                            rs.getString("observaciones")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar intervenciones");
        }
    }

    private void cargarBitacoraAlumno() {
        listBitacoraTutor.getItems().clear();

        if (idAlumnoActual == 0) {
            return;
        }

        String sql = """
                select evento,fecha_evento
                from(
                    select
                    concat(
                    '[ALERTA] ',
                    cta.nombre,
                    ' | ',
                    concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre),
                    ' | ',
                    m.nombre,
                    ' | ',
                    ifnull(a.motivo,'sin motivo')
                    ) as evento,
                    a.creada_en as fecha_evento
                    from alerta a
                    inner join carga c on a.id_carga=c.id_carga
                    inner join materia m on c.id_materia=m.id_materia
                    inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                    inner join grupo g on gc.id_grupo=g.id_grupo
                    inner join cat_turno ct on g.id_turno=ct.id_turno
                    inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                    inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                    inner join tutoria_asignacion ta on c.id_grupo_ciclo=ta.id_grupo_ciclo
                    where a.id_alumno=?
                    and ta.id_maestro_tutor=?
                    and ta.id_estatus_tutoria=1

                    union all

                    select
                    concat(
                    '[REPORTE] ',
                    cta.nombre,
                    ' | ',
                    concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre),
                    ' | ',
                    m.nombre,
                    ' | ',
                    ifnull(rd.descripcion,'sin descripcion')
                    ) as evento,
                    rd.creado_en as fecha_evento
                    from reporte_docente rd
                    inner join carga c on rd.id_carga=c.id_carga
                    inner join materia m on c.id_materia=m.id_materia
                    inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                    inner join grupo g on gc.id_grupo=g.id_grupo
                    inner join cat_turno ct on g.id_turno=ct.id_turno
                    inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                    inner join cat_tipo_alerta cta on rd.id_tipo_alerta=cta.id_tipo_alerta
                    inner join tutoria_asignacion ta on c.id_grupo_ciclo=ta.id_grupo_ciclo
                    where rd.id_alumno=?
                    and ta.id_maestro_tutor=?
                    and ta.id_estatus_tutoria=1

                    union all

                    select
                    concat(
                    '[ORIENTACION] ',
                    cti.nombre,
                    ' | acuerdos: ',
                    ifnull(it.acuerdos,'sin acuerdos'),
                    ' | ',
                    ifnull(it.observaciones,'sin observaciones')
                    ) as evento,
                    it.fecha_intervencion as fecha_evento
                    from intervencion_tutor it
                    inner join cat_tipo_intervencion cti on it.id_tipo_intervencion=cti.id_tipo_intervencion
                    where it.id_alumno=?
                    and it.id_maestro_tutor=?
                ) eventos
                order by fecha_evento desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAlumnoActual);
            ps.setInt(2, getIdTutorActual());
            ps.setInt(3, idAlumnoActual);
            ps.setInt(4, getIdTutorActual());
            ps.setInt(5, idAlumnoActual);
            ps.setInt(6, getIdTutorActual());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listBitacoraTutor.getItems().add(
                            rs.getString("fecha_evento") + "  -  " + rs.getString("evento")
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar bitacora");
        }
    }

    private void cerrarAlertaSiExiste(Connection con) throws Exception {
        if (SesionOrientacion.getIdAlerta() <= 0) {
            return;
        }

        String sql = """
                update alerta a
                inner join carga c on a.id_carga=c.id_carga
                inner join tutoria_asignacion ta on c.id_grupo_ciclo=ta.id_grupo_ciclo
                set
                a.id_estatus_alerta=0,
                a.cerrada_en=now()
                where a.id_alerta=?
                and ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, SesionOrientacion.getIdAlerta());
            ps.setInt(2, getIdTutorActual());
            ps.executeUpdate();
        }
    }

    private void cerrarReporteSiExiste(Connection con) throws Exception {
        if (SesionOrientacion.getIdReporteDocente() <= 0) {
            return;
        }

        String sql = """
                update reporte_docente rd
                inner join carga c on rd.id_carga=c.id_carga
                inner join tutoria_asignacion ta on c.id_grupo_ciclo=ta.id_grupo_ciclo
                set rd.id_estatus_reporte_docente=0
                where rd.id_reporte_docente=?
                and ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, SesionOrientacion.getIdReporteDocente());
            ps.setInt(2, getIdTutorActual());
            ps.executeUpdate();
        }
    }

    private String textoSeguro(String valor) {
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

    public static class GrupoItem {
        private final int idGrupoCiclo;
        private final String nombre;
        private final String semestre;
        private final String turno;
        private final String ciclo;

        public GrupoItem(int idGrupoCiclo, String nombre, String semestre, String turno, String ciclo) {
            this.idGrupoCiclo = idGrupoCiclo;
            this.nombre = nombre == null ? "" : nombre;
            this.semestre = semestre == null ? "" : semestre;
            this.turno = turno == null ? "" : turno;
            this.ciclo = ciclo == null ? "" : ciclo;
        }

        public int getIdGrupoCiclo() {
            return idGrupoCiclo;
        }

        public String getSemestre() {
            return semestre;
        }

        public String getTurno() {
            return turno;
        }

        @Override
        public String toString() {
            return nombre + " - " + turno + " - " + ciclo;
        }
    }

    public static class AlumnoItem {
        private final int idAlumno;
        private final String numControl;
        private final String nombre;

        public AlumnoItem(int idAlumno, String numControl, String nombre) {
            this.idAlumno = idAlumno;
            this.numControl = numControl == null ? "" : numControl;
            this.nombre = nombre == null ? "" : nombre;
        }

        public int getIdAlumno() {
            return idAlumno;
        }

        @Override
        public String toString() {
            return numControl + " - " + nombre;
        }
    }

    public static class TipoIntervencionItem {
        private final int idTipoIntervencion;
        private final String nombre;

        public TipoIntervencionItem(int idTipoIntervencion, String nombre) {
            this.idTipoIntervencion = idTipoIntervencion;
            this.nombre = nombre == null ? "" : nombre;
        }

        public int getIdTipoIntervencion() {
            return idTipoIntervencion;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    public static class Intervencion {
        private final String alumno;
        private final String grupo;
        private final String fecha;
        private final String tipo;
        private final String acuerdos;
        private final String descripcion;

        public Intervencion(String alumno, String grupo, String fecha, String tipo, String acuerdos, String descripcion) {
            this.alumno = alumno == null ? "" : alumno;
            this.grupo = grupo == null ? "" : grupo;
            this.fecha = fecha == null ? "" : fecha;
            this.tipo = tipo == null ? "" : tipo;
            this.acuerdos = acuerdos == null ? "" : acuerdos;
            this.descripcion = descripcion == null ? "" : descripcion;
        }

        public String getAlumno() {
            return alumno;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getFecha() {
            return fecha;
        }

        public String getTipo() {
            return tipo;
        }

        public String getAcuerdos() {
            return acuerdos;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }
}