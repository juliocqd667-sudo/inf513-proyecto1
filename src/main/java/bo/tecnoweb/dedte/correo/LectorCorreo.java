package bo.tecnoweb.dedte.correo;

import bo.tecnoweb.dedte.config.Configuracion;
import bo.tecnoweb.dedte.config.RegistroLog;

import javax.mail.internet.MimeUtility;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente POP3 por socket TCP (puerto 110) con decodificación MIME UTF-8.
 */
public class LectorCorreo {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    public static class Correo {
        public final String remitente;
        public final String subject;

        public Correo(String remitente, String subject) {
            this.remitente = remitente;
            this.subject = subject;
        }
    }

    public List<Correo> leerCorreos() {
        List<Correo> lista = new ArrayList<>();
        String host = Configuracion.getMailHost();
        int puerto = Configuracion.getPop3Port();

        try (Socket socket = new Socket(host, puerto)) {
            socket.setSoTimeout(15000);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());

            // Banner inicial +OK
            entrada.readLine();

            // Autenticación POP3
            enviarComando(salida, "USER " + Configuracion.getMailUser());
            String respUser = entrada.readLine();
            if (respUser == null || !respUser.startsWith("+OK")) {
                RegistroLog.error("LectorCorreo", "POP3 USER rechazado: " + respUser, null);
                return lista;
            }

            enviarComando(salida, "PASS " + Configuracion.getMailPassword());
            String respPass = entrada.readLine();
            if (respPass == null || !respPass.startsWith("+OK")) {
                RegistroLog.error("LectorCorreo", "POP3 PASS rechazado: " + respPass, null);
                return lista;
            }

            // Consultar cantidad de mensajes
            enviarComando(salida, "STAT");
            String respStat = entrada.readLine();
            int total = 0;
            if (respStat != null && respStat.startsWith("+OK")) {
                String[] partes = respStat.trim().split("\\s+");
                if (partes.length >= 2) {
                    total = Integer.parseInt(partes[1]);
                }
            }

            for (int i = 1; i <= total; i++) {
                enviarComando(salida, "RETR " + i);
                String respRetr = entrada.readLine();
                if (respRetr == null || !respRetr.startsWith("+OK")) {
                    continue;
                }

                String remitente = "";
                StringBuilder subjectRaw = new StringBuilder();
                boolean leyendoSubject = false;
                boolean enCabeceras = true;

                String linea;
                while ((linea = entrada.readLine()) != null) {
                    if (linea.equals(".")) {
                        break;
                    }
                    if (enCabeceras) {
                        if (linea.isEmpty()) {
                            enCabeceras = false;
                            leyendoSubject = false;
                            continue;
                        }
                        // Cabecera continuada (folded header)
                        if (leyendoSubject && (linea.startsWith(" ") || linea.startsWith("\t"))) {
                            subjectRaw.append(" ").append(linea.trim());
                            continue;
                        } else {
                            leyendoSubject = false;
                        }

                        String lower = linea.toLowerCase();
                        if (lower.startsWith("from:") && remitente.isEmpty()) {
                            remitente = extraerEmail(linea.substring(5));
                        } else if (lower.startsWith("subject:")) {
                            subjectRaw.setLength(0);
                            subjectRaw.append(linea.substring(8).trim());
                            leyendoSubject = true;
                        }
                    }
                }

                String subjectDecodificado = decodificarMime(subjectRaw.toString());
                if (!remitente.isEmpty()) {
                    lista.add(new Correo(remitente, subjectDecodificado));
                }

                // Marcar como eliminado para no reprocesarlo
                enviarComando(salida, "DELE " + i);
                entrada.readLine();
            }

            enviarComando(salida, "QUIT");
            entrada.readLine();

        } catch (Exception e) {
            RegistroLog.error("LectorCorreo", "Error leyendo correos POP3 en " + host + ":" + puerto, e);
        }

        return lista;
    }

    private void enviarComando(DataOutputStream salida, String cmd) throws Exception {
        salida.writeBytes(cmd + "\r\n");
        salida.flush();
    }

    private String extraerEmail(String texto) {
        Matcher m = EMAIL_PATTERN.matcher(texto);
        if (m.find()) {
            return m.group();
        }
        return texto.trim();
    }

    private String decodificarMime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            return MimeUtility.decodeText(raw).trim();
        } catch (Exception e) {
            return raw.trim();
        }
    }
}
