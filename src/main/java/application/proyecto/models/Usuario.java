package application.proyecto.models;

public class Usuario {
    private int idUsuario;
    private String usuario;
    private String rol;

    public Usuario(int idUsuario, String usuario, String rol) {
        this.idUsuario = idUsuario;
        this.usuario = usuario;
        this.rol = rol;
    }

    // Getters
    public int getIdUsuario() { return idUsuario; }
    public String getUsuario() { return usuario; }
    public String getRol() { return rol; }
}