package bo.tecnoweb.dedte.correo;

import bo.tecnoweb.dedte.config.Configuracion;
import bo.tecnoweb.dedte.config.RegistroLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cliente SMTP por socket TCP (puerto 25) con soporte UTF-8 y cuerpo HTML.
 */
public class EmisorCorreo {

    public boolean enviarRespuesta(String destinatario, String asunto, String cuerpoHTML) {
        String host = Configuracion.getMailHost();
        int puerto = Configuracion.getSmtpPort();
        String origen = Configuracion.getMailAddress();
        String destLimpio = destinatario.replace("<", "").replace(">", "").trim();

        try (Socket socket = new Socket(host, puerto)) {
            socket.setSoTimeout(20000);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String banner = entrada.readLine(); // 220 banner
            if (banner == null || !banner.startsWith("220")) {
                RegistroLog.error("EmisorCorreo", "Banner SMTP inesperado: " + banner, null);
                return false;
            }

            String rHelo = enviar(salida, entrada, "HELO " + host);
            if (rHelo == null || !rHelo.startsWith("250")) {
                RegistroLog.error("EmisorCorreo", "HELO rechazado: " + rHelo, null);
                return false;
            }

            String rFrom = enviar(salida, entrada, "MAIL FROM:<" + origen + ">");
            if (rFrom == null || !rFrom.startsWith("250")) {
                RegistroLog.error("EmisorCorreo", "MAIL FROM rechazado: " + rFrom, null);
                return false;
            }

            String rRcpt = enviar(salida, entrada, "RCPT TO:<" + destLimpio + ">");
            if (rRcpt == null || !rRcpt.startsWith("250")) {
                RegistroLog.error("EmisorCorreo", "RCPT TO rechazado para " + destLimpio + ": " + rRcpt, null);
                enviar(salida, entrada, "QUIT");
                return false;
            }

            String rData = enviar(salida, entrada, "DATA");
            if (rData == null || !rData.startsWith("354")) {
                RegistroLog.error("EmisorCorreo", "DATA rechazado: " + rData, null);
                enviar(salida, entrada, "QUIT");
                return false;
            }

            salida.print("From: DEDTE Analytics UAGRM <" + origen + ">\r\n");
            salida.print("To: <" + destLimpio + ">\r\n");
            salida.print("Subject: " + codificarAsuntoUtf8(asunto) + "\r\n");
            salida.print("MIME-Version: 1.0\r\n");
            salida.print("Content-Type: text/html; charset=UTF-8\r\n");
            salida.print("Content-Transfer-Encoding: 8bit\r\n");
            salida.print("\r\n");

            // Ajuste de líneas <=200 chars y dot-stuffing según RFC 5321
            String htmlNorm = cuerpoHTML.replace("\r\n", "\n").replace("\r", "\n");
            for (String lineaOrig : htmlNorm.split("\n", -1)) {
                String linea = lineaOrig;
                while (linea.length() > 200) {
                    int corte = 200;
                    for (int j = 200; j > 100; j--) {
                        if (linea.charAt(j) == '>') {
                            corte = j + 1;
                            break;
                        }
                    }
                    String trozo = linea.substring(0, corte);
                    salida.print((trozo.startsWith(".") ? "." : "") + trozo + "\r\n");
                    linea = linea.substring(corte);
                }
                salida.print((linea.startsWith(".") ? "." : "") + linea + "\r\n");
            }

            salida.print(".\r\n");
            salida.flush();
            String respData = entrada.readLine();

            enviar(salida, entrada, "QUIT");

            if (respData == null || !respData.startsWith("250")) {
                RegistroLog.error("EmisorCorreo", "Cuerpo SMTP rechazado: " + respData, null);
                return false;
            }
            return true;
        } catch (Exception e) {
            RegistroLog.error("EmisorCorreo", "Error enviando SMTP a " + destLimpio, e);
            return false;
        }
    }

    private String enviar(PrintWriter salida, BufferedReader entrada, String comando) throws Exception {
        salida.print(comando + "\r\n");
        salida.flush();
        return entrada.readLine();
    }

    private String codificarAsuntoUtf8(String asunto) {
        String base64 = Base64.getEncoder().encodeToString(asunto.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + base64 + "?=";
    }
}
