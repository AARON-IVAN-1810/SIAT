package application.proyecto.utils;

public class SesionOrientacion {

    private static int idAlerta;
    private static int idAlumno;

    private static String numControl;
    private static String nombreAlumno;
    private static String grupo;
    private static String semestre;
    private static String turno;
    private static String materia;

    private static String tipoAlerta;
    private static String prioridad;
    private static String motivoDetalle;
    private static String fechaAlerta;

    private static int idReporteDocente;

    // ===== GETTERS =====
    public static int getIdReporteDocente() {
        return idReporteDocente;
    }


    public static int getIdAlerta() {
        return idAlerta;
    }

    public static int getIdAlumno() {
        return idAlumno;
    }

    public static String getNumControl() {
        return numControl;
    }

    public static String getNombreAlumno() {
        return nombreAlumno;
    }

    public static String getGrupo() {
        return grupo;
    }

    public static String getSemestre() {
        return semestre;
    }

    public static String getTurno() {
        return turno;
    }

    public static String getMateria() {
        return materia;
    }

    public static String getTipoAlerta() {
        return tipoAlerta;
    }

    public static String getPrioridad() {
        return prioridad;
    }

    public static String getMotivoDetalle() {
        return motivoDetalle;
    }

    public static String getFechaAlerta() {
        return fechaAlerta;
    }

    // ===== SETTERS =====
    public static void setIdReporteDocente(int idReporteDocente) {
        SesionOrientacion.idReporteDocente = idReporteDocente;
    }


    public static void setIdAlerta(int idAlerta) {
        SesionOrientacion.idAlerta = idAlerta;
    }

    public static void setIdAlumno(int idAlumno) {
        SesionOrientacion.idAlumno = idAlumno;
    }

    public static void setNumControl(String numControl) {
        SesionOrientacion.numControl = numControl;
    }

    public static void setNombreAlumno(String nombreAlumno) {
        SesionOrientacion.nombreAlumno = nombreAlumno;
    }

    public static void setGrupo(String grupo) {
        SesionOrientacion.grupo = grupo;
    }

    public static void setSemestre(String semestre) {
        SesionOrientacion.semestre = semestre;
    }

    public static void setTurno(String turno) {
        SesionOrientacion.turno = turno;
    }

    public static void setMateria(String materia) {
        SesionOrientacion.materia = materia;
    }

    public static void setTipoAlerta(String tipoAlerta) {
        SesionOrientacion.tipoAlerta = tipoAlerta;
    }

    public static void setPrioridad(String prioridad) {
        SesionOrientacion.prioridad = prioridad;
    }

    public static void setMotivoDetalle(String motivoDetalle) {
        SesionOrientacion.motivoDetalle = motivoDetalle;
    }

    public static void setFechaAlerta(String fechaAlerta) {
        SesionOrientacion.fechaAlerta = fechaAlerta;
    }

    // ===== LIMPIAR SESION =====

    public static void limpiar() {
        idReporteDocente = 0;

        idAlerta = 0;
        idAlumno = 0;

        numControl = null;
        nombreAlumno = null;
        grupo = null;
        semestre = null;
        turno = null;
        materia = null;

        tipoAlerta = null;
        prioridad = null;
        motivoDetalle = null;
        fechaAlerta = null;
    }
}