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
import java.sql.Statement;

public class GruposJDMController extends BaseController {

    @FXML private TextField txtNombreGrupo;
    @FXML private TextField txtClaveGrupo;
    @FXML private TextField txtBuscarGrupoInterno;

    @FXML private ComboBox<ItemCombo> cbTurnoGrupo;
    @FXML private ComboBox<Integer> cbSemestreGrupo;
    @FXML private ComboBox<ItemCombo> cbCicloGrupo;
    @FXML private ComboBox<ItemCombo> cbTutorGrupo;
    @FXML private ComboBox<String> cbFiltroGrupos;

    @FXML private ListView<ItemCombo> listViewMateriasGrupo;

    @FXML private TableView<GrupoJDM> tablaGrupos;
    @FXML private TableColumn<GrupoJDM, String> colNombreGrupo;
    @FXML private TableColumn<GrupoJDM, String> colClaveGrupo;
    @FXML private TableColumn<GrupoJDM, String> colTurnoGrupo;
    @FXML private TableColumn<GrupoJDM, Integer> colSemestreGrupo;
    @FXML private TableColumn<GrupoJDM, String> colCicloGrupo;
    @FXML private TableColumn<GrupoJDM, String> colTutorGrupo;
    @FXML private TableColumn<GrupoJDM, String> colMateriasGrupo;
    @FXML private TableColumn<GrupoJDM, String> colEstatusGrupo;

    private final ObservableList<GrupoJDM> listaGrupos = FXCollections.observableArrayList();

    private GrupoJDM grupoSeleccionado;
    private boolean modoEdicion = false;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        cargarTurnos();
        cargarCiclos();
        cargarTutores();
        cargarGrupos();
        configurarBusqueda();
    }

    private void configurarTabla() {
        colNombreGrupo.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colClaveGrupo.setCellValueFactory(new PropertyValueFactory<>("clave"));
        colTurnoGrupo.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colSemestreGrupo.setCellValueFactory(new PropertyValueFactory<>("semestre"));
        colCicloGrupo.setCellValueFactory(new PropertyValueFactory<>("ciclo"));
        colTutorGrupo.setCellValueFactory(new PropertyValueFactory<>("tutor"));
        colMateriasGrupo.setCellValueFactory(new PropertyValueFactory<>("materias"));
        colEstatusGrupo.setCellValueFactory(new PropertyValueFactory<>("estatus"));
    }

    private void configurarCombos() {
        cbSemestreGrupo.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9));

        cbFiltroGrupos.setItems(FXCollections.observableArrayList("todos", "activo", "inactivo"));
        cbFiltroGrupos.setValue("todos");
    }

    private void configurarEventos() {
        tablaGrupos.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            grupoSeleccionado = newValue;

            if (grupoSeleccionado != null) {
                cargarClasesDelGrupo(grupoSeleccionado.getIdGrupoCiclo());
            } else {
                listViewMateriasGrupo.getItems().clear();
            }
        });
    }

    private void cargarTurnos() {
        cbTurnoGrupo.getItems().clear();

        String sql = "select id_turno,nombre from cat_turno order by id_turno";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbTurnoGrupo.getItems().add(new ItemCombo(
                        rs.getInt("id_turno"),
                        rs.getString("nombre")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar turnos");
        }
    }

    private void cargarCiclos() {
        cbCicloGrupo.getItems().clear();

        String sql = """
                select id_ciclo_escolar,nombre
                from ciclo_escolar
                where id_estatus_general=1
                order by anio_inicio desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbCicloGrupo.getItems().add(new ItemCombo(
                        rs.getInt("id_ciclo_escolar"),
                        rs.getString("nombre")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar ciclos");
        }
    }

    private void cargarTutores() {
        cbTutorGrupo.getItems().clear();
        cbTutorGrupo.getItems().add(new ItemCombo(0, "sin tutor"));

        String sql = """
                select distinct
                m.id_maestro,
                concat(m.nombre,' ',m.apellido_paterno,' ',m.apellido_materno) as maestro
                from maestro m
                inner join usuario u on m.id_usuario=u.id_usuario
                inner join usuario_rol ur on u.id_usuario=ur.id_usuario
                inner join rol r on ur.id_rol=r.id_rol
                where r.nombre='tutor'
                and m.id_estatus_general=1
                order by maestro
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbTutorGrupo.getItems().add(new ItemCombo(
                        rs.getInt("id_maestro"),
                        rs.getString("maestro")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar tutores");
        }
    }

    private void cargarGrupos() {
        listaGrupos.clear();

        String sql = """
                select
                gc.id_grupo_ciclo,
                g.id_grupo,
                g.nombre,
                g.clave,
                g.semestre,
                g.id_turno,
                ct.nombre as turno,
                ce.id_ciclo_escolar,
                ce.nombre as ciclo,
                ifnull(ta.id_maestro_tutor,0) as id_tutor,
                ifnull(concat(mt.nombre,' ',mt.apellido_paterno,' ',mt.apellido_materno),'sin tutor') as tutor,
                ceg.nombre as estatus,
                ifnull(group_concat(distinct concat(m.nombre,' (',m.clave,')') order by m.nombre separator ', '),'sin clases') as materias
                from grupo_ciclo gc
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_estatus_general ceg on gc.id_estatus_general=ceg.id_estatus_general
                left join tutoria_asignacion ta on ta.id_grupo_ciclo=gc.id_grupo_ciclo and ta.id_estatus_tutoria=1
                left join maestro mt on ta.id_maestro_tutor=mt.id_maestro
                left join carga c on c.id_grupo_ciclo=gc.id_grupo_ciclo and c.id_estatus_general=1
                left join materia m on c.id_materia=m.id_materia
                group by
                gc.id_grupo_ciclo,
                g.id_grupo,
                g.nombre,
                g.clave,
                g.semestre,
                g.id_turno,
                ct.nombre,
                ce.id_ciclo_escolar,
                ce.nombre,
                ta.id_maestro_tutor,
                mt.nombre,
                mt.apellido_paterno,
                mt.apellido_materno,
                ceg.nombre
                order by ce.nombre desc,g.semestre,g.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaGrupos.add(new GrupoJDM(
                        rs.getInt("id_grupo_ciclo"),
                        rs.getInt("id_grupo"),
                        rs.getString("nombre"),
                        rs.getString("clave"),
                        rs.getInt("semestre"),
                        rs.getInt("id_turno"),
                        rs.getString("turno"),
                        rs.getInt("id_ciclo_escolar"),
                        rs.getString("ciclo"),
                        rs.getInt("id_tutor"),
                        rs.getString("tutor"),
                        rs.getString("materias"),
                        rs.getString("estatus")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos");
        }
    }

    private void cargarClasesDelGrupo(int idGrupoCiclo) {
        listViewMateriasGrupo.getItems().clear();

        String sql = """
                select
                c.id_carga,
                concat(m.clave,' - ',m.nombre,' | ',ma.nombre,' ',ma.apellido_paterno) as clase
                from carga c
                inner join materia m on c.id_materia=m.id_materia
                inner join maestro ma on c.id_maestro=ma.id_maestro
                where c.id_grupo_ciclo=?
                and c.id_estatus_general=1
                order by m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idGrupoCiclo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listViewMateriasGrupo.getItems().add(new ItemCombo(
                            rs.getInt("id_carga"),
                            rs.getString("clase")
                    ));
                }
            }

            if (listViewMateriasGrupo.getItems().isEmpty()) {
                listViewMateriasGrupo.getItems().add(new ItemCombo(0, "sin clases registradas"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar clases del grupo");
        }
    }

    @FXML
    private void handleGuardarGrupo() {
        String nombre = txtNombreGrupo.getText() == null ? "" : txtNombreGrupo.getText().trim().toLowerCase();
        String clave = txtClaveGrupo.getText() == null ? "" : txtClaveGrupo.getText().trim().toLowerCase();

        ItemCombo turno = cbTurnoGrupo.getValue();
        Integer semestre = cbSemestreGrupo.getValue();
        ItemCombo ciclo = cbCicloGrupo.getValue();
        ItemCombo tutor = cbTutorGrupo.getValue();

        if (nombre.isEmpty()) {
            mostrarError("captura el nombre del grupo");
            return;
        }

        if (clave.isEmpty()) {
            mostrarError("captura la clave del grupo");
            return;
        }

        if (turno == null) {
            mostrarError("selecciona el turno del grupo");
            return;
        }

        if (semestre == null) {
            mostrarError("selecciona el semestre del grupo");
            return;
        }

        if (ciclo == null) {
            mostrarError("selecciona el ciclo escolar");
            return;
        }

        if (modoEdicion && grupoSeleccionado != null) {
            actualizarGrupo(nombre, clave, turno.getId(), semestre, ciclo.getId(), tutor);
        } else {
            insertarGrupo(nombre, clave, turno.getId(), semestre, ciclo.getId(), tutor);
        }
    }

    private void insertarGrupo(String nombre, String clave, int idTurno, int semestre, int idCiclo, ItemCombo tutor) {
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            int idGrupo = obtenerOCrearGrupo(con, nombre, clave, idTurno, semestre);
            int idGrupoCiclo = obtenerOCrearGrupoCiclo(con, idGrupo, idCiclo);

            guardarTutorGrupo(con, idGrupoCiclo, tutor);

            con.commit();

            mostrarInfo("grupo guardado correctamente");
            limpiarFormulario();
            cargarGrupos();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar grupo. verifica que la clave no exista en el mismo ciclo");
        }
    }

    private int obtenerOCrearGrupo(Connection con, String nombre, String clave, int idTurno, int semestre) throws Exception {
        String sqlBuscar = """
                select id_grupo
                from grupo
                where clave=?
                limit 1
                """;

        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, clave);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idGrupo = rs.getInt("id_grupo");

                    String sqlActualizar = """
                            update grupo
                            set nombre=?,
                            id_turno=?,
                            semestre=?,
                            id_estatus_general=1
                            where id_grupo=?
                            """;

                    try (PreparedStatement psUpdate = con.prepareStatement(sqlActualizar)) {
                        psUpdate.setString(1, nombre);
                        psUpdate.setInt(2, idTurno);
                        psUpdate.setInt(3, semestre);
                        psUpdate.setInt(4, idGrupo);
                        psUpdate.executeUpdate();
                    }

                    return idGrupo;
                }
            }
        }

        String sqlInsert = """
                insert into grupo(nombre,clave,id_turno,semestre,id_estatus_general)
                values(?,?,?,?,1)
                """;

        try (PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, clave);
            ps.setInt(3, idTurno);
            ps.setInt(4, semestre);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new Exception("no se pudo crear el grupo");
    }

    private int obtenerOCrearGrupoCiclo(Connection con, int idGrupo, int idCiclo) throws Exception {
        String sqlBuscar = """
                select id_grupo_ciclo
                from grupo_ciclo
                where id_grupo=?
                and id_ciclo_escolar=?
                limit 1
                """;

        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setInt(1, idGrupo);
            ps.setInt(2, idCiclo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idGrupoCiclo = rs.getInt("id_grupo_ciclo");

                    String sqlActualizar = """
                            update grupo_ciclo
                            set id_estatus_general=1
                            where id_grupo_ciclo=?
                            """;

                    try (PreparedStatement psUpdate = con.prepareStatement(sqlActualizar)) {
                        psUpdate.setInt(1, idGrupoCiclo);
                        psUpdate.executeUpdate();
                    }

                    return idGrupoCiclo;
                }
            }
        }

        String sqlInsert = """
                insert into grupo_ciclo(id_grupo,id_ciclo_escolar,id_estatus_general)
                values(?,?,1)
                """;

        try (PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idGrupo);
            ps.setInt(2, idCiclo);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new Exception("no se pudo crear grupo ciclo");
    }

    private void guardarTutorGrupo(Connection con, int idGrupoCiclo, ItemCombo tutor) throws Exception {
        if (tutor == null || tutor.getId() == 0) {
            String sql = """
                    update tutoria_asignacion
                    set id_estatus_tutoria=0
                    where id_grupo_ciclo=?
                    """;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idGrupoCiclo);
                ps.executeUpdate();
            }

            return;
        }

        String sql = """
                insert into tutoria_asignacion(
                id_maestro_tutor,
                id_grupo_ciclo,
                id_estatus_tutoria
                )
                values(?,?,1)
                on duplicate key update
                id_maestro_tutor=values(id_maestro_tutor),
                id_estatus_tutoria=1
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tutor.getId());
            ps.setInt(2, idGrupoCiclo);
            ps.executeUpdate();
        }
    }

    private void actualizarGrupo(String nombre, String clave, int idTurno, int semestre, int idCiclo, ItemCombo tutor) {
        String sqlGrupo = """
                update grupo
                set nombre=?,
                clave=?,
                id_turno=?,
                semestre=?,
                id_estatus_general=1
                where id_grupo=?
                """;

        String sqlGrupoCiclo = """
                update grupo_ciclo
                set id_ciclo_escolar=?,
                id_estatus_general=1
                where id_grupo_ciclo=?
                """;

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlGrupo)) {
                ps.setString(1, nombre);
                ps.setString(2, clave);
                ps.setInt(3, idTurno);
                ps.setInt(4, semestre);
                ps.setInt(5, grupoSeleccionado.getIdGrupo());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlGrupoCiclo)) {
                ps.setInt(1, idCiclo);
                ps.setInt(2, grupoSeleccionado.getIdGrupoCiclo());
                ps.executeUpdate();
            }

            guardarTutorGrupo(con, grupoSeleccionado.getIdGrupoCiclo(), tutor);

            con.commit();

            mostrarInfo("grupo actualizado correctamente");
            limpiarFormulario();
            cargarGrupos();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al actualizar grupo");
        }
    }

    @FXML
    private void handleEditarGrupo() {
        if (grupoSeleccionado == null) {
            mostrarError("selecciona un grupo de la tabla");
            return;
        }

        modoEdicion = true;

        txtNombreGrupo.setText(grupoSeleccionado.getNombre());
        txtClaveGrupo.setText(grupoSeleccionado.getClave());

        cbSemestreGrupo.setValue(grupoSeleccionado.getSemestre());
        seleccionarComboPorId(cbTurnoGrupo, grupoSeleccionado.getIdTurno());
        seleccionarComboPorId(cbCicloGrupo, grupoSeleccionado.getIdCiclo());
        seleccionarComboPorId(cbTutorGrupo, grupoSeleccionado.getIdTutor());

        cargarClasesDelGrupo(grupoSeleccionado.getIdGrupoCiclo());
    }

    @FXML
    private void handleCambiarEstatusGrupo() {
        if (grupoSeleccionado == null) {
            mostrarError("selecciona un grupo de la tabla");
            return;
        }

        int nuevoEstatus = grupoSeleccionado.getEstatus().equalsIgnoreCase("activo") ? 0 : 1;

        String sql = """
                update grupo_ciclo
                set id_estatus_general=?
                where id_grupo_ciclo=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, nuevoEstatus);
            ps.setInt(2, grupoSeleccionado.getIdGrupoCiclo());
            ps.executeUpdate();

            mostrarInfo("estatus actualizado correctamente");
            limpiarFormulario();
            cargarGrupos();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cambiar estatus");
        }
    }

    private void configurarBusqueda() {
        FilteredList<GrupoJDM> filtro = new FilteredList<>(listaGrupos, p -> true);

        txtBuscarGrupoInterno.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));
        cbFiltroGrupos.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));

        tablaGrupos.setItems(filtro);
    }

    private void aplicarFiltro(FilteredList<GrupoJDM> filtro) {
        String texto = txtBuscarGrupoInterno.getText() == null ? "" : txtBuscarGrupoInterno.getText().toLowerCase();
        String estatus = cbFiltroGrupos.getValue() == null ? "todos" : cbFiltroGrupos.getValue().toLowerCase();

        filtro.setPredicate(grupo -> {
            boolean coincideTexto =
                    grupo.getNombre().toLowerCase().contains(texto) ||
                            grupo.getClave().toLowerCase().contains(texto) ||
                            grupo.getTurno().toLowerCase().contains(texto) ||
                            grupo.getCiclo().toLowerCase().contains(texto) ||
                            grupo.getTutor().toLowerCase().contains(texto) ||
                            grupo.getMaterias().toLowerCase().contains(texto) ||
                            String.valueOf(grupo.getSemestre()).contains(texto);

            boolean coincideEstatus =
                    estatus.equals("todos") ||
                            grupo.getEstatus().toLowerCase().equals(estatus);

            return coincideTexto && coincideEstatus;
        });
    }

    private void seleccionarComboPorId(ComboBox<ItemCombo> combo, int id) {
        for (ItemCombo item : combo.getItems()) {
            if (item.getId() == id) {
                combo.setValue(item);
                return;
            }
        }
    }

    private void limpiarFormulario() {
        txtNombreGrupo.clear();
        txtClaveGrupo.clear();

        cbTurnoGrupo.setValue(null);
        cbSemestreGrupo.setValue(null);
        cbCicloGrupo.setValue(null);
        cbTutorGrupo.setValue(null);

        listViewMateriasGrupo.getItems().clear();

        grupoSeleccionado = null;
        modoEdicion = false;
        tablaGrupos.getSelectionModel().clearSelection();
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

    public static class ItemCombo {
        private final int id;
        private final String nombre;

        public ItemCombo(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public int getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    public static class GrupoJDM {
        private final int idGrupoCiclo;
        private final int idGrupo;
        private final String nombre;
        private final String clave;
        private final int semestre;
        private final int idTurno;
        private final String turno;
        private final int idCiclo;
        private final String ciclo;
        private final int idTutor;
        private final String tutor;
        private final String materias;
        private final String estatus;

        public GrupoJDM(int idGrupoCiclo, int idGrupo, String nombre, String clave, int semestre, int idTurno, String turno, int idCiclo, String ciclo, int idTutor, String tutor, String materias, String estatus) {
            this.idGrupoCiclo = idGrupoCiclo;
            this.idGrupo = idGrupo;
            this.nombre = nombre;
            this.clave = clave;
            this.semestre = semestre;
            this.idTurno = idTurno;
            this.turno = turno;
            this.idCiclo = idCiclo;
            this.ciclo = ciclo;
            this.idTutor = idTutor;
            this.tutor = tutor;
            this.materias = materias;
            this.estatus = estatus;
        }

        public int getIdGrupoCiclo() {
            return idGrupoCiclo;
        }

        public int getIdGrupo() {
            return idGrupo;
        }

        public String getNombre() {
            return nombre;
        }

        public String getClave() {
            return clave;
        }

        public int getSemestre() {
            return semestre;
        }

        public int getIdTurno() {
            return idTurno;
        }

        public String getTurno() {
            return turno;
        }

        public int getIdCiclo() {
            return idCiclo;
        }

        public String getCiclo() {
            return ciclo;
        }

        public int getIdTutor() {
            return idTutor;
        }

        public String getTutor() {
            return tutor;
        }

        public String getMaterias() {
            return materias;
        }

        public String getEstatus() {
            return estatus;
        }
    }
}