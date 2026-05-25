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

public class CatalogoMateriasJDMController extends BaseController {

    @FXML private TextField txtNombreMateria;
    @FXML private TextField txtClaveMateria;
    @FXML private ComboBox<Integer> cbSemestreMateria;

    @FXML private TextField txtBuscarMateriaInterna;
    @FXML private ComboBox<String> cbFiltroMaterias;

    @FXML private TableView<CatalogoMateriaJDM> tablaMaterias;
    @FXML private TableColumn<CatalogoMateriaJDM, String> colNombreMateria;
    @FXML private TableColumn<CatalogoMateriaJDM, String> colClaveMateria;
    @FXML private TableColumn<CatalogoMateriaJDM, Integer> colSemestreMateria;
    @FXML private TableColumn<CatalogoMateriaJDM, String> colEstatusMateria;

    private final ObservableList<CatalogoMateriaJDM> listaMaterias = FXCollections.observableArrayList();
    private CatalogoMateriaJDM materiaSeleccionada;
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
        colEstatusMateria.setCellValueFactory(new PropertyValueFactory<>("estatus"));
    }

    private void configurarCombos() {
        cbSemestreMateria.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9));

        cbFiltroMaterias.setItems(FXCollections.observableArrayList("todos","activo","inactivo"));
        cbFiltroMaterias.setValue("todos");
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
                id_materia,
                nombre_materia,
                clave_materia,
                semestre,
                estatus
                from vw_catalogo_materias_jefe
                order by semestre,nombre_materia
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaMaterias.add(new CatalogoMateriaJDM(
                        rs.getInt("id_materia"),
                        rs.getString("nombre_materia"),
                        rs.getString("clave_materia"),
                        rs.getInt("semestre"),
                        rs.getString("estatus")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar catalogo de materias");
        }
    }

    @FXML
    private void handleGuardarMateria() {
        String nombre = txtNombreMateria.getText() == null ? "" : txtNombreMateria.getText().trim().toLowerCase();
        String clave = txtClaveMateria.getText() == null ? "" : txtClaveMateria.getText().trim().toLowerCase();
        Integer semestre = cbSemestreMateria.getValue();

        if (nombre.isEmpty() || clave.isEmpty() || semestre == null) {
            mostrarError("captura nombre, clave y semestre");
            return;
        }

        if (modoEdicion && materiaSeleccionada != null) {
            actualizarMateria(nombre, clave, semestre);
        } else {
            insertarMateria(nombre, clave, semestre);
        }
    }

    private void insertarMateria(String nombre, String clave, int semestre) {
        String sql = """
                insert into materia(clave,nombre,semestre,id_estatus_general)
                values(?,?,?,1)
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, clave);
            ps.setString(2, nombre);
            ps.setInt(3, semestre);
            ps.executeUpdate();

            mostrarInfo("materia guardada correctamente");
            limpiarFormulario();
            cargarMaterias();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar materia. verifica que la clave no exista");
        }
    }

    private void actualizarMateria(String nombre, String clave, int semestre) {
        String sql = """
                update materia
                set nombre=?,
                clave=?,
                semestre=?
                where id_materia=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, clave);
            ps.setInt(3, semestre);
            ps.setInt(4, materiaSeleccionada.getIdMateria());
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
        FilteredList<CatalogoMateriaJDM> filtro = new FilteredList<>(listaMaterias, p -> true);

        txtBuscarMateriaInterna.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));
        cbFiltroMaterias.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));

        tablaMaterias.setItems(filtro);
    }

    private void aplicarFiltro(FilteredList<CatalogoMateriaJDM> filtro) {
        String texto = txtBuscarMateriaInterna.getText() == null ? "" : txtBuscarMateriaInterna.getText().toLowerCase();
        String estatus = cbFiltroMaterias.getValue() == null ? "todos" : cbFiltroMaterias.getValue().toLowerCase();

        filtro.setPredicate(materia -> {
            boolean coincideTexto =
                    materia.getNombre().toLowerCase().contains(texto) ||
                            materia.getClave().toLowerCase().contains(texto) ||
                            String.valueOf(materia.getSemestre()).contains(texto) ||
                            materia.getEstatus().toLowerCase().contains(texto);

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

    public static class CatalogoMateriaJDM {
        private final int idMateria;
        private final String nombre;
        private final String clave;
        private final int semestre;
        private final String estatus;

        public CatalogoMateriaJDM(int idMateria, String nombre, String clave, int semestre, String estatus) {
            this.idMateria = idMateria;
            this.nombre = nombre;
            this.clave = clave;
            this.semestre = semestre;
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

        public String getEstatus() {
            return estatus;
        }
    }
}