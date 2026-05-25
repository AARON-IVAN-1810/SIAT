package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Normalizer;

public class HistorialAlumnoMController extends BaseController {

    @FXML private ComboBox<ClaseItem> cbGrupo;
    @FXML private ComboBox<AlumnoItem> cbAlumno;
    @FXML private Button btnCargarHistorial;

    @FXML private Label lblTotalAlertas;
    @FXML private Label lblAlertasAsistencia;
    @FXML private Label lblAlertasActividad;
    @FXML private Label lblReportesGenerados;
    @FXML private Label lblEstadoActual;

    @FXML private Label lblDiagnostico;
    @FXML private Label lblCausasDetectadas;
    @FXML private Label lblRecomendacion;

    @FXML private TableView<AlertaItem> tablaAlertas;
    @FXML private TableColumn<AlertaItem, String> colFechaAlerta;
    @FXML private TableColumn<AlertaItem, String> colGrupoAlerta;
    @FXML private TableColumn<AlertaItem, String> colMateriaAlerta;
    @FXML private TableColumn<AlertaItem, String> colMotivoAlerta;
    @FXML private TableColumn<AlertaItem, String> colEstadoAlerta;

    @FXML private TableView<ReporteItem> tablaReportes;
    @FXML private TableColumn<ReporteItem, String> colFechaReporte;
    @FXML private TableColumn<ReporteItem, String> colGrupoReporte;
    @FXML private TableColumn<ReporteItem, String> colMateriaReporte;
    @FXML private TableColumn<ReporteItem, String> colMotivoReporte;
    @FXML private TableColumn<ReporteItem, String> colDescripcionReporte;
    @FXML private TableColumn<ReporteItem, String> colEstadoReporte;

    private final ObservableList<AlertaItem> listaAlertas = FXCollections.observableArrayList();
    private final ObservableList<ReporteItem> listaReportes = FXCollections.observableArrayList();

    private int totalAlertas = 0;
    private int alertasAsistencia = 0;
    private int alertasActividad = 0;
    private int reportesGenerados = 0;

    @FXML
    public void initialize() {
        configurarTablas();
        configurarEventos();
        cargarClases();
        limpiarDatos();
    }

    private int getIdMaestroActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTablas() {
        colFechaAlerta.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colGrupoAlerta.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMateriaAlerta.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colMotivoAlerta.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colEstadoAlerta.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colFechaReporte.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colGrupoReporte.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMateriaReporte.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colMotivoReporte.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colDescripcionReporte.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colEstadoReporte.setCellValueFactory(new PropertyValueFactory<>("estado"));

        tablaAlertas.setItems(listaAlertas);
        tablaReportes.setItems(listaReportes);
    }

    private void configurarEventos() {
        btnCargarHistorial.setOnAction(event -> cargarHistorial());

        cbGrupo.setOnAction(event -> {
            cargarAlumnosPorClase();
            limpiarDatos();
        });

        cbAlumno.setOnAction(event -> limpiarDatos());
    }

    private void cargarClases() {
        cbGrupo.getItems().clear();
        cbAlumno.getItems().clear();

        int idMaestro = getIdMaestroActual();

        if (idMaestro == 0) {
            mostrarError("no hay maestro en sesion");
            return;
        }

        String sql = """
                select
                c.id_carga,
                concat(m.clave,' - ',m.nombre,' | ',g.nombre,' - ',ct.nombre,' - ',ce.nombre) as clase
                from carga c
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                where c.id_maestro=?
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                order by ce.nombre desc,g.semestre,g.nombre,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbGrupo.getItems().add(new ClaseItem(
                            rs.getInt("id_carga"),
                            rs.getString("clase")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar clases");
        }
    }

    private void cargarAlumnosPorClase() {
        cbAlumno.getItems().clear();

        ClaseItem clase = cbGrupo.getValue();

        if (clase == null) {
            return;
        }

        String sql = """
                select
                al.id_alumno,
                al.num_control,
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as alumno
                from alumno_carga ac
                inner join alumno al on ac.id_alumno=al.id_alumno
                where ac.id_carga=?
                and ac.id_estatus_general=1
                and al.id_estatus_general=1
                order by al.apellido_paterno,al.apellido_materno,al.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, clase.getIdCarga());

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
            mostrarError("error al cargar alumnos de la clase");
        }
    }

    @FXML
    private void cargarHistorial() {
        ClaseItem clase = cbGrupo.getValue();
        AlumnoItem alumno = cbAlumno.getValue();

        if (clase == null) {
            mostrarError("selecciona una clase");
            return;
        }

        if (alumno == null) {
            mostrarError("selecciona un alumno");
            return;
        }

        cargarMetricas(alumno.getIdAlumno(), clase.getIdCarga());
        cargarAlertas(alumno.getIdAlumno(), clase.getIdCarga());
        cargarReportes(alumno.getIdAlumno(), clase.getIdCarga());
        generarDiagnostico();
    }

    private void cargarMetricas(int idAlumno, int idCarga) {
        String sqlAlertas = """
                select
                count(*) as total_alertas,
                coalesce(sum(case when lower(cta.nombre)='asistencia' then 1 else 0 end),0) as alertas_asistencia,
                coalesce(sum(case when lower(cta.nombre) in ('actividad','calificacion') then 1 else 0 end),0) as alertas_actividad
                from alerta a
                inner join carga c on a.id_carga=c.id_carga
                inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                where a.id_alumno=?
                and a.id_carga=?
                and c.id_maestro=?
                """;

        String sqlReportes = """
                select count(*) as total_reportes
                from reporte_docente rd
                inner join carga c on rd.id_carga=c.id_carga
                where rd.id_alumno=?
                and rd.id_carga=?
                and c.id_maestro=?
                """;

        try (Connection con = ConexionBD.conectar()) {
            try (PreparedStatement ps = con.prepareStatement(sqlAlertas)) {
                ps.setInt(1, idAlumno);
                ps.setInt(2, idCarga);
                ps.setInt(3, getIdMaestroActual());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalAlertas = rs.getInt("total_alertas");
                        alertasAsistencia = rs.getInt("alertas_asistencia");
                        alertasActividad = rs.getInt("alertas_actividad");
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlReportes)) {
                ps.setInt(1, idAlumno);
                ps.setInt(2, idCarga);
                ps.setInt(3, getIdMaestroActual());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        reportesGenerados = rs.getInt("total_reportes");
                    }
                }
            }

            lblTotalAlertas.setText(String.valueOf(totalAlertas));
            lblAlertasAsistencia.setText(String.valueOf(alertasAsistencia));
            lblAlertasActividad.setText(String.valueOf(alertasActividad));
            lblReportesGenerados.setText(String.valueOf(reportesGenerados));
            lblEstadoActual.setText(calcularEstado());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar metricas");
        }
    }

    private void cargarAlertas(int idAlumno, int idCarga) {
        listaAlertas.clear();

        String sql = """
                select
                date_format(a.creada_en,'%Y-%m-%d') as fecha,
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo,
                concat(m.nombre,' (',m.clave,')') as materia,
                cta.nombre as motivo,
                cea.nombre as estado
                from alerta a
                inner join carga c on a.id_carga=c.id_carga
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                inner join cat_estatus_alerta cea on a.id_estatus_alerta=cea.id_estatus_alerta
                where a.id_alumno=?
                and a.id_carga=?
                and c.id_maestro=?
                order by a.creada_en desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAlumno);
            ps.setInt(2, idCarga);
            ps.setInt(3, getIdMaestroActual());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaAlertas.add(new AlertaItem(
                            rs.getString("fecha"),
                            rs.getString("grupo"),
                            rs.getString("materia"),
                            rs.getString("motivo"),
                            rs.getString("estado")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alertas");
        }
    }

    private void cargarReportes(int idAlumno, int idCarga) {
        listaReportes.clear();

        String sql = """
                select
                date_format(rd.creado_en,'%Y-%m-%d') as fecha,
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo,
                concat(m.nombre,' (',m.clave,')') as materia,
                cta.nombre as motivo,
                rd.descripcion,
                cer.nombre as estado
                from reporte_docente rd
                inner join carga c on rd.id_carga=c.id_carga
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_tipo_alerta cta on rd.id_tipo_alerta=cta.id_tipo_alerta
                inner join cat_estatus_reporte_docente cer on rd.id_estatus_reporte_docente=cer.id_estatus_reporte_docente
                where rd.id_alumno=?
                and rd.id_carga=?
                and c.id_maestro=?
                order by rd.creado_en desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAlumno);
            ps.setInt(2, idCarga);
            ps.setInt(3, getIdMaestroActual());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaReportes.add(new ReporteItem(
                            rs.getString("fecha"),
                            rs.getString("grupo"),
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

    private String calcularEstado() {
        if (totalAlertas >= 5 || reportesGenerados >= 3) {
            return "riesgo alto";
        }

        if (totalAlertas >= 3 || reportesGenerados >= 2) {
            return "riesgo medio";
        }

        if (totalAlertas > 0 || reportesGenerados > 0) {
            return "riesgo bajo";
        }

        return "sin riesgo";
    }

    private void generarDiagnostico() {
        String problemaDominante;

        if (alertasAsistencia > alertasActividad) {
            problemaDominante = "asistencia";
        } else if (alertasActividad > alertasAsistencia) {
            problemaDominante = "actividad";
        } else if (totalAlertas > 0) {
            problemaDominante = "asistencia y actividad";
        } else {
            problemaDominante = "sin alertas registradas";
        }

        String textoReportes = obtenerTextoReportes();
        String causas = detectarCausas(textoReportes);

        lblDiagnostico.setText(
                "El alumno presenta un estado actual de " + calcularEstado() +
                        ". El problema dominante detectado es " + problemaDominante +
                        ". Tiene " + totalAlertas + " alertas registradas y " +
                        reportesGenerados + " reportes generados en esta clase."
        );

        lblCausasDetectadas.setText(causas);
        lblRecomendacion.setText(generarRecomendacion(problemaDominante, causas));
    }

    private String obtenerTextoReportes() {
        StringBuilder texto = new StringBuilder();

        for (ReporteItem reporte : listaReportes) {
            texto.append(" ").append(reporte.getDescripcion());
        }

        return normalizarTexto(texto.toString());
    }

    private String detectarCausas(String texto) {
        boolean salud = contiene(texto, "salud", "enfermedad", "enfermo", "enferma", "enfermedades", "medico", "medica", "hospital", "consulta", "incapacidad", "dolor", "tratamiento", "cita medica", "malestar");
        boolean economico = contiene(texto, "economico", "economica", "economicos", "economicas", "dinero", "pago", "pagos", "deuda", "deudas", "beca", "trabajo", "empleo", "trabajar", "laboral", "sueldo", "salario");
        boolean familia = contiene(texto, "familia", "familiar", "casa", "hogar", "mama", "papa", "padre", "madre", "hermano", "hermana", "hermanos", "cuidado", "cuidar", "fallecimiento", "problemas familiares");
        boolean transporte = contiene(texto, "transporte", "camion", "ruta", "pasaje", "traslado", "distancia", "lejos", "llegar tarde", "trafico", "carro", "gasolina");
        boolean conducta = contiene(texto, "conducta", "comportamiento", "disciplina", "respeto", "conflicto", "agresion", "pelea", "falta de respeto", "actitud", "reporte conductual");

        StringBuilder causas = new StringBuilder();

        if (salud) causas.append("salud, ");
        if (economico) causas.append("situacion economica o laboral, ");
        if (familia) causas.append("situacion familiar, ");
        if (transporte) causas.append("transporte o traslado, ");
        if (conducta) causas.append("conducta, ");

        if (causas.isEmpty()) {
            return "No se detecto una causa especifica en los reportes. Se recomienda revisar las descripciones y dar seguimiento preventivo.";
        }

        return "Posibles causas detectadas: " + causas.substring(0, causas.length() - 2) + ".";
    }

    private String generarRecomendacion(String problemaDominante, String causas) {
        if (causas.contains("salud")) {
            return "Se recomienda verificar si las faltas tienen justificacion medica y mantener seguimiento preventivo con el alumno.";
        }

        if (causas.contains("economica") || causas.contains("laboral")) {
            return "Se recomienda revisar si la situacion economica o laboral esta afectando la asistencia o entrega de actividades.";
        }

        if (causas.contains("familiar")) {
            return "Se recomienda canalizar el caso a tutoria para conocer si existe una situacion familiar que afecte el desempeno.";
        }

        if (causas.contains("transporte")) {
            return "Se recomienda revisar si los horarios o el traslado estan afectando la asistencia del alumno.";
        }

        if (causas.contains("conducta")) {
            return "Se recomienda documentar el seguimiento conductual y mantener comunicacion con tutoria.";
        }

        if (problemaDominante.equals("asistencia")) {
            return "Se recomienda priorizar el seguimiento de asistencia y revisar patrones de faltas repetidas.";
        }

        if (problemaDominante.equals("actividad")) {
            return "Se recomienda revisar entregas pendientes y establecer acuerdos de recuperacion.";
        }

        return "Se recomienda mantener monitoreo del alumno y actualizar el historial cuando se generen nuevos reportes.";
    }

    private boolean contiene(String texto, String... palabras) {
        for (String palabra : palabras) {
            if (texto.contains(normalizarTexto(palabra))) {
                return true;
            }
        }

        return false;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        String limpio = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD);
        return limpio.replaceAll("\\p{M}", "");
    }

    private void limpiarDatos() {
        totalAlertas = 0;
        alertasAsistencia = 0;
        alertasActividad = 0;
        reportesGenerados = 0;

        lblTotalAlertas.setText("0");
        lblAlertasAsistencia.setText("0");
        lblAlertasActividad.setText("0");
        lblReportesGenerados.setText("0");
        lblEstadoActual.setText("--");

        lblDiagnostico.setText("Selecciona un alumno para generar el diagnostico del historial.");
        lblCausasDetectadas.setText("Sin causas detectadas.");
        lblRecomendacion.setText("Sin recomendacion disponible.");

        listaAlertas.clear();
        listaReportes.clear();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class ClaseItem {
        private final int idCarga;
        private final String nombre;

        public ClaseItem(int idCarga, String nombre) {
            this.idCarga = idCarga;
            this.nombre = nombre;
        }

        public int getIdCarga() {
            return idCarga;
        }

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

        public int getIdAlumno() {
            return idAlumno;
        }

        @Override
        public String toString() {
            return numControl + " - " + nombre;
        }
    }

    public static class AlertaItem {
        private final String fecha;
        private final String grupo;
        private final String materia;
        private final String motivo;
        private final String estado;

        public AlertaItem(String fecha, String grupo, String materia, String motivo, String estado) {
            this.fecha = fecha;
            this.grupo = grupo;
            this.materia = materia;
            this.motivo = motivo;
            this.estado = estado;
        }

        public String getFecha() {
            return fecha;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getMateria() {
            return materia;
        }

        public String getMotivo() {
            return motivo;
        }

        public String getEstado() {
            return estado;
        }
    }

    public static class ReporteItem {
        private final String fecha;
        private final String grupo;
        private final String materia;
        private final String motivo;
        private final String descripcion;
        private final String estado;

        public ReporteItem(String fecha, String grupo, String materia, String motivo, String descripcion, String estado) {
            this.fecha = fecha;
            this.grupo = grupo;
            this.materia = materia;
            this.motivo = motivo;
            this.descripcion = descripcion == null ? "" : descripcion;
            this.estado = estado;
        }

        public String getFecha() {
            return fecha;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getMateria() {
            return materia;
        }

        public String getMotivo() {
            return motivo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getEstado() {
            return estado;
        }
    }
}