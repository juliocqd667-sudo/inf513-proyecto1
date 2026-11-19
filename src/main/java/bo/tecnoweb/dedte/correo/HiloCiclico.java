package bo.tecnoweb.dedte.correo;

import bo.tecnoweb.dedte.config.Configuracion;
import bo.tecnoweb.dedte.config.RegistroLog;
import bo.tecnoweb.dedte.negocio.DespachadorComandos;

import java.util.List;

/**
 * Demonio (hilo cíclico) que sondea periódicamente la casilla POP3,
 * procesa cada correo entrante y envía la respuesta HTML por SMTP.
 */
public class HiloCiclico extends Thread {

    private final LectorCorreo lector = new LectorCorreo();
    private final EmisorCorreo emisor = new EmisorCorreo();
    private final DespachadorComandos despachador = new DespachadorComandos();
    private volatile boolean activo = true;

    public void detener() {
        this.activo = false;
        this.interrupt();
    }

    @Override
    public void run() {
        long intervalo = Configuracion.getDaemonIntervalMs();
        System.out.println("[DAEMON] Escuchando buzón " + Configuracion.getMailAddress()
                + " cada " + (intervalo / 1000.0) + " segundos...");

        while (activo) {
            try {
                List<LectorCorreo.Correo> correos = lector.leerCorreos();
                for (LectorCorreo.Correo correo : correos) {
                    procesarCorreo(correo);
                }
                Thread.sleep(intervalo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                RegistroLog.error("HiloCiclico", "Error inesperado en ciclo principal", e);
            }
        }
        System.out.println("[DAEMON] Servicio detenido.");
    }

    private void procesarCorreo(LectorCorreo.Correo correo) {
        DespachadorComandos.Resultado resultado = despachador.procesar(correo.remitente, correo.subject);
        boolean enviado = emisor.enviarRespuesta(correo.remitente, resultado.asuntoRespuesta, resultado.cuerpoHtml);

        String estado = (resultado.exito ? "OK" : "ERROR") + (enviado ? "_SENT" : "_SMTP_FAIL");
        RegistroLog.access(correo.remitente, correo.subject, estado, resultado.resumenLog);
    }
}
