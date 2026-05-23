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

public class MateriasJDMController extends BaseController {

    @FXML private TextField txtNombreMateria;
    @FXML private TextField txtClaveMateria;
    @FXML private ComboBox<Integer> cbSemestreMateria;
    @FXML private ComboBox<ComboItem> cbTurnoMateria;
    @FXML private ComboBox<ComboItem> cbMaestroMateria;

    @FXML private TextField txtBuscarMateriaInterna;
    @FXML private ComboBox<String> cbFiltroMaterias;

    @FXML private TableView<MateriaJDM> tablaMaterias;
    @FXML private TableColumn<MateriaJDM, String> colNombreMateria;
    @FXML private TableColumn<MateriaJDM, String> colClaveMateria;
    @FXML private TableColumn<MateriaJDM, Integer> colSemestreMateria;
    @FXML private TableColumn<MateriaJDM, String> colTurnoMateria;
    @FXML private TableColumn<MateriaJDM, String> colMaestroMateria;
    @FXML private TableColumn<MateriaJDM, String> colEstatusMateria;

    private final ObservableList<MateriaJDM> listaMaterias = FXCollections.observableArrayList();
    private MateriaJDM materiaSeleccionada;
    private boolean modoEdicion = false;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        cargarMaterias();
        configurarBusqueda();
    }

    private void configurarTabla() {
        colNombreMateria.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colClaveMateria.setCellValueFactory(new PropertyValueFactory<>("clave"));
        colSemestreMateria.setCellValueFactory(new PropertyValueFactory<>("semestre"));
        colTurnoMateria.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colMaestroMateria.setCellValueFactory(new PropertyValueFactory<>("maestro"));
        colEstatusMateria.setCellValueFactory(new PropertyValueFactory<>("estatus"));
    }

    private void configurarCombos() {
        cbSemestreMateria.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9));

        cbFiltroMaterias.setItems(FXCollections.observableArrayList("todos","activo","inactivo"));
        cbFiltroMaterias.setValue("todos");

        cargarTurnos();
        cargarMaestros();
    }

    private void cargarTurnos() {
        cbTurnoMateria.getItems().clear();

        String sql = "select id_turno,nombre from cat_turno order by id_turno";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbTurnoMateria.getItems().add(new ComboItem(
                        rs.getInt("id_turno"),
                        rs.getString("nombre")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar turnos");
        }
    }

    private void cargarMaestros() {
        cbMaestroMateria.getItems().clear();

        String sql = """
                select
                id_maestro,
                concat(nombre,' ',apellido_paterno,' ',apellido_materno) as maestro
                from maestro
                where id_estatus_general=1
                order by nombre,apellido_paterno
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbMaestroMateria.getItems().add(new ComboItem(
                        rs.getInt("id_maestro"),
                        rs.getString("maestro")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar maestros");
        }
    }

    private void configurarEventos() {
        tablaMaterias.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            materiaSeleccionada = newValue;
        });
    }

    private void cargarMaterias() {
        listaMaterias.clear();

        String sql = """
                select
                m.id_materia,
                m.nombre,
                m.clave,
                m.semestre,
                m.id_turno,
                ct.nombre as turno,
                m.id_maestro,
                concat(ma.nombre,' ',ma.apellido_paterno,' ',ma.apellido_materno) as maestro,
                ceg.nombre as estatus
                from materia m
                inner join cat_turno ct on m.id_turno=ct.id_turno
                inner join maestro ma on m.id_maestro=ma.id_maestro
                inner join cat_estatus_general ceg on m.id_estatus_general=ceg.id_estatus_general
                order by m.semestre,ct.nombre,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaMaterias.add(new MateriaJDM(
                        rs.getInt("id_materia"),
                        rs.getString("nombre"),
                        rs.getString("clave"),
                        rs.getInt("semestre"),
                        rs.getInt("id_turno"),
                        rs.getString("turno"),
                        rs.getInt("id_maestro"),
                        rs.getString("maestro"),
                        rs.getString("estatus")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar materias");
        }
    }

    @FXML
    private void handleGuardarMateria() {
        String nombre = txtNombreMateria.getText() == null ? "" : txtNombreMateria.getText().trim().toLowerCase();
        String clave = txtClaveMateria.getText() == null ? "" : txtClaveMateria.getText().trim().toLowerCase();
        Integer semestre = cbSemestreMateria.getValue();
        ComboItem turno = cbTurnoMateria.getValue();
        ComboItem maestro = cbMaestroMateria.getValue();

        if (nombre.isEmpty() || clave.isEmpty() || semestre == null || turno == null || maestro == null) {
            mostrarError("captura nombre, clave, semestre, turno y maestro");
            return;
        }

        if (modoEdicion && materiaSeleccionada != null) {
            actualizarMateria(nombre, clave, semestre, turno.getId(), maestro.getId());
        } else {
            insertarMateria(nombre, clave, semestre, turno.getId(), maestro.getId());
        }
    }

    private void insertarMateria(String nombre, String clave, int semestre, int idTurno, int idMaestro) {
        String sql = """
                insert into materia(clave,nombre,semestre,id_turno,id_maestro,id_estatus_general)
                values(?,?,?,?,?,1)
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, clave);
            ps.setString(2, nombre);
            ps.setInt(3, semestre);
            ps.setInt(4, idTurno);
            ps.setInt(5, idMaestro);
            ps.executeUpdate();

            mostrarInfo("materia guardada correctamente");
            limpiarFormulario();
            cargarMaterias();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar materia. verifica que la clave no exista");
        }
    }

    private void actualizarMateria(String nombre, String clave, int semestre, int idTurno, int idMaestro) {
        String sql = """
                update materia
                set nombre=?,
                clave=?,
                semestre=?,
                id_turno=?,
                id_maestro=?
                where id_materia=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, clave);
            ps.setInt(3, semestre);
            ps.setInt(4, idTurno);
            ps.setInt(5, idMaestro);
            ps.setInt(6, materiaSeleccionada.getIdMateria());
            ps.executeUpdate();

            mostrarInfo("materia actualizada correctamente");
            limpiarFormulario();
            cargarMaterias();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al actualizar materia");
        }
    }

    @FXML
    private void handleEditarMateria() {
        if (materiaSeleccionada == null) {
            mostrarError("selecciona una materia de la tabla");
            return;
        }

        modoEdicion = true;

        txtNombreMateria.setText(materiaSeleccionada.getNombre());
        txtClaveMateria.setText(materiaSeleccionada.getClave());
        cbSemestreMateria.setValue(materiaSeleccionada.getSemestre());

        seleccionarCombo(cbTurnoMateria, materiaSeleccionada.getIdTurno());
        seleccionarCombo(cbMaestroMateria, materiaSeleccionada.getIdMaestro());
    }

    private void seleccionarCombo(ComboBox<ComboItem> combo, int id) {
        for (ComboItem item : combo.getItems()) {
            if (item.getId() == id) {
                combo.setValue(item);
                return;
            }
        }
    }

    @FXML
    private void handleCambiarEstatusMateria() {
        if (materiaSeleccionada == null) {
            mostrarError("selecciona una materia de la tabla");
            return;
        }

        int nuevoEstatus = materiaSeleccionada.getEstatus().equalsIgnoreCase("activo") ? 0 : 1;

        String sql = "update materia set id_estatus_general=? where id_materia=?";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, nuevoEstatus);
            ps.setInt(2, materiaSeleccionada.getIdMateria());
            ps.executeUpdate();

            mostrarInfo("estatus actualizado correctamente");
            limpiarFormulario();
            cargarMaterias();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cambiar estatus de materia");
        }
    }

    private void configurarBusqueda() {
        FilteredList<MateriaJDM> filtro = new FilteredList<>(listaMaterias, p -> true);

        txtBuscarMateriaInterna.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));
        cbFiltroMaterias.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));

        tablaMaterias.setItems(filtro);
    }

    private void aplicarFiltro(FilteredList<MateriaJDM> filtro) {
        String texto = txtBuscarMateriaInterna.getText() == null ? "" : txtBuscarMateriaInterna.getText().toLowerCase();
        String estatus = cbFiltroMaterias.getValue() == null ? "todos" : cbFiltroMaterias.getValue().toLowerCase();

        filtro.setPredicate(materia -> {
            boolean coincideTexto =
                    materia.getNombre().toLowerCase().contains(texto) ||
                            materia.getClave().toLowerCase().contains(texto) ||
                            materia.getTurno().toLowerCase().contains(texto) ||
                            materia.getMaestro().toLowerCase().contains(texto) ||
                            String.valueOf(materia.getSemestre()).contains(texto);

            boolean coincideEstatus =
                    estatus.equals("todos") ||
                            materia.getEstatus().toLowerCase().equals(estatus);

            return coincideTexto && coincideEstatus;
        });
    }

    private void limpiarFormulario() {
        txtNombreMateria.clear();
        txtClaveMateria.clear();
        cbSemestreMateria.setValue(null);
        cbTurnoMateria.setValue(null);
        cbMaestroMateria.setValue(null);

        materiaSeleccionada = null;
        modoEdicion = false;
        tablaMaterias.getSelectionModel().clearSelection();
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

    public static class ComboItem {
        private final int id;
        private final String nombre;

        public ComboItem(int id, String nombre) {
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
    }

    public static class MateriaJDM {
        private final int idMateria;
        private final String nombre;
        private final String clave;
        private final int semestre;
        private final int idTurno;
        private final String turno;
        private final int idMaestro;
        private final String maestro;
        private final String estatus;

        public MateriaJDM(int idMateria, String nombre, String clave, int semestre, int idTurno, String turno, int idMaestro, String maestro, String estatus) {
            this.idMateria = idMateria;
            this.nombre = nombre;
            this.clave = clave;
            this.semestre = semestre;
            this.idTurno = idTurno;
            this.turno = turno;
            this.idMaestro = idMaestro;
            this.maestro = maestro;
            this.estatus = estatus;
        }

        public int getIdMateria() {
            return idMateria;
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

        public int getIdMaestro() {
            return idMaestro;
        }

        public String getMaestro() {
            return maestro;
        }

        public String getEstatus() {
            return estatus;
        }
    }
}