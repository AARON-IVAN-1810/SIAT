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
    @FXML private TextField txtBuscarGrupoInterno;

    @FXML private ComboBox<ItemCombo> cbTurnoGrupo;
    @FXML private ComboBox<Integer> cbSemestreGrupo;
    @FXML private ComboBox<ItemCombo> cbCicloGrupo;
    @FXML private ComboBox<ItemCombo> cbTutorGrupo;
    @FXML private ComboBox<String> cbFiltroGrupos;

    @FXML private ListView<ItemCombo> listViewMateriasGrupo;

    @FXML private TableView<GrupoJDM> tablaGrupos;
    @FXML private TableColumn<GrupoJDM, String> colNombreGrupo;
    @FXML private TableColumn<GrupoJDM, String> colTurnoGrupo;
    @FXML private TableColumn<GrupoJDM, Integer> colSemestreGrupo;
    @FXML private TableColumn<GrupoJDM, String> colCicloGrupo;
    @FXML private TableColumn<GrupoJDM, String> colMateriasGrupo;
    @FXML private TableColumn<GrupoJDM, String> colEstatusGrupo;

    private final ObservableList<GrupoJDM> listaGrupos = FXCollections.observableArrayList();
    private final ObservableList<ItemCombo> materiasSeleccionadas = FXCollections.observableArrayList();

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
        colTurnoGrupo.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colSemestreGrupo.setCellValueFactory(new PropertyValueFactory<>("semestre"));
        colCicloGrupo.setCellValueFactory(new PropertyValueFactory<>("ciclo"));
        colMateriasGrupo.setCellValueFactory(new PropertyValueFactory<>("materias"));
        colEstatusGrupo.setCellValueFactory(new PropertyValueFactory<>("estatus"));
    }

    private void configurarCombos() {
        cbSemestreGrupo.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9));

        cbFiltroGrupos.setItems(FXCollections.observableArrayList("todos","activo","inactivo"));
        cbFiltroGrupos.setValue("todos");

        listViewMateriasGrupo.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(ItemCombo item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                checkBox.setText(item.toString());
                checkBox.setSelected(materiasSeleccionadas.contains(item));

                checkBox.setOnAction(event -> {
                    if (checkBox.isSelected()) {
                        if (!materiasSeleccionadas.contains(item)) {
                            materiasSeleccionadas.add(item);
                        }
                    } else {
                        materiasSeleccionadas.remove(item);
                    }
                });

                setGraphic(checkBox);
                setText(null);
            }
        });
    }

    private void configurarEventos() {
        cbSemestreGrupo.setOnAction(event -> cargarMateriasPorSemestreYTurno());
        cbTurnoGrupo.setOnAction(event -> cargarMateriasPorSemestreYTurno());

        tablaGrupos.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            grupoSeleccionado = newValue;
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

    private void cargarMateriasPorSemestreYTurno() {
        listViewMateriasGrupo.getItems().clear();
        materiasSeleccionadas.clear();

        Integer semestre = cbSemestreGrupo.getValue();
        ItemCombo turno = cbTurnoGrupo.getValue();

        if (semestre == null || turno == null) {
            return;
        }

        String sql = """
                select
                m.id_materia,
                concat(m.nombre,' - ',m.clave,' - ',ma.nombre,' ',ma.apellido_paterno) as materia
                from materia m
                inner join maestro ma on m.id_maestro=ma.id_maestro
                where m.semestre=?
                and m.id_turno=?
                and m.id_estatus_general=1
                order by m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, semestre);
            ps.setInt(2, turno.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listViewMateriasGrupo.getItems().add(new ItemCombo(
                            rs.getInt("id_materia"),
                            rs.getString("materia")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar materias");
        }
    }

    private void cargarGrupos() {
        listaGrupos.clear();

        String sql = """
                select
                gc.id_grupo_ciclo,
                g.id_grupo,
                g.nombre,
                g.semestre,
                g.id_turno,
                ct.nombre as turno,
                ce.id_ciclo_escolar,
                ce.nombre as ciclo,
                ceg.nombre as estatus,
                ifnull(group_concat(distinct concat(m.nombre,' (',m.clave,')') order by m.nombre separator ', '),'sin materias') as materias
                from grupo_ciclo gc
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_estatus_general ceg on gc.id_estatus_general=ceg.id_estatus_general
                left join carga c on c.id_grupo_ciclo=gc.id_grupo_ciclo and c.id_estatus_general=1
                left join materia m on c.id_materia=m.id_materia
                group by
                gc.id_grupo_ciclo,
                g.id_grupo,
                g.nombre,
                g.semestre,
                g.id_turno,
                ct.nombre,
                ce.id_ciclo_escolar,
                ce.nombre,
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
                        rs.getInt("semestre"),
                        rs.getInt("id_turno"),
                        rs.getString("turno"),
                        rs.getInt("id_ciclo_escolar"),
                        rs.getString("ciclo"),
                        rs.getString("materias"),
                        rs.getString("estatus")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos");
        }
    }

    @FXML
    private void handleGuardarGrupo() {
        String nombre = txtNombreGrupo.getText() == null ? "" : txtNombreGrupo.getText().trim().toLowerCase();
        ItemCombo turno = cbTurnoGrupo.getValue();
        Integer semestre = cbSemestreGrupo.getValue();
        ItemCombo ciclo = cbCicloGrupo.getValue();
        ItemCombo tutor = cbTutorGrupo.getValue();

        if (nombre.isEmpty()) {
            mostrarError("captura el nombre del grupo");
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

        if (materiasSeleccionadas.isEmpty()) {
            mostrarError("selecciona al menos una materia");
            return;
        }

        if (modoEdicion && grupoSeleccionado != null) {
            actualizarGrupo(nombre, turno.getId(), semestre, ciclo.getId(), materiasSeleccionadas, tutor);
        } else {
            insertarGrupo(nombre, turno.getId(), semestre, ciclo.getId(), materiasSeleccionadas, tutor);
        }
    }

    private void insertarGrupo(String nombre, int idTurno, int semestre, int idCiclo, ObservableList<ItemCombo> materias, ItemCombo tutor) {
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            int idGrupo = obtenerOCrearGrupo(con, nombre, idTurno, semestre);
            int idGrupoCiclo = crearGrupoCiclo(con, idGrupo, idCiclo);

            insertarCargasSinDuplicar(con, idGrupoCiclo, materias);
            guardarTutorGrupo(con, idGrupoCiclo, tutor);

            con.commit();

            mostrarInfo("grupo guardado correctamente");
            limpiarFormulario();
            cargarGrupos();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar grupo. verifica que no exista el mismo grupo en el mismo ciclo");
        }
    }

    private int obtenerOCrearGrupo(Connection con, String nombre, int idTurno, int semestre) throws Exception {
        String sqlBuscar = """
                select id_grupo
                from grupo
                where nombre=?
                and id_turno=?
                and semestre=?
                limit 1
                """;

        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, nombre);
            ps.setInt(2, idTurno);
            ps.setInt(3, semestre);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_grupo");
                }
            }
        }

        String sqlInsert = """
                insert into grupo(nombre,clave,id_turno,semestre,id_estatus_general)
                values(?,?,?,?,1)
                """;

        try (PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, nombre);
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

    private int crearGrupoCiclo(Connection con, int idGrupo, int idCiclo) throws Exception {
        String sql = """
                insert into grupo_ciclo(id_grupo,id_ciclo_escolar,id_estatus_general)
                values(?,?,1)
                """;

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    private void insertarCargasSinDuplicar(Connection con, int idGrupoCiclo, ObservableList<ItemCombo> materias) throws Exception {
        String sql = """
                insert into carga(
                id_grupo_ciclo,
                id_materia,
                id_maestro,
                id_turno,
                id_estatus_general
                )
                select
                ?,
                m.id_materia,
                m.id_maestro,
                m.id_turno,
                1
                from materia m
                where m.id_materia=?
                on duplicate key update
                id_estatus_general=1,
                id_maestro=values(id_maestro),
                id_turno=values(id_turno)
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (ItemCombo materia : materias) {
                ps.setInt(1, idGrupoCiclo);
                ps.setInt(2, materia.getId());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    private void guardarTutorGrupo(Connection con, int idGrupoCiclo, ItemCombo tutor) throws Exception {
        if (tutor == null) {
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

    private void actualizarGrupo(String nombre, int idTurno, int semestre, int idCiclo, ObservableList<ItemCombo> materias, ItemCombo tutor) {
        String sqlGrupo = """
                update grupo
                set nombre=?,
                clave=?,
                id_turno=?,
                semestre=?
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
                ps.setString(2, nombre);
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

            insertarCargasSinDuplicar(con, grupoSeleccionado.getIdGrupoCiclo(), materias);
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
        cbSemestreGrupo.setValue(grupoSeleccionado.getSemestre());
        seleccionarComboPorId(cbTurnoGrupo, grupoSeleccionado.getIdTurno());
        seleccionarComboPorId(cbCicloGrupo, grupoSeleccionado.getIdCiclo());

        cargarMateriasPorSemestreYTurno();
        seleccionarMateriasAsignadas(grupoSeleccionado.getIdGrupoCiclo());
        seleccionarTutorAsignado(grupoSeleccionado.getIdGrupoCiclo());
    }

    private void seleccionarMateriasAsignadas(int idGrupoCiclo) {
        materiasSeleccionadas.clear();

        String sql = "select id_materia from carga where id_grupo_ciclo=? and id_estatus_general=1";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idGrupoCiclo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idMateria = rs.getInt("id_materia");

                    for (ItemCombo item : listViewMateriasGrupo.getItems()) {
                        if (item.getId() == idMateria) {
                            materiasSeleccionadas.add(item);
                            break;
                        }
                    }
                }
            }

            listViewMateriasGrupo.refresh();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al seleccionar materias");
        }
    }

    private void seleccionarTutorAsignado(int idGrupoCiclo) {
        cbTutorGrupo.setValue(null);

        String sql = """
                select id_maestro_tutor
                from tutoria_asignacion
                where id_grupo_ciclo=?
                and id_estatus_tutoria=1
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idGrupoCiclo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    seleccionarComboPorId(cbTutorGrupo, rs.getInt("id_maestro_tutor"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al seleccionar tutor");
        }
    }

    @FXML
    private void handleCambiarEstatusGrupo() {
        if (grupoSeleccionado == null) {
            mostrarError("selecciona un grupo de la tabla");
            return;
        }

        int nuevoEstatus = grupoSeleccionado.getEstatus().equalsIgnoreCase("activo") ? 0 : 1;

        String sqlGrupo = """
            update grupo
            set id_estatus_general=?
            where id_grupo=?
            """;

        String sqlGrupoCiclo = """
            update grupo_ciclo
            set id_estatus_general=?
            where id_grupo_ciclo=?
            """;

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlGrupo)) {
                ps.setInt(1, nuevoEstatus);
                ps.setInt(2, grupoSeleccionado.getIdGrupo());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlGrupoCiclo)) {
                ps.setInt(1, nuevoEstatus);
                ps.setInt(2, grupoSeleccionado.getIdGrupoCiclo());
                ps.executeUpdate();
            }

            con.commit();

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
                            grupo.getTurno().toLowerCase().contains(texto) ||
                            grupo.getCiclo().toLowerCase().contains(texto) ||
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
        cbTurnoGrupo.setValue(null);
        cbSemestreGrupo.setValue(null);
        cbCicloGrupo.setValue(null);
        cbTutorGrupo.setValue(null);

        listViewMateriasGrupo.getItems().clear();
        materiasSeleccionadas.clear();

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

        @Override
        public String toString() {
            return nombre;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ItemCombo item)) return false;
            return id == item.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    public static class GrupoJDM {
        private final int idGrupoCiclo;
        private final int idGrupo;
        private final String nombre;
        private final int semestre;
        private final int idTurno;
        private final String turno;
        private final int idCiclo;
        private final String ciclo;
        private final String materias;
        private final String estatus;

        public GrupoJDM(int idGrupoCiclo, int idGrupo, String nombre, int semestre, int idTurno, String turno, int idCiclo, String ciclo, String materias, String estatus) {
            this.idGrupoCiclo = idGrupoCiclo;
            this.idGrupo = idGrupo;
            this.nombre = nombre;
            this.semestre = semestre;
            this.idTurno = idTurno;
            this.turno = turno;
            this.idCiclo = idCiclo;
            this.ciclo = ciclo;
            this.materias = materias;
            this.estatus = estatus;
        }

        public int getIdGrupoCiclo() { return idGrupoCiclo; }
        public int getIdGrupo() { return idGrupo; }
        public String getNombre() { return nombre; }
        public int getSemestre() { return semestre; }
        public int getIdTurno() { return idTurno; }
        public String getTurno() { return turno; }
        public int getIdCiclo() { return idCiclo; }
        public String getCiclo() { return ciclo; }
        public String getMaterias() { return materias; }
        public String getEstatus() { return estatus; }
    }
}