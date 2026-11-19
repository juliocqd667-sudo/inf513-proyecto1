package bo.tecnoweb.dedte.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CU4: Acceso a datos para monitoreo_academico (Matrícula, Densidad y Rendimiento).
 */
public class MonitoreoDAO {

    public int insertar(int aulaId, int inscritos, int activos, int aprobados, int reprobados,
                        double promedioNotas, double densidadPct) throws SQLException {
        String sql = "INSERT INTO monitoreo_academico "
                + "(aula_id, inscritos, activos, aprobados, reprobados, promedio_notas, densidad_pct) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, aulaId);
            ps.setInt(2, inscritos);
            ps.setInt(3, activos);
            ps.setInt(4, aprobados);
            ps.setInt(5, reprobados);
            ps.setDouble(6, promedioNotas);
            ps.setDouble(7, densidadPct);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public boolean actualizar(int id, int inscritos, int activos, int aprobados, int reprobados,
                              double promedioNotas, double densidadPct) throws SQLException {
        String sql = "UPDATE monitoreo_academico SET inscritos = ?, activos = ?, aprobados = ?, reprobados = ?, "
                + "promedio_notas = ?, densidad_pct = ?, fecha_corte = CURRENT_DATE WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inscritos);
            ps.setInt(2, activos);
            ps.setInt(3, aprobados);
            ps.setInt(4, reprobados);
            ps.setDouble(5, promedioNotas);
            ps.setDouble(6, densidadPct);
            ps.setInt(7, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM monitoreo_academico WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<String[]> listar(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean todos = (filtro == null || filtro.equals("*") || filtro.isBlank());
        String base = "SELECT m.id, a.sigla || '-' || a.grupo AS curso, a.facultad, m.inscritos, m.activos, "
                + "m.aprobados, m.reprobados, m.promedio_notas, m.densidad_pct, m.fecha_corte "
                + "FROM monitoreo_academico m JOIN aulas_apertura a ON a.id = m.aula_id ";
        String sql = todos
                ? base + "ORDER BY m.id ASC"
                : base + "WHERE UPPER(a.facultad) = UPPER(?) OR UPPER(a.sigla) = UPPER(?) OR UPPER(a.gestion) = UPPER(?) ORDER BY m.id ASC";

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
                            rs.getString("curso"),
                            rs.getString("facultad"),
                            String.valueOf(rs.getInt("inscritos")),
                            String.valueOf(rs.getInt("activos")),
                            String.valueOf(rs.getInt("aprobados")),
                            String.valueOf(rs.getInt("reprobados")),
                            String.format("%.2f", rs.getDouble("promedio_notas")),
                            String.format("%.1f%%", rs.getDouble("densidad_pct")),
                            String.valueOf(rs.getDate("fecha_corte"))
                    });
                }
            }
        }
        return filas;
    }
}
