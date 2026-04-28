package application.proyecto.models;

import javafx.scene.control.CheckBox;

public class AlumnoAsistencia {
    private String numControl;
    private String nombre;
    private String grupo;
    private String turno;
    private CheckBox estado; // Esta será la casilla para marcar la asistencia

    public AlumnoAsistencia(String numControl, String nombre, String grupo, String turno) {
        this.numControl = numControl;
        this.nombre = nombre;
        this.grupo = grupo;
        this.turno = turno;
        
        // Inicializamos el CheckBox como "marcado" (presente) por defecto
        this.estado = new CheckBox("Presente");
        this.estado.setSelected(true);
    }

    // Getters necesarios para que la tabla los pueda leer
    public String getNumControl() { return numControl; }
    public String getNombre() { return nombre; }
    public String getGrupo() { return grupo; }
    public String getTurno() { return turno; }
    public CheckBox getEstado() { return estado; }
}