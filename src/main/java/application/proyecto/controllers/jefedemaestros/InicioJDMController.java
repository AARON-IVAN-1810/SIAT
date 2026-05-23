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
    @FXML private TableColumn<AlertaReciente, String> colNombreAlerta;
    @FXML private TableColumn<AlertaReciente, String> colGrupoAlerta;
    @FXML private TableColumn<AlertaReciente, String> colMateriaAlerta;
    @FXML private TableColumn<AlertaReciente, Double> colPorcentajeEntregaAlerta;
    @FXML private TableColumn<AlertaReciente, Double> colPorcentajeFaltasAlerta;
    @FXML private TableColumn<AlertaReciente, String> colEstatusAlerta;

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
                "cerrada"
        ));
        cmbFiltroAlertas.setValue("todas");
    }

    private void cargarMetricas() {
        String sql = """
                select
                fn_total_alumnos() as total_alumnos,
                fn_total_alertas_rojas_activas() as alertas_rojas,
                fn_total_alertas_amarillas_activas() as alertas_medias,
                fn_total_grupos() as total_grupos,
                fn_total_maestros() as total_maestros
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int rojas = rs.getInt("alertas_rojas");
                int medias = rs.getInt("alertas_medias");

                lblTotalAlumnos.setText(String.valueOf(rs.getInt("total_alumnos")));
                lblAlertasActivas.setText(String.valueOf(rojas + medias));
                lblRiesgoBajo.setText(String.valueOf(medias));
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
                g.nombre as grupo,
                m.nombre as materia,
                ifnull(round((
                    sum(case when ae.entrego=1 then 1 else 0 end) / nullif(count(ae.id_actividad_entrega),0)
                ) * 100,2),0) as porcentaje_entrega,
                ifnull(round((
                    select
                    (sum(case when ad.id_estado_asistencia=0 then 1 else 0 end) / nullif(count(*),0)) * 100
                    from asistencia_detalle ad
                    inner join asistencia_sesion s on ad.id_asistencia_sesion=s.id_asistencia_sesion
                    where ad.id_alumno=al.id_alumno
                    and s.id_carga=c.id_carga
                ),2),0) as porcentaje_faltas,
                cea.nombre as estatus
                from alerta a
                inner join alumno al on a.id_alumno=al.id_alumno
                inner join carga c on a.id_carga=c.id_carga
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join materia m on c.id_materia=m.id_materia
                inner join cat_estatus_alerta cea on a.id_estatus_alerta=cea.id_estatus_alerta
                left join actividad act on act.id_carga=c.id_carga
                left join actividad_entrega ae on ae.id_actividad=act.id_actividad
                and ae.id_alumno=al.id_alumno
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
                            alerta.getMateria().toLowerCase().contains(texto);

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