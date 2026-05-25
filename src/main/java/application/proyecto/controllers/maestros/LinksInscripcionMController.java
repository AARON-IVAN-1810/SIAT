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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Base64;

public class LinksInscripcionMController extends BaseController {

    @FXML private Label lblTotalClases;
    @FXML private Label lblLinksAbiertos;
    @FXML private Label lblLinksCerrados;
    @FXML private Label lblTotalInscritos;

    @FXML private ComboBox<ClaseItem> cbClase;
    @FXML private DatePicker dpFechaLimite;
    @FXML private TextField txtMaxInscripciones;
    @FXML private TextField txtUrlBase;
    @FXML private TextArea txtNotas;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cbClaseFiltro;
    @FXML private ComboBox<String> cbCicloFiltro;
    @FXML private ComboBox<String> cbEstadoFiltro;
    @FXML private ComboBox<String> cbOrigenFiltro;

    @FXML private TableView<LinkInscripcionItem> tablaLinks;
    @FXML private TableColumn<LinkInscripcionItem, String> colMateria;
    @FXML private TableColumn<LinkInscripcionItem, String> colClave;
    @FXML private TableColumn<LinkInscripcionItem, String> colGrupo;
    @FXML private TableColumn<LinkInscripcionItem, String> colTurno;
    @FXML private TableColumn<LinkInscripcionItem, String> colCiclo;
    @FXML private TableColumn<LinkInscripcionItem, String> colFechaLimite;
    @FXML private TableColumn<LinkInscripcionItem, Integer> colInscritos;
    @FXML private TableColumn<LinkInscripcionItem, String> colEstado;

    @FXML private Label lblDetalleClase;
    @FXML private Label lblDetalleToken;
    @FXML private TextArea txtLinkCompleto;
    @FXML private Label lblDetalleFechas;
    @FXML private Label lblDetalleInscritos;
    @FXML private Label lblDetalleEstado;
    @FXML private TextArea txtDetalleNotas;

    @FXML private ImageView imgQr;
    @FXML private Label lblQrMensaje;

    private final ObservableList<LinkInscripcionItem> listaLinks = FXCollections.observableArrayList();
    private FilteredList<LinkInscripcionItem> listaFiltrada;
    private LinkInscripcionItem linkSeleccionado;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltrosBase();
        configurarEventos();
        cargarClases();
        cargarLinks();
        cargarMetricas();
        limpiarDetalle();
    }

    private int getIdMaestroActual() {
        return SesionUsuario.getIdMaestro();
    }

    private int getIdUsuarioActual() {
        return SesionUsuario.getIdUsuario();
    }

    private void configurarTabla() {
        colMateria.setCellValueFactory(new PropertyValueFactory<>("nombreMateria"));
        colClave.setCellValueFactory(new PropertyValueFactory<>("claveMateria"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colTurno.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colCiclo.setCellValueFactory(new PropertyValueFactory<>("cicloEscolar"));
        colFechaLimite.setCellValueFactory(new PropertyValueFactory<>("fechaLimite"));
        colInscritos.setCellValueFactory(new PropertyValueFactory<>("totalInscritos"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoFormulario"));

        listaFiltrada = new FilteredList<>(listaLinks, p -> true);
        tablaLinks.setItems(listaFiltrada);
    }

    private void configurarFiltrosBase() {
        cbClaseFiltro.setItems(FXCollections.observableArrayList("todos"));
        cbCicloFiltro.setItems(FXCollections.observableArrayList("todos"));

        cbEstadoFiltro.setItems(FXCollections.observableArrayList(
                "todos",
                "abierto",
                "cerrado",
                "vencido",
                "lleno"
        ));

        cbOrigenFiltro.setItems(FXCollections.observableArrayList(
                "todos",
                "maestro",
                "jefe"
        ));

        cbClaseFiltro.setValue("todos");
        cbCicloFiltro.setValue("todos");
        cbEstadoFiltro.setValue("todos");
        cbOrigenFiltro.setValue("todos");
    }

    private void configurarEventos() {
        tablaLinks.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            linkSeleccionado = newValue;
            mostrarDetalle(newValue);
        });

        txtBuscar.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cbClaseFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cbCicloFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cbEstadoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
        cbOrigenFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void cargarClases() {
        cbClase.getItems().clear();

        int idMaestro = getIdMaestroActual();

        if (idMaestro == 0) {
            mostrarError("no hay maestro en sesion");
            return;
        }

        String sql = """
                select
                c.id_carga,
                concat(m.clave,' - ',m.nombre,' | ',g.nombre,' - ',ct.nombre,' - ',ce.nombre) as clase
                from carga c
                inner join materia m on c.id_materia=m.id_materia
                inner join grupo_ciclo gc on c.id_grupo_ciclo=gc.id_grupo_ciclo
                inner join grupo g on gc.id_grupo=g.id_grupo
                inner join ciclo_escolar ce on gc.id_ciclo_escolar=ce.id_ciclo_escolar
                inner join cat_turno ct on ct.id_turno=coalesce(c.id_turno,g.id_turno)
                where c.id_maestro=?
                and c.id_estatus_general=1
                and gc.id_estatus_general=1
                and g.id_estatus_general=1
                and m.id_estatus_general=1
                order by ce.nombre,g.semestre,g.nombre,m.nombre
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbClase.getItems().add(new ClaseItem(
                            rs.getInt("id_carga"),
                            rs.getString("clase")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar clases");
        }
    }

    private void cargarLinks() {
        listaLinks.clear();

        int idMaestro = getIdMaestroActual();

        if (idMaestro == 0) {
            mostrarError("no hay maestro en sesion");
            return;
        }

        String sql = """
                select
                id_link_inscripcion,
                id_carga,
                id_maestro,
                clave_materia,
                nombre_materia,
                semestre,
                grupo,
                turno,
                ciclo_escolar,
                maestro,
                token,
                url_base,
                link_inscripcion,
                origen,
                id_maestro_creador,
                maestro_creador,
                id_usuario_creador,
                fecha_apertura,
                fecha_limite,
                max_inscripciones,
                total_inscritos,
                estatus,
                estado_formulario,
                notas,
                creado_en,
                actualizado_en,
                cerrado_en
                from vw_links_inscripcion_maestro
                where id_maestro=?
                order by creado_en desc
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaLinks.add(new LinkInscripcionItem(
                            rs.getInt("id_link_inscripcion"),
                            rs.getInt("id_carga"),
                            rs.getInt("id_maestro"),
                            rs.getString("clave_materia"),
                            rs.getString("nombre_materia"),
                            rs.getInt("semestre"),
                            rs.getString("grupo"),
                            rs.getString("turno"),
                            rs.getString("ciclo_escolar"),
                            rs.getString("maestro"),
                            rs.getString("token"),
                            rs.getString("url_base"),
                            rs.getString("link_inscripcion"),
                            rs.getString("origen"),
                            rs.getInt("id_maestro_creador"),
                            rs.getString("maestro_creador"),
                            rs.getInt("id_usuario_creador"),
                            rs.getString("fecha_apertura"),
                            rs.getString("fecha_limite"),
                            rs.getObject("max_inscripciones") == null ? 0 : rs.getInt("max_inscripciones"),
                            rs.getInt("total_inscritos"),
                            rs.getString("estatus"),
                            rs.getString("estado_formulario"),
                            rs.getString("notas"),
                            rs.getString("creado_en"),
                            rs.getString("actualizado_en"),
                            rs.getString("cerrado_en")
                    ));
                }
            }

            cargarValoresFiltros();
            aplicarFiltro();
            limpiarDetalle();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar links de inscripcion");
        }
    }

    private void cargarValoresFiltros() {
        String claseActual = cbClaseFiltro.getValue();
        String cicloActual = cbCicloFiltro.getValue();

        ObservableList<String> clases = FXCollections.observableArrayList("todos");
        ObservableList<String> ciclos = FXCollections.observableArrayList("todos");

        for (LinkInscripcionItem item : listaLinks) {
            String clase = item.getClaveMateria() + " - " + item.getNombreMateria();

            if (!clases.contains(clase)) {
                clases.add(clase);
            }

            if (!ciclos.contains(item.getCicloEscolar())) {
                ciclos.add(item.getCicloEscolar());
            }
        }

        cbClaseFiltro.setItems(clases);
        cbCicloFiltro.setItems(ciclos);

        cbClaseFiltro.setValue(clases.contains(claseActual) ? claseActual : "todos");
        cbCicloFiltro.setValue(ciclos.contains(cicloActual) ? cicloActual : "todos");
    }

    private void cargarMetricas() {
        int idMaestro = getIdMaestroActual();

        if (idMaestro == 0) {
            lblTotalClases.setText("0");
            lblLinksAbiertos.setText("0");
            lblLinksCerrados.setText("0");
            lblTotalInscritos.setText("0");
            return;
        }

        String sqlClases = """
            select count(*) as total_clases
            from carga
            where id_maestro=?
            and id_estatus_general=1
            """;

        String sqlLinks = """
            select
            coalesce(sum(case when v.estado_formulario='abierto' then 1 else 0 end),0) as abiertos,
            coalesce(sum(case when v.estado_formulario='cerrado' then 1 else 0 end),0) as cerrados,
            coalesce((
            select count(distinct ac.id_alumno)
            from alumno_carga ac
            inner join carga c on ac.id_carga=c.id_carga
            inner join (
            select distinct id_carga
            from link_inscripcion_clase
            ) l on ac.id_carga=l.id_carga
            where c.id_maestro=?
            and c.id_estatus_general=1
            and ac.id_estatus_general=1
            ),0) as total_inscritos
            from vw_links_inscripcion_maestro v
            where v.id_maestro=?
            """;

        try (Connection con = ConexionBD.conectar()) {
            try (PreparedStatement ps = con.prepareStatement(sqlClases)) {
                ps.setInt(1, idMaestro);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lblTotalClases.setText(String.valueOf(rs.getInt("total_clases")));
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlLinks)) {
                ps.setInt(1, idMaestro);
                ps.setInt(2, idMaestro);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lblLinksAbiertos.setText(String.valueOf(rs.getInt("abiertos")));
                        lblLinksCerrados.setText(String.valueOf(rs.getInt("cerrados")));
                        lblTotalInscritos.setText(String.valueOf(rs.getInt("total_inscritos")));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar metricas");
        }
    }

    private void aplicarFiltro() {
        if (listaFiltrada == null) {
            return;
        }

        String texto = obtenerTexto(txtBuscar).toLowerCase();
        String clase = valorCombo(cbClaseFiltro).toLowerCase();
        String ciclo = valorCombo(cbCicloFiltro).toLowerCase();
        String estado = valorCombo(cbEstadoFiltro).toLowerCase();
        String origen = valorCombo(cbOrigenFiltro).toLowerCase();

        listaFiltrada.setPredicate(item -> {
            String claseItem = (item.getClaveMateria() + " - " + item.getNombreMateria()).toLowerCase();

            boolean coincideTexto =
                    texto.isEmpty() ||
                            item.getNombreMateria().toLowerCase().contains(texto) ||
                            item.getClaveMateria().toLowerCase().contains(texto) ||
                            item.getGrupo().toLowerCase().contains(texto) ||
                            item.getTurno().toLowerCase().contains(texto) ||
                            item.getCicloEscolar().toLowerCase().contains(texto) ||
                            item.getToken().toLowerCase().contains(texto) ||
                            item.getLinkInscripcion().toLowerCase().contains(texto);

            boolean coincideClase = clase.equals("todos") || claseItem.equals(clase);
            boolean coincideCiclo = ciclo.equals("todos") || item.getCicloEscolar().toLowerCase().equals(ciclo);
            boolean coincideEstado = estado.equals("todos") || item.getEstadoFormulario().toLowerCase().equals(estado);
            boolean coincideOrigen = origen.equals("todos") || item.getOrigen().toLowerCase().equals(origen);

            return coincideTexto && coincideClase && coincideCiclo && coincideEstado && coincideOrigen;
        });
    }

    @FXML
    private void handleGenerarLink() {
        ClaseItem clase = cbClase.getValue();

        if (clase == null) {
            mostrarError("selecciona una clase");
            return;
        }

        String urlBase = obtenerTexto(txtUrlBase);

        if (urlBase.isEmpty()) {
            mostrarError("captura la url base del servidor");
            return;
        }

        if (getIdMaestroActual() == 0) {
            mostrarError("no hay maestro en sesion");
            return;
        }

        if (!clasePerteneceAlMaestro(clase.getIdCarga(), getIdMaestroActual())) {
            mostrarError("la clase seleccionada no pertenece al maestro actual");
            return;
        }

        Integer maxInscripciones = obtenerMaxInscripciones();

        if (maxInscripciones != null && maxInscripciones <= 0) {
            mostrarError("el maximo debe ser mayor a 0");
            return;
        }

        if (existeLinkActivo(clase.getIdCarga())) {
            mostrarError("esta clase ya tiene un link activo");
            return;
        }

        String token = generarToken();
        String urlLimpia = limpiarUrlBase(urlBase);

        String sql = """
                insert into link_inscripcion_clase(
                id_carga,
                token,
                url_base,
                origen,
                id_maestro_creador,
                id_usuario_creador,
                fecha_limite,
                max_inscripciones,
                notas,
                id_estatus_general
                )
                values(?,?,?,?,?,?,?,?,?,1)
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, clase.getIdCarga());
            ps.setString(2, token);
            ps.setString(3, urlLimpia);
            ps.setString(4, "maestro");
            ps.setInt(5, getIdMaestroActual());

            if (getIdUsuarioActual() > 0) {
                ps.setInt(6, getIdUsuarioActual());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }

            if (dpFechaLimite.getValue() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(dpFechaLimite.getValue().atTime(LocalTime.of(23, 59, 59))));
            } else {
                ps.setNull(7, java.sql.Types.TIMESTAMP);
            }

            if (maxInscripciones != null) {
                ps.setInt(8, maxInscripciones);
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }

            String notas = obtenerTexto(txtNotas);

            if (notas.isEmpty()) {
                ps.setNull(9, java.sql.Types.VARCHAR);
            } else {
                ps.setString(9, notas);
            }

            ps.executeUpdate();

            mostrarInfo("link generado correctamente");
            limpiarFormulario();
            recargarTodo();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al generar link");
        }
    }

    private boolean clasePerteneceAlMaestro(int idCarga, int idMaestro) {
        String sql = """
                select id_carga
                from carga
                where id_carga=?
                and id_maestro=?
                and id_estatus_general=1
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCarga);
            ps.setInt(2, idMaestro);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean existeLinkActivo(int idCarga) {
        String sql = """
                select id_link_inscripcion
                from link_inscripcion_clase
                where id_carga=?
                and id_estatus_general=1
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCarga);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @FXML
    private void handleCerrarLink() {
        if (linkSeleccionado == null) {
            mostrarError("selecciona un link");
            return;
        }

        if (!clasePerteneceAlMaestro(linkSeleccionado.getIdCarga(), getIdMaestroActual())) {
            mostrarError("no puedes cerrar un link de otra clase");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("confirmar");
        confirmacion.setHeaderText("cerrar link");
        confirmacion.setContentText("seguro que deseas cerrar este link?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String sql = """
                update link_inscripcion_clase
                set id_estatus_general=0,
                cerrado_en=now(),
                id_maestro_cierre=?,
                id_usuario_cierre=?
                where id_link_inscripcion=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, getIdMaestroActual());

            if (getIdUsuarioActual() > 0) {
                ps.setInt(2, getIdUsuarioActual());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }

            ps.setInt(3, linkSeleccionado.getIdLinkInscripcion());
            ps.executeUpdate();

            mostrarInfo("link cerrado correctamente");
            recargarTodo();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cerrar link");
        }
    }

    @FXML
    private void handleReactivarLink() {
        if (linkSeleccionado == null) {
            mostrarError("selecciona un link");
            return;
        }

        if (!clasePerteneceAlMaestro(linkSeleccionado.getIdCarga(), getIdMaestroActual())) {
            mostrarError("no puedes reactivar un link de otra clase");
            return;
        }

        if (existeOtroLinkActivo(linkSeleccionado.getIdCarga(), linkSeleccionado.getIdLinkInscripcion())) {
            mostrarError("esta clase ya tiene otro link activo");
            return;
        }

        String sql = """
                update link_inscripcion_clase
                set id_estatus_general=1,
                cerrado_en=null,
                id_maestro_cierre=null,
                id_usuario_cierre=null,
                fecha_limite=null
                where id_link_inscripcion=?
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, linkSeleccionado.getIdLinkInscripcion());
            ps.executeUpdate();

            mostrarInfo("link reactivado correctamente");
            recargarTodo();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al reactivar link");
        }
    }

    private boolean existeOtroLinkActivo(int idCarga, int idLinkExcluir) {
        String sql = """
                select id_link_inscripcion
                from link_inscripcion_clase
                where id_carga=?
                and id_estatus_general=1
                and id_link_inscripcion<>?
                limit 1
                """;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCarga);
            ps.setInt(2, idLinkExcluir);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @FXML
    private void handleCopiarLink() {
        if (linkSeleccionado == null || linkSeleccionado.getLinkInscripcion().isEmpty()) {
            mostrarError("no hay link para copiar");
            return;
        }

        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(linkSeleccionado.getLinkInscripcion());
            Clipboard.getSystemClipboard().setContent(content);
            mostrarInfo("link copiado al portapapeles");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al copiar link");
        }
    }

    @FXML
    private void handleVerQr() {
        if (linkSeleccionado == null || linkSeleccionado.getLinkInscripcion().isEmpty()) {
            mostrarError("no hay link para generar qr");
            return;
        }

        String link = linkSeleccionado.getLinkInscripcion();
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" +
                URLEncoder.encode(link, StandardCharsets.UTF_8);

        try {
            imgQr.setImage(new Image(qrUrl, true));
            lblQrMensaje.setText("qr generado para el link seleccionado");
        } catch (Exception e) {
            e.printStackTrace();
            lblQrMensaje.setText("no se pudo cargar el qr");
        }
    }

    @FXML
    private void handleVerInscritos() {
        if (linkSeleccionado == null) {
            mostrarError("selecciona un link");
            return;
        }

        if (!clasePerteneceAlMaestro(linkSeleccionado.getIdCarga(), getIdMaestroActual())) {
            mostrarError("no puedes ver alumnos de otra clase");
            return;
        }

        String sql = """
                select
                al.num_control,
                concat(al.nombre,' ',al.apellido_paterno,' ',al.apellido_materno) as alumno
                from alumno_carga ac
                inner join alumno al on ac.id_alumno=al.id_alumno
                where ac.id_carga=?
                and ac.id_estatus_general=1
                order by al.apellido_paterno,al.apellido_materno,al.nombre
                """;

        StringBuilder contenido = new StringBuilder();

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, linkSeleccionado.getIdCarga());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contenido.append(rs.getString("num_control"))
                            .append(" - ")
                            .append(rs.getString("alumno"))
                            .append("\n");
                }
            }

            if (contenido.length() == 0) {
                contenido.append("no hay alumnos inscritos en esta clase");
            }

            TextArea area = new TextArea(contenido.toString());
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefWidth(520);
            area.setPrefHeight(320);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("alumnos inscritos");
            alert.setHeaderText("alumnos inscritos en la clase");
            alert.getDialogPane().setContent(area);
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("error al cargar alumnos inscritos");
        }
    }

    @FXML
    private void handleActualizar() {
        recargarTodo();
    }

    @FXML
    private void handleLimpiarFormulario() {
        limpiarFormulario();
    }

    private void recargarTodo() {
        cargarClases();
        cargarLinks();
        cargarMetricas();
        limpiarDetalle();
    }

    private void mostrarDetalle(LinkInscripcionItem item) {
        if (item == null) {
            limpiarDetalle();
            return;
        }

        lblDetalleClase.setText(item.getClaveMateria() + " - " + item.getNombreMateria() + " | " +
                item.getGrupo() + " - " + item.getTurno() + " - " + item.getCicloEscolar());

        lblDetalleToken.setText(item.getToken());
        txtLinkCompleto.setText(item.getLinkInscripcion());

        lblDetalleFechas.setText("apertura: " + item.getFechaApertura() +
                "\nlimite: " + textoVacio(item.getFechaLimite(), "sin limite") +
                "\ncerrado: " + textoVacio(item.getCerradoEn(), "no cerrado"));

        String maximo = item.getMaxInscripciones() <= 0 ? "sin limite" : String.valueOf(item.getMaxInscripciones());

        lblDetalleInscritos.setText(item.getTotalInscritos() + " inscritos | maximo: " + maximo);
        lblDetalleEstado.setText(item.getEstadoFormulario());
        txtDetalleNotas.setText(item.getNotas());

        imgQr.setImage(null);
        lblQrMensaje.setText("el qr aparecera aqui al seleccionar ver qr");
    }

    private void limpiarDetalle() {
        linkSeleccionado = null;

        lblDetalleClase.setText("selecciona un link");
        lblDetalleToken.setText("---");
        txtLinkCompleto.setText("");
        lblDetalleFechas.setText("---");
        lblDetalleInscritos.setText("---");
        lblDetalleEstado.setText("---");
        txtDetalleNotas.setText("");

        if (imgQr != null) {
            imgQr.setImage(null);
        }

        if (lblQrMensaje != null) {
            lblQrMensaje.setText("el qr aparecera aqui");
        }
    }

    private void limpiarFormulario() {
        cbClase.setValue(null);
        dpFechaLimite.setValue(null);
        txtMaxInscripciones.clear();
        txtNotas.clear();
    }

    private String generarToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Integer obtenerMaxInscripciones() {
        String texto = obtenerTexto(txtMaxInscripciones);

        if (texto.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(texto);
        } catch (Exception e) {
            return -1;
        }
    }

    private String limpiarUrlBase(String url) {
        String limpia = url == null ? "" : url.trim();
        return limpia.replaceAll("/+$", "");
    }

    private String obtenerTexto(TextInputControl campo) {
        if (campo == null || campo.getText() == null) {
            return "";
        }

        return campo.getText().trim();
    }

    private String valorCombo(ComboBox<String> combo) {
        if (combo == null || combo.getValue() == null) {
            return "todos";
        }

        return combo.getValue();
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

    public static class ClaseItem {
        private final int idCarga;
        private final String nombre;

        public ClaseItem(int idCarga, String nombre) {
            this.idCarga = idCarga;
            this.nombre = nombre == null ? "" : nombre;
        }

        public int getIdCarga() {
            return idCarga;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    public static class LinkInscripcionItem {
        private final int idLinkInscripcion;
        private final int idCarga;
        private final int idMaestro;
        private final String claveMateria;
        private final String nombreMateria;
        private final int semestre;
        private final String grupo;
        private final String turno;
        private final String cicloEscolar;
        private final String maestro;
        private final String token;
        private final String urlBase;
        private final String linkInscripcion;
        private final String origen;
        private final int idMaestroCreador;
        private final String maestroCreador;
        private final int idUsuarioCreador;
        private final String fechaApertura;
        private final String fechaLimite;
        private final int maxInscripciones;
        private final int totalInscritos;
        private final String estatus;
        private final String estadoFormulario;
        private final String notas;
        private final String creadoEn;
        private final String actualizadoEn;
        private final String cerradoEn;

        public LinkInscripcionItem(int idLinkInscripcion, int idCarga, int idMaestro, String claveMateria,
                                   String nombreMateria, int semestre, String grupo, String turno,
                                   String cicloEscolar, String maestro, String token, String urlBase,
                                   String linkInscripcion, String origen, int idMaestroCreador,
                                   String maestroCreador, int idUsuarioCreador, String fechaApertura,
                                   String fechaLimite, int maxInscripciones, int totalInscritos,
                                   String estatus, String estadoFormulario, String notas, String creadoEn,
                                   String actualizadoEn, String cerradoEn) {
            this.idLinkInscripcion = idLinkInscripcion;
            this.idCarga = idCarga;
            this.idMaestro = idMaestro;
            this.claveMateria = textoSeguro(claveMateria);
            this.nombreMateria = textoSeguro(nombreMateria);
            this.semestre = semestre;
            this.grupo = textoSeguro(grupo);
            this.turno = textoSeguro(turno);
            this.cicloEscolar = textoSeguro(cicloEscolar);
            this.maestro = textoSeguro(maestro);
            this.token = textoSeguro(token);
            this.urlBase = textoSeguro(urlBase);
            this.linkInscripcion = textoSeguro(linkInscripcion);
            this.origen = textoSeguro(origen);
            this.idMaestroCreador = idMaestroCreador;
            this.maestroCreador = textoSeguro(maestroCreador);
            this.idUsuarioCreador = idUsuarioCreador;
            this.fechaApertura = textoSeguro(fechaApertura);
            this.fechaLimite = textoSeguro(fechaLimite);
            this.maxInscripciones = maxInscripciones;
            this.totalInscritos = totalInscritos;
            this.estatus = textoSeguro(estatus);
            this.estadoFormulario = textoSeguro(estadoFormulario);
            this.notas = textoSeguro(notas);
            this.creadoEn = textoSeguro(creadoEn);
            this.actualizadoEn = textoSeguro(actualizadoEn);
            this.cerradoEn = textoSeguro(cerradoEn);
        }

        private static String textoSeguro(String valor) {
            return valor == null ? "" : valor;
        }

        public int getIdLinkInscripcion() {
            return idLinkInscripcion;
        }

        public int getIdCarga() {
            return idCarga;
        }

        public int getIdMaestro() {
            return idMaestro;
        }

        public String getClaveMateria() {
            return claveMateria;
        }

        public String getNombreMateria() {
            return nombreMateria;
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

        public String getCicloEscolar() {
            return cicloEscolar;
        }

        public String getMaestro() {
            return maestro;
        }

        public String getToken() {
            return token;
        }

        public String getUrlBase() {
            return urlBase;
        }

        public String getLinkInscripcion() {
            return linkInscripcion;
        }

        public String getOrigen() {
            return origen;
        }

        public int getIdMaestroCreador() {
            return idMaestroCreador;
        }

        public String getMaestroCreador() {
            return maestroCreador;
        }

        public int getIdUsuarioCreador() {
            return idUsuarioCreador;
        }

        public String getFechaApertura() {
            return fechaApertura;
        }

        public String getFechaLimite() {
            return fechaLimite;
        }

        public int getMaxInscripciones() {
            return maxInscripciones;
        }

        public int getTotalInscritos() {
            return totalInscritos;
        }

        public String getEstatus() {
            return estatus;
        }

        public String getEstadoFormulario() {
            return estadoFormulario;
        }

        public String getNotas() {
            return notas;
        }

        public String getCreadoEn() {
            return creadoEn;
        }

        public String getActualizadoEn() {
            return actualizadoEn;
        }

        public String getCerradoEn() {
            return cerradoEn;
        }
    }
}