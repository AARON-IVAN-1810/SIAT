package application.proyecto.controllers.maestros;

import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Optional;

public class NuevaActividadMController {

    @FXML private ComboBox<GrupoItem> cbGrupo;
    @FXML private TextField txtTurno;
    @FXML private ComboBox<MateriaItem> cbMateria;
    @FXML private TextField txtNombreActividad;
    @FXML private ComboBox<TipoActividadItem> cbTipoActividad;
    @FXML private DatePicker dpFecha;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardarActividad;

    @FXML
    public void initialize() {
        configurarEventos();
        cargarGrupos();
        cargarTiposActividad();
        dpFecha.setValue(LocalDate.now());
    }

    private int getIdMaestroActual() {
        return SesionUsuario.getIdMaestro();
    }

    private void configurarEventos() {
        cbGrupo.setOnAction(event -> {
            GrupoItem grupo = cbGrupo.getValue();

            cbMateria.getItems().clear();
            cbMateria.setValue(null);

            if (grupo != null) {
                txtTurno.setText(grupo.getTurno());
                cargarMaterias(grupo.getIdGrupoCiclo());
            } else {
                txtTurno.clear();
            }
        });

        btnCancelar.setOnAction(event -> cerrarVentana());
        btnGuardarActividad.setOnAction(event -> confirmarGuardado());
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
                m.clave,
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
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
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

    private void cargarTiposActividad() {
        cbTipoActividad.getItems().clear();

        String sql = """
                select id_tipo_actividad,nombre
                from cat_tipo_actividad
                order by id_tipo_actividad
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            TipoActividadItem tarea = null;

            while (rs.next()) {
                TipoActividadItem item = new TipoActividadItem(
                        rs.getInt("id_tipo_actividad"),
                        rs.getString("nombre")
                );

                cbTipoActividad.getItems().add(item);

                if (item.getNombre().equalsIgnoreCase("tarea")) {
                    tarea = item;
                }
            }

            if (tarea != null) {
                cbTipoActividad.setValue(tarea);
            } else if (!cbTipoActividad.getItems().isEmpty()) {
                cbTipoActividad.setValue(cbTipoActividad.getItems().get(0));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar tipos de actividad");
        }
    }

    private void confirmarGuardado() {
        if (!validarCampos()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("confirmar");
        alert.setHeaderText("guardar actividad");
        alert.setContentText("seguro que deseas guardar esta actividad?");

        Optional<ButtonType> respuesta = alert.showAndWait();

        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            guardarActividad();
        }
    }

    private boolean validarCampos() {
        if (cbGrupo.getValue() == null) {
            mostrarError("selecciona un grupo");
            return false;
        }

        if (cbMateria.getValue() == null) {
            mostrarError("selecciona una materia");
            return false;
        }

        if (txtNombreActividad.getText() == null || txtNombreActividad.getText().trim().isEmpty()) {
            mostrarError("captura el nombre de la actividad");
            return false;
        }

        if (cbTipoActividad.getValue() == null) {
            mostrarError("selecciona el tipo de actividad");
            return false;
        }

        if (dpFecha.getValue() == null) {
            mostrarError("selecciona la fecha");
            return false;
        }

        return true;
    }

    private void guardarActividad() {
        MateriaItem materia = cbMateria.getValue();
        TipoActividadItem tipo = cbTipoActividad.getValue();

        String titulo = txtNombreActividad.getText().trim();
        String descripcion = txtDescripcion.getText() == null ? "" : txtDescripcion.getText().trim();

        String sql = """
                insert into actividad(
                id_carga,
                titulo,
                id_tipo_actividad,
                fecha,
                descripcion,
                id_estatus_general
                )
                values(?,?,?,?,?,1)
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, materia.getIdCarga());
            ps.setString(2, titulo);
            ps.setInt(3, tipo.getIdTipoActividad());
            ps.setDate(4, java.sql.Date.valueOf(dpFecha.getValue()));
            ps.setString(5, descripcion);

            ps.executeUpdate();

            mostrarInfo("actividad guardada correctamente");
            cerrarVentana();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar actividad");
        }
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
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

    public static class TipoActividadItem {
        private final int idTipoActividad;
        private final String nombre;

        public TipoActividadItem(int idTipoActividad, String nombre) {
            this.idTipoActividad = idTipoActividad;
            this.nombre = nombre;
        }

        public int getIdTipoActividad() {
            return idTipoActividad;
        }

        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
}