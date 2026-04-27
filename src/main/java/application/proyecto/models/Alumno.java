package application.proyecto.models;

public class Alumno {
    private int idAlumno;
    private String numControl;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String grupo;

    // Constructor completo
    public Alumno(int idAlumno, String numControl, String nombre, String apellidoPaterno, String apellidoMaterno, String grupo) {
        this.idAlumno = idAlumno;
        this.numControl = numControl;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.grupo = grupo;
    }

    // Getters (Importantes para que JavaFX los encuentre)
    public int getIdAlumno() { return idAlumno; }
    public String getNumControl() { return numControl; }
    public String getNombre() { return nombre; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public String getGrupo() { return grupo; }

    // Helper para la tabla
    public String getNombreCompleto() {
        return nombre + " " + apellidoPaterno + " " + apellidoMaterno;
    }
}