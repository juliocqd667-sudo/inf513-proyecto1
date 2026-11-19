package bo.tecnoweb.dedte;

import bo.tecnoweb.dedte.config.Configuracion;
import bo.tecnoweb.dedte.correo.HiloCiclico;
import bo.tecnoweb.dedte.datos.ConexionDB;
import bo.tecnoweb.dedte.lexico.AnalizadorLexico;
import bo.tecnoweb.dedte.lexico.Comando;
import bo.tecnoweb.dedte.negocio.DespachadorComandos;

/**
 * Punto de entrada del Sistema Email-Driven DEDTE-UAGRM (INF513 - Grupo #11 SA).
 *
 * Modos de ejecución:
 *   java -jar dedte-mail-system-1.0.0-jar-with-dependencies.jar
 *       -> Inicia el demonio POP3/SMTP conectado a PostgreSQL.
 *   java -jar ... --test-db
 *       -> Verifica únicamente la conexión a PostgreSQL.
 *   java -jar ... --simular "HELP[\"*\"]"
 *       -> Simula la ejecución de un comando localmente sin usar POP3/SMTP.
 *   java -jar ... --parse "INSUSU[\"123\",\"Juan\",\"Perez\",\"DOCENTE\",\"a@b.com\",\"700\"]"
 *       -> Verifica el analizador léxico sin requerir conexión a BD.
 */
public class App {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("  UAGRM · FICCT · INF513 Tecnología Web (Gestión 2026-2)");
        System.out.println("  Proyecto #1: Sistema de Analítica DEDTE vía Correo (G11)");
        System.out.println("============================================================");

        Configuracion.cargar();

        if (args.length > 0) {
            if ("--test-db".equalsIgnoreCase(args[0])) {
                boolean ok = ConexionDB.probarConexion();
                System.exit(ok ? 0 : 1);
            }
            if ("--parse".equalsIgnoreCase(args[0]) && args.length > 1) {
                AnalizadorLexico lex = new AnalizadorLexico();
                Comando cmd = lex.analizar(args[1]);
                if (cmd.esValido()) {
                    System.out.println("[PARSE OK] Comando=" + cmd.getNombre() + " | Params=" + cmd.getParametros());
                } else {
                    System.out.println("[PARSE ERROR] " + cmd.getError());
                }
                return;
            }
            if ("--simular".equalsIgnoreCase(args[0]) && args.length > 1) {
                DespachadorComandos despachador = new DespachadorComandos();
                DespachadorComandos.Resultado res = despachador.procesar("simulacion@uagrm.edu.bo", args[1]);
                System.out.println("Exito  : " + res.exito);
                System.out.println("Asunto : " + res.asuntoRespuesta);
                System.out.println("Log    : " + res.resumenLog);
                return;
            }
        }

        if (!ConexionDB.probarConexion()) {
            System.err.println("[ERROR] No se pudo conectar a PostgreSQL. Revise config.properties y su IP habilitada.");
            System.exit(1);
        }

        HiloCiclico daemon = new HiloCiclico();
        Runtime.getRuntime().addShutdownHook(new Thread(daemon::detener));
        daemon.start();
    }
}
