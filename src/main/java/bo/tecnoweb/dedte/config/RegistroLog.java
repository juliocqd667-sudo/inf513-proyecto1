package bo.tecnoweb.dedte.config;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Auditoría local en archivos access.log y error.log (UTF-8) según requisitos INF513.
 */
public final class RegistroLog {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RegistroLog() {}

    public static synchronized void access(String remitente, String asunto, String estado, String detalle) {
        String linea = String.format("[%s] [ACCESS] FROM=<%s> | SUBJECT=\"%s\" | STATUS=%s | DETAIL=%s",
                LocalDateTime.now().format(FMT),
                limpiar(remitente),
                limpiar(asunto),
                limpiar(estado),
                limpiar(detalle));
        System.out.println(linea);
        escribir(Configuracion.getAccessLogPath(), linea);
    }

    public static synchronized void error(String contexto, String mensaje, Throwable ex) {
        String trace = (ex != null) ? (" | EX=" + ex.getClass().getSimpleName() + ": " + ex.getMessage()) : "";
        String linea = String.format("[%s] [ERROR] CTX=%s | MSG=%s%s",
                LocalDateTime.now().format(FMT),
                limpiar(contexto),
                limpiar(mensaje),
                limpiar(trace));
        System.err.println(linea);
        escribir(Configuracion.getErrorLogPath(), linea);
    }

    private static void escribir(String ruta, String linea) {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(ruta, true), StandardCharsets.UTF_8))) {
            bw.write(linea);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("[LOG] No se pudo escribir en " + ruta + ": " + e.getMessage());
        }
    }

    private static String limpiar(String s) {
        if (s == null) return "";
        return s.replace("\r", " ").replace("\n", " ").trim();
    }
}
