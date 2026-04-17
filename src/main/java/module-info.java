module application.proyecto {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens application.proyecto to javafx.fxml;
    exports application.proyecto;
    exports application.proyecto.controllers;
    opens application.proyecto.controllers to javafx.fxml;
}