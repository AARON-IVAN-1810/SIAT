package application.proyecto.models;

public class Alerta {
    private String numControl;
    private String nombreAlumno;
    private String materia;
    private String tipo;

    public Alerta(String numControl, String nombreAlumno, String materia, String tipo) {
        this.numControl = numControl;
        this.nombreAlumno = nombreAlumno;
        this.materia = materia;
        this.tipo = tipo;
    }


    public String getNumControl() { 
        return numControl; 
    }
    
    public String getNombreAlumno() { 
        return nombreAlumno; 
    }
    
    public String getMateria() { 
        return materia; 
    }
    
    public String getTipo() { 
        return tipo; 
    }
}