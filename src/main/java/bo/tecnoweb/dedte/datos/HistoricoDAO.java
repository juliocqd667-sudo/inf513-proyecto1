package bo.tecnoweb.dedte.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CU7: Acceso a datos para historicos_semestrales (Históricos y Comparativas).
 */
public class HistoricoDAO {

    public int insertar(String gestion, int entornoId, String facultad, int totalAulas,
                        int totalInscritos, double tasaAprobacion, double tasaDesercion,
                        double promedioGeneral) throws SQLException {
        String sql = "INSERT INTO historicos_semestrales "
                + "(gestion, entorno_id, facultad, total_aulas, total_inscritos, tasa_aprobacion, tasa_desercion, promedio_general) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gestion);
            ps.setInt(2, entornoId);
            ps.setString(3, facultad.toUpperCase());
            ps.setInt(4, totalAulas);
            ps.setInt(5, totalInscritos);
            ps.setDouble(6, tasaAprobacion);
            ps.setDouble(7, tasaDesercion);
            ps.setDouble(8, promedioGeneral);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public List<String[]> listar(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean todos = (filtro == null || filtro.equals("*") || filtro.isBlank());
        String base = "SELECT h.id, h.gestion, e.codigo AS entorno, h.facultad, h.total_aulas, "
                + "h.total_inscritos, h.tasa_aprobacion, h.tasa_desercion, h.promedio_general "
                + "FROM historicos_semestrales h JOIN entornos_moodle e ON e.id = h.entorno_id ";
        String sql = todos
                ? base + "ORDER BY h.gestion DESC, h.facultad ASC"
                : base + "WHERE UPPER(h.gestion) = UPPER(?) OR UPPER(h.facultad) = UPPER(?) OR UPPER(e.codigo) = UPPER(?) "
                  + "ORDER BY h.gestion DESC, h.facultad ASC";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, filtro);
                ps.setString(2, filtro);
                ps.setString(3, filtro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("gestion"),
                            rs.getString("entorno"),
                            rs.getString("facultad"),
                            String.valueOf(rs.getInt("total_aulas")),
                            String.valueOf(rs.getInt("total_inscritos")),
                            String.format("%.2f%%", rs.getDouble("tasa_aprobacion")),
                            String.format("%.2f%%", rs.getDouble("tasa_desercion")),
                            String.format("%.2f", rs.getDouble("promedio_general"))
                    });
                }
            }
        }
        return filas;
    }

    public List<String[]> compararGestiones(String gestionA, String gestionB) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        String sql = "SELECT h.gestion, e.codigo AS entorno, h.facultad, h.total_aulas, h.total_inscritos, "
                + "h.tasa_aprobacion, h.tasa_desercion, h.promedio_general "
                + "FROM historicos_semestrales h JOIN entornos_moodle e ON e.id = h.entorno_id "
                + "WHERE UPPER(h.gestion) IN (UPPER(?), UPPER(?)) "
                + "ORDER BY h.facultad ASC, h.gestion ASC";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gestionA);
            ps.setString(2, gestionB);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                            rs.getString("gestion"),
                            rs.getString("entorno"),
                            rs.getString("facultad"),
                            String.valueOf(rs.getInt("total_aulas")),
                            String.valueOf(rs.getInt("total_inscritos")),
                            String.format("%.2f%%", rs.getDouble("tasa_aprobacion")),
                            String.format("%.2f%%", rs.getDouble("tasa_desercion")),
                            String.format("%.2f", rs.getDouble("promedio_general"))
                    });
                }
            }
        }
        return filas;
    }
}
