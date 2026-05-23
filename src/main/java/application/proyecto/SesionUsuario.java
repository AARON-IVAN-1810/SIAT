package application.proyecto.utils;

public class SesionUsuario {

    private static int idUsuario;
    private static int idMaestro;
    private static String usuario;
    private static String nombreCompleto;
    private static String rol;

    public static void iniciarSesion(int idUsuario, int idMaestro, String usuario, String nombreCompleto, String rol) {
        SesionUsuario.idUsuario = idUsuario;
        SesionUsuario.idMaestro = idMaestro;
        SesionUsuario.usuario = usuario;
        SesionUsuario.nombreCompleto = nombreCompleto;
        SesionUsuario.rol = rol;
    }

    public static int getIdUsuario() {
        return idUsuario;
    }

    public static int getIdMaestro() {
        return idMaestro;
    }

    public static String getUsuario() {
        return usuario;
    }

    public static String getNombreCompleto() {
        return nombreCompleto;
    }

    public static String getRol() {
        return rol;
    }

    public static void cerrarSesion() {
        idUsuario = 0;
        idMaestro = 0;
        usuario = null;
        nombreCompleto = null;
        rol = null;
    }
}