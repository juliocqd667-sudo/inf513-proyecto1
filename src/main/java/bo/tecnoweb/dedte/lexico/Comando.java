package bo.tecnoweb.dedte.lexico;

import java.util.Collections;
import java.util.List;

/**
 * Representa el resultado del análisis léxico y sintáctico de un Subject de correo.
 */
public class Comando {

    private final String nombre;
    private final List<String> parametros;
    private final String error;

    public Comando(String nombre, List<String> parametros) {
        this.nombre = nombre;
        this.parametros = Collections.unmodifiableList(parametros);
        this.error = null;
    }

    private Comando(String error) {
        this.nombre = "INVALIDO";
        this.parametros = Collections.emptyList();
        this.error = error;
    }

    public static Comando error(String mensaje) {
        return new Comando(mensaje);
    }

    public boolean esValido() {
        return error == null;
    }

    public String getNombre() {
        return nombre;
    }

    public List<String> getParametros() {
        return parametros;
    }

    public String getParam(int index) {
        return parametros.get(index);
    }

    public int cantidadParametros() {
        return parametros.size();
    }

    public String getError() {
        return error;
    }
}
