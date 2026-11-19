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

        try (Socket socket = new Socket(host, puerto)) {
            socket.setSoTimeout(15000);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            entrada.readLine(); // 220 banner

            enviar(salida, entrada, "HELO " + host);
            enviar(salida, entrada, "MAIL FROM:<" + origen + ">");
            enviar(salida, entrada, "RCPT TO:<" + destinatario + ">");
            enviar(salida, entrada, "DATA");

            salida.print("From: DEDTE Analytics UAGRM <" + origen + ">\r\n");
            salida.print("To: <" + destinatario + ">\r\n");
            salida.print("Subject: " + codificarAsuntoUtf8(asunto) + "\r\n");
            salida.print("MIME-Version: 1.0\r\n");
            salida.print("Content-Type: text/html; charset=UTF-8\r\n");
            salida.print("Content-Transfer-Encoding: 8bit\r\n");
            salida.print("\r\n");

            // Dot-stuffing según RFC 5321
            String htmlNorm = cuerpoHTML.replace("\r\n", "\n").replace("\r", "\n");
            for (String linea : htmlNorm.split("\n", -1)) {
                if (linea.startsWith(".")) {
                    salida.print("." + linea + "\r\n");
                } else {
                    salida.print(linea + "\r\n");
                }
            }

            salida.print(".\r\n");
            salida.flush();
            String respData = entrada.readLine();

            enviar(salida, entrada, "QUIT");

            return respData != null && respData.startsWith("250");
        } catch (Exception e) {
            RegistroLog.error("EmisorCorreo", "Error enviando SMTP a " + destinatario, e);
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
