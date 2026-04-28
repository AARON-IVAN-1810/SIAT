package application.proyecto.controllers.tutores;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

public class BitacoraTController extends BaseController {

    // --- CABLES DEL FXML ---
    @FXML private ComboBox<String> cbAlumnoBitacora;
    @FXML private Button btnBuscarAlumnoBitacora;

    @FXML
    public void initialize() {
        System.out.println("Módulo de Bitácora (Tutores) listo.");
        // Aquí podrías cargar la lista de alumnos asignados al tutor
    }

    @FXML
    private void handleBuscarAlumno() {
        String alumnoSeleccionado = cbAlumnoBitacora.getValue();
        if (alumnoSeleccionado != null) {
            System.out.println("Buscando historial para: " + alumnoSeleccionado);
            // Lógica para cambiar el VBox de "Estado Inicial" por la tabla de seguimiento
        }
    }
}