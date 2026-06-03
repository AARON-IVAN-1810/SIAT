package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import application.proyecto.utils.ConexionBD;
import application.proyecto.utils.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.text.Normalizer;
import java.time.LocalTime;
import java.util.*;

public class ImportacionClasesJDMController extends BaseController {

    @FXML private Label lblTotalFilas;
    @FXML private Label lblFilasValidas;
    @FXML private Label lblFilasPendientes;
    @FXML private Label lblFilasCargadas;
    @FXML private Label lblLinksAbiertos;

    @FXML private ComboBox<CicloItem> cbCicloEscolar;
    @FXML private TextField txtNombreLote;
    @FXML private TextField txtArchivo;
    @FXML private TextArea txtNotasLote;

    @FXML private Label lblLoteSeleccionado;
    @FXML private TextField txtUrlBase;
    @FXML private DatePicker dpFechaLimite;
    @FXML private TextArea txtNotasLink;
    @FXML private TextArea txtLinkCompleto;
    @FXML private Label lblEstadoLink;

    @FXML private TextField txtBuscarDetalle;
    @FXML private ComboBox<String> cbEstatusFiltro;

    @FXML private TableView<LoteItem> tablaLotes;
    @FXML private TableColumn<LoteItem, String> colLoteNombre;
    @FXML private TableColumn<LoteItem, String> colLoteArchivo;
    @FXML private TableColumn<LoteItem, String> colLoteCiclo;
    @FXML private TableColumn<LoteItem, Integer> colLoteTotal;
    @FXML private TableColumn<LoteItem, Integer> colLoteValidas;
    @FXML private TableColumn<LoteItem, Integer> colLotePendientes;
    @FXML private TableColumn<LoteItem, Integer> colLoteCargadas;
    @FXML private TableColumn<LoteItem, String> colLoteEstado;
    @FXML private TableColumn<LoteItem, String> colLoteFecha;

    @FXML private TableView<DetalleItem> tablaDetalle;
    @FXML private TableColumn<DetalleItem, Integer> colFila;
    @FXML private TableColumn<DetalleItem, String> colGrupoDocumento;
    @FXML private TableColumn<DetalleItem, String> colMateriaDocumento;
    @FXML private TableColumn<DetalleItem, String> colPaqueteDocumento;
    @FXML private TableColumn<DetalleItem, String> colDocenteDocumento;
    @FXML private TableColumn<DetalleItem, Integer> colSemestreDetectado;
    @FXML private TableColumn<DetalleItem, String> colMateriaDetectada;
    @FXML private TableColumn<DetalleItem, String> colGrupoDetectado;
    @FXML private TableColumn<DetalleItem, String> colMaestroDetectado;
    @FXML private TableColumn<DetalleItem, String> colEstatusImportacion;
    @FXML private TableColumn<DetalleItem, String> colObservaciones;

    @FXML private TableView<LinkItem> tablaLinks;
    @FXML private TableColumn<LinkItem, String> colLinkLote;
    @FXML private TableColumn<LinkItem, String> colLinkCiclo;
    @FXML private TableColumn<LinkItem, String> colLinkFechaLimite;
    @FXML private TableColumn<LinkItem, Integer> colLinkDisponibles;
    @FXML private TableColumn<LinkItem, Integer> colLinkCargadas;
    @FXML private TableColumn<LinkItem, String> colLinkEstado;
    @FXML private TableColumn<LinkItem, String> colLinkCreado;

    private final ObservableList<LoteItem> listaLotes = FXCollections.observableArrayList();
    private final ObservableList<DetalleItem> listaDetalle = FXCollections.observableArrayList();
    private final ObservableList<LinkItem> listaLinks = FXCollections.observableArrayList();

    private FilteredList<DetalleItem> detalleFiltrado;

    private File archivoSeleccionado;
    private LoteItem loteSeleccionado;
    private LinkItem linkSeleccionado;

    @FXML
    public void initialize() {
        configurarTablas();
        configurarFiltros();
        configurarEventos();
        cargarCiclos();
        cargarLotes();
        cargarLinks();
        limpiarDetalleLink();
    }

    private int getIdUsuarioActual() {
        try {
            return SesionUsuario.getIdUsuario();
        } catch (Exception e) {
            return 0;
        }
    }

    private void configurarTablas() {
        colLoteNombre.setCellValueFactory(new PropertyValueFactory<>("nombreLote"));
        colLoteArchivo.setCellValueFactory(new PropertyValueFactory<>("nombreArchivo"));
        colLoteCiclo.setCellValueFactory(new PropertyValueFactory<>("cicloEscolar"));
        colLoteTotal.setCellValueFactory(new PropertyValueFactory<>("totalFilas"));
        colLoteValidas.setCellValueFactory(new PropertyValueFactory<>("filasValidas"));
        colLotePendientes.setCellValueFactory(new PropertyValueFactory<>("filasPendientes"));
        colLoteCargadas.setCellValueFactory(new PropertyValueFactory<>("filasCargadas"));
        colLoteEstado.setCellValueFactory(new PropertyValueFactory<>("estatus"));
        colLoteFecha.setCellValueFactory(new PropertyValueFactory<>("creadoEn"));
        tablaLotes.setItems(listaLotes);

        colFila.setCellValueFactory(new PropertyValueFactory<>("filaArchivo"));
        colGrupoDocumento.setCellValueFactory(new PropertyValueFactory<>("grupoDocumento"));
        colMateriaDocumento.setCellValueFactory(new PropertyValueFactory<>("materiaDocumento"));
        colPaqueteDocumento.setCellValueFactory(new PropertyValueFactory<>("paqueteDocumento"));
        colDocenteDocumento.setCellValueFactory(new PropertyValueFactory<>("docenteDocumento"));
        colSemestreDetectado.setCellValueFactory(new PropertyValueFactory<>("semestreDetectado"));
        colMateriaDetectada.setCellValueFactory(new PropertyValueFactory<>("materiaDetectada"));
        colGrupoDetectado.setCellValueFactory(new PropertyValueFactory<>("grupoDetectado"));
        colMaestroDetectado.setCellValueFactory(new PropertyValueFactory<>("maestroDetectado"));
        colEstatusImportacion.setCellValueFactory(new PropertyValueFactory<>("estatusImportacion"));
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));

        detalleFiltrado = new FilteredList<>(listaDetalle, p -> true);
        tablaDetalle.setItems(detalleFiltrado);

        colLinkLote.setCellValueFactory(new PropertyValueFactory<>("nombreLote"));
        colLinkCiclo.setCellValueFactory(new PropertyValueFactory<>("cicloEscolar"));
        colLinkFechaLimite.setCellValueFactory(new PropertyValueFactory<>("fechaLimite"));
        colLinkDisponibles.setCellValueFactory(new PropertyValueFactory<>("totalDisponibles"));
        colLinkCargadas.setCellValueFactory(new PropertyValueFactory<>("totalCargadas"));
        colLinkEstado.setCellValueFactory(new PropertyValueFactory<>("estadoLink"));
        colLinkCreado.setCellValueFactory(new PropertyValueFactory<>("creadoEn"));
        tablaLinks.setItems(listaLinks);
    }

    private void configurarFiltros() {
        cbEstatusFiltro.setItems(FXCollections.observableArrayList(
                "todos",
                "pendiente",
                "validada",
                "materia_no_encontrada",
                "grupo_no_encontrado",
                "docente_no_encontrado",
                "pendiente_docente",
                "cargada",
                "duplicada",
                "descartada",
                "error"
        ));
        cbEstatusFiltro.setValue("todos");
    }

    private void configurarEventos() {
        tablaLotes.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            loteSeleccionado = newValue;
            mostrarLoteSeleccionado();
            cargarDetalleLote();
        });

        tablaLinks.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            linkSeleccionado = newValue;
            mostrarDetalleLink();
        });

        txtBuscarDetalle.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltroDetalle());
        cbEstatusFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltroDetalle());
    }

    private void cargarCiclos() {
        cbCicloEscolar.getItems().clear();

        String sql = """
                select id_ciclo_escolar,nombre
                from ciclo_escolar
                where id_estatus_general=1
                order by nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbCicloEscolar.getItems().add(new CicloItem(
                        rs.getInt("id_ciclo_escolar"),
                        rs.getString("nombre")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar ciclos escolares");
        }
    }

    private void cargarLotes() {
        listaLotes.clear();

        String sql = """
                select
                l.id_lote_importacion,
                l.nombre_lote,
                l.nombre_archivo,
                l.extension_archivo,
                l.id_ciclo_escolar,
                ce.nombre as ciclo_escolar,
                l.total_filas,
                l.filas_validas,
                l.filas_pendientes,
                l.filas_cargadas,
                l.id_estatus_general,
                ceg.nombre as estatus,
                l.creado_en
                from importacion_clase_lote l
                inner join ciclo_escolar ce on l.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_estatus_general ceg on l.id_estatus_general=ceg.id_estatus_general
                order by l.creado_en desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaLotes.add(new LoteItem(
                        rs.getInt("id_lote_importacion"),
                        rs.getString("nombre_lote"),
                        rs.getString("nombre_archivo"),
                        rs.getString("extension_archivo"),
                        rs.getInt("id_ciclo_escolar"),
                        rs.getString("ciclo_escolar"),
                        rs.getInt("total_filas"),
                        rs.getInt("filas_validas"),
                        rs.getInt("filas_pendientes"),
                        rs.getInt("filas_cargadas"),
                        rs.getInt("id_estatus_general"),
                        rs.getString("estatus"),
                        rs.getString("creado_en")
                ));
            }

            cargarMetricas();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar lotes");
        }
    }

    private void cargarDetalleLote() {
        listaDetalle.clear();

        if (loteSeleccionado == null) {
            aplicarFiltroDetalle();
            cargarMetricas();
            return;
        }

        String sql = """
                select
                id_detalle_importacion,
                id_lote_importacion,
                fila_archivo,
                grupo_documento,
                materia_documento,
                paquete_documento,
                docente_documento,
                semestre_detectado,
                materia_detectada,
                grupo_detectado,
                turno_detectado,
                maestro_detectado,
                estatus_importacion,
                observaciones
                from vw_importacion_clases_jefe
                where id_lote_importacion=?
                order by fila_archivo
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, loteSeleccionado.getIdLoteImportacion());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaDetalle.add(new DetalleItem(
                            rs.getInt("id_detalle_importacion"),
                            rs.getInt("id_lote_importacion"),
                            rs.getInt("fila_archivo"),
                            rs.getString("grupo_documento"),
                            rs.getString("materia_documento"),
                            rs.getString("paquete_documento"),
                            rs.getString("docente_documento"),
                            rs.getObject("semestre_detectado") == null ? 0 : rs.getInt("semestre_detectado"),
                            rs.getString("materia_detectada"),
                            rs.getString("grupo_detectado"),
                            rs.getString("turno_detectado"),
                            rs.getString("maestro_detectado"),
                            rs.getString("estatus_importacion"),
                            rs.getString("observaciones")
                    ));
                }
            }

            aplicarFiltroDetalle();
            cargarMetricas();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar clases importadas");
        }
    }

    private void cargarLinks() {
        listaLinks.clear();

        String sql = """
                select
                id_link_seleccion_clases,
                id_lote_importacion,
                nombre_lote,
                ciclo_escolar,
                token,
                url_base,
                link_seleccion,
                fecha_apertura,
                fecha_limite,
                total_clases_importadas,
                total_disponibles,
                total_cargadas,
                estatus,
                estado_link,
                creado_en,
                cerrado_en
                from vw_links_seleccion_clases_jefe
                order by creado_en desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaLinks.add(new LinkItem(
                        rs.getInt("id_link_seleccion_clases"),
                        rs.getInt("id_lote_importacion"),
                        rs.getString("nombre_lote"),
                        rs.getString("ciclo_escolar"),
                        rs.getString("token"),
                        rs.getString("url_base"),
                        rs.getString("link_seleccion"),
                        rs.getString("fecha_apertura"),
                        rs.getString("fecha_limite"),
                        rs.getInt("total_clases_importadas"),
                        rs.getInt("total_disponibles"),
                        rs.getInt("total_cargadas"),
                        rs.getString("estatus"),
                        rs.getString("estado_link"),
                        rs.getString("creado_en"),
                        rs.getString("cerrado_en")
                ));
            }

            cargarMetricas();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar links de seleccion");
        }
    }

    private void cargarMetricas() {
        if (loteSeleccionado != null) {
            lblTotalFilas.setText(String.valueOf(loteSeleccionado.getTotalFilas()));
            lblFilasValidas.setText(String.valueOf(loteSeleccionado.getFilasValidas()));
            lblFilasPendientes.setText(String.valueOf(loteSeleccionado.getFilasPendientes()));
            lblFilasCargadas.setText(String.valueOf(loteSeleccionado.getFilasCargadas()));
        } else {
            lblTotalFilas.setText("0");
            lblFilasValidas.setText("0");
            lblFilasPendientes.setText("0");
            lblFilasCargadas.setText("0");
        }

        int abiertos = 0;

        for (LinkItem item : listaLinks) {
            if ("abierto".equalsIgnoreCase(item.getEstadoLink())) {
                abiertos++;
            }
        }

        lblLinksAbiertos.setText(String.valueOf(abiertos));
    }

    private void aplicarFiltroDetalle() {
        if (detalleFiltrado == null) {
            return;
        }

        String texto = obtenerTexto(txtBuscarDetalle).toLowerCase();
        String estatus = cbEstatusFiltro.getValue() == null ? "todos" : cbEstatusFiltro.getValue().toLowerCase();

        detalleFiltrado.setPredicate(item -> {
            boolean coincideTexto = texto.isEmpty()
                    || item.getGrupoDocumento().toLowerCase().contains(texto)
                    || item.getMateriaDocumento().toLowerCase().contains(texto)
                    || item.getPaqueteDocumento().toLowerCase().contains(texto)
                    || item.getDocenteDocumento().toLowerCase().contains(texto)
                    || item.getMateriaDetectada().toLowerCase().contains(texto)
                    || item.getGrupoDetectado().toLowerCase().contains(texto)
                    || item.getMaestroDetectado().toLowerCase().contains(texto)
                    || item.getEstatusImportacion().toLowerCase().contains(texto)
                    || item.getObservaciones().toLowerCase().contains(texto);

            boolean coincideEstatus = estatus.equals("todos")
                    || item.getEstatusImportacion().toLowerCase().equals(estatus);

            return coincideTexto && coincideEstatus;
        });
    }

    @FXML
    private void handleSeleccionarArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("seleccionar archivo de clases");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("archivos permitidos", "*.xlsx", "*.csv", "*.pdf"),
                new FileChooser.ExtensionFilter("excel", "*.xlsx"),
                new FileChooser.ExtensionFilter("csv", "*.csv"),
                new FileChooser.ExtensionFilter("pdf", "*.pdf")
        );

        Window window = txtArchivo.getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);

        if (file != null) {
            archivoSeleccionado = file;
            txtArchivo.setText(file.getAbsolutePath());

            if (obtenerTexto(txtNombreLote).isEmpty()) {
                String nombre = file.getName();
                int punto = nombre.lastIndexOf(".");
                txtNombreLote.setText(punto > 0 ? nombre.substring(0, punto) : nombre);
            }
        }
    }

    @FXML
    private void handleImportarArchivo() {
        CicloItem ciclo = cbCicloEscolar.getValue();
        String nombreLote = obtenerTexto(txtNombreLote);

        if (ciclo == null) {
            mostrarError("selecciona un ciclo escolar");
            return;
        }

        if (nombreLote.isEmpty()) {
            mostrarError("captura el nombre del lote");
            return;
        }

        if (archivoSeleccionado == null || !archivoSeleccionado.exists()) {
            mostrarError("selecciona un archivo valido");
            return;
        }

        try {
            List<FilaImportada> filas = leerArchivo(archivoSeleccionado);

            if (filas.isEmpty()) {
                mostrarError("no se encontraron filas validas en el archivo");
                return;
            }

            int idLote = crearLote(nombreLote, archivoSeleccionado, ciclo.getIdCicloEscolar(), obtenerTexto(txtNotasLote));

            for (FilaImportada fila : filas) {
                insertarDetalle(idLote, fila);
            }

            validarLote(idLote);

            mostrarInfo("archivo importado correctamente");
            cargarLotes();
            cargarLinks();
            seleccionarLote(idLote);
            limpiarFormularioArchivo();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al importar archivo: " + e.getMessage());
        }
    }

    private int crearLote(String nombreLote, File archivo, int idCicloEscolar, String notas) throws SQLException {
        String extension = obtenerExtension(archivo.getName());

        String sql = "{call sp_crear_lote_importacion(?,?,?,?,?,?,?)}";

        try (Connection con = ConexionBD.conectar();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setString(1, nombreLote);
            cs.setString(2, archivo.getName());
            cs.setString(3, extension);
            cs.setInt(4, idCicloEscolar);

            if (getIdUsuarioActual() > 0) {
                cs.setInt(5, getIdUsuarioActual());
            } else {
                cs.setNull(5, Types.INTEGER);
            }

            if (notas.isEmpty()) {
                cs.setNull(6, Types.VARCHAR);
            } else {
                cs.setString(6, notas);
            }

            cs.registerOutParameter(7, Types.INTEGER);
            cs.execute();

            return cs.getInt(7);
        }
    }

    private void insertarDetalle(int idLote, FilaImportada fila) throws SQLException {
        String sql = "{call sp_insertar_detalle_importacion(?,?,?,?,?,?)}";

        try (Connection con = ConexionBD.conectar();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setInt(1, idLote);
            cs.setInt(2, fila.getFilaArchivo());
            cs.setString(3, fila.getGrupo());
            cs.setString(4, fila.getMateria());
            cs.setString(5, fila.getPaquete());
            cs.setString(6, fila.getDocente());
            cs.execute();
        }
    }

    private void validarLote(int idLote) throws SQLException {
        String sql = "{call sp_validar_importacion_lote(?)}";

        try (Connection con = ConexionBD.conectar();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setInt(1, idLote);
            cs.execute();
        }
    }

    @FXML
    private void handleValidarLote() {
        if (loteSeleccionado == null) {
            mostrarError("selecciona un lote");
            return;
        }

        try {
            validarLote(loteSeleccionado.getIdLoteImportacion());
            mostrarInfo("lote validado correctamente");
            cargarLotes();
            cargarLinks();
            seleccionarLote(loteSeleccionado.getIdLoteImportacion());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al validar lote");
        }
    }

    @FXML
    private void handleGenerarLink() {
        if (loteSeleccionado == null) {
            mostrarError("selecciona un lote");
            return;
        }

        String urlBase = limpiarUrlBase(obtenerTexto(txtUrlBase));

        if (urlBase.isEmpty()) {
            mostrarError("captura la url base");
            return;
        }

        String token = generarToken();

        String sql = "{call sp_generar_link_seleccion_clases(?,?,?,?,?,?)}";

        try (Connection con = ConexionBD.conectar();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setInt(1, loteSeleccionado.getIdLoteImportacion());
            cs.setString(2, token);
            cs.setString(3, urlBase);

            if (dpFechaLimite.getValue() != null) {
                cs.setTimestamp(4, Timestamp.valueOf(dpFechaLimite.getValue().atTime(LocalTime.of(23, 59, 59))));
            } else {
                cs.setNull(4, Types.TIMESTAMP);
            }

            String notas = obtenerTexto(txtNotasLink);

            if (notas.isEmpty()) {
                cs.setNull(5, Types.VARCHAR);
            } else {
                cs.setString(5, notas);
            }

            if (getIdUsuarioActual() > 0) {
                cs.setInt(6, getIdUsuarioActual());
            } else {
                cs.setNull(6, Types.INTEGER);
            }

            cs.execute();

            mostrarInfo("link generado correctamente");
            cargarLinks();
            seleccionarLinkPorToken(token);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al generar link");
        }
    }

    @FXML
    private void handleCopiarLink() {
        if (linkSeleccionado == null || linkSeleccionado.getLinkSeleccion().isEmpty()) {
            mostrarError("selecciona un link");
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(linkSeleccionado.getLinkSeleccion());
        Clipboard.getSystemClipboard().setContent(content);

        mostrarInfo("link copiado al portapapeles");
    }

    @FXML
    private void handleCerrarLink() {
        if (linkSeleccionado == null) {
            mostrarError("selecciona un link");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("confirmar");
        confirmacion.setHeaderText("cerrar link");
        confirmacion.setContentText("seguro que deseas cerrar este link?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String sql = "{call sp_cerrar_link_seleccion_clases(?,?)}";

        try (Connection con = ConexionBD.conectar();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setInt(1, linkSeleccionado.getIdLinkSeleccionClases());

            if (getIdUsuarioActual() > 0) {
                cs.setInt(2, getIdUsuarioActual());
            } else {
                cs.setNull(2, Types.INTEGER);
            }

            cs.execute();

            mostrarInfo("link cerrado correctamente");
            cargarLinks();
            limpiarDetalleLink();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cerrar link");
        }
    }

    @FXML
    private void handleActualizar() {
        cargarLotes();
        cargarLinks();

        if (loteSeleccionado != null) {
            seleccionarLote(loteSeleccionado.getIdLoteImportacion());
        }
    }

    @FXML
    private void handleLimpiarFormulario() {
        limpiarFormularioArchivo();
        txtUrlBase.clear();
        dpFechaLimite.setValue(null);
        txtNotasLink.clear();
    }

    private void limpiarFormularioArchivo() {
        archivoSeleccionado = null;
        txtArchivo.clear();
        txtNombreLote.clear();
        txtNotasLote.clear();
        cbCicloEscolar.setValue(null);
    }

    private void mostrarLoteSeleccionado() {
        if (loteSeleccionado == null) {
            lblLoteSeleccionado.setText("selecciona un lote");
            return;
        }

        lblLoteSeleccionado.setText(
                loteSeleccionado.getNombreLote() + " | " +
                        loteSeleccionado.getCicloEscolar() + " | " +
                        loteSeleccionado.getTotalFilas() + " filas"
        );
    }

    private void mostrarDetalleLink() {
        if (linkSeleccionado == null) {
            limpiarDetalleLink();
            return;
        }

        txtLinkCompleto.setText(linkSeleccionado.getLinkSeleccion());
        lblEstadoLink.setText(
                "estado: " + linkSeleccionado.getEstadoLink() +
                        "\nfecha apertura: " + linkSeleccionado.getFechaApertura() +
                        "\nfecha limite: " + textoVacio(linkSeleccionado.getFechaLimite(), "sin limite") +
                        "\ncerrado: " + textoVacio(linkSeleccionado.getCerradoEn(), "no cerrado")
        );
    }

    private void limpiarDetalleLink() {
        linkSeleccionado = null;

        if (txtLinkCompleto != null) {
            txtLinkCompleto.clear();
        }

        if (lblEstadoLink != null) {
            lblEstadoLink.setText("---");
        }

        if (tablaLinks != null) {
            tablaLinks.getSelectionModel().clearSelection();
        }
    }

    private void seleccionarLote(int idLote) {
        for (LoteItem item : listaLotes) {
            if (item.getIdLoteImportacion() == idLote) {
                tablaLotes.getSelectionModel().select(item);
                tablaLotes.scrollTo(item);
                return;
            }
        }
    }

    private void seleccionarLinkPorToken(String token) {
        for (LinkItem item : listaLinks) {
            if (item.getToken().equals(token)) {
                tablaLinks.getSelectionModel().select(item);
                tablaLinks.scrollTo(item);
                linkSeleccionado = item;
                mostrarDetalleLink();
                return;
            }
        }
    }

    private List<FilaImportada> leerArchivo(File archivo) throws Exception {
        String extension = obtenerExtension(archivo.getName()).toLowerCase();

        switch (extension) {
            case "xlsx":
                return leerXlsx(archivo);
            case "csv":
                return leerCsv(archivo);
            case "pdf":
                return leerPdf(archivo);
            default:
                throw new IllegalArgumentException("extension no soportada");
        }
    }

    private List<FilaImportada> leerXlsx(File archivo) throws Exception {
        List<FilaImportada> filas = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream input = new FileInputStream(archivo);
             Workbook workbook = WorkbookFactory.create(input)) {

            Sheet sheet = workbook.getSheetAt(0);
            HeaderInfo header = null;

            for (Row row : sheet) {
                List<String> valores = new ArrayList<>();

                for (int i = 0; i < Math.max(row.getLastCellNum(), 8); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    valores.add(formatter.formatCellValue(cell).trim());
                }

                if (header == null && esFilaEncabezado(valores)) {
                    header = crearHeader(valores);
                    continue;
                }

                if (header == null) {
                    header = HeaderInfo.fallback();
                }

                FilaImportada fila = crearFilaImportada(row.getRowNum() + 1, valores, header);

                if (fila != null) {
                    filas.add(fila);
                }
            }
        }

        return filas;
    }

    private List<FilaImportada> leerCsv(File archivo) throws Exception {
        List<FilaImportada> filas = new ArrayList<>();
        HeaderInfo header = null;
        int numeroFila = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;

            while ((linea = br.readLine()) != null) {
                numeroFila++;

                if (linea.trim().isEmpty()) {
                    continue;
                }

                List<String> valores = parsearCsv(linea);

                if (header == null && esFilaEncabezado(valores)) {
                    header = crearHeader(valores);
                    continue;
                }

                if (header == null) {
                    header = HeaderInfo.fallback();
                }

                FilaImportada fila = crearFilaImportada(numeroFila, valores, header);

                if (fila != null) {
                    filas.add(fila);
                }
            }
        }

        return filas;
    }

    private List<FilaImportada> leerPdf(File archivo) throws Exception {
        List<FilaImportada> filas = new ArrayList<>();
        HeaderInfo header = HeaderInfo.fallback();
        boolean despuesEncabezado = false;
        int numeroFila = 0;

        try (PDDocument document = PDDocument.load(archivo)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(document);
            String[] lineas = texto.split("\\R");

            for (String linea : lineas) {
                numeroFila++;

                if (linea.trim().isEmpty()) {
                    continue;
                }

                List<String> valores = Arrays.asList(linea.trim().split("\\s{2,}"));

                if (!despuesEncabezado && esFilaEncabezado(valores)) {
                    header = crearHeader(valores);
                    despuesEncabezado = true;
                    continue;
                }

                if (!despuesEncabezado && !linea.toLowerCase().contains("grupo")) {
                    continue;
                }

                FilaImportada fila = crearFilaImportada(numeroFila, valores, header);

                if (fila != null) {
                    filas.add(fila);
                }
            }
        }

        return filas;
    }

    private boolean esFilaEncabezado(List<String> valores) {
        String texto = normalizar(String.join(" ", valores));

        return texto.contains("grupo")
                && texto.contains("materia")
                && (texto.contains("paq") || texto.contains("paquete") || texto.contains("docente"));
    }

    private HeaderInfo crearHeader(List<String> valores) {
        HeaderInfo header = new HeaderInfo();

        for (int i = 0; i < valores.size(); i++) {
            String valor = normalizar(valores.get(i));

            if (valor.equals("grupo")) {
                header.grupo = i;
            } else if (valor.equals("materia")) {
                header.materia = i;
            } else if (valor.equals("paq") || valor.equals("paquete")) {
                header.paquete = i;
            } else if (valor.equals("docente")) {
                header.docente = i;
            }
        }

        if (!header.esValido()) {
            return HeaderInfo.fallback();
        }

        return header;
    }

    private FilaImportada crearFilaImportada(int numeroFila, List<String> valores, HeaderInfo header) {
        String grupo = valorEn(valores, header.grupo);
        String materia = valorEn(valores, header.materia);
        String paquete = valorEn(valores, header.paquete);
        String docente = valorEn(valores, header.docente);

        if (materia.isEmpty()) {
            return null;
        }

        if (normalizar(materia).contains("materia") || normalizar(materia).contains("total")) {
            return null;
        }

        return new FilaImportada(numeroFila, grupo, materia, paquete, docente);
    }

    private List<String> parsearCsv(String linea) {
        char separador = detectarSeparador(linea);
        List<String> valores = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean comillas = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);

            if (c == '"') {
                comillas = !comillas;
            } else if (c == separador && !comillas) {
                valores.add(actual.toString().trim());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }

        valores.add(actual.toString().trim());
        return valores;
    }

    private char detectarSeparador(String linea) {
        int comas = contar(linea, ',');
        int puntoComas = contar(linea, ';');
        int tabs = contar(linea, '\t');

        if (tabs >= comas && tabs >= puntoComas) {
            return '\t';
        }

        if (puntoComas >= comas) {
            return ';';
        }

        return ',';
    }

    private int contar(String texto, char caracter) {
        int total = 0;

        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == caracter) {
                total++;
            }
        }

        return total;
    }

    private String valorEn(List<String> valores, int index) {
        if (index < 0 || index >= valores.size()) {
            return "";
        }

        return valores.get(index) == null ? "" : valores.get(index).trim();
    }

    private String obtenerExtension(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf(".");

        if (punto < 0) {
            return "";
        }

        return nombreArchivo.substring(punto + 1).toLowerCase();
    }

    private String generarToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String limpiarUrlBase(String url) {
        if (url == null) {
            return "";
        }

        return url.trim().replaceAll("/+$", "");
    }

    private String obtenerTexto(TextInputControl control) {
        if (control == null || control.getText() == null) {
            return "";
        }

        return control.getText().trim();
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        String limpio = Normalizer.normalize(texto, Normalizer.Form.NFD);
        limpio = limpio.replaceAll("\\p{M}", "");
        return limpio.toLowerCase().trim();
    }

    private String textoVacio(String valor, String reemplazo) {
        if (valor == null || valor.trim().isEmpty()) {
            return reemplazo;
        }

        return valor;
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

    private static class HeaderInfo {
        int grupo = -1;
        int materia = -1;
        int paquete = -1;
        int docente = -1;

        boolean esValido() {
            return grupo >= 0 && materia >= 0 && paquete >= 0;
        }

        static HeaderInfo fallback() {
            HeaderInfo header = new HeaderInfo();
            header.grupo = 1;
            header.materia = 2;
            header.paquete = 5;
            header.docente = 6;
            return header;
        }
    }

    public static class FilaImportada {
        private final int filaArchivo;
        private final String grupo;
        private final String materia;
        private final String paquete;
        private final String docente;

        public FilaImportada(int filaArchivo, String grupo, String materia, String paquete, String docente) {
            this.filaArchivo = filaArchivo;
            this.grupo = grupo == null ? "" : grupo;
            this.materia = materia == null ? "" : materia;
            this.paquete = paquete == null ? "" : paquete;
            this.docente = docente == null ? "" : docente;
        }

        public int getFilaArchivo() {
            return filaArchivo;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getMateria() {
            return materia;
        }

        public String getPaquete() {
            return paquete;
        }

        public String getDocente() {
            return docente;
        }
    }

    public static class CicloItem {
        private final int idCicloEscolar;
        private final String nombre;

        public CicloItem(int idCicloEscolar, String nombre) {
            this.idCicloEscolar = idCicloEscolar;
            this.nombre = nombre == null ? "" : nombre;
        }

        public int getIdCicloEscolar() {
            return idCicloEscolar;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    public static class LoteItem {
        private final int idLoteImportacion;
        private final String nombreLote;
        private final String nombreArchivo;
        private final String extensionArchivo;
        private final int idCicloEscolar;
        private final String cicloEscolar;
        private final int totalFilas;
        private final int filasValidas;
        private final int filasPendientes;
        private final int filasCargadas;
        private final int idEstatusGeneral;
        private final String estatus;
        private final String creadoEn;

        public LoteItem(int idLoteImportacion, String nombreLote, String nombreArchivo, String extensionArchivo,
                        int idCicloEscolar, String cicloEscolar, int totalFilas, int filasValidas,
                        int filasPendientes, int filasCargadas, int idEstatusGeneral, String estatus,
                        String creadoEn) {
            this.idLoteImportacion = idLoteImportacion;
            this.nombreLote = textoSeguro(nombreLote);
            this.nombreArchivo = textoSeguro(nombreArchivo);
            this.extensionArchivo = textoSeguro(extensionArchivo);
            this.idCicloEscolar = idCicloEscolar;
            this.cicloEscolar = textoSeguro(cicloEscolar);
            this.totalFilas = totalFilas;
            this.filasValidas = filasValidas;
            this.filasPendientes = filasPendientes;
            this.filasCargadas = filasCargadas;
            this.idEstatusGeneral = idEstatusGeneral;
            this.estatus = textoSeguro(estatus);
            this.creadoEn = textoSeguro(creadoEn);
        }

        private static String textoSeguro(String valor) {
            return valor == null ? "" : valor;
        }

        public int getIdLoteImportacion() {
            return idLoteImportacion;
        }

        public String getNombreLote() {
            return nombreLote;
        }

        public String getNombreArchivo() {
            return nombreArchivo;
        }

        public String getExtensionArchivo() {
            return extensionArchivo;
        }

        public int getIdCicloEscolar() {
            return idCicloEscolar;
        }

        public String getCicloEscolar() {
            return cicloEscolar;
        }

        public int getTotalFilas() {
            return totalFilas;
        }

        public int getFilasValidas() {
            return filasValidas;
        }

        public int getFilasPendientes() {
            return filasPendientes;
        }

        public int getFilasCargadas() {
            return filasCargadas;
        }

        public int getIdEstatusGeneral() {
            return idEstatusGeneral;
        }

        public String getEstatus() {
            return estatus;
        }

        public String getCreadoEn() {
            return creadoEn;
        }
    }

    public static class DetalleItem {
        private final int idDetalleImportacion;
        private final int idLoteImportacion;
        private final int filaArchivo;
        private final String grupoDocumento;
        private final String materiaDocumento;
        private final String paqueteDocumento;
        private final String docenteDocumento;
        private final int semestreDetectado;
        private final String materiaDetectada;
        private final String grupoDetectado;
        private final String turnoDetectado;
        private final String maestroDetectado;
        private final String estatusImportacion;
        private final String observaciones;

        public DetalleItem(int idDetalleImportacion, int idLoteImportacion, int filaArchivo, String grupoDocumento,
                           String materiaDocumento, String paqueteDocumento, String docenteDocumento,
                           int semestreDetectado, String materiaDetectada, String grupoDetectado,
                           String turnoDetectado, String maestroDetectado, String estatusImportacion,
                           String observaciones) {
            this.idDetalleImportacion = idDetalleImportacion;
            this.idLoteImportacion = idLoteImportacion;
            this.filaArchivo = filaArchivo;
            this.grupoDocumento = textoSeguro(grupoDocumento);
            this.materiaDocumento = textoSeguro(materiaDocumento);
            this.paqueteDocumento = textoSeguro(paqueteDocumento);
            this.docenteDocumento = textoSeguro(docenteDocumento);
            this.semestreDetectado = semestreDetectado;
            this.materiaDetectada = textoSeguro(materiaDetectada);
            this.grupoDetectado = textoSeguro(grupoDetectado);
            this.turnoDetectado = textoSeguro(turnoDetectado);
            this.maestroDetectado = textoSeguro(maestroDetectado);
            this.estatusImportacion = textoSeguro(estatusImportacion);
            this.observaciones = textoSeguro(observaciones);
        }

        private static String textoSeguro(String valor) {
            return valor == null ? "" : valor;
        }

        public int getIdDetalleImportacion() {
            return idDetalleImportacion;
        }

        public int getIdLoteImportacion() {
            return idLoteImportacion;
        }

        public int getFilaArchivo() {
            return filaArchivo;
        }

        public String getGrupoDocumento() {
            return grupoDocumento;
        }

        public String getMateriaDocumento() {
            return materiaDocumento;
        }

        public String getPaqueteDocumento() {
            return paqueteDocumento;
        }

        public String getDocenteDocumento() {
            return docenteDocumento;
        }

        public int getSemestreDetectado() {
            return semestreDetectado;
        }

        public String getMateriaDetectada() {
            return materiaDetectada;
        }

        public String getGrupoDetectado() {
            return grupoDetectado;
        }

        public String getTurnoDetectado() {
            return turnoDetectado;
        }

        public String getMaestroDetectado() {
            return maestroDetectado;
        }

        public String getEstatusImportacion() {
            return estatusImportacion;
        }

        public String getObservaciones() {
            return observaciones;
        }
    }

    public static class LinkItem {
        private final int idLinkSeleccionClases;
        private final int idLoteImportacion;
        private final String nombreLote;
        private final String cicloEscolar;
        private final String token;
        private final String urlBase;
        private final String linkSeleccion;
        private final String fechaApertura;
        private final String fechaLimite;
        private final int totalClasesImportadas;
        private final int totalDisponibles;
        private final int totalCargadas;
        private final String estatus;
        private final String estadoLink;
        private final String creadoEn;
        private final String cerradoEn;

        public LinkItem(int idLinkSeleccionClases, int idLoteImportacion, String nombreLote,
                        String cicloEscolar, String token, String urlBase, String linkSeleccion,
                        String fechaApertura, String fechaLimite, int totalClasesImportadas,
                        int totalDisponibles, int totalCargadas, String estatus, String estadoLink,
                        String creadoEn, String cerradoEn) {
            this.idLinkSeleccionClases = idLinkSeleccionClases;
            this.idLoteImportacion = idLoteImportacion;
            this.nombreLote = textoSeguro(nombreLote);
            this.cicloEscolar = textoSeguro(cicloEscolar);
            this.token = textoSeguro(token);
            this.urlBase = textoSeguro(urlBase);
            this.linkSeleccion = textoSeguro(linkSeleccion);
            this.fechaApertura = textoSeguro(fechaApertura);
            this.fechaLimite = textoSeguro(fechaLimite);
            this.totalClasesImportadas = totalClasesImportadas;
            this.totalDisponibles = totalDisponibles;
            this.totalCargadas = totalCargadas;
            this.estatus = textoSeguro(estatus);
            this.estadoLink = textoSeguro(estadoLink);
            this.creadoEn = textoSeguro(creadoEn);
            this.cerradoEn = textoSeguro(cerradoEn);
        }

        private static String textoSeguro(String valor) {
            return valor == null ? "" : valor;
        }

        public int getIdLinkSeleccionClases() {
            return idLinkSeleccionClases;
        }

        public int getIdLoteImportacion() {
            return idLoteImportacion;
        }

        public String getNombreLote() {
            return nombreLote;
        }

        public String getCicloEscolar() {
            return cicloEscolar;
        }

        public String getToken() {
            return token;
        }

        public String getUrlBase() {
            return urlBase;
        }

        public String getLinkSeleccion() {
            return linkSeleccion;
        }

        public String getFechaApertura() {
            return fechaApertura;
        }

        public String getFechaLimite() {
            return fechaLimite;
        }

        public int getTotalClasesImportadas() {
            return totalClasesImportadas;
        }

        public int getTotalDisponibles() {
            return totalDisponibles;
        }

        public int getTotalCargadas() {
            return totalCargadas;
        }

        public String getEstatus() {
            return estatus;
        }

        public String getEstadoLink() {
            return estadoLink;
        }

        public String getCreadoEn() {
            return creadoEn;
        }

        public String getCerradoEn() {
            return cerradoEn;
        }
    }
}