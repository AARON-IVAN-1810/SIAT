package application.proyecto.controllers.jefedemaestros;

import application.proyecto.controllers.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class GruposJDMController extends BaseController {

    @FXML private TextField txtNombreGrupo, txtClaveGrupo, txtBuscarGrupoInterno;
    @FXML private ComboBox<String> cbTurnoGrupo, cbSemestreGrupo, cbFiltroGrupos;
    @FXML private ListView<?> listViewMateriasGrupo;
    @FXML private TableView<?> tablaGrupos;
    @FXML private TableColumn<?, ?> colIdGrupo, colNombreGrupo, colClaveGrupo, colTurnoGrupo, colSemestreGrupo, colMateriasGrupo, colAccionGrupo;  
    
    @FXML
    public void initialize(){
        System.out.println("Vista de grupos cargada");
    }
}
