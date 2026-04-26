package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class EstadisticasJDMController extends BaseController {

    @FXML private Label lblTotalAlumnos;
    @FXML private Label lblPromedioGeneral;
    @FXML private Label lblAlertas;
    @FXML private TableView<?> tablaComparacionGrupos;

    @FXML
    public void initialize() {
        actualizarMetricas(450, 85.5, 12);
    }

    private void actualizarMetricas(int total, double promedio, int alertas) {
        lblTotalAlumnos.setText(String.valueOf(total));
        lblPromedioGeneral.setText(String.format("%.1f", promedio));
        lblAlertas.setText(String.valueOf(alertas));
    }

    @FXML
    private void btnGenerarEstadisticas() {
        System.out.println("Generando reporte para el grupo seleccionado...");
    }
}