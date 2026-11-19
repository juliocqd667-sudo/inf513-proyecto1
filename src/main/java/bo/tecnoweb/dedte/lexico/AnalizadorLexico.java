package bo.tecnoweb.dedte.lexico;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador Léxico y Sintáctico para el formato oficial INF513:
 *   COMANDO["param1","param2",...]
 *
 * Soporta:
 *  - Espacios en blanco adicionales alrededor del comando, corchetes y comas.
 *  - Caracteres UTF-8 (tildes, ñ, símbolos).
 *  - Comillas dobles escapadas (\") y barras invertidas (\\) dentro de parámetros.
 *  - Comando HELP con o sin corchetes.
 */
public class AnalizadorLexico {

    public Comando analizar(String subjectRaw) {
        if (subjectRaw == null || subjectRaw.trim().isEmpty()) {
            return Comando.error("El asunto del correo está vacío. Envíe HELP[\"*\"] para ver los comandos disponibles.");
        }

        // Limpiar prefijos típicos de respuesta (Re:, Fwd:) si el cliente respondió un hilo
        String s = subjectRaw.trim();
        while (s.toUpperCase().startsWith("RE:") || s.toUpperCase().startsWith("FWD:") || s.toUpperCase().startsWith("RV:")) {
            int idxDosPuntos = s.indexOf(':');
            s = s.substring(idxDosPuntos + 1).trim();
        }

        if (s.isEmpty()) {
            return Comando.error("El asunto no contiene ningún comando válido. Envíe HELP[\"*\"] para ayuda.");
        }

        // Atajo amigable para HELP sin corchetes
        if (s.equalsIgnoreCase("HELP") || s.equalsIgnoreCase("AYUDA")) {
            return new Comando("HELP", List.of("*"));
        }

        int n = s.length();
        int i = 0;

        // 1. Leer identificador de comando: letras, dígitos, guion o guion bajo
        int inicioCmd = i;
        while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_' || s.charAt(i) == '-')) {
            i++;
        }

        if (i == inicioCmd) {
            return Comando.error("No se reconoció un nombre de comando válido al inicio del asunto. Formato esperado: COMANDO[\"param1\",\"param2\"]");
        }

        String nombreComando = s.substring(inicioCmd, i).toUpperCase();

        // Saltar espacios antes de '['
        while (i < n && Character.isWhitespace(s.charAt(i))) {
            i++;
        }

        if (i >= n || s.charAt(i) != '[') {
            return Comando.error("Sintaxis inválida para el comando '" + nombreComando
                    + "': falta el corchete de apertura '['. Ejemplo: " + nombreComando + "[\"*\"]");
        }
        i++; // consumir '['

        // Saltar espacios dentro de '['
        while (i < n && Character.isWhitespace(s.charAt(i))) {
            i++;
        }

        List<String> parametros = new ArrayList<>();

        // Permitir lista vacía solo si cierra inmediatamente: COMANDO[]
        if (i < n && s.charAt(i) == ']') {
            i++;
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            if (i < n) {
                return Comando.error("Caracteres inesperados después del corchete de cierre ']': '" + s.substring(i) + "'");
            }
            return new Comando(nombreComando, parametros);
        }

        // 2. Leer lista de parámetros entre comillas dobles separados por comas
        while (true) {
            while (i < n && Character.isWhitespace(s.charAt(i))) {
                i++;
            }

            if (i >= n) {
                return Comando.error("Sintaxis incompleta: falta el corchete de cierre ']'.");
            }

            if (s.charAt(i) != '"') {
                return Comando.error("Cada parámetro debe estar encerrado entre comillas dobles (\"). "
                        + "Se encontró '" + s.charAt(i) + "' en la posición " + (i + 1) + ".");
            }
            i++; // consumir comilla de apertura '"'

            StringBuilder valor = new StringBuilder();
            boolean cerrado = false;
            while (i < n) {
                char c = s.charAt(i);
                if (c == '\\') {
                    if (i + 1 >= n) {
                        return Comando.error("Secuencia de escape incompleta al final del parámetro.");
                    }
                    char siguiente = s.charAt(i + 1);
                    if (siguiente == '"' || siguiente == '\\') {
                        valor.append(siguiente);
                        i += 2;
                    } else {
                        valor.append(c);
                        i++;
                    }
                } else if (c == '"') {
                    cerrado = true;
                    i++; // consumir comilla de cierre
                    break;
                } else {
                    valor.append(c);
                    i++;
                }
            }

            if (!cerrado) {
                return Comando.error("Comilla doble sin cerrar en el parámetro #" + (parametros.size() + 1) + ".");
            }

            parametros.add(valor.toString().trim());

            // Saltar espacios después de la comilla de cierre
            while (i < n && Character.isWhitespace(s.charAt(i))) {
                i++;
            }

            if (i >= n) {
                return Comando.error("Falta el corchete de cierre ']' al final del comando.");
            }

            char separador = s.charAt(i);
            if (separador == ',') {
                i++; // consumir ',' y continuar con el siguiente parámetro
            } else if (separador == ']') {
                i++; // consumir ']' y terminar
                break;
            } else {
                return Comando.error("Se esperaba ',' o ']' después del parámetro #" + parametros.size()
                        + ", pero se encontró '" + separador + "'.");
            }
        }

        // Verificar que no haya texto sobrante fuera de los corchetes
        while (i < n && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        if (i < n) {
            return Comando.error("Texto inesperado después del cierre ']': '" + s.substring(i) + "'.");
        }

        return new Comando(nombreComando, parametros);
    }
}
