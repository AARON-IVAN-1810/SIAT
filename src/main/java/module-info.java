module application.proyecto {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;

    // Abrir paquetes para que JavaFX pueda usar Reflexión (importante para FXML)
    opens application.proyecto to javafx.fxml;
    opens application.proyecto.controllers to javafx.fxml;
    opens application.proyecto.controllers.jefedemaestros to javafx.fxml;
    opens application.proyecto.controllers.maestros to javafx.fxml;
    opens application.proyecto.controllers.tutores to javafx.fxml;

    // Exportar paquetes para que sean visibles
    exports application.proyecto;
    exports application.proyecto.controllers;
    exports application.proyecto.controllers.jefedemaestros;
    exports application.proyecto.controllers.maestros;
    exports application.proyecto.controllers.tutores;
}