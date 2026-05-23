package application.proyecto.controllers.maestros;

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

public class InicioMController extends BaseController {

    @FXML private Label lblTotalClases;
    @FXML private Label lblTotalAlumnos;
    @FXML private Label lblTotalAlertas;

    @FXML private TableView<GrupoMaestro> tablaGrupos;
    @FXML private TableColumn<GrupoMaestro, String> colCodigoGrupo;
    @FXML private TableColumn<GrupoMaestro, String> colNombreMateriaGrupo;
    @FXML private TableColumn<GrupoMaestro, Integer> colTotalAlumnosGrupo;

    @FXML private TextField txtBuscarAlertas;
    @FXML private ComboBox<String> cbFiltroTipoAlerta;

    @FXML private TableView<AlertaMaestro> tablaAlertas;
    @FXML private TableColumn<AlertaMaestro, String> colGrupoAlerta;
    @FXML private TableColumn<AlertaMaestro, String> colNoControlAlerta;
    @FXML private TableColumn<AlertaMaestro, String> colNombreAlerta;
    @FXML private TableColumn<AlertaMaestro, String> colMateriaAlerta;
    @FXML private TableColumn<AlertaMaestro, String> colTipoAlerta;

    private final ObservableList<GrupoMaestro> listaGrupos = FXCollections.observableArrayList();
    private final ObservableList<AlertaMaestro> listaAlertas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTablas();
        configurarFiltro();
        cargarMetricas();
        cargarGrupos();
        cargarAlertas();
        configurarBusquedaAlertas();
    }

    private int getIdMaestroActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarTablas() {
        colCodigoGrupo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombreMateriaGrupo.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colTotalAlumnosGrupo.setCellValueFactory(new PropertyValueFactory<>("totalAlumnos"));

        colGrupoAlerta.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colNoControlAlerta.setCellValueFactory(new PropertyValueFactory<>("numControl"));
        colNombreAlerta.setCellValueFactory(new PropertyValueFactory<>("nombreAlumno"));
        colMateriaAlerta.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colTipoAlerta.setCellValueFactory(new PropertyValueFactory<>("tipoAlerta"));

        tablaGrupos.setItems(listaGrupos);
        tablaAlertas.setItems(listaAlertas);
    }

    private void configurarFiltro() {
        cbFiltroTipoAlerta.setItems(FXCollections.observableArrayList(
                "todas",
                "asistencia",
                "actividad"
        ));
        cbFiltroTipoAlerta.setValue("todas");
    }

    private void cargarMetricas() {
        int idMaestro = getIdMaestroActual();

        if (idMaestro == 0) {
            mostrarError("no hay maestro en sesion");
            return;
        }

        String sql = """
            select
            (
                select count(*)
                from carga c
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                where c.id_maestro=?
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
            ) as total_clases,
            (
                select count(distinct al.id_alumno)
                from carga c
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join alumno al on al.id_grupo_ciclo=gc.id_grupo_ciclo
                where c.id_maestro=?
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                and al.id_estatus_general=1
            ) as total_alumnos,
            (
                select count(*)
                from alerta a
                inner join carga c on a.id_carga=c.id_carga
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                where c.id_maestro=?
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                and a.id_estatus_alerta in (1,2)
            ) as total_alertas
            """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);
            ps.setInt(2, idMaestro);
            ps.setInt(3, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblTotalClases.setText(String.valueOf(rs.getInt("total_clases")));
                    lblTotalAlumnos.setText(String.valueOf(rs.getInt("total_alumnos")));
                    lblTotalAlertas.setText(String.valueOf(rs.getInt("total_alertas")));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar metricas");
        }
    }

    private void cargarGrupos() {
        listaGrupos.clear();

        int idMaestro = getIdMaestroActual();

        String sql = """
            select
            g.nombre as codigo,
            m.nombre as materia,
            count(distinct al.id_alumno) as total_alumnos
            from carga c
            inner join materia m on c.id_materia=m.id_materia
            inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
            inner join grupo g on gc.id_grupo=g.id_grupo
            left join alumno al on al.id_grupo_ciclo=gc.id_grupo_ciclo
            and al.id_estatus_general=1
            where c.id_maestro=?
            and c.id_estatus_general=1
            and gc.id_estatus_general=1
            and g.id_estatus_general=1
            group by c.id_carga,g.nombre,m.nombre
            order by g.nombre,m.nombre
            """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaGrupos.add(new GrupoMaestro(
                            rs.getString("codigo"),
                            rs.getString("materia"),
                            rs.getInt("total_alumnos")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos");
        }
    }

    private void cargarAlertas() {
        listaAlertas.clear();

        int idMaestro = getIdMaestroActual();

        String sql = """
            select
            g.nombre as grupo,
            al.num_control,
            concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as nombre_alumno,
            m.nombre as materia,
            cta.nombre as tipo_alerta
            from alerta a
            inner join alumno al on a.id_alumno=al.id_alumno
            inner join carga c on a.id_carga=c.id_carga
            inner join materia m on c.id_materia=m.id_materia
            inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
            inner join grupo g on gc.id_grupo=g.id_grupo
            inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
            where c.id_maestro=?
            and c.id_estatus_general=1
            and gc.id_estatus_general=1
            and g.id_estatus_general=1
            and al.id_estatus_general=1
            and a.id_estatus_alerta in (1,2)
            order by g.nombre,m.nombre,nombre_alumno
            """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaAlertas.add(new AlertaMaestro(
                            rs.getString("grupo"),
                            rs.getString("num_control"),
                            rs.getString("nombre_alumno"),
                            rs.getString("materia"),
                            rs.getString("tipo_alerta")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alertas");
        }
    }

    private void configurarBusquedaAlertas() {
        FilteredList<AlertaMaestro> filtro = new FilteredList<>(listaAlertas, p -> true);

        txtBuscarAlertas.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltroAlertas(filtro));
        cbFiltroTipoAlerta.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltroAlertas(filtro));

        tablaAlertas.setItems(filtro);
    }

    private void aplicarFiltroAlertas(FilteredList<AlertaMaestro> filtro) {
        String texto = txtBuscarAlertas.getText() == null ? "" : txtBuscarAlertas.getText().toLowerCase();
        String tipo = cbFiltroTipoAlerta.getValue() == null ? "todas" : cbFiltroTipoAlerta.getValue().toLowerCase();

        filtro.setPredicate(alerta -> {
            boolean coincideTexto =
                    alerta.getGrupo().toLowerCase().contains(texto) ||
                            alerta.getMateria().toLowerCase().contains(texto) ||
                            alerta.getNombreAlumno().toLowerCase().contains(texto) ||
                            alerta.getNumControl().toLowerCase().contains(texto);

            boolean coincideTipo =
                    tipo.equals("todas") ||
                            alerta.getTipoAlerta().toLowerCase().equals(tipo);

            return coincideTexto && coincideTipo;
        });
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class GrupoMaestro {
        private final String codigo;
        private final String materia;
        private final int totalAlumnos;

        public GrupoMaestro(String codigo, String materia, int totalAlumnos) {
            this.codigo = codigo;
            this.materia = materia;
            this.totalAlumnos = totalAlumnos;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getMateria() {
            return materia;
        }

        public int getTotalAlumnos() {
            return totalAlumnos;
        }
    }

    public static class AlertaMaestro {
        private final String grupo;
        private final String numControl;
        private final String nombreAlumno;
        private final String materia;
        private final String tipoAlerta;

        public AlertaMaestro(String grupo, String numControl, String nombreAlumno, String materia, String tipoAlerta) {
            this.grupo = grupo;
            this.numControl = numControl;
            this.nombreAlumno = nombreAlumno;
            this.materia = materia;
            this.tipoAlerta = tipoAlerta;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getNumControl() {
            return numControl;
        }

        public String getNombreAlumno() {
            return nombreAlumno;
        }

        public String getMateria() {
            return materia;
        }

        public String getTipoAlerta() {
            return tipoAlerta;
        }
    }
}