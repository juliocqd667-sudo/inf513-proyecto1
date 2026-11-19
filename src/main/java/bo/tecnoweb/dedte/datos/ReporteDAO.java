package bo.tecnoweb.dedte.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CU6 (Dashboard Directivo) y CU8 (Reportes y Estadísticas) en PostgreSQL.
 */
public class ReporteDAO {

    /**
     * CU6: Resumen ejecutivo de KPIs en tiempo real (global o por gestión).
     */
    public List<String[]> dashboardDirectivo(String gestionFiltro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean todas = (gestionFiltro == null || gestionFiltro.equals("*") || gestionFiltro.isBlank());
        String where = todas ? "" : " WHERE UPPER(a.gestion) = UPPER(?) ";

        String sql = "SELECT "
                + "COUNT(DISTINCT a.id) AS total_aulas, "
                + "COALESCE(SUM(m.inscritos), 0) AS total_inscritos, "
                + "COALESCE(SUM(m.activos), 0) AS total_activos, "
                + "COALESCE(ROUND(AVG(m.promedio_notas), 2), 0) AS promedio_global, "
                + "COALESCE(ROUND(AVG(r.tasa_desercion), 2), 0) AS desercion_media, "
                + "COUNT(CASE WHEN r.semaforo = 'VERDE' THEN 1 END) AS sem_verde, "
                + "COUNT(CASE WHEN r.semaforo = 'AMARILLO' THEN 1 END) AS sem_amarillo, "
                + "COUNT(CASE WHEN r.semaforo = 'ROJO' THEN 1 END) AS sem_rojo "
                + "FROM aulas_apertura a "
                + "LEFT JOIN monitoreo_academico m ON m.aula_id = a.id "
                + "LEFT JOIN evaluacion_riesgo r ON r.aula_id = a.id "
                + where;

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todas) {
                ps.setString(1, gestionFiltro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    filas.add(new String[]{"Total Aulas Monitoreadas", String.valueOf(rs.getInt("total_aulas"))});
                    filas.add(new String[]{"Total Estudiantes Inscritos", String.valueOf(rs.getInt("total_inscritos"))});
                    filas.add(new String[]{"Total Estudiantes Activos", String.valueOf(rs.getInt("total_activos"))});
                    filas.add(new String[]{"Promedio Académico Global", String.format("%.2f / 100", rs.getDouble("promedio_global"))});
                    filas.add(new String[]{"Tasa Media de Deserción", String.format("%.2f%%", rs.getDouble("desercion_media"))});
                    filas.add(new String[]{"Aulas en Semáforo VERDE", String.valueOf(rs.getInt("sem_verde"))});
                    filas.add(new String[]{"Aulas en Semáforo AMARILLO", String.valueOf(rs.getInt("sem_amarillo"))});
                    filas.add(new String[]{"Aulas en Semáforo ROJO (Crítico)", String.valueOf(rs.getInt("sem_rojo"))});
                }
            }
        }
        return filas;
    }

    /**
     * CU8: Reporte estadístico consolidado por Facultad.
     */
    public List<String[]> reportePorFacultad(String filtroFacultad) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean todas = (filtroFacultad == null || filtroFacultad.equals("*") || filtroFacultad.isBlank());
        String where = todas ? "" : " WHERE UPPER(a.facultad) = UPPER(?) ";

        String sql = "SELECT a.facultad, "
                + "COUNT(DISTINCT a.id) AS aulas, "
                + "COALESCE(SUM(m.inscritos), 0) AS inscritos, "
                + "COALESCE(ROUND(AVG(m.promedio_notas), 2), 0) AS promedio, "
                + "COALESCE(ROUND(AVG(r.tasa_desercion), 2), 0) AS desercion, "
                + "COUNT(CASE WHEN r.semaforo = 'ROJO' THEN 1 END) AS aulas_rojas "
                + "FROM aulas_apertura a "
                + "LEFT JOIN monitoreo_academico m ON m.aula_id = a.id "
                + "LEFT JOIN evaluacion_riesgo r ON r.aula_id = a.id "
                + where
                + "GROUP BY a.facultad ORDER BY aulas DESC, a.facultad ASC";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todas) {
                ps.setString(1, filtroFacultad);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                            rs.getString("facultad"),
                            String.valueOf(rs.getInt("aulas")),
                            String.valueOf(rs.getInt("inscritos")),
                            String.format("%.2f", rs.getDouble("promedio")),
                            String.format("%.2f%%", rs.getDouble("desercion")),
                            String.valueOf(rs.getInt("aulas_rojas"))
                    });
                }
            }
        }
        return filas;
    }

    /**
     * CU8: Reporte estadístico consolidado por Entorno Moodle (Virtual, Presencial, Capacitación).
     */
    public List<String[]> reportePorEntorno(String filtroEntorno) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean todos = (filtroEntorno == null || filtroEntorno.equals("*") || filtroEntorno.isBlank());
        String where = todos ? "" : " WHERE UPPER(e.tipo) = UPPER(?) OR UPPER(e.codigo) = UPPER(?) ";

        String sql = "SELECT e.codigo, e.nombre, e.tipo, e.estado_conexion, "
                + "COUNT(DISTINCT a.id) AS aulas, "
                + "COALESCE(SUM(m.inscritos), 0) AS inscritos, "
                + "COALESCE(ROUND(AVG(m.promedio_notas), 2), 0) AS promedio "
                + "FROM entornos_moodle e "
                + "LEFT JOIN aulas_apertura a ON a.entorno_id = e.id "
                + "LEFT JOIN monitoreo_academico m ON m.aula_id = a.id "
                + where
                + "GROUP BY e.codigo, e.nombre, e.tipo, e.estado_conexion ORDER BY e.codigo ASC";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, filtroEntorno);
                ps.setString(2, filtroEntorno);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getString("tipo"),
                            rs.getString("estado_conexion"),
                            String.valueOf(rs.getInt("aulas")),
                            String.valueOf(rs.getInt("inscritos")),
                            String.format("%.2f", rs.getDouble("promedio"))
                    });
                }
            }
        }
        return filas;
    }
}
