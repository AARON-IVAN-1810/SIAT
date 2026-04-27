package application.proyecto.controllers.maestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import application.proyecto.daos.MaestroDAO;
import application.proyecto.models.Materia;
import application.proyecto.models.Alerta;

public class InicioMController extends BaseController {

    // --- TARJETAS DE RESUMEN ---
    @FXML private Label lblTotalClases;
    @FXML private Label lblTotalAlumnos;
    @FXML private Label lblTotalAlertas;

    // --- TABLA: MIS GRUPOS ---
    // Cambiamos los <?> por <Materia> y el tipo de dato de cada columna
    @FXML private TableView<Materia> tablaGrupos;
    @FXML private TableColumn<Materia, String> colCodigoGrupo;
    @FXML private TableColumn<Materia, String> colNombreMateriaGrupo;
    @FXML private TableColumn<Materia, Integer> colTotalAlumnosGrupo;

    // --- TABLA: ALERTAS ACTIVAS ---
    // Hacemos lo mismo para las alertas
    @FXML private TableView<Alerta> tablaAlertas;
    @FXML private TableColumn<Alerta, String> colNoControlAlerta;
    @FXML private TableColumn<Alerta, String> colNombreAlerta;
    @FXML private TableColumn<Alerta, String> colMateriaAlerta;
    @FXML private TableColumn<Alerta, String> colTipoAlerta;

    @FXML
    public void initialize() {
        System.out.println("Panel de Inicio (Maestro) cargado.");
        
        MaestroDAO maestroDAO = new MaestroDAO();
        int idDelMaestroLogueado = 1; // El de Juan Pérez en tu SQL

        // 1. Llenar tarjetas
        lblTotalAlumnos.setText(String.valueOf(maestroDAO.obtenerContador("fn_total_alumnos_maestro", idDelMaestroLogueado)));
        lblTotalClases.setText(String.valueOf(maestroDAO.obtenerContador("fn_total_clases_maestro", idDelMaestroLogueado)));
        lblTotalAlertas.setText(String.valueOf(maestroDAO.obtenerContador("fn_total_alertas_maestro", idDelMaestroLogueado)));

        // 2. Configurar tabla
        colCodigoGrupo.setCellValueFactory(new PropertyValueFactory<>("clave"));
        colNombreMateriaGrupo.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTotalAlumnosGrupo.setCellValueFactory(new PropertyValueFactory<>("totalAlumnos"));

        // 3. Cargar datos
        tablaGrupos.setItems(maestroDAO.obtenerMisGrupos(idDelMaestroLogueado));

        colNoControlAlerta.setCellValueFactory(new PropertyValueFactory<>("numControl"));
        colNombreAlerta.setCellValueFactory(new PropertyValueFactory<>("nombreAlumno"));
        colMateriaAlerta.setCellValueFactory(new PropertyValueFactory<>("materia"));
        colTipoAlerta.setCellValueFactory(new PropertyValueFactory<>("tipo"));

        // Llenar la tabla de alertas
        tablaAlertas.setItems(maestroDAO.obtenerAlertasMaestro(idDelMaestroLogueado));
    }
}
