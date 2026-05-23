package application.proyecto.utils;

public class SesionAlerta {

    private static int idAlerta;
    private static int idAlumno;
    private static int idCarga;
    private static String grupo;
    private static String turno;
    private static String materia;
    private static String alumno;
    private static String numControl;
    private static String motivo;

    public static void limpiar() {
        idAlerta = 0;
        idAlumno = 0;
        idCarga = 0;
        grupo = null;
        turno = null;
        materia = null;
        alumno = null;
        numControl = null;
        motivo = null;
    }

    public static void seleccionarAlerta(int idAlerta, int idAlumno, int idCarga, String grupo, String turno, String materia, String alumno, String numControl, String motivo) {
        SesionAlerta.idAlerta = idAlerta;
        SesionAlerta.idAlumno = idAlumno;
        SesionAlerta.idCarga = idCarga;
        SesionAlerta.grupo = grupo;
        SesionAlerta.turno = turno;
        SesionAlerta.materia = materia;
        SesionAlerta.alumno = alumno;
        SesionAlerta.numControl = numControl;
        SesionAlerta.motivo = motivo;
    }

    public static int getIdAlerta() { return idAlerta; }
    public static int getIdAlumno() { return idAlumno; }
    public static int getIdCarga() { return idCarga; }
    public static String getGrupo() { return grupo; }
    public static String getTurno() { return turno; }
    public static String getMateria() { return materia; }
    public static String getAlumno() { return alumno; }
    public static String getNumControl() { return numControl; }
    public static String getMotivo() { return motivo; }
}