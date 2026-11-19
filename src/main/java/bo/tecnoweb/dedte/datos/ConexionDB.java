package bo.tecnoweb.dedte.datos;

import bo.tecnoweb.dedte.config.Configuracion;
import bo.tecnoweb.dedte.config.RegistroLog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona la conexión JDBC hacia PostgreSQL (db_grupo11sa en tecnoweb.org.bo).
 */
public final class ConexionDB {

    private ConexionDB() {}

    public static Connection obtenerConexion() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(
                    Configuracion.getJdbcUrl(),
                    Configuracion.getDbUser(),
                    Configuracion.getDbPassword()
            );
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC de PostgreSQL no encontrado: " + e.getMessage(), e);
        }
    }

    public static boolean probarConexion() {
        try (Connection conn = obtenerConexion()) {
            System.out.println("[DB] Conexión exitosa a " + Configuracion.getJdbcUrl()
                    + " como usuario '" + Configuracion.getDbUser() + "'");
            return true;
        } catch (SQLException e) {
            RegistroLog.error("ConexionDB", "No se pudo conectar a " + Configuracion.getJdbcUrl(), e);
            return false;
        }
    }
}
