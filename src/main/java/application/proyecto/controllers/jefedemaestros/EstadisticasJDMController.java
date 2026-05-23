package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EstadisticasJDMController extends BaseController {

    @FXML private Label lblTotalAlertas;
    @FXML private Label lblAlertasAsistencia;
    @FXML private Label lblAlertasActividad;
    @FXML private Label lblPorcentajeHombres;
    @FXML private Label lblPorcentajeMujeres;
    @FXML private Label lblRiesgoGeneral;
    @FXML private Label lblDiagnosticoAutomatico;
    @FXML private Label lblRecomendacionAutomatica;

    @FXML private TableView<RankingItem> tablaRankingGrupos;
    @FXML private TableColumn<RankingItem, String> colGrupo;
    @FXML private TableColumn<RankingItem, Integer> colAlertasGrupo;
    @FXML private TableColumn<RankingItem, Double> colPorcentajeGrupo;

    @FXML private TableView<RankingItem> tablaRankingMaterias;
    @FXML private TableColumn<RankingItem, String> colMateria;
    @FXML private TableColumn<RankingItem, Integer> colAlertasMateria;
    @FXML private TableColumn<RankingItem, Double> colPorcentajeMateria;

    @FXML private TableView<RankingItem> tablaRankingMaestros;
    @FXML private TableColumn<RankingItem, String> colMaestro;
    @FXML private TableColumn<RankingItem, Integer> colAlertasMaestro;
    @FXML private TableColumn<RankingItem, Double> colPorcentajeMaestro;

    private final ObservableList<RankingItem> listaGrupos = FXCollections.observableArrayList();
    private final ObservableList<RankingItem> listaMaterias = FXCollections.observableArrayList();
    private final ObservableList<RankingItem> listaMaestros = FXCollections.observableArrayList();

    private int totalAlertas = 0;
    private int alertasAsistencia = 0;
    private int alertasActividad = 0;

    private String sexoMayorRiesgo = "sin datos";
    private String grupoMayorRiesgo = "sin datos";
    private String materiaMayorRiesgo = "sin datos";
    private String maestroMayorRiesgo = "sin datos";
    private String tipoDominante = "sin datos";

    @FXML
    public void initialize() {
        configurarTablas();
        cargarEstadisticas();
    }

    private void configurarTablas() {
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colAlertasGrupo.setCellValueFactory(new PropertyValueFactory<>("total"));
        colPorcentajeGrupo.setCellValueFactory(new PropertyValueFactory<>("porcentaje"));

        colMateria.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colAlertasMateria.setCellValueFactory(new PropertyValueFactory<>("total"));
        colPorcentajeMateria.setCellValueFactory(new PropertyValueFactory<>("porcentaje"));

        colMaestro.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colAlertasMaestro.setCellValueFactory(new PropertyValueFactory<>("total"));
        colPorcentajeMaestro.setCellValueFactory(new PropertyValueFactory<>("porcentaje"));
    }

    @FXML
    private void handleActualizarEstadisticas() {
        cargarEstadisticas();
    }

    private void cargarEstadisticas() {
        cargarMetricasGenerales();
        cargarAnalisisSexo();
        cargarRankingGrupos();
        cargarRankingMaterias();
        cargarRankingMaestros();
        generarDiagnostico();
    }

    private void cargarMetricasGenerales() {
        String sql = """
                select
                count(*) as total_alertas,
                sum(case when cta.nombre='asistencia' then 1 else 0 end) as alertas_asistencia,
                sum(case when cta.nombre='actividad' then 1 else 0 end) as alertas_actividad
                from alerta a
                inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                where a.id_estatus_alerta in (1,2)
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                totalAlertas = rs.getInt("total_alertas");
                alertasAsistencia = rs.getInt("alertas_asistencia");
                alertasActividad = rs.getInt("alertas_actividad");

                lblTotalAlertas.setText(String.valueOf(totalAlertas));
                lblAlertasAsistencia.setText(String.valueOf(alertasAsistencia));
                lblAlertasActividad.setText(String.valueOf(alertasActividad));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar metricas generales");
        }
    }

    private void cargarAnalisisSexo() {
        String sql = """
                select
                cs.nombre as sexo,
                count(*) as total
                from alerta a
                inner join alumno al on a.id_alumno=al.id_alumno
                inner join cat_sexo cs on al.id_sexo=cs.id_sexo
                where a.id_estatus_alerta in (1,2)
                group by cs.nombre
                """;

        int hombres = 0;
        int mujeres = 0;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String sexo = rs.getString("sexo");
                int total = rs.getInt("total");

                if (sexo.equalsIgnoreCase("hombre")) {
                    hombres = total;
                }

                if (sexo.equalsIgnoreCase("mujer")) {
                    mujeres = total;
                }
            }

            int totalSexo = hombres + mujeres;
            double porcentajeHombres = totalSexo == 0 ? 0 : (hombres * 100.0) / totalSexo;
            double porcentajeMujeres = totalSexo == 0 ? 0 : (mujeres * 100.0) / totalSexo;

            lblPorcentajeHombres.setText(String.format("%.1f%%", porcentajeHombres));
            lblPorcentajeMujeres.setText(String.format("%.1f%%", porcentajeMujeres));

            if (hombres > mujeres) {
                sexoMayorRiesgo = "hombres";
            } else if (mujeres > hombres) {
                sexoMayorRiesgo = "mujeres";
            } else if (totalSexo > 0) {
                sexoMayorRiesgo = "hombres y mujeres por igual";
            } else {
                sexoMayorRiesgo = "sin datos";
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar analisis por sexo");
        }
    }

    private void cargarRankingGrupos() {
        listaGrupos.clear();

        String sql = """
                select
                g.nombre as nombre,
                count(*) as total,
                round((count(*) / nullif((select count(*) from alerta where id_estatus_alerta in (1,2)),0)) * 100,2) as porcentaje
                from alerta a
                inner join carga c on a.id_carga=c.id_carga
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                where a.id_estatus_alerta in (1,2)
                group by g.nombre
                order by total desc
                limit 5
                """;

        cargarRanking(sql, listaGrupos, tablaRankingGrupos);

        grupoMayorRiesgo = listaGrupos.isEmpty() ? "sin datos" : listaGrupos.get(0).getNombre();
    }

    private void cargarRankingMaterias() {
        listaMaterias.clear();

        String sql = """
                select
                m.nombre as nombre,
                count(*) as total,
                round((count(*) / nullif((select count(*) from alerta where id_estatus_alerta in (1,2)),0)) * 100,2) as porcentaje
                from alerta a
                inner join carga c on a.id_carga=c.id_carga
                inner join materia m on c.id_materia=m.id_materia
                where a.id_estatus_alerta in (1,2)
                group by m.nombre
                order by total desc
                limit 5
                """;

        cargarRanking(sql, listaMaterias, tablaRankingMaterias);

        materiaMayorRiesgo = listaMaterias.isEmpty() ? "sin datos" : listaMaterias.get(0).getNombre();
    }

    private void cargarRankingMaestros() {
        listaMaestros.clear();

        String sql = """
                select
                concat(ma.nombre,' ',ma.apellido_paterno,' ',ma.apellido_materno) as nombre,
                count(*) as total,
                round((count(*) / nullif((select count(*) from alerta where id_estatus_alerta in (1,2)),0)) * 100,2) as porcentaje
                from alerta a
                inner join carga c on a.id_carga=c.id_carga
                inner join maestro ma on c.id_maestro=ma.id_maestro
                where a.id_estatus_alerta in (1,2)
                group by ma.id_maestro,ma.nombre,ma.apellido_paterno,ma.apellido_materno
                order by total desc
                limit 5
                """;

        cargarRanking(sql, listaMaestros, tablaRankingMaestros);

        maestroMayorRiesgo = listaMaestros.isEmpty() ? "sin datos" : listaMaestros.get(0).getNombre();
    }

    private void cargarRanking(String sql, ObservableList<RankingItem> lista, TableView<RankingItem> tabla) {
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new RankingItem(
                        rs.getString("nombre"),
                        rs.getInt("total"),
                        rs.getDouble("porcentaje")
                ));
            }

            tabla.setItems(lista);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar ranking");
        }
    }

    private void generarDiagnostico() {
        if (alertasAsistencia > alertasActividad) {
            tipoDominante = "asistencia";
        } else if (alertasActividad > alertasAsistencia) {
            tipoDominante = "actividad";
        } else if (totalAlertas > 0) {
            tipoDominante = "asistencia y actividad";
        } else {
            tipoDominante = "sin datos";
        }

        String riesgo;

        if (totalAlertas >= 50) {
            riesgo = "alto";
        } else if (totalAlertas >= 20) {
            riesgo = "medio";
        } else if (totalAlertas > 0) {
            riesgo = "bajo";
        } else {
            riesgo = "sin riesgo";
        }

        lblRiesgoGeneral.setText(riesgo);

        if (totalAlertas == 0) {
            lblDiagnosticoAutomatico.setText(
                    "Actualmente no hay alertas activas o en seguimiento. El sistema no detecta un riesgo institucional relevante en este momento."
            );

            lblRecomendacionAutomatica.setText(
                    "Se recomienda mantener el monitoreo de asistencias y entregas para detectar cambios a tiempo."
            );
            return;
        }

        lblDiagnosticoAutomatico.setText(
                "El sistema detecta un riesgo general " + riesgo +
                        ". El grupo con mayor concentracion de alertas es " + grupoMayorRiesgo +
                        ", la materia con mas incidencias es " + materiaMayorRiesgo +
                        " y el maestro asociado con mas alertas es " + maestroMayorRiesgo +
                        ". El tipo de alerta dominante es " + tipoDominante +
                        ". En el analisis por sexo, el mayor porcentaje de alertas se concentra en " + sexoMayorRiesgo + "."
        );

        lblRecomendacionAutomatica.setText(
                "Se recomienda priorizar el seguimiento del grupo " + grupoMayorRiesgo +
                        ", revisar el comportamiento de la materia " + materiaMayorRiesgo +
                        " y analizar con el maestro " + maestroMayorRiesgo +
                        " las posibles causas de las alertas. Tambien se sugiere atender primero los casos relacionados con " +
                        tipoDominante + ", ya que representan el indicador mas repetido en el sistema."
        );
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class RankingItem {
        private final String nombre;
        private final int total;
        private final double porcentaje;

        public RankingItem(String nombre, int total, double porcentaje) {
            this.nombre = nombre;
            this.total = total;
            this.porcentaje = porcentaje;
        }

        public String getNombre() {
            return nombre;
        }

        public int getTotal() {
            return total;
        }

        public double getPorcentaje() {
            return porcentaje;
        }
    }
}