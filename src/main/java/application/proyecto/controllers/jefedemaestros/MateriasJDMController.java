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

    @FXML private ComboBox<ComboItem> cbMateriaCatalogo;
    @FXML private ComboBox<ComboItem> cbGrupoCicloMateria;
    @FXML private ComboBox<ComboItem> cbMaestroMateria;

    @FXML private TextField txtBuscarMateriaInterna;
    @FXML private ComboBox<String> cbFiltroMaterias;

    @FXML private TableView<MateriaJDM> tablaMaterias;
    @FXML private TableColumn<MateriaJDM, String> colNombreMateria;
    @FXML private TableColumn<MateriaJDM, String> colClaveMateria;
    @FXML private TableColumn<MateriaJDM, Integer> colSemestreMateria;
    @FXML private TableColumn<MateriaJDM, String> colGrupoMateria;
    @FXML private TableColumn<MateriaJDM, String> colTurnoMateria;
    @FXML private TableColumn<MateriaJDM, String> colCicloMateria;
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
        colGrupoMateria.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colTurnoMateria.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colCicloMateria.setCellValueFactory(new PropertyValueFactory<>("ciclo"));
        colMaestroMateria.setCellValueFactory(new PropertyValueFactory<>("maestro"));
        colEstatusMateria.setCellValueFactory(new PropertyValueFactory<>("estatus"));
    }

    private void configurarCombos() {
        cbFiltroMaterias.setItems(FXCollections.observableArrayList("todos", "activo", "inactivo"));
        cbFiltroMaterias.setValue("todos");

        cargarCatalogoMaterias();
        cargarGruposCiclo();
        cargarMaestros();
    }

    private void cargarCatalogoMaterias() {
        cbMateriaCatalogo.getItems().clear();

        String sql = """
                select
                m.id_materia,
                concat(m.clave,' - ',m.nombre,' (sem ',m.semestre,')') as materia
                from materia m
                where m.id_estatus_general=1
                order by m.semestre,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbMateriaCatalogo.getItems().add(new ComboItem(
                        rs.getInt("id_materia"),
                        rs.getString("materia")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar catalogo de materias");
        }
    }

    private void cargarGruposCiclo() {
        cbGrupoCicloMateria.getItems().clear();

        String sql = """
                select
                gc.id_grupo_ciclo,
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo_ciclo
                from grupo_ciclo gc
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_turno ct on g.id_turno=ct.id_turno
                where gc.id_estatus_general=1
                and g.id_estatus_general=1
                order by ce.nombre,g.semestre,g.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbGrupoCicloMateria.getItems().add(new ComboItem(
                        rs.getInt("id_grupo_ciclo"),
                        rs.getString("grupo_ciclo")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos");
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
                order by nombre,apellido_paterno,apellido_materno
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
                c.id_carga,
                c.id_materia,
                c.id_grupo_ciclo,
                c.id_maestro,
                m.nombre as nombre_materia,
                m.clave as clave_materia,
                m.semestre,
                g.nombre as grupo,
                ct.nombre as turno,
                ce.nombre as ciclo_escolar,
                concat(ma.nombre,' ',ma.apellido_paterno,' ',ma.apellido_materno) as maestro,
                ceg.nombre as estatus
                from carga c
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_turno ct on ct.id_turno=coalesce(c.id_turno,g.id_turno)
                inner join maestro ma on c.id_maestro=ma.id_maestro
                inner join cat_estatus_general ceg on c.id_estatus_general=ceg.id_estatus_general
                order by ce.nombre,g.semestre,g.nombre,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaMaterias.add(new MateriaJDM(
                        rs.getInt("id_carga"),
                        rs.getInt("id_materia"),
                        rs.getInt("id_grupo_ciclo"),
                        rs.getInt("id_maestro"),
                        rs.getString("nombre_materia"),
                        rs.getString("clave_materia"),
                        rs.getInt("semestre"),
                        rs.getString("grupo"),
                        rs.getString("turno"),
                        rs.getString("ciclo_escolar"),
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
        ComboItem materia = cbMateriaCatalogo.getValue();
        ComboItem grupoCiclo = cbGrupoCicloMateria.getValue();
        ComboItem maestro = cbMaestroMateria.getValue();

        if (materia == null || grupoCiclo == null || maestro == null) {
            mostrarError("selecciona materia, grupo y maestro");
            return;
        }

        if (modoEdicion && materiaSeleccionada != null) {
            actualizarMateria(materia.getId(), grupoCiclo.getId(), maestro.getId());
        } else {
            insertarMateria(materia.getId(), grupoCiclo.getId(), maestro.getId());
        }
    }

    private void insertarMateria(int idMateria, int idGrupoCiclo, int idMaestro) {
        int idTurno = obtenerTurnoPorGrupoCiclo(idGrupoCiclo);

        if (idTurno == 0) {
            mostrarError("no se pudo obtener el turno del grupo seleccionado");
            return;
        }

        CargaExistente cargaExistente = buscarCargaExistente(idMateria, idGrupoCiclo, 0);

        if (cargaExistente != null) {
            if (cargaExistente.getIdEstatusGeneral() == 1) {
                mostrarError("esta clase ya existe activa en el grupo seleccionado");
                return;
            }

            reactivarMateria(cargaExistente.getIdCarga(), idMaestro, idTurno);
            return;
        }

        String sql = """
                insert into carga(id_grupo_ciclo,id_materia,id_maestro,id_turno,id_estatus_general)
                values(?,?,?,?,1)
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idGrupoCiclo);
            ps.setInt(2, idMateria);
            ps.setInt(3, idMaestro);
            ps.setInt(4, idTurno);
            ps.executeUpdate();

            mostrarInfo("clase guardada correctamente");
            limpiarFormulario();
            cargarMaterias();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al guardar clase");
        }
    }

    private void reactivarMateria(int idCarga, int idMaestro, int idTurno) {
        String sql = """
                update carga
                set id_maestro=?,
                id_turno=?,
                id_estatus_general=1
                where id_carga=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);
            ps.setInt(2, idTurno);
            ps.setInt(3, idCarga);
            ps.executeUpdate();

            mostrarInfo("clase reactivada correctamente");
            limpiarFormulario();
            cargarMaterias();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al reactivar clase");
        }
    }

    private void actualizarMateria(int idMateria, int idGrupoCiclo, int idMaestro) {
        int idTurno = obtenerTurnoPorGrupoCiclo(idGrupoCiclo);

        if (idTurno == 0) {
            mostrarError("no se pudo obtener el turno del grupo seleccionado");
            return;
        }

        CargaExistente cargaExistente = buscarCargaExistente(idMateria, idGrupoCiclo, materiaSeleccionada.getIdCarga());

        if (cargaExistente != null) {
            mostrarError("ya existe otra clase con esa materia y ese grupo");
            return;
        }

        String sql = """
                update carga
                set id_grupo_ciclo=?,
                id_materia=?,
                id_maestro=?,
                id_turno=?
                where id_carga=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idGrupoCiclo);
            ps.setInt(2, idMateria);
            ps.setInt(3, idMaestro);
            ps.setInt(4, idTurno);
            ps.setInt(5, materiaSeleccionada.getIdCarga());
            ps.executeUpdate();

            mostrarInfo("clase actualizada correctamente");
            limpiarFormulario();
            cargarMaterias();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al actualizar clase");
        }
    }

    private CargaExistente buscarCargaExistente(int idMateria, int idGrupoCiclo, int idCargaExcluir) {
        String sql = """
                select id_carga,id_estatus_general
                from carga
                where id_materia=?
                and id_grupo_ciclo=?
                and id_carga<>?
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMateria);
            ps.setInt(2, idGrupoCiclo);
            ps.setInt(3, idCargaExcluir);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CargaExistente(
                            rs.getInt("id_carga"),
                            rs.getInt("id_estatus_general")
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private int obtenerTurnoPorGrupoCiclo(int idGrupoCiclo) {
        String sql = """
                select g.id_turno
                from grupo_ciclo gc
                inner join grupo g on gc.id_grupo=g.id_grupo
                where gc.id_grupo_ciclo=?
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idGrupoCiclo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_turno");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    @FXML
    private void handleEditarMateria() {
        if (materiaSeleccionada == null) {
            mostrarError("selecciona una materia de la tabla");
            return;
        }

        modoEdicion = true;

        seleccionarCombo(cbMateriaCatalogo, materiaSeleccionada.getIdMateria());
        seleccionarCombo(cbGrupoCicloMateria, materiaSeleccionada.getIdGrupoCiclo());
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

        String sql = "update carga set id_estatus_general=? where id_carga=?";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, nuevoEstatus);
            ps.setInt(2, materiaSeleccionada.getIdCarga());
            ps.executeUpdate();

            mostrarInfo("estatus actualizado correctamente");
            limpiarFormulario();
            cargarMaterias();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cambiar estatus de clase");
        }
    }

    private void configurarBusqueda() {
        FilteredList<MateriaJDM> filtro = new FilteredList<>(listaMaterias, p -> true);

        txtBuscarMateriaInterna.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));
        cbFiltroMaterias.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(filtro));

        tablaMaterias.setItems(filtro);
    }

    private void aplicarFiltro(FilteredList<MateriaJDM> filtro) {
        String texto = txtBuscarMateriaInterna.getText() == null ? "" : txtBuscarMateriaInterna.getText().toLowerCase().trim();
        String estatus = cbFiltroMaterias.getValue() == null ? "todos" : cbFiltroMaterias.getValue().toLowerCase().trim();

        filtro.setPredicate(materia -> {
            boolean coincideTexto =
                    materia.getNombre().toLowerCase().contains(texto) ||
                            materia.getClave().toLowerCase().contains(texto) ||
                            materia.getGrupo().toLowerCase().contains(texto) ||
                            materia.getTurno().toLowerCase().contains(texto) ||
                            materia.getCiclo().toLowerCase().contains(texto) ||
                            materia.getMaestro().toLowerCase().contains(texto) ||
                            materia.getEstatus().toLowerCase().contains(texto) ||
                            String.valueOf(materia.getSemestre()).contains(texto);

            boolean coincideEstatus =
                    estatus.equals("todos") ||
                            materia.getEstatus().toLowerCase().equals(estatus);

            return coincideTexto && coincideEstatus;
        });
    }

    private void limpiarFormulario() {
        cbMateriaCatalogo.setValue(null);
        cbGrupoCicloMateria.setValue(null);
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
            this.nombre = nombre == null ? "" : nombre;
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

    public static class CargaExistente {
        private final int idCarga;
        private final int idEstatusGeneral;

        public CargaExistente(int idCarga, int idEstatusGeneral) {
            this.idCarga = idCarga;
            this.idEstatusGeneral = idEstatusGeneral;
        }

        public int getIdCarga() {
            return idCarga;
        }

        public int getIdEstatusGeneral() {
            return idEstatusGeneral;
        }
    }

    public static class MateriaJDM {
        private final int idCarga;
        private final int idMateria;
        private final int idGrupoCiclo;
        private final int idMaestro;
        private final String nombre;
        private final String clave;
        private final int semestre;
        private final String grupo;
        private final String turno;
        private final String ciclo;
        private final String maestro;
        private final String estatus;

        public MateriaJDM(int idCarga, int idMateria, int idGrupoCiclo, int idMaestro, String nombre, String clave, int semestre, String grupo, String turno, String ciclo, String maestro, String estatus) {
            this.idCarga = idCarga;
            this.idMateria = idMateria;
            this.idGrupoCiclo = idGrupoCiclo;
            this.idMaestro = idMaestro;
            this.nombre = nombre == null ? "" : nombre;
            this.clave = clave == null ? "" : clave;
            this.semestre = semestre;
            this.grupo = grupo == null ? "" : grupo;
            this.turno = turno == null ? "" : turno;
            this.ciclo = ciclo == null ? "" : ciclo;
            this.maestro = maestro == null ? "" : maestro;
            this.estatus = estatus == null ? "" : estatus;
        }

        public int getIdCarga() {
            return idCarga;
        }

        public int getIdMateria() {
            return idMateria;
        }

        public int getIdGrupoCiclo() {
            return idGrupoCiclo;
        }

        public int getIdMaestro() {
            return idMaestro;
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

        public String getGrupo() {
            return grupo;
        }

        public String getTurno() {
            return turno;
        }

        public String getCiclo() {
            return ciclo;
        }

        public String getMaestro() {
            return maestro;
        }

        public String getEstatus() {
            return estatus;
        }
    }
}