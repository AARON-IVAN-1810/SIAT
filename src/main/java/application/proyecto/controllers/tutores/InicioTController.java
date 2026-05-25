package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
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

public class InicioTController extends BaseController {

    @FXML private Label lblAlertasActivas;
    @FXML private Label lblCasosSeguimiento;
    @FXML private Label lblCasosCerrados;
    @FXML private Label lblIntervencionesRecientes;

    @FXML private ComboBox<String> cmbGrupoFiltro;
    @FXML private ComboBox<String> cmbTipoFiltro;
    @FXML private TextField txtBuscarTabla;

    @FXML private TableView<AlertaTutor> tablaAlertasTutor;
    @FXML private TableColumn<AlertaTutor, String> colAlumno;
    @FXML private TableColumn<AlertaTutor, String> colGrupo;
    @FXML private TableColumn<AlertaTutor, String> colTipoAlerta;
    @FXML private TableColumn<AlertaTutor, String> colPrioridad;
    @FXML private TableColumn<AlertaTutor, String> colEstatus;
    @FXML private TableColumn<AlertaTutor, String> colFecha;

    private final ObservableList<AlertaTutor> listaAlertas = FXCollections.observableArrayList();
    private FilteredList<AlertaTutor> listaFiltrada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        configurarEventos();
        cargarMetricas();
        cargarGruposFiltro();
        cargarAlertas();
    }

    private int getIdTutorActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTabla() {
        colAlumno.setCellValueFactory(new PropertyValueFactory<>("alumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colTipoAlerta.setCellValueFactory(new PropertyValueFactory<>("tipoAlerta"));
        colPrioridad.setCellValueFactory(new PropertyValueFactory<>("prioridad"));
        colEstatus.setCellValueFactory(new PropertyValueFactory<>("estatus"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        listaFiltrada = new FilteredList<>(listaAlertas, p -> true);
        tablaAlertasTutor.setItems(listaFiltrada);
    }

    private void configurarFiltros() {
        cmbGrupoFiltro.setItems(FXCollections.observableArrayList("todos"));
        cmbGrupoFiltro.setValue("todos");

        cmbTipoFiltro.setItems(FXCollections.observableArrayList(
                "todos",
                "asistencia",
                "actividad",
                "calificacion",
                "conducta"
        ));

        cmbTipoFiltro.setValue("todos");
    }

    private void configurarEventos() {
        txtBuscarTabla.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbGrupoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbTipoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void cargarMetricas() {
        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        String sqlAlertas = """
                select
                count(distinct case when a.id_estatus_alerta in (1,2) then a.id_alerta end) as alertas_activas,
                count(distinct case when a.id_estatus_alerta=2 then a.id_alerta end) as casos_seguimiento,
                count(distinct case when a.id_estatus_alerta=0 then a.id_alerta end) as casos_cerrados
                from tutoria_asignacion ta
                inner join carga c on ta.id_grupo_ciclo=c.id_grupo_ciclo
                inner join alerta a on c.id_carga=a.id_carga
                inner join alumno al on a.id_alumno=al.id_alumno
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                and al.id_estatus_general=1
                """;

        String sqlIntervenciones = """
                select count(*) as intervenciones
                from intervencion_tutor it
                where it.id_maestro_tutor=?
                and it.fecha_intervencion>=date_sub(curdate(), interval 30 day)
                """;

        try (Connection con = ConexionBD.conectar()) {
            try (PreparedStatement ps = con.prepareStatement(sqlAlertas)) {
                ps.setInt(1, idTutor);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lblAlertasActivas.setText(String.valueOf(rs.getInt("alertas_activas")));
                        lblCasosSeguimiento.setText(String.valueOf(rs.getInt("casos_seguimiento")));
                        lblCasosCerrados.setText(String.valueOf(rs.getInt("casos_cerrados")));
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlIntervenciones)) {
                ps.setInt(1, idTutor);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lblIntervencionesRecientes.setText(String.valueOf(rs.getInt("intervenciones")));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar metricas del tutor");
        }
    }

    private void cargarGruposFiltro() {
        cmbGrupoFiltro.getItems().clear();
        cmbGrupoFiltro.getItems().add("todos");

        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        String sql = """
                select distinct
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo
                from tutoria_asignacion ta
                inner join grupo_ciclo gc on ta.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join carga c on gc.id_grupo_ciclo=c.id_grupo_ciclo
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                order by grupo
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cmbGrupoFiltro.getItems().add(rs.getString("grupo"));
                }
            }

            cmbGrupoFiltro.setValue("todos");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos");
        }
    }

    private void cargarAlertas() {
        listaAlertas.clear();

        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        String sql = """
                select distinct
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as alumno,
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre,' | ',m.clave,' - ',m.nombre) as grupo,
                cta.nombre as tipo_alerta,
                cpa.nombre as prioridad,
                cea.nombre as estatus,
                a.creada_en as fecha_orden,
                date_format(a.creada_en,'%Y-%m-%d') as fecha
                from tutoria_asignacion ta
                inner join carga c on ta.id_grupo_ciclo=c.id_grupo_ciclo
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join alerta a on c.id_carga=a.id_carga
                inner join alumno al on a.id_alumno=al.id_alumno
                inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                inner join cat_prioridad_alerta cpa on a.id_prioridad_alerta=cpa.id_prioridad_alerta
                inner join cat_estatus_alerta cea on a.id_estatus_alerta=cea.id_estatus_alerta
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                and al.id_estatus_general=1
                order by fecha_orden desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaAlertas.add(new AlertaTutor(
                            rs.getString("alumno"),
                            rs.getString("grupo"),
                            rs.getString("tipo_alerta"),
                            rs.getString("prioridad"),
                            rs.getString("estatus"),
                            rs.getString("fecha")
                    ));
                }
            }

            aplicarFiltro();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alertas del tutor");
        }
    }

    private void aplicarFiltro() {
        if (listaFiltrada == null) {
            return;
        }

        String texto = txtBuscarTabla.getText() == null ? "" : txtBuscarTabla.getText().toLowerCase().trim();
        String grupo = cmbGrupoFiltro.getValue() == null ? "todos" : cmbGrupoFiltro.getValue().toLowerCase().trim();
        String tipo = cmbTipoFiltro.getValue() == null ? "todos" : cmbTipoFiltro.getValue().toLowerCase().trim();

        listaFiltrada.setPredicate(alerta -> {
            boolean coincideTexto =
                    texto.isEmpty() ||
                            alerta.getAlumno().toLowerCase().contains(texto) ||
                            alerta.getGrupo().toLowerCase().contains(texto) ||
                            alerta.getTipoAlerta().toLowerCase().contains(texto) ||
                            alerta.getPrioridad().toLowerCase().contains(texto) ||
                            alerta.getEstatus().toLowerCase().contains(texto) ||
                            alerta.getFecha().toLowerCase().contains(texto);

            String grupoAlerta = alerta.getGrupo().toLowerCase();
            String tipoAlerta = alerta.getTipoAlerta().toLowerCase();

            boolean coincideGrupo =
                    grupo.equals("todos") ||
                            grupoAlerta.contains(grupo);

            boolean coincideTipo =
                    tipo.equals("todos") ||
                            tipoAlerta.equals(tipo) ||
                            (tipo.equals("actividad") && tipoAlerta.equals("calificacion")) ||
                            (tipo.equals("calificacion") && tipoAlerta.equals("actividad"));

            return coincideTexto && coincideGrupo && coincideTipo;
        });
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class AlertaTutor {
        private final String alumno;
        private final String grupo;
        private final String tipoAlerta;
        private final String prioridad;
        private final String estatus;
        private final String fecha;

        public AlertaTutor(String alumno, String grupo, String tipoAlerta, String prioridad, String estatus, String fecha) {
            this.alumno = textoSeguro(alumno);
            this.grupo = textoSeguro(grupo);
            this.tipoAlerta = textoSeguro(tipoAlerta);
            this.prioridad = textoSeguro(prioridad);
            this.estatus = textoSeguro(estatus);
            this.fecha = textoSeguro(fecha);
        }

        private static String textoSeguro(String valor) {
            return valor == null ? "" : valor;
        }

        public String getAlumno() {
            return alumno;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getTipoAlerta() {
            return tipoAlerta;
        }

        public String getPrioridad() {
            return prioridad;
        }

        public String getEstatus() {
            return estatus;
        }

        public String getFecha() {
            return fecha;
        }
    }
}