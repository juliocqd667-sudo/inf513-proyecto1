package bo.tecnoweb.dedte.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Carga la configuración desde config.properties (con respaldo de variables de entorno).
 */
public final class Configuracion {

    private static final Properties PROPS = new Properties();
    private static boolean cargada = false;

    private Configuracion() {}

    public static synchronized void cargar() {
        if (cargada) {
            return;
        }
        Path archivo = Path.of("config.properties");
        if (Files.exists(archivo)) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(archivo.toFile()), StandardCharsets.UTF_8)) {
                PROPS.load(reader);
                System.out.println("[CONFIG] Cargado config.properties desde: " + archivo.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("[CONFIG] Error leyendo config.properties: " + e.getMessage());
            }
        } else {
            System.out.println("[CONFIG] No se encontró config.properties, usando valores por defecto.");
        }
        cargada = true;
    }

    private static String get(String key, String envKey, String defaultVal) {
        cargar();
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String val = PROPS.getProperty(key);
        return (val != null && !val.isBlank()) ? val.trim() : defaultVal;
    }

    private static int getInt(String key, String envKey, int defaultVal) {
        try {
            return Integer.parseInt(get(key, envKey, String.valueOf(defaultVal)));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // --- Base de Datos ---
    public static String getDbHost()     { return get("db.host", "DEDTE_DB_HOST", "mail.tecnoweb.org.bo"); }
    public static int    getDbPort()     { return getInt("db.port", "DEDTE_DB_PORT", 5432); }
    public static String getDbName()     { return get("db.name", "DEDTE_DB_NAME", "db_grupo11sa"); }
    public static String getDbUser()     { return get("db.user", "DEDTE_DB_USER", "grupo11sa"); }
    public static String getDbPassword() { return get("db.password", "DEDTE_DB_PASSWORD", "grup11grup11*"); }

    public static String getJdbcUrl() {
        return "jdbc:postgresql://" + getDbHost() + ":" + getDbPort() + "/" + getDbName();
    }

    // --- Correo POP3 / SMTP ---
    public static String getMailHost()     { return get("mail.host", "DEDTE_MAIL_HOST", "mail.tecnoweb.org.bo"); }
    public static int    getPop3Port()     { return getInt("mail.pop3.port", "DEDTE_POP3_PORT", 110); }
    public static int    getSmtpPort()     { return getInt("mail.smtp.port", "DEDTE_SMTP_PORT", 25); }
    public static String getMailUser()     { return get("mail.user", "DEDTE_MAIL_USER", "grupo11sa"); }
    public static String getMailPassword() { return get("mail.password", "DEDTE_MAIL_PASSWORD", "grup11grup11*"); }
    public static String getMailAddress()  { return get("mail.address", "DEDTE_MAIL_ADDRESS", "grupo11sa@tecnoweb.org.bo"); }

    // --- Daemon y Logs ---
    public static long   getDaemonIntervalMs() { return getInt("daemon.interval.ms", "DEDTE_INTERVAL_MS", 5000); }
    public static String getAccessLogPath()    { return get("log.access", "DEDTE_ACCESS_LOG", "access.log"); }
    public static String getErrorLogPath()     { return get("log.error", "DEDTE_ERROR_LOG", "error.log"); }
}
