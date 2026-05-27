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

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class InicioJDMController extends BaseController {

    @FXML private Label lblTotalAlumnos;
    @FXML private Label lblAlertasActivas;
    @FXML private Label lblRiesgoBajo;
    @FXML private Label lblTotalGrupos;
    @FXML private Label lblTotalMaestros;

    @FXML private TableView<AlertaReciente> tablaAlertasRecientes;
    @FXML private TableColumn<AlertaReciente, String> colNombreAlerta;
    @FXML private TableColumn<AlertaReciente, String> colGrupoAlerta;
    @FXML private TableColumn<AlertaReciente, String> colMateriaAlerta;
    @FXML private TableColumn<AlertaReciente, Double> colPorcentajeEntregaAlerta;
    @FXML private TableColumn<AlertaReciente, Double> colPorcentajeFaltasAlerta;
    @FXML private TableColumn<AlertaReciente, String> colEstatusAlerta;

    @FXML private ComboBox<String> cmbFiltroAlertas;
    @FXML private TextField txtBuscarAlertasRecientes;

    @FXML private Button btnGruposSinTutor;

    @FXML
    private void handleAbrirGruposSinTutor(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/jefedemaestros/GruposSinTutorJDM.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al abrir grupos sin tutor");
        }
    }

    private final ObservableList<AlertaReciente> listaAlertas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombo();
        cargarMetricas();
        cargarAlertasRecientes();
        configurarBusqueda();
    }

    private void configurarTabla() {
        colNombreAlerta.setCellValueFactory(new PropertyValueFactory<>("nombreAlumno"));
        colGrupoAlerta.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMateriaAlerta.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colPorcentajeEntregaAlerta.setCellValueFactory(new PropertyValueFactory<>("porcentajeEntrega"));
        colPorcentajeFaltasAlerta.setCellValueFactory(new PropertyValueFactory<>("porcentajeFaltas"));
        colEstatusAlerta.setCellValueFactory(new PropertyValueFactory<>("estatus"));
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
                (select count(*)
                 from alumno
                 where id_estatus_general=1) as total_alumnos,

                (select count(*)
                 from alerta
                 where id_estatus_alerta in(1,2)) as alertas_activas,

                (select count(*)
                 from alerta
                 where id_estatus_alerta in(1,2)
                 and id_prioridad_alerta=2) as riesgo_bajo,

                (select count(*)
                 from grupo_ciclo gc
                 inner join grupo g on gc.id_grupo=g.id_grupo
                 where gc.id_estatus_general=1
                 and g.id_estatus_general=1) as total_grupos,

                (select count(*)
                 from maestro
                 where id_estatus_general=1) as total_maestros
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                lblTotalAlumnos.setText(String.valueOf(rs.getInt("total_alumnos")));
                lblAlertasActivas.setText(String.valueOf(rs.getInt("alertas_activas")));
                lblRiesgoBajo.setText(String.valueOf(rs.getInt("riesgo_bajo")));
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
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as nombre_alumno,
                coalesce(concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre),'sin clase') as grupo,
                coalesce(concat(m.nombre,' (',m.clave,')'),'sin materia') as materia,

                coalesce(round((
                    sum(case when ae.entrego=1 then 1 else 0 end) /
                    nullif(count(distinct act.id_actividad),0)
                ) * 100,2),0) as porcentaje_entrega,

                coalesce(round((
                    select
                    (sum(case when ad.id_estado_asistencia=0 then 1 else 0 end) /
                    nullif(count(*),0)) * 100
                    from asistencia_detalle ad
                    inner join asistencia_sesion s on ad.id_asistencia_sesion=s.id_asistencia_sesion
                    where ad.id_alumno=al.id_alumno
                    and s.id_carga=c.id_carga
                ),2),0) as porcentaje_faltas,

                cea.nombre as estatus
                from alerta a
                inner join alumno al on a.id_alumno=al.id_alumno
                left join carga c on a.id_carga=c.id_carga
                left join materia m on c.id_materia=m.id_materia
                left join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                left join grupo g on gc.id_grupo=g.id_grupo
                left join cat_turno ct on g.id_turno=ct.id_turno
                left join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_estatus_alerta cea on a.id_estatus_alerta=cea.id_estatus_alerta
                left join actividad act on act.id_carga=c.id_carga
                and act.id_estatus_general=1
                left join actividad_entrega ae on ae.id_actividad=act.id_actividad
                and ae.id_alumno=al.id_alumno
                group by
                a.id_alerta,
                al.id_alumno,
                al.nombre,
                al.apellido_paterno,
                al.apellido_materno,
                g.nombre,
                ct.nombre,
                ce.nombre,
                m.nombre,
                m.clave,
                c.id_carga,
                cea.nombre,
                a.creada_en
                order by a.creada_en desc
                limit 50
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaAlertas.add(new AlertaReciente(
                        rs.getString("nombre_alumno"),
                        rs.getString("grupo"),
                        rs.getString("materia"),
                        rs.getDouble("porcentaje_entrega"),
                        rs.getDouble("porcentaje_faltas"),
                        rs.getString("estatus")
                ));
            }

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

        private final String nombreAlumno;
        private final String grupo;
        private final String materia;
        private final double porcentajeEntrega;
        private final double porcentajeFaltas;
        private final String estatus;

        public AlertaReciente(String nombreAlumno, String grupo, String materia, double porcentajeEntrega, double porcentajeFaltas, String estatus) {
            this.nombreAlumno = nombreAlumno;
            this.grupo = grupo;
            this.materia = materia;
            this.porcentajeEntrega = porcentajeEntrega;
            this.porcentajeFaltas = porcentajeFaltas;
            this.estatus = estatus;
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

        public double getPorcentajeEntrega() {
            return porcentajeEntrega;
        }

        public double getPorcentajeFaltas() {
            return porcentajeFaltas;
        }

        public String getEstatus() {
            return estatus;
        }
    }
}