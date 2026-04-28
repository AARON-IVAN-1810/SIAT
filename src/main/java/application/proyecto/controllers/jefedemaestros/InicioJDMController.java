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

public class InicioJDMController extends BaseController {

    @FXML private Label lblTotalAlumnos;
    @FXML private Label lblAlertasActivas;
    @FXML private Label lblRiesgoBajo;
    @FXML private Label lblTotalGrupos;
    @FXML private Label lblTotalMaestros;

    @FXML private TableView<AlertaReciente> tablaAlertasRecientes;
    @FXML private TableColumn<AlertaReciente, Integer> colIdAlerta;
    @FXML private TableColumn<AlertaReciente, String> colNombreAlerta;
    @FXML private TableColumn<AlertaReciente, String> colGrupoAlerta;
    @FXML private TableColumn<AlertaReciente, String> colMateriaAlerta;
    @FXML private TableColumn<AlertaReciente, Double> colCalificacionAlerta;
    @FXML private TableColumn<AlertaReciente, Integer> colFaltasAlerta;
    @FXML private TableColumn<AlertaReciente, String> colEstatusAlerta;
    @FXML private TableColumn<AlertaReciente, String> colAccionAlerta;

    @FXML private ComboBox<String> cmbFiltroAlertas;
    @FXML private TextField txtBuscarAlertasRecientes;

    private final ObservableList<AlertaReciente> listaAlertas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombo();
        cargarMetricas();
        cargarAlertasRecientes();
        configurarBusqueda();

        System.out.println("inicio jefe de maestros cargado");
    }

    private void configurarTabla() {
        colIdAlerta.setCellValueFactory(new PropertyValueFactory<>("idAlerta"));
        colNombreAlerta.setCellValueFactory(new PropertyValueFactory<>("nombreAlumno"));
        colGrupoAlerta.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMateriaAlerta.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colCalificacionAlerta.setCellValueFactory(new PropertyValueFactory<>("calificacion"));
        colFaltasAlerta.setCellValueFactory(new PropertyValueFactory<>("faltas"));
        colEstatusAlerta.setCellValueFactory(new PropertyValueFactory<>("estatus"));
        colAccionAlerta.setCellValueFactory(new PropertyValueFactory<>("accion"));
    }

    private void configurarCombo() {
        cmbFiltroAlertas.setItems(FXCollections.observableArrayList(
                "todas",
                "pendiente",
                "seguimiento",
                "cerrada"
        ));
        cmbFiltroAlertas.setValue("todas");
    }

    private void cargarMetricas() {
        String sql = """
                select
                fn_total_alumnos() as total_alumnos,
                fn_total_alertas_rojas_activas() as alertas_rojas,
                fn_total_alertas_amarillas_activas() as alertas_amarillas,
                fn_total_grupos() as total_grupos,
                fn_total_maestros() as total_maestros
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int rojas = rs.getInt("alertas_rojas");
                int amarillas = rs.getInt("alertas_amarillas");

                lblTotalAlumnos.setText(String.valueOf(rs.getInt("total_alumnos")));
                lblAlertasActivas.setText(String.valueOf(rojas + amarillas));
                lblRiesgoBajo.setText(String.valueOf(amarillas));
                lblTotalGrupos.setText(String.valueOf(rs.getInt("total_grupos")));
                lblTotalMaestros.setText(String.valueOf(rs.getInt("total_maestros")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar las metricas");
        }
    }

    private void cargarAlertasRecientes() {
        listaAlertas.clear();

        String sql = """
                select
                a.id_alerta,
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as nombre_alumno,
                g.nombre as grupo,
                m.nombre as materia,
                ifnull(round(avg(ca.calificacion),2),0) as calificacion,
                ifnull((
                    select count(*)
                    from asistencia_detalle ad
                    inner join asistencia_sesion s on ad.id_asistencia_sesion=s.id_asistencia_sesion
                    where ad.id_alumno=al.id_alumno
                    and s.id_carga=c.id_carga
                    and ad.id_estado_asistencia=0
                ),0) as faltas,
                cea.nombre as estatus,
                case
                    when cpa.nombre='alta' then 'atender'
                    when cpa.nombre='media' then 'revisar'
                    else 'monitorear'
                end as accion
                from alerta a
                inner join alumno al on a.id_alumno=al.id_alumno
                inner join carga c on a.id_carga=c.id_carga
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join materia m on c.id_materia=m.id_materia
                inner join cat_estatus_alerta cea on a.id_estatus_alerta=cea.id_estatus_alerta
                inner join cat_prioridad_alerta cpa on a.id_prioridad_alerta=cpa.id_prioridad_alerta
                left join actividad act on act.id_carga=c.id_carga
                left join calificacion ca on ca.id_actividad=act.id_actividad
                and ca.id_alumno=al.id_alumno
                group by
                a.id_alerta,
                al.id_alumno,
                al.nombre,
                al.apellido_paterno,
                al.apellido_materno,
                g.nombre,
                m.nombre,
                c.id_carga,
                cea.nombre,
                cpa.nombre,
                a.creada_en
                order by a.creada_en desc
                limit 50
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaAlertas.add(new AlertaReciente(
                        rs.getInt("id_alerta"),
                        rs.getString("nombre_alumno"),
                        rs.getString("grupo"),
                        rs.getString("materia"),
                        rs.getDouble("calificacion"),
                        rs.getInt("faltas"),
                        rs.getString("estatus"),
                        rs.getString("accion")
                ));
            }

            tablaAlertasRecientes.setItems(listaAlertas);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alertas recientes");
        }
    }

    private void configurarBusqueda() {
        FilteredList<AlertaReciente> filtro = new FilteredList<>(listaAlertas, p -> true);

        txtBuscarAlertasRecientes.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));
        cmbFiltroAlertas.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));

        tablaAlertasRecientes.setItems(filtro);
    }

    private void aplicarFiltro(FilteredList<AlertaReciente> filtro) {
        String texto = txtBuscarAlertasRecientes.getText() == null ? "" : txtBuscarAlertasRecientes.getText().toLowerCase();
        String estatus = cmbFiltroAlertas.getValue() == null ? "todas" : cmbFiltroAlertas.getValue().toLowerCase();

        filtro.setPredicate(alerta -> {
            boolean coincideTexto =
                    alerta.getNombreAlumno().toLowerCase().contains(texto) ||
                            alerta.getGrupo().toLowerCase().contains(texto) ||
                            alerta.getMateria().toLowerCase().contains(texto) ||
                            alerta.getEstatus().toLowerCase().contains(texto);

            boolean coincideEstatus =
                    estatus.equals("todas") ||
                            alerta.getEstatus().toLowerCase().equals(estatus);

            return coincideTexto && coincideEstatus;
        });
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class AlertaReciente {

        private final int idAlerta;
        private final String nombreAlumno;
        private final String grupo;
        private final String materia;
        private final double calificacion;
        private final int faltas;
        private final String estatus;
        private final String accion;

        public AlertaReciente(int idAlerta, String nombreAlumno, String grupo, String materia, double calificacion, int faltas, String estatus, String accion) {
            this.idAlerta = idAlerta;
            this.nombreAlumno = nombreAlumno;
            this.grupo = grupo;
            this.materia = materia;
            this.calificacion = calificacion;
            this.faltas = faltas;
            this.estatus = estatus;
            this.accion = accion;
        }

        public int getIdAlerta() {
            return idAlerta;
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

        public double getCalificacion() {
            return calificacion;
        }

        public int getFaltas() {
            return faltas;
        }

        public String getEstatus() {
            return estatus;
        }

        public String getAccion() {
            return accion;
        }
    }
}