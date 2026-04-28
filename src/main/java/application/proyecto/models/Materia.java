package application.proyecto.models;

public class Materia {
    private int idCarga; 
    private String clave;
    private String nombre;
    private int totalAlumnos;

    public Materia(int idCarga, String clave, String nombre, int totalAlumnos) {
        this.idCarga = idCarga;
        this.clave = clave;
        this.nombre = nombre;
        this.totalAlumnos = totalAlumnos;
    }

    public int getIdCarga() { return idCarga; }
    public String getClave() { return clave; }
    public String getNombre() { return nombre; }
    public int getTotalAlumnos() { return totalAlumnos; }
}