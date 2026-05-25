package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionOrientacion;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GestionDeAlertasTController extends BaseController {

    @FXML private ComboBox<String> cmbGrupoFiltro;
    @FXML private ComboBox<String> cmbTipoFiltro;
    @FXML private ComboBox<String> cmbEstatusFiltro;
    @FXML private TextField txtBuscarTabla;

    @FXML private TableView<AlertaTutor> tablaGestionAlertasTutor;
    @FXML private TableColumn<AlertaTutor, String> colAlumno;
    @FXML private TableColumn<AlertaTutor, String> colGrupo;
    @FXML private TableColumn<AlertaTutor, String> colMateria;
    @FXML private TableColumn<AlertaTutor, String> colTipoAlerta;
    @FXML private TableColumn<AlertaTutor, String> colPrioridad;
    @FXML private TableColumn<AlertaTutor, String> colEstatus;

    @FXML private Label lblNombreAlumno;
    @FXML private Label lblNumeroControl;
    @FXML private Label lblGrupo;
    @FXML private Label lblSemestre;
    @FXML private Label lblTurno;
    @FXML private Label lblMateria;
    @FXML private Label lblTipoAlerta;
    @FXML private Label lblPrioridadDetalle;
    @FXML private Label lblMotivoDetalle;
    @FXML private Label lblFechaCreacion;
    @FXML private Label lblEstatusDetalle;

    private final ObservableList<AlertaTutor> listaAlertas = FXCollections.observableArrayList();
    private FilteredList<AlertaTutor> listaFiltrada;
    private AlertaTutor alertaSeleccionada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        configurarEventos();
        cargarGrupos();
        cargarAlertas();
        limpiarDetalle();
    }

    private int getIdTutorActual() {
        return SesionUsuario.getIdMaestro();
    }

    private <S> void configurarColumnaPrioridad(TableColumn<S, String> columna) {
        columna.setCellFactory(tc -> new TableCell<S, String>() {
            @Override
            protected void updateItem(String prioridad, boolean empty) {
                super.updateItem(prioridad, empty);

                if (empty || prioridad == null || prioridad.trim().isEmpty()) {
                    setText(null);
                    setStyle("");
                    return;
                }

                String valor = prioridad.toLowerCase().trim();
                setText(prioridad);

                if (valor.contains("alta")) {
                    setStyle("""
                        -fx-background-color: #fee2e2;
                        -fx-text-fill: #991b1b;
                        -fx-font-weight: bold;
                        -fx-alignment: center;
                        """);
                } else if (valor.contains("media")) {
                    setStyle("""
                        -fx-background-color: #fef3c7;
                        -fx-text-fill: #92400e;
                        -fx-font-weight: bold;
                        -fx-alignment: center;
                        """);
                } else if (valor.contains("baja")) {
                    setStyle("""
                        -fx-background-color: #dcfce7;
                        -fx-text-fill: #166534;
                        -fx-font-weight: bold;
                        -fx-alignment: center;
                        """);
                } else {
                    setStyle("""
                        -fx-background-color: transparent;
                        -fx-text-fill: #111827;
                        -fx-alignment: center;
                        """);
                }
            }
        });
    }

    private void configurarTabla() {
        colAlumno.setCellValueFactory(new PropertyValueFactory<>("alumno"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMateria.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colTipoAlerta.setCellValueFactory(new PropertyValueFactory<>("tipoAlerta"));
        colPrioridad.setCellValueFactory(new PropertyValueFactory<>("prioridad"));
        colPrioridad.setCellValueFactory(new PropertyValueFactory<>("prioridad"));
        configurarColumnaPrioridad(colPrioridad);
        colEstatus.setCellValueFactory(new PropertyValueFactory<>("estatus"));

        listaFiltrada = new FilteredList<>(listaAlertas, p -> true);
        tablaGestionAlertasTutor.setItems(listaFiltrada);

        tablaGestionAlertasTutor.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            alertaSeleccionada = newValue;
            mostrarDetalle(newValue);
        });
    }

    private void configurarFiltros() {
        cmbGrupoFiltro.setItems(FXCollections.observableArrayList("todos"));
        cmbGrupoFiltro.setValue("todos");

        cmbTipoFiltro.setItems(FXCollections.observableArrayList(
                "todos",
                "asistencia",
                "actividad",
                "conducta"
        ));
        cmbTipoFiltro.setValue("todos");

        cmbEstatusFiltro.setItems(FXCollections.observableArrayList(
                "todos",
                "activa",
                "inactiva"
        ));
        cmbEstatusFiltro.setValue("todos");
    }

    private void configurarEventos() {
        cmbGrupoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbTipoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cmbEstatusFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        txtBuscarTabla.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void cargarGrupos() {
        cmbGrupoFiltro.getItems().clear();
        cmbGrupoFiltro.getItems().add("todos");

        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        String sql = """
                select distinct
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo
                from tutoria_asignacion ta
                inner join grupo_ciclo gc on ta.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                order by grupo
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cmbGrupoFiltro.getItems().add(rs.getString("grupo"));
                }
            }

            cmbGrupoFiltro.setValue("todos");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar grupos");
        }
    }

    private void cargarAlertas() {
        listaAlertas.clear();

        int idTutor = getIdTutorActual();

        if (idTutor == 0) {
            mostrarError("no hay tutor en sesion");
            return;
        }

        String sql = """
                select distinct
                a.id_alerta,
                a.id_carga,
                al.id_alumno,
                al.num_control,
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as alumno,
                concat(g.nombre,' - ',ct.nombre,' - ',ce.nombre) as grupo,
                g.semestre,
                ct.nombre as turno,
                concat(m.clave,' - ',m.nombre) as materia,
                cta.nombre as tipo_alerta,
                cpa.nombre as prioridad,
                ifnull(a.motivo,'sin motivo') as motivo_detalle,
                cea.nombre as estatus,
                a.creada_en as fecha_orden,
                date_format(a.creada_en,'%Y-%m-%d') as fecha
                from tutoria_asignacion ta
                inner join carga c on ta.id_grupo_ciclo=c.id_grupo_ciclo
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join cat_turno ct on g.id_turno=ct.id_turno
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join alerta a on c.id_carga=a.id_carga
                inner join alumno al on a.id_alumno=al.id_alumno
                inner join cat_tipo_alerta cta on a.id_tipo_alerta=cta.id_tipo_alerta
                inner join cat_prioridad_alerta cpa on a.id_prioridad_alerta=cpa.id_prioridad_alerta
                inner join cat_estatus_alerta cea on a.id_estatus_alerta=cea.id_estatus_alerta
                where ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                and al.id_estatus_general=1
                order by fecha_orden desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idTutor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaAlertas.add(new AlertaTutor(
                            rs.getInt("id_alerta"),
                            rs.getInt("id_carga"),
                            rs.getInt("id_alumno"),
                            rs.getString("num_control"),
                            rs.getString("alumno"),
                            rs.getString("grupo"),
                            rs.getString("semestre"),
                            rs.getString("turno"),
                            rs.getString("materia"),
                            rs.getString("tipo_alerta"),
                            rs.getString("prioridad"),
                            rs.getString("motivo_detalle"),
                            rs.getString("estatus"),
                            rs.getString("fecha")
                    ));
                }
            }

            aplicarFiltro();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alertas");
        }
    }

    private void aplicarFiltro() {
        if (listaFiltrada == null) {
            return;
        }

        String grupo = cmbGrupoFiltro.getValue() == null ? "todos" : cmbGrupoFiltro.getValue().toLowerCase().trim();
        String tipo = cmbTipoFiltro.getValue() == null ? "todos" : cmbTipoFiltro.getValue().toLowerCase().trim();
        String estatusFiltro = cmbEstatusFiltro.getValue() == null ? "todos" : cmbEstatusFiltro.getValue().toLowerCase().trim();
        String texto = txtBuscarTabla.getText() == null ? "" : txtBuscarTabla.getText().toLowerCase().trim();

        listaFiltrada.setPredicate(alerta -> {
            boolean coincideGrupo =
                    grupo.equals("todos") ||
                            alerta.getGrupo().toLowerCase().contains(grupo);

            String tipoAlerta = alerta.getTipoAlerta().toLowerCase();

            boolean coincideTipo =
                    tipo.equals("todos") ||
                            tipoAlerta.equals(tipo) ||
                            (tipo.equals("actividad") && tipoAlerta.equals("calificacion")) ||
                            (tipo.equals("calificacion") && tipoAlerta.equals("actividad"));

            boolean coincideEstatus = switch (estatusFiltro) {
                case "pendiente" -> alerta.getEstatus().equalsIgnoreCase("pendiente");
                case "seguimiento" -> alerta.getEstatus().equalsIgnoreCase("seguimiento");
                case "cerrada" -> alerta.getEstatus().equalsIgnoreCase("cerrada");
                case "activa" -> alerta.getEstatus().equalsIgnoreCase("pendiente") ||
                        alerta.getEstatus().equalsIgnoreCase("seguimiento");
                case "inactiva" -> alerta.getEstatus().equalsIgnoreCase("cerrada");
                default -> true;
            };

            boolean coincideTexto =
                    texto.isEmpty() ||
                            alerta.getAlumno().toLowerCase().contains(texto) ||
                            alerta.getNumControl().toLowerCase().contains(texto) ||
                            alerta.getGrupo().toLowerCase().contains(texto) ||
                            alerta.getMateria().toLowerCase().contains(texto) ||
                            alerta.getTipoAlerta().toLowerCase().contains(texto) ||
                            alerta.getPrioridad().toLowerCase().contains(texto) ||
                            alerta.getEstatus().toLowerCase().contains(texto) ||
                            alerta.getFecha().toLowerCase().contains(texto);

            return coincideGrupo && coincideTipo && coincideEstatus && coincideTexto;
        });
    }

    private void mostrarDetalle(AlertaTutor alerta) {
        if (alerta == null) {
            limpiarDetalle();
            return;
        }

        lblNombreAlumno.setText(alerta.getAlumno());
        lblNumeroControl.setText(alerta.getNumControl());
        lblGrupo.setText(alerta.getGrupo());
        lblSemestre.setText(alerta.getSemestre());
        lblTurno.setText(alerta.getTurno());
        lblMateria.setText(alerta.getMateria());
        lblTipoAlerta.setText(alerta.getTipoAlerta());
        lblPrioridadDetalle.setText(alerta.getPrioridad());
        lblMotivoDetalle.setText(alerta.getMotivoDetalle());
        lblFechaCreacion.setText(alerta.getFecha());
        lblEstatusDetalle.setText(alerta.getEstatus());
    }

    private void limpiarDetalle() {
        lblNombreAlumno.setText("Selecciona una alerta");
        lblNumeroControl.setText("---");
        lblGrupo.setText("---");
        lblSemestre.setText("---");
        lblTurno.setText("---");
        lblMateria.setText("---");
        lblTipoAlerta.setText("Sin seleccionar");
        lblPrioridadDetalle.setText("Sin seleccionar");
        lblMotivoDetalle.setText("Selecciona una alerta para mostrar el motivo detallado.");
        lblFechaCreacion.setText("---");
        lblEstatusDetalle.setText("---");
    }

    @FXML
    private void handleCambiarEstatus() {
        if (alertaSeleccionada == null) {
            mostrarError("selecciona una alerta");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>("seguimiento", "pendiente", "seguimiento", "cerrada");
        dialog.setTitle("cambiar estatus");
        dialog.setHeaderText("Selecciona el nuevo estatus");
        dialog.setContentText("Estatus:");

        dialog.showAndWait().ifPresent(this::actualizarEstatus);
    }

    @FXML
    private void handleRegistrarOrientacion(ActionEvent event) {
        if (alertaSeleccionada == null) {
            mostrarError("selecciona una alerta");
            return;
        }

        SesionOrientacion.limpiar();

        SesionOrientacion.setIdAlerta(alertaSeleccionada.getIdAlerta());
        SesionOrientacion.setIdAlumno(alertaSeleccionada.getIdAlumno());
        SesionOrientacion.setNumControl(alertaSeleccionada.getNumControl());
        SesionOrientacion.setNombreAlumno(alertaSeleccionada.getAlumno());
        SesionOrientacion.setGrupo(alertaSeleccionada.getGrupo());
        SesionOrientacion.setSemestre(alertaSeleccionada.getSemestre());
        SesionOrientacion.setTurno(alertaSeleccionada.getTurno());
        SesionOrientacion.setMateria(alertaSeleccionada.getMateria());
        SesionOrientacion.setTipoAlerta(alertaSeleccionada.getTipoAlerta());
        SesionOrientacion.setPrioridad(alertaSeleccionada.getPrioridad());
        SesionOrientacion.setMotivoDetalle(alertaSeleccionada.getMotivoDetalle());
        SesionOrientacion.setFechaAlerta(alertaSeleccionada.getFecha());

        cargarVistaOrientacion(event);
    }

    private void cargarVistaOrientacion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/proyecto/views/tutor/OrientacionT.fxml"));
            Parent vista = loader.load();

            Node source = (Node) event.getSource();
            StackPane contentArea = (StackPane) source.getScene().lookup("#contentArea");

            if (contentArea == null) {
                mostrarError("no se encontro el contenedor principal");
                return;
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(vista);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("no se pudo abrir la vista de orientacion");
        }
    }

    @FXML
    private void handleCerrarAlerta() {
        if (alertaSeleccionada == null) {
            mostrarError("selecciona una alerta");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("confirmar");
        confirmacion.setHeaderText("cerrar alerta");
        confirmacion.setContentText("seguro que deseas cerrar esta alerta?");

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.OK) {
                actualizarEstatus("cerrada");
            }
        });
    }

    private void actualizarEstatus(String estatus) {
        int idEstatus = switch (estatus.toLowerCase()) {
            case "pendiente" -> 1;
            case "seguimiento" -> 2;
            case "cerrada" -> 0;
            default -> 1;
        };

        String sql = """
                update alerta a
                inner join carga c on a.id_carga=c.id_carga
                inner join tutoria_asignacion ta on c.id_grupo_ciclo=ta.id_grupo_ciclo
                set
                a.id_estatus_alerta=?,
                a.cerrada_en=case when ?=0 then now() else null end
                where a.id_alerta=?
                and ta.id_maestro_tutor=?
                and ta.id_estatus_tutoria=1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idEstatus);
            ps.setInt(2, idEstatus);
            ps.setInt(3, alertaSeleccionada.getIdAlerta());
            ps.setInt(4, getIdTutorActual());

            ps.executeUpdate();

            mostrarInfo("estatus actualizado correctamente");
            cargarAlertas();
            limpiarDetalle();
            alertaSeleccionada = null;

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al actualizar estatus");
        }
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

    public static class AlertaTutor {
        private final int idAlerta;
        private final int idCarga;
        private final int idAlumno;
        private final String numControl;
        private final String alumno;
        private final String grupo;
        private final String semestre;
        private final String turno;
        private final String materia;
        private final String tipoAlerta;
        private final String prioridad;
        private final String motivoDetalle;
        private final String estatus;
        private final String fecha;

        public AlertaTutor(int idAlerta, int idCarga, int idAlumno, String numControl, String alumno, String grupo, String semestre, String turno,
                           String materia, String tipoAlerta, String prioridad, String motivoDetalle, String estatus, String fecha) {
            this.idAlerta = idAlerta;
            this.idCarga = idCarga;
            this.idAlumno = idAlumno;
            this.numControl = textoSeguro(numControl);
            this.alumno = textoSeguro(alumno);
            this.grupo = textoSeguro(grupo);
            this.semestre = textoSeguro(semestre);
            this.turno = textoSeguro(turno);
            this.materia = textoSeguro(materia);
            this.tipoAlerta = textoSeguro(tipoAlerta);
            this.prioridad = textoSeguro(prioridad);
            this.motivoDetalle = textoSeguro(motivoDetalle);
            this.estatus = textoSeguro(estatus);
            this.fecha = textoSeguro(fecha);
        }

        private static String textoSeguro(String valor) {
            return valor == null ? "" : valor;
        }

        public int getIdAlerta() {
            return idAlerta;
        }

        public int getIdCarga() {
            return idCarga;
        }

        public int getIdAlumno() {
            return idAlumno;
        }

        public String getNumControl() {
            return numControl;
        }

        public String getAlumno() {
            return alumno;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getSemestre() {
            return semestre;
        }

        public String getTurno() {
            return turno;
        }

        public String getMateria() {
            return materia;
        }

        public String getTipoAlerta() {
            return tipoAlerta;
        }

        public String getPrioridad() {
            return prioridad;
        }

        public String getMotivoDetalle() {
            return motivoDetalle;
        }

        public String getEstatus() {
            return estatus;
        }

        public String getFecha() {
            return fecha;
        }
    }
}